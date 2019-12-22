/*
 * Copyright 2018-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.elasticsearch.core;

import static org.elasticsearch.index.VersionType.*;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;
import org.elasticsearch.client.Requests;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.index.get.GetResult;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.WrapperQueryBuilder;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.NoSuchIndexException;
import org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient;
import org.springframework.data.elasticsearch.core.EntityOperations.AdaptibleEntity;
import org.springframework.data.elasticsearch.core.EntityOperations.Entity;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.document.DocumentAdapters;
import org.springframework.data.elasticsearch.core.document.SearchDocument;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentProperty;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * @author Christoph Strobl
 * @author Mark Paluch
 * @author Farid Azaza
 * @author Martin Choraine
 * @author Peter-Josef Meisch
 * @author Mathias Teier
 * @since 3.2
 */
public class ReactiveElasticsearchTemplate implements ReactiveElasticsearchOperations {

	private static final Logger QUERY_LOGGER = LoggerFactory
			.getLogger("org.springframework.data.elasticsearch.core.QUERY");

	private final ReactiveElasticsearchClient client;
	private final ElasticsearchConverter converter;
	private final MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> mappingContext;
	private final ElasticsearchExceptionTranslator exceptionTranslator;
	private final EntityOperations operations;
	protected RequestFactory requestFactory;

	private @Nullable RefreshPolicy refreshPolicy = RefreshPolicy.IMMEDIATE;
	private @Nullable IndicesOptions indicesOptions = IndicesOptions.strictExpandOpenAndForbidClosedIgnoreThrottled();

	// region Initialization
	public ReactiveElasticsearchTemplate(ReactiveElasticsearchClient client) {
		this(client, new MappingElasticsearchConverter(new SimpleElasticsearchMappingContext()));
	}

	public ReactiveElasticsearchTemplate(ReactiveElasticsearchClient client, ElasticsearchConverter converter) {
		Assert.notNull(client, "client must not be null");
		Assert.notNull(converter, "converter must not be null");

		this.client = client;
		this.converter = converter;
		this.mappingContext = converter.getMappingContext();

		this.exceptionTranslator = new ElasticsearchExceptionTranslator();
		this.operations = new EntityOperations(this.mappingContext);
		this.requestFactory = new RequestFactory(converter);
	}
	// endregion

	// region DocumentOperations
	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.core.ReactiveDElasticsearchOperations#index(Object, IndexCoordinates)
	 */
	@Override
	public <T> Mono<T> save(T entity, IndexCoordinates index) {

		Assert.notNull(entity, "Entity must not be null!");

		AdaptibleEntity<T> adaptableEntity = operations.forEntity(entity, converter.getConversionService());

		return doIndex(entity, adaptableEntity, index) //
				.map(it -> {
					return adaptableEntity.populateIdIfNecessary(it.getId());
				});
	}

	@Override
	public <T> Mono<T> save(T entity) {
		return save(entity, getIndexCoordinatesFor(entity.getClass()));
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

		return Mono.from(execute(client -> client.get(request))) //
				.onErrorResume(NoSuchIndexException.class, it -> Mono.empty());
	}

	@Override
	public Mono<Boolean> exists(String id, Class<?> entityType) {
		return exists(id, entityType, getIndexCoordinatesFor(entityType));
	}

	/**
	 * Customization hook on the actual execution result {@link Publisher}. <br />
	 *
	 * @param request the already prepared {@link GetRequest} ready to be executed.
	 * @return a {@link Mono} emitting the result of the operation.
	 */
	protected Mono<Boolean> doExists(GetRequest request) {

		return Mono.from(execute(client -> client.exists(request))) //
				.onErrorReturn(NoSuchIndexException.class, false);
	}

	private Mono<IndexResponse> doIndex(Object value, AdaptibleEntity<?> entity, IndexCoordinates index) {

		return Mono.defer(() -> {

			Object id = entity.getId();

			IndexRequest request = id != null
					? new IndexRequest(index.getIndexName(), index.getTypeName(), converter.convertId(id))
					: new IndexRequest(index.getIndexName(), index.getTypeName());

			request.source(converter.mapObject(value).toJson(), Requests.INDEX_CONTENT_TYPE);

			if (entity.isVersionedEntity()) {

				Object version = entity.getVersion();
				if (version != null) {
					request.version(((Number) version).longValue());
					request.versionType(EXTERNAL);
				}
			}

			request = prepareIndexRequest(value, request);
			return doIndex(request);
		});
	}

	@Override
	public <T> Mono<T> findById(String id, Class<T> entityType) {
		return findById(id, entityType, getIndexCoordinatesFor(entityType));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations#findById(String, Class, IndexCoordinates)
	 */
	@Override
	public <T> Mono<T> findById(String id, Class<T> entityType, IndexCoordinates index) {

		Assert.notNull(id, "Id must not be null!");

		return doFindById(id, getPersistentEntityFor(entityType), index)
				.map(it -> converter.mapDocument(DocumentAdapters.from(it), entityType));
	}

	private Mono<GetResult> doFindById(String id, ElasticsearchPersistentEntity<?> entity, IndexCoordinates index) {

		return Mono.defer(() -> {

			return doFindById(new GetRequest(index.getIndexName(), index.getTypeName(), id));
		});
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations#exists(String, Class, IndexCoordinates)
	 */
	@Override
	public Mono<Boolean> exists(String id, Class<?> entityType, IndexCoordinates index) {

		Assert.notNull(id, "Id must not be null!");

		return doExists(id, getPersistentEntityFor(entityType), index);
	}

	private Mono<Boolean> doExists(String id, ElasticsearchPersistentEntity<?> entity, @Nullable IndexCoordinates index) {

		return Mono.defer(() -> doExists(new GetRequest(index.getIndexName(), index.getTypeName(), id)));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations#delete(Object, String, String)
	 */
	@Override
	public Mono<String> delete(Object entity, IndexCoordinates index) {

		Entity<?> elasticsearchEntity = operations.forEntity(entity);

		return Mono.defer(() -> doDeleteById(entity, converter.convertId(elasticsearchEntity.getId()),
				elasticsearchEntity.getPersistentEntity(), index));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations#delete(String, Class, IndexCoordinates)
	 */
	@Override
	public Mono<String> deleteById(String id, Class<?> entityType, IndexCoordinates index) {

		Assert.notNull(id, "Id must not be null!");

		return doDeleteById(null, id, getPersistentEntityFor(entityType), index);

	}

	@Override
	public Mono<String> delete(Object entity) {
		return delete(entity, getIndexCoordinatesFor(entity.getClass()));
	}

	@Override
	public Mono<String> deleteById(String id, Class<?> entityType) {
		return deleteById(id, entityType, getIndexCoordinatesFor(entityType));
	}

	private Mono<String> doDeleteById(@Nullable Object source, String id, ElasticsearchPersistentEntity<?> entity,
			IndexCoordinates index) {

		return Mono.defer(() -> {

			return doDelete(prepareDeleteRequest(source, new DeleteRequest(index.getIndexName(), index.getTypeName(), id)));
		});
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations#deleteBy(Query, Class, IndexCoordinates)
	 */
	@Override
	public Mono<Long> deleteBy(Query query, Class<?> entityType, IndexCoordinates index) {

		Assert.notNull(query, "Query must not be null!");

		return doDeleteBy(query, getPersistentEntityFor(entityType), index).map(BulkByScrollResponse::getDeleted)
				.publishNext();
	}

	@Override
	public Mono<Long> deleteBy(Query query, Class<?> entityType) {
		return deleteBy(query, entityType, getIndexCoordinatesFor(entityType));
	}

	private Flux<BulkByScrollResponse> doDeleteBy(Query query, ElasticsearchPersistentEntity<?> entity,
			IndexCoordinates index) {

		return Flux.defer(() -> {
			DeleteByQueryRequest request = new DeleteByQueryRequest(index.getIndexNames());
			request.types(index.getTypeNames());
			request.setQuery(mappedQuery(query, entity));

			return doDeleteBy(prepareDeleteByRequest(request));
		});
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
				}) //
				.onErrorResume(NoSuchIndexException.class, it -> Mono.empty());
	}

	/**
	 * Customization hook on the actual execution result {@link Publisher}. <br />
	 *
	 * @param request the already prepared {@link DeleteByQueryRequest} ready to be executed.
	 * @return a {@link Mono} emitting the result of the operation.
	 */
	protected Mono<BulkByScrollResponse> doDeleteBy(DeleteByQueryRequest request) {

		return Mono.from(execute(client -> client.deleteBy(request))) //
				.onErrorResume(NoSuchIndexException.class, it -> Mono.empty());
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

	// endregion

	// region SearchOperations
	@Override
	public <T> Flux<SearchHit<T>> search(Query query, Class<?> entityType, Class<T> resultType, IndexCoordinates index) {

		return doFind(query, entityType, index).map(searchDocument -> converter.read(resultType, searchDocument));
	}

	@Override
	public <T> Flux<SearchHit<T>> search(Query query, Class<?> entityType, Class<T> returnType) {
		return search(query, entityType, returnType, getIndexCoordinatesFor(entityType));
	}

	private Flux<SearchDocument> doFind(Query query, Class<?> clazz, IndexCoordinates index) {

		return Flux.defer(() -> {
			SearchRequest request = requestFactory.searchRequest(query, clazz, index);

			if (indicesOptions != null) {
				request.indicesOptions(indicesOptions);
			}

			if (query.getPageable().isPaged() || query.isLimiting()) {
				return doFind(request);
			} else {
				return doScroll(request);
			}
		});
	}

	@Override
	public Mono<Long> count(Query query, Class<?> entityType) {
		return count(query, entityType, getIndexCoordinatesFor(entityType));
	}

	@Override
	public Mono<Long> count(Query query, Class<?> entityType, IndexCoordinates index) {
		return doCount(query, getPersistentEntityFor(entityType), index);
	}

	private Mono<Long> doCount(Query query, ElasticsearchPersistentEntity<?> entity, IndexCoordinates index) {
		return Mono.defer(() -> {

			CountRequest countRequest = buildCountRequest(query, entity, index);
			CountRequest request = prepareCountRequest(countRequest);
			return doCount(request);
		});

	}

	private CountRequest buildCountRequest(Query query, ElasticsearchPersistentEntity<?> entity, IndexCoordinates index) {

		CountRequest request = new CountRequest(index.getIndexNames());
		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
		searchSourceBuilder.query(mappedQuery(query, entity));
		searchSourceBuilder.trackScores(query.getTrackScores());

		QueryBuilder postFilterQuery = mappedFilterQuery(query, entity);
		if (postFilterQuery != null) {
			searchSourceBuilder.postFilter(postFilterQuery);
		}

		if (query.getSourceFilter() != null) {
			searchSourceBuilder.fetchSource(query.getSourceFilter().getIncludes(), query.getSourceFilter().getExcludes());
		}

		if (query instanceof NativeSearchQuery && ((NativeSearchQuery) query).getCollapseBuilder() != null) {
			searchSourceBuilder.collapse(((NativeSearchQuery) query).getCollapseBuilder());
		}

		sort(query, entity).forEach(searchSourceBuilder::sort);

		if (query.getMinScore() > 0) {
			searchSourceBuilder.minScore(query.getMinScore());
		}

		if (query.getIndicesOptions() != null) {
			request.indicesOptions(query.getIndicesOptions());
		}

		if (query.getPreference() != null) {
			request.preference(query.getPreference());
		}
		request.source(searchSourceBuilder);
		return request;
	}

	/**
	 * Customization hook on the actual execution result {@link Publisher}. <br />
	 *
	 * @param request the already prepared {@link SearchRequest} ready to be executed.
	 * @return a {@link Flux} emitting the result of the operation converted to {@link SearchDocument}s.
	 */
	protected Flux<SearchDocument> doFind(SearchRequest request) {

		if (QUERY_LOGGER.isDebugEnabled()) {
			QUERY_LOGGER.debug("Executing doFind: {}", request);
		}

		return Flux.from(execute(client -> client.search(request))).map(DocumentAdapters::from) //
				.onErrorResume(NoSuchIndexException.class, it -> Mono.empty());
	}

	/**
	 * Customization hook on the actual execution result {@link Publisher}. <br />
	 *
	 * @param request the already prepared {@link CountRequest} ready to be executed.
	 * @return a {@link Mono} emitting the result of the operation.
	 */
	protected Mono<Long> doCount(CountRequest request) {

		if (QUERY_LOGGER.isDebugEnabled()) {
			QUERY_LOGGER.debug("Executing doCount: {}", request);
		}

		return Mono.from(execute(client -> client.count(request))) //
				.onErrorResume(NoSuchIndexException.class, it -> Mono.empty());
	}

	/**
	 * Customization hook on the actual execution result {@link Publisher}. <br />
	 *
	 * @param request the already prepared {@link SearchRequest} ready to be executed.
	 * @return a {@link Flux} emitting the result of the operation converted to {@link SearchDocument}s.
	 */
	protected Flux<SearchDocument> doScroll(SearchRequest request) {

		if (QUERY_LOGGER.isDebugEnabled()) {
			QUERY_LOGGER.debug("Executing doScroll: {}", request);
		}

		return Flux.from(execute(client -> client.scroll(request))) //
				.map(DocumentAdapters::from).onErrorResume(NoSuchIndexException.class, it -> Mono.empty());
	}

	@Nullable
	private QueryBuilder mappedFilterQuery(Query query, ElasticsearchPersistentEntity<?> entity) {

		if (query instanceof NativeSearchQuery) {
			return ((NativeSearchQuery) query).getFilter();
		}

		return null;
	}

	private QueryBuilder mappedQuery(Query query, ElasticsearchPersistentEntity<?> entity) {

		// TODO: we need to actually map the fields to the according field names!

		QueryBuilder elasticsearchQuery = null;

		if (query instanceof CriteriaQuery) {
			elasticsearchQuery = new CriteriaQueryProcessor().createQueryFromCriteria(((CriteriaQuery) query).getCriteria());
		} else if (query instanceof StringQuery) {
			elasticsearchQuery = new WrapperQueryBuilder(((StringQuery) query).getSource());
		} else if (query instanceof NativeSearchQuery) {
			elasticsearchQuery = ((NativeSearchQuery) query).getQuery();
		} else {
			throw new IllegalArgumentException(String.format("Unknown query type '%s'.", query.getClass()));
		}

		return elasticsearchQuery != null ? elasticsearchQuery : QueryBuilders.matchAllQuery();
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

	/**
	 * Customization hook to modify a generated {@link SearchRequest} prior to its execution. Eg. by setting the
	 * {@link SearchRequest#indicesOptions(IndicesOptions) indices options} if applicable.
	 *
	 * @param request the generated {@link CountRequest}.
	 * @return never {@literal null}.
	 */
	protected CountRequest prepareCountRequest(CountRequest request) {

		if (indicesOptions == null) {
			return request;
		}

		return request.indicesOptions(indicesOptions);
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

	// endregion

	// region Helper methods
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
	 * @see org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations#getElasticsearchConverter()
	 */
	@Override
	public ElasticsearchConverter getElasticsearchConverter() {
		return converter;
	}

	@Override
	public IndexCoordinates getIndexCoordinatesFor(Class<?> clazz) {
		return getPersistentEntityFor(clazz).getIndexCoordinates();
	}

	@Override
	@Nullable
	public ElasticsearchPersistentEntity<?> getPersistentEntityFor(@Nullable Class<?> type) {
		return type != null ? mappingContext.getPersistentEntity(type) : null;
	}

	private Throwable translateException(Throwable throwable) {

		RuntimeException exception = throwable instanceof RuntimeException ? (RuntimeException) throwable
				: new RuntimeException(throwable.getMessage(), throwable);
		RuntimeException potentiallyTranslatedException = exceptionTranslator.translateExceptionIfPossible(exception);

		return potentiallyTranslatedException != null ? potentiallyTranslatedException : throwable;
	}

	/**
	 * Obtain the {@link ReactiveElasticsearchClient} to operate upon.
	 *
	 * @return never {@literal null}.
	 */
	protected ReactiveElasticsearchClient getClient() {
		return this.client;
	}

	// endregion

}
