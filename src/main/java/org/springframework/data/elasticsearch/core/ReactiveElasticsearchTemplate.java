/*
 * Copyright 2018-2020 the original author or authors.
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
import reactor.util.function.Tuple2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.MultiGetRequest;
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
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.convert.EntityReader;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.ElasticsearchException;
import org.springframework.data.elasticsearch.NoSuchIndexException;
import org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient;
import org.springframework.data.elasticsearch.core.EntityOperations.AdaptibleEntity;
import org.springframework.data.elasticsearch.core.EntityOperations.Entity;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.document.DocumentAdapters;
import org.springframework.data.elasticsearch.core.document.SearchDocument;
import org.springframework.data.elasticsearch.core.event.ReactiveAfterConvertCallback;
import org.springframework.data.elasticsearch.core.event.ReactiveAfterSaveCallback;
import org.springframework.data.elasticsearch.core.event.ReactiveBeforeConvertCallback;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentProperty;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.data.elasticsearch.core.query.BulkOptions;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.SeqNoPrimaryTerm;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.data.elasticsearch.support.VersionInfo;
import org.springframework.data.mapping.callback.ReactiveEntityCallbacks;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * @author Christoph Strobl
 * @author Mark Paluch
 * @author Farid Azaza
 * @author Martin Choraine
 * @author Peter-Josef Meisch
 * @author Mathias Teier
 * @author Aleksei Arsenev
 * @author Roman Puchkovskiy
 * @author Russell Parry
 * @since 3.2
 */
public class ReactiveElasticsearchTemplate implements ReactiveElasticsearchOperations, ApplicationContextAware {

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

	private @Nullable ReactiveEntityCallbacks entityCallbacks;

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

		logVersions();
	}

	private void logVersions() {
		getClusterVersion() //
				.doOnSuccess(VersionInfo::logVersions) //
				.doOnError(e -> VersionInfo.logVersions(null)) //
				.subscribe();
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

		if (entityCallbacks == null) {
			setEntityCallbacks(ReactiveEntityCallbacks.create(applicationContext));
		}
	}

	/**
	 * Set the {@link ReactiveEntityCallbacks} instance to use when invoking {@link ReactiveEntityCallbacks callbacks}
	 * like the {@link ReactiveBeforeConvertCallback}.
	 * <p />
	 * Overrides potentially existing {@link ReactiveEntityCallbacks}.
	 *
	 * @param entityCallbacks must not be {@literal null}.
	 * @throws IllegalArgumentException if the given instance is {@literal null}.
	 * @since 4.0
	 */
	public void setEntityCallbacks(ReactiveEntityCallbacks entityCallbacks) {

		Assert.notNull(entityCallbacks, "EntityCallbacks must not be null!");

		this.entityCallbacks = entityCallbacks;
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

		return maybeCallBeforeConvert(entity, index)
				.flatMap(entityAfterBeforeConversionCallback -> doIndex(entityAfterBeforeConversionCallback, index)) //
				.map(it -> {
					T savedEntity = it.getT1();
					IndexResponse indexResponse = it.getT2();
					AdaptibleEntity<T> adaptableEntity = operations.forEntity(savedEntity, converter.getConversionService());
					return adaptableEntity.populateIdIfNecessary(indexResponse.getId());
				}).flatMap(saved -> maybeCallAfterSave(saved, index));
	}

	@Override
	public <T> Mono<T> save(T entity) {
		return save(entity, getIndexCoordinatesFor(entity.getClass()));
	}

	@Override
	public <T> Flux<T> saveAll(Mono<? extends Collection<? extends T>> entitiesPublisher, IndexCoordinates index) {

		Assert.notNull(entitiesPublisher, "Entities must not be null!");

		return entitiesPublisher.flatMapMany(entities -> {
			return Flux.fromIterable(entities) //
					.concatMap(entity -> maybeCallBeforeConvert(entity, index));
		}).collectList().map(Entities::new).flatMapMany(entities -> {
			if (entities.isEmpty()) {
				return Flux.empty();
			}

			return doBulkOperation(entities.indexQueries(), BulkOptions.defaultOptions(), index) //
					.index().flatMap(indexAndResponse -> {
						T savedEntity = entities.entityAt(indexAndResponse.getT1());
						BulkItemResponse bulkItemResponse = indexAndResponse.getT2();

						AdaptibleEntity<T> adaptibleEntity = operations.forEntity(savedEntity, converter.getConversionService());
						adaptibleEntity.populateIdIfNecessary(bulkItemResponse.getResponse().getId());

						return maybeCallAfterSave(savedEntity, index);
					});
		});
	}

	@Override
	public <T> Flux<T> multiGet(Query query, Class<T> clazz, IndexCoordinates index) {

		Assert.notNull(index, "Index must not be null");
		Assert.notNull(clazz, "Class must not be null");
		Assert.notNull(query, "Query must not be null");
		Assert.notEmpty(query.getIds(), "No Id define for Query");

		DocumentCallback<T> callback = new ReadDocumentCallback<>(converter, clazz, index);

		MultiGetRequest request = requestFactory.multiGetRequest(query, index);
		return Flux.from(execute(client -> client.multiGet(request))) //
				.concatMap(result -> callback.doWith(DocumentAdapters.from(result)));
	}

	@Override
	public Mono<Void> bulkUpdate(List<UpdateQuery> queries, BulkOptions bulkOptions, IndexCoordinates index) {

		Assert.notNull(queries, "List of UpdateQuery must not be null");
		Assert.notNull(bulkOptions, "BulkOptions must not be null");
		Assert.notNull(index, "Index must not be null");

		return doBulkOperation(queries, bulkOptions, index).then();
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

	protected Flux<BulkItemResponse> doBulkOperation(List<?> queries, BulkOptions bulkOptions, IndexCoordinates index) {
		BulkRequest bulkRequest = prepareWriteRequest(requestFactory.bulkRequest(queries, bulkOptions, index));
		return client.bulk(bulkRequest) //
				.onErrorMap(e -> new ElasticsearchException("Error while bulk for request: " + bulkRequest.toString(), e)) //
				.flatMap(this::checkForBulkOperationFailure) //
				.flatMapMany(response -> Flux.fromArray(response.getItems()));
	}

	protected Mono<BulkResponse> checkForBulkOperationFailure(BulkResponse bulkResponse) {

		if (bulkResponse.hasFailures()) {
			Map<String, String> failedDocuments = new HashMap<>();
			for (BulkItemResponse item : bulkResponse.getItems()) {

				if (item.isFailed()) {
					failedDocuments.put(item.getId(), item.getFailureMessage());
				}
			}
			ElasticsearchException exception = new ElasticsearchException(
					"Bulk operation has failures. Use ElasticsearchException.getFailedDocuments() for detailed messages ["
							+ failedDocuments + ']',
					failedDocuments);
			return Mono.error(exception);
		} else {
			return Mono.just(bulkResponse);
		}
	}

	@Override
	public Mono<Boolean> exists(String id, Class<?> entityType) {
		return doExists(id, getIndexCoordinatesFor(entityType));
	}

	@Override
	public Mono<Boolean> exists(String id, IndexCoordinates index) {
		return doExists(id, index);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations#exists(String, Class, IndexCoordinates)
	 */
	@Override
	public Mono<Boolean> exists(String id, Class<?> entityType, IndexCoordinates index) {

		Assert.notNull(id, "Id must not be null!");

		return doExists(id, index);
	}

	private Mono<Boolean> doExists(String id, @Nullable IndexCoordinates index) {

		return Mono.defer(() -> doExists(new GetRequest(index.getIndexName(), id)));
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

	private <T> Mono<Tuple2<T, IndexResponse>> doIndex(T entity, IndexCoordinates index) {

		AdaptibleEntity<?> adaptibleEntity = operations.forEntity(entity, converter.getConversionService());
		IndexRequest request = getIndexRequest(entity, adaptibleEntity, index);
		request = prepareIndexRequest(entity, request);
		return Mono.just(entity).zipWith(doIndex(request));
	}

	private IndexRequest getIndexRequest(Object value, AdaptibleEntity<?> entity, IndexCoordinates index) {
		Object id = entity.getId();

		IndexRequest request = id != null ? new IndexRequest(index.getIndexName()).id(converter.convertId(id))
				: new IndexRequest(index.getIndexName());

		request.source(converter.mapObject(value).toJson(), Requests.INDEX_CONTENT_TYPE);

		boolean usingSeqNo = false;
		if (entity.hasSeqNoPrimaryTerm()) {
			SeqNoPrimaryTerm seqNoPrimaryTerm = entity.getSeqNoPrimaryTerm();

			if (seqNoPrimaryTerm != null) {
				request.setIfSeqNo(seqNoPrimaryTerm.getSequenceNumber());
				request.setIfPrimaryTerm(seqNoPrimaryTerm.getPrimaryTerm());
				usingSeqNo = true;
			}
		}

		// seq_no and version are incompatible in the same request
		if (!usingSeqNo && entity.isVersionedEntity()) {

			Number version = entity.getVersion();

			if (version != null) {
				request.version(version.longValue());
				request.versionType(EXTERNAL);
			}
		}

		return request;
	}

	private IndexQuery getIndexQuery(Object value) {
		AdaptibleEntity<?> entity = operations.forEntity(value, converter.getConversionService());

		Object id = entity.getId();
		IndexQuery query = new IndexQuery();
		if (id != null) {
			query.setId(id.toString());
		}
		query.setObject(value);

		if (entity.isVersionedEntity()) {
			Number version = entity.getVersion();

			if (version != null) {
				query.setVersion(version.longValue());
			}
		}
		return query;
	}

	@Override
	public <T> Mono<T> get(String id, Class<T> entityType) {
		return get(id, entityType, getIndexCoordinatesFor(entityType));
	}

	@Override
	public <T> Mono<T> get(String id, Class<T> entityType, IndexCoordinates index) {

		Assert.notNull(id, "Id must not be null!");

		DocumentCallback<T> callback = new ReadDocumentCallback<>(converter, entityType, index);

		return doGet(id, getPersistentEntityFor(entityType), index)
				.flatMap(it -> callback.doWith(DocumentAdapters.from(it)));
	}

	private Mono<GetResult> doGet(String id, ElasticsearchPersistentEntity<?> entity, IndexCoordinates index) {
		return Mono.defer(() -> {
			return doGet(new GetRequest(index.getIndexName(), id));
		});
	}

	/**
	 * Customization hook on the actual execution result {@link Publisher}. <br />
	 *
	 * @param request the already prepared {@link GetRequest} ready to be executed.
	 * @return a {@link Mono} emitting the result of the operation.
	 */
	protected Mono<GetResult> doGet(GetRequest request) {

		return Mono.from(execute(client -> client.get(request))) //
				.onErrorResume(NoSuchIndexException.class, it -> Mono.empty());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations#delete(Object, String, String)
	 */
	@Override
	public Mono<String> delete(Object entity, IndexCoordinates index) {

		Entity<?> elasticsearchEntity = operations.forEntity(entity);

		return Mono.defer(() -> doDeleteById(converter.convertId(elasticsearchEntity.getId()), index));
	}

	@Override
	public Mono<String> delete(Object entity) {
		return delete(entity, getIndexCoordinatesFor(entity.getClass()));
	}

	@Override
	public Mono<String> delete(String id, Class<?> entityType) {

		Assert.notNull(id, "id must not be null");
		Assert.notNull(entityType, "entityType must not be null");

		return delete(id, getIndexCoordinatesFor(entityType));
	}

	@Override
	public Mono<String> delete(String id, IndexCoordinates index) {

		Assert.notNull(id, "id must not be null");
		Assert.notNull(index, "index must not be null");

		return doDeleteById(id, index);
	}

	private Mono<String> doDeleteById(String id, IndexCoordinates index) {

		return Mono.defer(() -> {

			return doDelete(prepareDeleteRequest(new DeleteRequest(index.getIndexName(), id)));
		});
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations#delete(Query, Class, IndexCoordinates)
	 */
	@Override
	public Mono<Long> delete(Query query, Class<?> entityType, IndexCoordinates index) {

		Assert.notNull(query, "Query must not be null!");

		return doDeleteBy(query, getPersistentEntityFor(entityType), index).map(BulkByScrollResponse::getDeleted)
				.publishNext();
	}

	@Override
	public Mono<Long> delete(Query query, Class<?> entityType) {
		return delete(query, entityType, getIndexCoordinatesFor(entityType));
	}

	private Flux<BulkByScrollResponse> doDeleteBy(Query query, ElasticsearchPersistentEntity<?> entity,
			IndexCoordinates index) {

		return Flux.defer(() -> {
			DeleteByQueryRequest request = new DeleteByQueryRequest(index.getIndexNames());
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
	 * @param request the generated {@link DeleteRequest}.
	 * @return never {@literal null}.
	 */
	protected DeleteRequest prepareDeleteRequest(DeleteRequest request) {
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
		SearchDocumentCallback<T> callback = new ReadSearchDocumentCallback<>(resultType, index);
		return doFind(query, entityType, index).concatMap(callback::doWith);
	}

	@Override
	public <T> Flux<SearchHit<T>> search(Query query, Class<?> entityType, Class<T> returnType) {
		return search(query, entityType, returnType, getIndexCoordinatesFor(entityType));
	}

	private Flux<SearchDocument> doFind(Query query, Class<?> clazz, IndexCoordinates index) {

		if (query instanceof CriteriaQuery) {
			converter.updateQuery((CriteriaQuery) query, clazz);
		}

		return Flux.defer(() -> {
			SearchRequest request = requestFactory.searchRequest(query, clazz, index);
			request = prepareSearchRequest(request);

			if (query.getPageable().isPaged() || query.isLimiting()) {
				return doFind(request);
			} else {
				return doScroll(request);
			}
		});
	}

	@Override
	public Flux<Aggregation> aggregate(Query query, Class<?> entityType) {
		return aggregate(query, entityType, getIndexCoordinatesFor(entityType));
	}

	@Override
	public Flux<Aggregation> aggregate(Query query, Class<?> entityType, IndexCoordinates index) {
		return doAggregate(query, entityType, index);
	}

	private Flux<Aggregation> doAggregate(Query query, Class<?> entityType, IndexCoordinates index) {
		return Flux.defer(() -> {
			SearchRequest request = requestFactory.searchRequest(query, entityType, index);
			request = prepareSearchRequest(request);
			return doAggregate(request);
		});
	}

	@Override
	public Mono<Long> count(Query query, Class<?> entityType) {
		return count(query, entityType, getIndexCoordinatesFor(entityType));
	}

	@Override
	public Mono<Long> count(Query query, Class<?> entityType, IndexCoordinates index) {
		return doCount(query, entityType, index);
	}

	private Mono<Long> doCount(Query query, Class<?> entityType, IndexCoordinates index) {
		return Mono.defer(() -> {

			SearchRequest request = requestFactory.searchRequest(query, entityType, index);
			request = prepareSearchRequest(request);
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
	 * @param request the already prepared {@link SearchRequest} ready to be executed.
	 * @return a {@link Flux} emitting the result of the operation.
	 */
	protected Flux<Aggregation> doAggregate(SearchRequest request) {

		if (QUERY_LOGGER.isDebugEnabled()) {
			QUERY_LOGGER.debug("Executing doCount: {}", request);
		}

		return Flux.from(execute(client -> client.aggregate(request))) //
				.onErrorResume(NoSuchIndexException.class, it -> Flux.empty());
	}

	/**
	 * Customization hook on the actual execution result {@link Publisher}. <br />
	 *
	 * @param request the already prepared {@link SearchRequest} ready to be executed.
	 * @return a {@link Mono} emitting the result of the operation.
	 */
	protected Mono<Long> doCount(SearchRequest request) {

		if (QUERY_LOGGER.isDebugEnabled()) {
			QUERY_LOGGER.debug("Executing doCount: {}", request);
		}

		return Mono.from(execute(client -> client.count(request))) //
				.onErrorResume(NoSuchIndexException.class, it -> Mono.just(0L));
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

		QueryBuilder elasticsearchQuery = null;

		if (query instanceof CriteriaQuery) {
			converter.updateQuery((CriteriaQuery) query, entity.getType());
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
	protected Mono<String> getClusterVersion() {
		try {
			return Mono.from(execute(client -> client.info())).map(mainResponse -> mainResponse.getVersion().toString());
		} catch (Exception ignored) {}
		return Mono.empty();
	}

	// endregion

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

	/**
	 * Obtain the {@link ReactiveElasticsearchClient} to operate upon.
	 *
	 * @return never {@literal null}.
	 */
	protected ReactiveElasticsearchClient getClient() {
		return this.client;
	}

	// endregion

	/**
	 * translates an Exception if possible. Exceptions that are no {@link RuntimeException}s are wrapped in a
	 * RuntimeException
	 *
	 * @param throwable the Throwable to map
	 * @return the potentially translated RuntimeException.
	 * @since 4.0
	 */
	private RuntimeException translateException(Throwable throwable) {

		RuntimeException runtimeException = throwable instanceof RuntimeException ? (RuntimeException) throwable
				: new RuntimeException(throwable.getMessage(), throwable);
		RuntimeException potentiallyTranslatedException = exceptionTranslator
				.translateExceptionIfPossible(runtimeException);

		return potentiallyTranslatedException != null ? potentiallyTranslatedException : runtimeException;
	}

	// region callbacks
	protected <T> Mono<T> maybeCallBeforeConvert(T entity, IndexCoordinates index) {

		if (null != entityCallbacks) {
			return entityCallbacks.callback(ReactiveBeforeConvertCallback.class, entity, index);
		}

		return Mono.just(entity);
	}

	protected <T> Mono<T> maybeCallAfterSave(T entity, IndexCoordinates index) {

		if (null != entityCallbacks) {
			return entityCallbacks.callback(ReactiveAfterSaveCallback.class, entity, index);
		}

		return Mono.just(entity);
	}

	protected <T> Mono<T> maybeCallAfterConvert(T entity, Document document, IndexCoordinates index) {

		if (null != entityCallbacks) {
			return entityCallbacks.callback(ReactiveAfterConvertCallback.class, entity, document, index);
		}

		return Mono.just(entity);
	}

	// endregion

	protected interface DocumentCallback<T> {

		@NonNull
		Mono<T> doWith(@Nullable Document document);
	}

	protected class ReadDocumentCallback<T> implements DocumentCallback<T> {
		private final EntityReader<? super T, Document> reader;
		private final Class<T> type;
		private final IndexCoordinates index;

		public ReadDocumentCallback(EntityReader<? super T, Document> reader, Class<T> type, IndexCoordinates index) {
			Assert.notNull(reader, "reader is null");
			Assert.notNull(type, "type is null");

			this.reader = reader;
			this.type = type;
			this.index = index;
		}

		@NonNull
		public Mono<T> doWith(@Nullable Document document) {
			if (document == null) {
				return Mono.empty();
			}

			T entity = reader.read(type, document);
			return maybeCallAfterConvert(entity, document, index);
		}
	}

	protected interface SearchDocumentCallback<T> {

		@NonNull
		Mono<SearchHit<T>> doWith(@NonNull SearchDocument response);
	}

	protected class ReadSearchDocumentCallback<T> implements SearchDocumentCallback<T> {
		private final DocumentCallback<T> delegate;
		private final Class<T> type;

		public ReadSearchDocumentCallback(Class<T> type, IndexCoordinates index) {
			Assert.notNull(type, "type is null");

			this.delegate = new ReadDocumentCallback<>(converter, type, index);
			this.type = type;
		}

		@Override
		public Mono<SearchHit<T>> doWith(SearchDocument response) {
			return delegate.doWith(response)
					.map(entity -> SearchHitMapping.mappingFor(type, converter.getMappingContext()).mapHit(response, entity));
		}
	}

	private class Entities<T> {
		private final List<T> entities;

		private Entities(List<T> entities) {
			Assert.notNull(entities, "entities cannot be null");

			this.entities = entities;
		}

		private boolean isEmpty() {
			return entities.isEmpty();
		}

		private List<IndexQuery> indexQueries() {
			return entities.stream().map(ReactiveElasticsearchTemplate.this::getIndexQuery).collect(Collectors.toList());
		}

		private T entityAt(long index) {
			// it's safe to cast to int because the original indexed colleciton was fitting in memory
			int intIndex = (int) index;
			return entities.get(intIndex);
		}
	}
}
