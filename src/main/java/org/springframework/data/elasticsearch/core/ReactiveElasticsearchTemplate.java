/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.elasticsearch.core;

import static org.elasticsearch.index.VersionType.*;

import lombok.NonNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;
import org.elasticsearch.client.Requests;
import org.elasticsearch.index.get.GetResult;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.WrapperQueryBuilder;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient;
import org.springframework.data.elasticsearch.core.EntityOperations.AdaptibleEntity;
import org.springframework.data.elasticsearch.core.EntityOperations.Entity;
import org.springframework.data.elasticsearch.core.EntityOperations.IndexCoordinates;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentProperty;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 4.0
 */
public class ReactiveElasticsearchTemplate implements ReactiveElasticsearchOperations {

	private static final Logger QUERY_LOGGER = LoggerFactory
			.getLogger("org.springframework.data.elasticsearch.core.QUERY");

	private final ReactiveElasticsearchClient client;
	private final ElasticsearchConverter converter;
	private final @NonNull MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> mappingContext;
	private final ResultsMapper resultMapper;
	private final ElasticsearchExceptionTranslator exceptionTranslator;
	private final EntityOperations operations;

	private @Nullable RefreshPolicy refreshPolicy = RefreshPolicy.IMMEDIATE;
	private @Nullable IndicesOptions indicesOptions = IndicesOptions.strictExpandOpenAndForbidClosed();

	public ReactiveElasticsearchTemplate(ReactiveElasticsearchClient client) {
		this(client, new MappingElasticsearchConverter(new SimpleElasticsearchMappingContext()));
	}

	public ReactiveElasticsearchTemplate(ReactiveElasticsearchClient client, ElasticsearchConverter converter) {

		this.client = client;
		this.converter = converter;
		this.mappingContext = converter.getMappingContext();
		this.resultMapper = new DefaultResultMapper(converter.getMappingContext());
		this.exceptionTranslator = new ElasticsearchExceptionTranslator();
		this.operations = new EntityOperations(this.mappingContext);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations#exctute(ClientCallback)
	 */
	@Override
	public <T> Publisher<T> execute(ClientCallback<Publisher<T>> callback) {
		return Flux.defer(() -> callback.doWithClient(getClient())).onErrorMap(this::translateException);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations#index(Object, String, String)
	 */
	@Override
	public <T> Mono<T> save(T entity, @Nullable String index, @Nullable String type) {

		Assert.notNull(entity, "Entity must not be null!");

		AdaptibleEntity<T> adaptableEntity = operations.forEntity(entity, converter.getConversionService());

		return doIndex(entity, adaptableEntity, index, type) //
				.map(it -> {
					return adaptableEntity.populateIdIfNecessary(it.getId());
				});
	}

	private Mono<IndexResponse> doIndex(Object value, AdaptibleEntity<?> entity, @Nullable String index,
			@Nullable String type) {

		return Mono.defer(() -> {

			Object id = entity.getId();

			IndexCoordinates indexCoordinates = operations.determineIndex(entity, index, type);

			IndexRequest request = id != null
					? new IndexRequest(indexCoordinates.getIndexName(), indexCoordinates.getTypeName(), id.toString())
					: new IndexRequest(indexCoordinates.getIndexName(), indexCoordinates.getTypeName());

			try {
				request.source(resultMapper.getEntityMapper().mapToString(value), Requests.INDEX_CONTENT_TYPE);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			if (entity.isVersionedEntity()) {

				Object version = entity.getVersion();
				if (version != null) {
					request.version(((Number) version).longValue());
					request.versionType(EXTERNAL);
				}
			}

			if (entity.hasParent()) {

				Object parentId = entity.getParentId();
				if (parentId != null) {
					request.parent(parentId.toString());
				}
			}

			request = prepareIndexRequest(value, request);
			return doIndex(request);
		});
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations#findById(String, Class, String, String)
	 */
	@Override
	public <T> Mono<T> findById(String id, Class<T> entityType, @Nullable String index, @Nullable String type) {

		Assert.notNull(id, "Id must not be null!");

		return doFindById(id, getPersistentEntity(entityType), index, type)
				.map(it -> resultMapper.mapEntity(it, entityType));
	}

	private Mono<GetResult> doFindById(String id, ElasticsearchPersistentEntity<?> entity, @Nullable String index,
			@Nullable String type) {

		return Mono.defer(() -> {

			IndexCoordinates indexCoordinates = operations.determineIndex(entity, index, type);

			return doFindById(new GetRequest(indexCoordinates.getIndexName(), indexCoordinates.getTypeName(), id));
		});
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations#exists(String, Class, String, String)
	 */
	@Override
	public Mono<Boolean> exists(String id, Class<?> entityType, String index, String type) {

		Assert.notNull(id, "Id must not be null!");

		return doExists(id, getPersistentEntity(entityType), index, type);
	}

	private Mono<Boolean> doExists(String id, ElasticsearchPersistentEntity<?> entity, @Nullable String index,
			@Nullable String type) {

		return Mono.defer(() -> {

			IndexCoordinates indexCoordinates = operations.determineIndex(entity, index, type);

			return doExists(new GetRequest(indexCoordinates.getIndexName(), indexCoordinates.getTypeName(), id));
		});
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations#find(Query, Class, String, String, Class)
	 */
	@Override
	public <T> Flux<T> find(Query query, Class<?> entityType, @Nullable String index, @Nullable String type,
			Class<T> resultType) {

		return doFind(query, getPersistentEntity(entityType), index, type)
				.map(it -> resultMapper.mapEntity(it, resultType));
	}

	private Flux<SearchHit> doFind(Query query, ElasticsearchPersistentEntity<?> entity, @Nullable String index,
			@Nullable String type) {

		return Flux.defer(() -> {

			IndexCoordinates indexCoordinates = operations.determineIndex(entity, index, type);
			SearchRequest request = new SearchRequest(indices(query, indexCoordinates::getIndexName));
			request.types(indexTypes(query, indexCoordinates::getTypeName));

			SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
			searchSourceBuilder.query(mappedQuery(query, entity));

			// TODO: request.source().postFilter(elasticsearchFilter); -- filter query

			searchSourceBuilder.version(entity.hasVersionProperty()); // This has been true by default before
			searchSourceBuilder.trackScores(query.getTrackScores());

			if (query.getSourceFilter() != null) {
				searchSourceBuilder.fetchSource(query.getSourceFilter().getIncludes(), query.getSourceFilter().getExcludes());
			}

			if (query.getPageable().isPaged()) {

				long offset = query.getPageable().getOffset();
				if (offset > Integer.MAX_VALUE) {
					throw new IllegalArgumentException(String.format("Offset must not be more than %s", Integer.MAX_VALUE));
				}

				searchSourceBuilder.from((int) offset);
				searchSourceBuilder.size(query.getPageable().getPageSize());
			}

			if (query.getIndicesOptions() != null) {
				request.indicesOptions(query.getIndicesOptions());
			}

			sort(query, entity).forEach(searchSourceBuilder::sort);

			if (query.getMinScore() > 0) {
				searchSourceBuilder.minScore(query.getMinScore());
			}

			request.source(searchSourceBuilder);

			request = prepareSearchRequest(request);

			return doFind(request);
		});
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations#count(Query, Class, String, String)
	 */
	@Override
	public Mono<Long> count(Query query, Class<?> entityType, String index, String type) {

		// TODO: ES 7.0 has a dedicated CountRequest - use that one once available.
		return find(query, entityType, index, type).count();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations#delete(Object, String, String)
	 */
	@Override
	public Mono<String> delete(Object entity, @Nullable String index, @Nullable String type) {

		Entity<?> elasticsearchEntity = operations.forEntity(entity);

		return Mono.defer(() -> doDeleteById(entity, ObjectUtils.nullSafeToString(elasticsearchEntity.getId()),
				elasticsearchEntity.getPersistentEntity(), index, type));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations#delete(String, Class, String, String)
	 */
	@Override
	public Mono<String> deleteById(String id, Class<?> entityType, @Nullable String index, @Nullable String type) {

		Assert.notNull(id, "Id must not be null!");

		return doDeleteById(null, id, getPersistentEntity(entityType), index, type);

	}

	private Mono<String> doDeleteById(@Nullable Object source, String id, ElasticsearchPersistentEntity<?> entity,
			@Nullable String index, @Nullable String type) {

		return Mono.defer(() -> {

			IndexCoordinates indexCoordinates = operations.determineIndex(entity, index, type);

			return doDelete(prepareDeleteRequest(source,
					new DeleteRequest(indexCoordinates.getIndexName(), indexCoordinates.getTypeName(), id)));
		});
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations#deleteBy(Query, Class, String, String)
	 */
	@Override
	public Mono<Long> deleteBy(Query query, Class<?> entityType, String index, String type) {

		Assert.notNull(query, "Query must not be null!");

		return doDeleteBy(query, getPersistentEntity(entityType), index, type).map(BulkByScrollResponse::getDeleted)
				.publishNext();
	}

	private Flux<BulkByScrollResponse> doDeleteBy(Query query, ElasticsearchPersistentEntity<?> entity,
			@Nullable String index, @Nullable String type) {

		return Flux.defer(() -> {

			IndexCoordinates indexCoordinates = operations.determineIndex(entity, index, type);

			DeleteByQueryRequest request = new DeleteByQueryRequest(indices(query, indexCoordinates::getIndexName));
			request.types(indexTypes(query, indexCoordinates::getTypeName));
			request.setQuery(mappedQuery(query, entity));

			return doDeleteBy(prepareDeleteByRequest(request));
		});
	}

	// Property Setters / Getters

	/**
	 * Set the default {@link RefreshPolicy} to apply when writing to Elasticsearch.
	 *
	 * @param refreshPolicy can be {@literal null}.
	 */
	public void setRefreshPolicy(@Nullable RefreshPolicy refreshPolicy) {
		this.refreshPolicy = refreshPolicy;
	}

	/**
	 * Set the default {@link IndicesOptions} for {@link SearchRequest search requests}.
	 *
	 * @param indicesOptions can be {@literal null}.
	 */
	public void setIndicesOptions(@Nullable IndicesOptions indicesOptions) {
		this.indicesOptions = indicesOptions;
	}

	// Customization Hooks

	/**
	 * Obtain the {@link ReactiveElasticsearchClient} to operate upon.
	 *
	 * @return never {@literal null}.
	 */
	protected ReactiveElasticsearchClient getClient() {
		return this.client;
	}

	/**
	 * Pre process the write request before it is sent to the server, eg. by setting the
	 * {@link WriteRequest#setRefreshPolicy(String) refresh policy} if applicable.
	 *
	 * @param request must not be {@literal null}.
	 * @param <R>
	 * @return the processed {@link WriteRequest}.
	 */
	protected <R extends WriteRequest<R>> R prepareWriteRequest(R request) {

		if (refreshPolicy == null) {
			return request;
		}

		return request.setRefreshPolicy(refreshPolicy);
	}

	/**
	 * Customization hook to modify a generated {@link IndexRequest} prior to its execution. Eg. by setting the
	 * {@link WriteRequest#setRefreshPolicy(String) refresh policy} if applicable.
	 *
	 * @param source the source object the {@link IndexRequest} was derived from.
	 * @param request the generated {@link IndexRequest}.
	 * @return never {@literal null}.
	 */
	protected IndexRequest prepareIndexRequest(Object source, IndexRequest request) {
		return prepareWriteRequest(request);
	}

	/**
	 * Customization hook to modify a generated {@link SearchRequest} prior to its execution. Eg. by setting the
	 * {@link SearchRequest#indicesOptions(IndicesOptions) indices options} if applicable.
	 *
	 * @param request the generated {@link SearchRequest}.
	 * @return never {@literal null}.
	 */
	protected SearchRequest prepareSearchRequest(SearchRequest request) {

		if (indicesOptions == null) {
			return request;
		}

		return request.indicesOptions(indicesOptions);
	}

	/**
	 * Customization hook to modify a generated {@link DeleteRequest} prior to its execution. Eg. by setting the
	 * {@link WriteRequest#setRefreshPolicy(String) refresh policy} if applicable.
	 *
	 * @param source the source object the {@link DeleteRequest} was derived from. My be {@literal null} if using the
	 *          {@literal id} directly.
	 * @param request the generated {@link DeleteRequest}.
	 * @return never {@literal null}.
	 */
	protected DeleteRequest prepareDeleteRequest(@Nullable Object source, DeleteRequest request) {
		return prepareWriteRequest(request);
	}

	/**
	 * Customization hook to modify a generated {@link DeleteByQueryRequest} prior to its execution. Eg. by setting the
	 * {@link WriteRequest#setRefreshPolicy(String) refresh policy} if applicable.
	 *
	 * @param request the generated {@link DeleteByQueryRequest}.
	 * @return never {@literal null}.
	 */
	protected DeleteByQueryRequest prepareDeleteByRequest(DeleteByQueryRequest request) {

		if (refreshPolicy != null && !RefreshPolicy.NONE.equals(refreshPolicy)) {
			request = request.setRefresh(true);
		}

		if (indicesOptions != null) {
			request = request.setIndicesOptions(indicesOptions);
		}

		return request;
	}

	/**
	 * Customization hook on the actual execution result {@link Publisher}. <br />
	 * You know what you're doing here? Well fair enough, go ahead on your own risk.
	 *
	 * @param request the already prepared {@link IndexRequest} ready to be executed.
	 * @return a {@link Mono} emitting the result of the operation.
	 */
	protected Mono<IndexResponse> doIndex(IndexRequest request) {
		return Mono.from(execute(client -> client.index(request)));
	}

	/**
	 * Customization hook on the actual execution result {@link Publisher}. <br />
	 *
	 * @param request the already prepared {@link GetRequest} ready to be executed.
	 * @return a {@link Mono} emitting the result of the operation.
	 */
	protected Mono<GetResult> doFindById(GetRequest request) {
		return Mono.from(execute(client -> client.get(request)));
	}

	/**
	 * Customization hook on the actual execution result {@link Publisher}. <br />
	 *
	 * @param request the already prepared {@link GetRequest} ready to be executed.
	 * @return a {@link Mono} emitting the result of the operation.
	 */
	protected Mono<Boolean> doExists(GetRequest request) {
		return Mono.from(execute(client -> client.exists(request)));
	}

	/**
	 * Customization hook on the actual execution result {@link Publisher}. <br />
	 *
	 * @param request the already prepared {@link SearchRequest} ready to be executed.
	 * @return a {@link Flux} emitting the result of the operation.
	 */
	protected Flux<SearchHit> doFind(SearchRequest request) {

		if (QUERY_LOGGER.isDebugEnabled()) {
			QUERY_LOGGER.debug("Executing doFind: {}", request);
		}

		return Flux.from(execute(client -> client.search(request)));
	}

	/**
	 * Customization hook on the actual execution result {@link Publisher}. <br />
	 *
	 * @param request the already prepared {@link DeleteRequest} ready to be executed.
	 * @return a {@link Mono} emitting the result of the operation.
	 */
	protected Mono<String> doDelete(DeleteRequest request) {

		return Mono.from(execute(client -> client.delete(request))) //

				.flatMap(it -> {

					if (HttpStatus.valueOf(it.status().getStatus()).equals(HttpStatus.NOT_FOUND)) {
						return Mono.empty();
					}

					return Mono.just(it.getId());
				});
	}

	/**
	 * Customization hook on the actual execution result {@link Publisher}. <br />
	 *
	 * @param request the already prepared {@link DeleteByQueryRequest} ready to be executed.
	 * @return a {@link Mono} emitting the result of the operation.
	 */
	protected Mono<BulkByScrollResponse> doDeleteBy(DeleteByQueryRequest request) {
		return Mono.from(execute(client -> client.deleteBy(request)));
	}

	// private helpers

	private static String[] indices(Query query, Supplier<String> index) {

		if (query.getIndices().isEmpty()) {
			return new String[] { index.get() };
		}

		return query.getIndices().toArray(new String[0]);
	}

	private static String[] indexTypes(Query query, Supplier<String> indexType) {

		if (query.getTypes().isEmpty()) {
			return new String[] { indexType.get() };
		}

		return query.getTypes().toArray(new String[0]);
	}

	private static List<FieldSortBuilder> sort(Query query, ElasticsearchPersistentEntity<?> entity) {

		if (query.getSort() == null || query.getSort().isUnsorted()) {
			return Collections.emptyList();
		}

		List<FieldSortBuilder> mappedSort = new ArrayList<>();
		for (Sort.Order order : query.getSort()) {

			ElasticsearchPersistentProperty property = entity.getPersistentProperty(order.getProperty());
			String fieldName = property != null ? property.getFieldName() : order.getProperty();

			FieldSortBuilder sort = SortBuilders.fieldSort(fieldName)
					.order(order.getDirection().isDescending() ? SortOrder.DESC : SortOrder.ASC);

			if (order.getNullHandling() == Sort.NullHandling.NULLS_FIRST) {
				sort.missing("_first");
			} else if (order.getNullHandling() == Sort.NullHandling.NULLS_LAST) {
				sort.missing("_last");
			}

			mappedSort.add(sort);
		}

		return mappedSort;
	}

	private QueryBuilder mappedQuery(Query query, ElasticsearchPersistentEntity<?> entity) {

		// TODO: we need to actually map the fields to the according field names!

		QueryBuilder elasticsearchQuery = null;

		if (query instanceof CriteriaQuery) {
			elasticsearchQuery = new CriteriaQueryProcessor().createQueryFromCriteria(((CriteriaQuery) query).getCriteria());
		} else if (query instanceof StringQuery) {
			elasticsearchQuery = new WrapperQueryBuilder(((StringQuery) query).getSource());
		} else {
			throw new IllegalArgumentException(String.format("Unknown query type '%s'.", query.getClass()));
		}

		return elasticsearchQuery != null ? elasticsearchQuery : QueryBuilders.matchAllQuery();
	}

	private QueryBuilder mappedFilterQuery(CriteriaQuery query, ElasticsearchPersistentEntity<?> entity) {

		// TODO: this is actually strange in the RestTemplate:L378 - need to check
		return null;
	}

	@Nullable
	private ElasticsearchPersistentEntity<?> getPersistentEntity(@Nullable Class<?> type) {
		return type != null ? mappingContext.getPersistentEntity(type) : null;
	}

	private Throwable translateException(Throwable throwable) {

		RuntimeException exception = throwable instanceof RuntimeException ? (RuntimeException) throwable
				: new RuntimeException(throwable.getMessage(), throwable);
		RuntimeException potentiallyTranslatedException = exceptionTranslator.translateExceptionIfPossible(exception);

		return potentiallyTranslatedException != null ? potentiallyTranslatedException : throwable;
	}
}
