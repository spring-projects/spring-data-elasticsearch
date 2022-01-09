/*
 * Copyright 2018-2022 the original author or authors.
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

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.Version;
import org.elasticsearch.action.DocWriteResponse;
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
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.index.get.GetResult;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.index.reindex.UpdateByQueryRequest;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.reactivestreams.Publisher;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.convert.EntityReader;
import org.springframework.data.elasticsearch.BulkFailureException;
import org.springframework.data.elasticsearch.NoSuchIndexException;
import org.springframework.data.elasticsearch.UncategorizedElasticsearchException;
import org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient;
import org.springframework.data.elasticsearch.core.EntityOperations.AdaptibleEntity;
import org.springframework.data.elasticsearch.core.cluster.DefaultReactiveClusterOperations;
import org.springframework.data.elasticsearch.core.cluster.ReactiveClusterOperations;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.document.DocumentAdapters;
import org.springframework.data.elasticsearch.core.document.SearchDocument;
import org.springframework.data.elasticsearch.core.document.SearchDocumentResponse;
import org.springframework.data.elasticsearch.core.event.ReactiveAfterConvertCallback;
import org.springframework.data.elasticsearch.core.event.ReactiveAfterLoadCallback;
import org.springframework.data.elasticsearch.core.event.ReactiveAfterSaveCallback;
import org.springframework.data.elasticsearch.core.event.ReactiveBeforeConvertCallback;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentProperty;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.data.elasticsearch.core.query.BulkOptions;
import org.springframework.data.elasticsearch.core.query.ByQueryResponse;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.SeqNoPrimaryTerm;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.data.elasticsearch.core.query.UpdateResponse;
import org.springframework.data.elasticsearch.core.routing.DefaultRoutingResolver;
import org.springframework.data.elasticsearch.core.routing.RoutingResolver;
import org.springframework.data.elasticsearch.core.suggest.response.Suggest;
import org.springframework.data.elasticsearch.support.VersionInfo;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.callback.ReactiveEntityCallbacks;
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
 * @author Thomas Geese
 * @author Farid Faoudi
 * @since 3.2
 */
public class ReactiveElasticsearchTemplate implements ReactiveElasticsearchOperations, ApplicationContextAware {

	private static final Log QUERY_LOGGER = LogFactory.getLog("org.springframework.data.elasticsearch.core.QUERY");

	private final ReactiveElasticsearchClient client;
	private final ElasticsearchConverter converter;
	private final SimpleElasticsearchMappingContext mappingContext;
	private final ElasticsearchExceptionTranslator exceptionTranslator;
	private final EntityOperations operations;
	protected RequestFactory requestFactory;

	private @Nullable RefreshPolicy refreshPolicy = RefreshPolicy.IMMEDIATE;
	private @Nullable IndicesOptions indicesOptions = IndicesOptions.strictExpandOpenAndForbidClosedIgnoreThrottled();

	private @Nullable ReactiveEntityCallbacks entityCallbacks;

	private RoutingResolver routingResolver;

	// region Initialization
	public ReactiveElasticsearchTemplate(ReactiveElasticsearchClient client) {
		this(client, new MappingElasticsearchConverter(new SimpleElasticsearchMappingContext()));
	}

	public ReactiveElasticsearchTemplate(ReactiveElasticsearchClient client, ElasticsearchConverter converter) {

		Assert.notNull(client, "client must not be null");
		Assert.notNull(converter, "converter must not be null");

		this.client = client;
		this.converter = converter;

		this.mappingContext = (SimpleElasticsearchMappingContext) converter.getMappingContext();
		this.routingResolver = new DefaultRoutingResolver(this.mappingContext);

		this.exceptionTranslator = new ElasticsearchExceptionTranslator();
		this.operations = new EntityOperations(this.mappingContext);
		this.requestFactory = new RequestFactory(converter);

		// initialize the VersionInfo class in the initialization phase
		// noinspection ResultOfMethodCallIgnored
		VersionInfo.versionProperties();
	}

	private ReactiveElasticsearchTemplate copy() {

		ReactiveElasticsearchTemplate copy = new ReactiveElasticsearchTemplate(client, converter);
		copy.setRefreshPolicy(refreshPolicy);
		copy.setIndicesOptions(indicesOptions);
		copy.setEntityCallbacks(entityCallbacks);
		copy.setRoutingResolver(routingResolver);
		return copy;
	}

	/**
	 * logs the versions of the different Elasticsearch components.
	 *
	 * @return a Mono signalling finished execution
	 * @since 4.3
	 */
	public Mono<Void> logVersions() {
		return getVendor() //
				.doOnNext(vendor -> getRuntimeLibraryVersion() //
						.doOnNext(runtimeLibraryVersion -> getClusterVersion() //
								.doOnNext(clusterVersion -> VersionInfo.logVersions(vendor, runtimeLibraryVersion, clusterVersion)))) //
				.then(); //
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

		if (entityCallbacks == null) {
			setEntityCallbacks(ReactiveEntityCallbacks.create(applicationContext));
		}
	}

	/**
	 * Set the default {@link RefreshPolicy} to apply when writing to Elasticsearch.
	 *
	 * @param refreshPolicy can be {@literal null}.
	 */
	public void setRefreshPolicy(@Nullable RefreshPolicy refreshPolicy) {
		this.refreshPolicy = refreshPolicy;
	}

	/**
	 * @return the current {@link RefreshPolicy}.
	 */
	@Nullable
	public RefreshPolicy getRefreshPolicy() {
		return refreshPolicy;
	}

	/**
	 * Set the default {@link IndicesOptions} for {@link SearchRequest search requests}.
	 *
	 * @param indicesOptions can be {@literal null}.
	 */
	public void setIndicesOptions(@Nullable IndicesOptions indicesOptions) {
		this.indicesOptions = indicesOptions;
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
					return updateIndexedObject(savedEntity, IndexedObjectInformation.of(indexResponse.getId(),
							indexResponse.getSeqNo(), indexResponse.getPrimaryTerm(), indexResponse.getVersion()));
				}).flatMap(saved -> maybeCallAfterSave(saved, index));
	}

	@Override
	public <T> Mono<T> save(T entity) {
		return save(entity, getIndexCoordinatesFor(entity.getClass()));
	}

	@Override
	public <T> Flux<T> saveAll(Mono<? extends Collection<? extends T>> entities, Class<T> clazz) {
		return saveAll(entities, getIndexCoordinatesFor(clazz));
	}

	@Override
	public <T> Flux<T> saveAll(Mono<? extends Collection<? extends T>> entitiesPublisher, IndexCoordinates index) {

		Assert.notNull(entitiesPublisher, "Entities must not be null!");

		return entitiesPublisher //
				.flatMapMany(entities -> Flux.fromIterable(entities) //
						.concatMap(entity -> maybeCallBeforeConvert(entity, index)) //
				).collectList() //
				.map(Entities::new) //
				.flatMapMany(entities -> {

					if (entities.isEmpty()) {
						return Flux.empty();
					}

					return doBulkOperation(entities.indexQueries(), BulkOptions.defaultOptions(), index) //
							.index().flatMap(indexAndResponse -> {
								T savedEntity = entities.entityAt(indexAndResponse.getT1());
								BulkItemResponse bulkItemResponse = indexAndResponse.getT2();

								DocWriteResponse response = bulkItemResponse.getResponse();
								updateIndexedObject(savedEntity, IndexedObjectInformation.of(response.getId(), response.getSeqNo(),
										response.getPrimaryTerm(), response.getVersion()));

								return maybeCallAfterSave(savedEntity, index);
							});
				});
	}

	private <T> T updateIndexedObject(T entity, IndexedObjectInformation indexedObjectInformation) {

		ElasticsearchPersistentEntity<?> persistentEntity = converter.getMappingContext()
				.getPersistentEntity(entity.getClass());

		if (persistentEntity != null) {
			PersistentPropertyAccessor<Object> propertyAccessor = persistentEntity.getPropertyAccessor(entity);
			ElasticsearchPersistentProperty idProperty = persistentEntity.getIdProperty();

			// Only deal with text because ES generated Ids are strings!
			if (indexedObjectInformation.getId() != null && idProperty != null
					&& idProperty.getType().isAssignableFrom(String.class)) {
				propertyAccessor.setProperty(idProperty, indexedObjectInformation.getId());
			}

			if (indexedObjectInformation.getSeqNo() != null && indexedObjectInformation.getPrimaryTerm() != null
					&& persistentEntity.hasSeqNoPrimaryTermProperty()) {
				ElasticsearchPersistentProperty seqNoPrimaryTermProperty = persistentEntity.getSeqNoPrimaryTermProperty();
				// noinspection ConstantConditions
				propertyAccessor.setProperty(seqNoPrimaryTermProperty,
						new SeqNoPrimaryTerm(indexedObjectInformation.getSeqNo(), indexedObjectInformation.getPrimaryTerm()));
			}

			if (indexedObjectInformation.getVersion() != null && persistentEntity.hasVersionProperty()) {
				ElasticsearchPersistentProperty versionProperty = persistentEntity.getVersionProperty();
				// noinspection ConstantConditions
				propertyAccessor.setProperty(versionProperty, indexedObjectInformation.getVersion());
			}

			// noinspection unchecked
			T updatedEntity = (T) propertyAccessor.getBean();
			return updatedEntity;
		} else {
			AdaptibleEntity<T> adaptibleEntity = operations.forEntity(entity, converter.getConversionService(),
					routingResolver);
			adaptibleEntity.populateIdIfNecessary(indexedObjectInformation.getId());
		}
		return entity;
	}

	@Override
	public <T> Flux<MultiGetItem<T>> multiGet(Query query, Class<T> clazz) {
		return multiGet(query, clazz, getIndexCoordinatesFor(clazz));
	}

	@Override
	public <T> Flux<MultiGetItem<T>> multiGet(Query query, Class<T> clazz, IndexCoordinates index) {

		Assert.notNull(index, "Index must not be null");
		Assert.notNull(clazz, "Class must not be null");
		Assert.notNull(query, "Query must not be null");

		DocumentCallback<T> callback = new ReadDocumentCallback<>(converter, clazz, index);

		MultiGetRequest request = requestFactory.multiGetRequest(query, clazz, index);
		return Flux.from(execute(client -> client.multiGet(request))) //
				.map(DocumentAdapters::from) //
				.flatMap(multiGetItem -> multiGetItem.isFailed() //
						? Mono.just(MultiGetItem.of(null, multiGetItem.getFailure())) //
						: callback.toEntity(multiGetItem.getItem())
								.map((T item) -> MultiGetItem.of(item, multiGetItem.getFailure())) //
				);
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
				.onErrorMap(e -> new UncategorizedElasticsearchException("Error while bulk for request: " + bulkRequest, e)) //
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
			BulkFailureException exception = new BulkFailureException(
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

	private Mono<Boolean> doExists(String id, IndexCoordinates index) {
		return Mono.defer(() -> doExists(requestFactory.getRequest(id, routingResolver.getRouting(), index)));
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

		IndexRequest request = requestFactory.indexRequest(getIndexQuery(entity), index);
		request = prepareIndexRequest(entity, request);
		return Mono.just(entity).zipWith(doIndex(request));
	}

	private IndexQuery getIndexQuery(Object value) {
		AdaptibleEntity<?> entity = operations.forEntity(value, converter.getConversionService(), routingResolver);

		Object id = entity.getId();
		IndexQuery query = new IndexQuery();

		if (id != null) {
			query.setId(id.toString());
		}
		query.setObject(value);

		boolean usingSeqNo = false;

		if (entity.hasSeqNoPrimaryTerm()) {
			SeqNoPrimaryTerm seqNoPrimaryTerm = entity.getSeqNoPrimaryTerm();

			if (seqNoPrimaryTerm != null) {
				query.setSeqNo(seqNoPrimaryTerm.getSequenceNumber());
				query.setPrimaryTerm(seqNoPrimaryTerm.getPrimaryTerm());
				usingSeqNo = true;
			}
		}

		// seq_no and version are incompatible in the same request
		if (!usingSeqNo && entity.isVersionedEntity()) {

			Number version = entity.getVersion();

			if (version != null) {
				query.setVersion(version.longValue());
			}
		}

		query.setRouting(entity.getRouting());

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

		return doGet(id, index).flatMap(response -> callback.toEntity(DocumentAdapters.from(response)));
	}

	private Mono<GetResult> doGet(String id, IndexCoordinates index) {
		return Mono.defer(() -> doGet(requestFactory.getRequest(id, routingResolver.getRouting(), index)));
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

		AdaptibleEntity<?> elasticsearchEntity = operations.forEntity(entity, converter.getConversionService(),
				routingResolver);

		if (elasticsearchEntity.getId() == null) {
			return Mono.error(new IllegalArgumentException("entity must have an id"));
		}

		return Mono.defer(() -> {
			String id = converter.convertId(elasticsearchEntity.getId());
			String routing = elasticsearchEntity.getRouting();
			return doDeleteById(id, routing, index);
		});
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

		return doDeleteById(id, routingResolver.getRouting(), index);
	}

	private Mono<String> doDeleteById(String id, @Nullable String routing, IndexCoordinates index) {

		return Mono.defer(() -> {
			DeleteRequest request = requestFactory.deleteRequest(id, routing, index);
			return doDelete(prepareDeleteRequest(request));
		});
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations#delete(Query, Class, IndexCoordinates)
	 */
	@Override
	public Mono<ByQueryResponse> delete(Query query, Class<?> entityType, IndexCoordinates index) {

		Assert.notNull(query, "Query must not be null!");

		return doDeleteBy(query, entityType, index).map(ResponseConverter::byQueryResponseOf);
	}

	@Override
	public Mono<UpdateResponse> update(UpdateQuery updateQuery, IndexCoordinates index) {

		Assert.notNull(updateQuery, "UpdateQuery must not be null");
		Assert.notNull(index, "Index must not be null");

		return Mono.defer(() -> {
			UpdateRequest request = requestFactory.updateRequest(updateQuery, index);

			if (updateQuery.getRefreshPolicy() == null && refreshPolicy != null) {
				request.setRefreshPolicy(RequestFactory.toElasticsearchRefreshPolicy(refreshPolicy));
			}

			if (updateQuery.getRouting() == null && routingResolver.getRouting() != null) {
				request.routing(routingResolver.getRouting());
			}

			return Mono.from(execute(client -> client.update(request)))
					.map(response -> new UpdateResponse(UpdateResponse.Result.valueOf(response.getResult().name())));
		});
	}

	@Override
	public Mono<ByQueryResponse> updateByQuery(UpdateQuery updateQuery, IndexCoordinates index) {

		Assert.notNull(updateQuery, "updateQuery must not be null");
		Assert.notNull(index, "Index must not be null");

		return Mono.defer(() -> {

			final UpdateByQueryRequest request = requestFactory.updateByQueryRequest(updateQuery, index);

			if (updateQuery.getRefreshPolicy() == null && refreshPolicy != null) {
				request.setRefresh(refreshPolicy == RefreshPolicy.IMMEDIATE);
			}

			if (updateQuery.getRouting() == null && routingResolver.getRouting() != null) {
				request.setRouting(routingResolver.getRouting());
			}

			return Mono.from(execute(client -> client.updateBy(request)));
		});
	}

	@Override
	public Mono<ByQueryResponse> delete(Query query, Class<?> entityType) {
		return delete(query, entityType, getIndexCoordinatesFor(entityType));
	}

	private Mono<BulkByScrollResponse> doDeleteBy(Query query, Class<?> entityType, IndexCoordinates index) {

		return Mono.defer(() -> {
			DeleteByQueryRequest request = requestFactory.deleteByQueryRequest(query, entityType, index);
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
	 * Customization hook to modify a generated {@link DeleteRequest} prior to its execution. E.g. by setting the
	 * {@link WriteRequest#setRefreshPolicy(String) refresh policy} if applicable.
	 *
	 * @param request the generated {@link DeleteRequest}.
	 * @return never {@literal null}.
	 */
	protected DeleteRequest prepareDeleteRequest(DeleteRequest request) {
		return prepareWriteRequest(request);
	}

	/**
	 * Customization hook to modify a generated {@link DeleteByQueryRequest} prior to its execution. E.g. by setting the
	 * {@link WriteRequest#setRefreshPolicy(String) refresh policy} if applicable.
	 *
	 * @param request the generated {@link DeleteByQueryRequest}.
	 * @return never {@literal null}.
	 */
	protected DeleteByQueryRequest prepareDeleteByRequest(DeleteByQueryRequest request) {

		if (refreshPolicy != null) {

			if (RefreshPolicy.NONE.equals(refreshPolicy)) {
				request = request.setRefresh(false);
			} else {
				request = request.setRefresh(true);
			}
		}

		if (indicesOptions != null) {
			request = request.setIndicesOptions(indicesOptions);
		}

		return request;
	}

	/**
	 * Customization hook to modify a generated {@link IndexRequest} prior to its execution. E.g. by setting the
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
	 * Preprocess the write request before it is sent to the server, e.g. by setting the
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

		return request.setRefreshPolicy(RequestFactory.toElasticsearchRefreshPolicy(refreshPolicy));
	}

	// endregion

	// region SearchOperations
	@Override
	public <T> Flux<SearchHit<T>> search(Query query, Class<?> entityType, Class<T> resultType, IndexCoordinates index) {
		SearchDocumentCallback<T> callback = new ReadSearchDocumentCallback<>(resultType, index);
		return doFind(query, entityType, index).concatMap(callback::toSearchHit);
	}

	@Override
	public <T> Flux<SearchHit<T>> search(Query query, Class<?> entityType, Class<T> returnType) {
		return search(query, entityType, returnType, getIndexCoordinatesFor(entityType));
	}

	@Override
	public <T> Mono<SearchPage<T>> searchForPage(Query query, Class<?> entityType, Class<T> resultType) {
		return searchForPage(query, entityType, resultType, getIndexCoordinatesFor(entityType));
	}

	@Override
	public <T> Mono<SearchPage<T>> searchForPage(Query query, Class<?> entityType, Class<T> resultType,
			IndexCoordinates index) {

		SearchDocumentCallback<T> callback = new ReadSearchDocumentCallback<>(resultType, index);

		return doFindForResponse(query, entityType, index) //
				.flatMap(searchDocumentResponse -> Flux.fromIterable(searchDocumentResponse.getSearchDocuments()) //
						.flatMap(callback::toEntity) //
						.collectList() //
						.map(entities -> SearchHitMapping.mappingFor(resultType, converter) //
								.mapHits(searchDocumentResponse, entities))) //
				.map(searchHits -> SearchHitSupport.searchPageFor(searchHits, query.getPageable()));
	}

	@Override
	public <T> Mono<ReactiveSearchHits<T>> searchForHits(Query query, Class<?> entityType, Class<T> resultType) {
		return searchForHits(query, entityType, resultType, getIndexCoordinatesFor(entityType));
	}

	@Override
	public <T> Mono<ReactiveSearchHits<T>> searchForHits(Query query, Class<?> entityType, Class<T> resultType,
			IndexCoordinates index) {

		Assert.notNull(query, "query must not be null");
		Assert.notNull(entityType, "entityType must not be null");
		Assert.notNull(resultType, "resultType must not be null");
		Assert.notNull(index, "index must not be null");

		SearchDocumentCallback<T> callback = new ReadSearchDocumentCallback<>(resultType, index);

		return doFindForResponse(query, entityType, index) //
				.flatMap(searchDocumentResponse -> Flux.fromIterable(searchDocumentResponse.getSearchDocuments()) //
						.flatMap(callback::toEntity) //
						.collectList() //
						.map(entities -> SearchHitMapping.mappingFor(resultType, converter) //
								.mapHits(searchDocumentResponse, entities))) //
				.map(ReactiveSearchHitSupport::searchHitsFor);
	}

	private Flux<SearchDocument> doFind(Query query, Class<?> clazz, IndexCoordinates index) {

		return Flux.defer(() -> {

			SearchRequest request = requestFactory.searchRequest(query, clazz, index);
			boolean useScroll = !(query.getPageable().isPaged() || query.isLimiting());
			request = prepareSearchRequest(request, useScroll);

			if (useScroll) {
				return doScroll(request);
			} else {
				return doFind(request);
			}
		});
	}

	private Mono<SearchDocumentResponse> doFindForResponse(Query query, Class<?> clazz, IndexCoordinates index) {

		return Mono.defer(() -> {
			SearchRequest request = requestFactory.searchRequest(query, clazz, index);
			request = prepareSearchRequest(request, false);

			SearchDocumentCallback<?> documentCallback = new ReadSearchDocumentCallback<>(clazz, index);
			Function<SearchDocument, Object> entityCreator = searchDocument -> documentCallback.toEntity(searchDocument)
					.block();
			return doFindForResponse(request, entityCreator);
		});
	}

	@Override
	public Flux<AggregationContainer<?>> aggregate(Query query, Class<?> entityType) {
		return aggregate(query, entityType, getIndexCoordinatesFor(entityType));
	}

	@Override
	public Flux<AggregationContainer<?>> aggregate(Query query, Class<?> entityType, IndexCoordinates index) {

		Assert.notNull(query, "query must not be null");
		Assert.notNull(entityType, "entityType must not be null");
		Assert.notNull(index, "index must not be null");

		return Flux.defer(() -> {
			SearchRequest request = requestFactory.searchRequest(query, entityType, index);
			request = prepareSearchRequest(request, false);
			return doAggregate(request);
		});
	}

	/**
	 * Customization hook on the actual execution result {@link Publisher}. <br />
	 *
	 * @param request the already prepared {@link SearchRequest} ready to be executed.
	 * @return a {@link Flux} emitting the result of the operation.
	 */
	protected Flux<AggregationContainer<?>> doAggregate(SearchRequest request) {

		if (QUERY_LOGGER.isDebugEnabled()) {
			QUERY_LOGGER.debug(String.format("Executing doCount: %s", request));
		}

		return Flux.from(execute(client -> client.aggregate(request))) //
				.onErrorResume(NoSuchIndexException.class, it -> Flux.empty()).map(ElasticsearchAggregation::new);
	}

	@Override
	public Mono<Suggest> suggest(Query query, Class<?> entityType) {
		return suggest(query, entityType, getIndexCoordinatesFor(entityType));
	}

	@Override
	public Mono<Suggest> suggest(Query query, Class<?> entityType, IndexCoordinates index) {

		Assert.notNull(query, "query must not be null");
		Assert.notNull(entityType, "entityType must not be null");
		Assert.notNull(index, "index must not be null");

		return doFindForResponse(query, entityType, index).mapNotNull(searchDocumentResponse -> {
			Suggest suggest = searchDocumentResponse.getSuggest();
			SearchHitMapping.mappingFor(entityType, converter).mapHitsInCompletionSuggestion(suggest);
			return suggest;
		});
	}

	@Override
	@Deprecated
	public Flux<org.elasticsearch.search.suggest.Suggest> suggest(SuggestBuilder suggestion, Class<?> entityType) {
		return doSuggest(suggestion, getIndexCoordinatesFor(entityType));
	}

	@Override
	@Deprecated
	public Flux<org.elasticsearch.search.suggest.Suggest> suggest(SuggestBuilder suggestion, IndexCoordinates index) {
		return doSuggest(suggestion, index);
	}

	@Deprecated
	private Flux<org.elasticsearch.search.suggest.Suggest> doSuggest(SuggestBuilder suggestion, IndexCoordinates index) {
		return Flux.defer(() -> {
			SearchRequest request = requestFactory.searchRequest(suggestion, index);
			return Flux.from(execute(client -> client.suggest(request)));
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
			request = prepareSearchRequest(request, false);
			return doCount(request);
		});
	}

	/**
	 * Customization hook on the actual execution result {@link Publisher}. <br />
	 *
	 * @param request the already prepared {@link SearchRequest} ready to be executed.
	 * @return a {@link Flux} emitting the result of the operation converted to {@link SearchDocument}s.
	 */
	protected Flux<SearchDocument> doFind(SearchRequest request) {

		if (QUERY_LOGGER.isDebugEnabled()) {
			QUERY_LOGGER.debug(String.format("Executing doFind: %s", request));
		}

		return Flux.from(execute(client -> client.search(request))).map(DocumentAdapters::from) //
				.onErrorResume(NoSuchIndexException.class, it -> Mono.empty());
	}

	/**
	 * Customization hook on the actual execution result {@link Mono}. <br />
	 *
	 * @param request the already prepared {@link SearchRequest} ready to be executed.
	 * @param entityCreator
	 * @return a {@link Mono} emitting the result of the operation converted to s {@link SearchDocumentResponse}.
	 */
	protected Mono<SearchDocumentResponse> doFindForResponse(SearchRequest request,
			Function<SearchDocument, ? extends Object> entityCreator) {

		if (QUERY_LOGGER.isDebugEnabled()) {
			QUERY_LOGGER.debug(String.format("Executing doFindForResponse: %s", request));
		}

		return Mono.from(execute(client -> client.searchForResponse(request)))
				.map(searchResponse -> SearchDocumentResponse.from(searchResponse, entityCreator));
	}

	/**
	 * Customization hook on the actual execution result {@link Publisher}. <br />
	 *
	 * @param request the already prepared {@link SearchRequest} ready to be executed.
	 * @return a {@link Mono} emitting the result of the operation.
	 */
	protected Mono<Long> doCount(SearchRequest request) {

		if (QUERY_LOGGER.isDebugEnabled()) {
			QUERY_LOGGER.debug(String.format("Executing doCount: %s", request));
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
			QUERY_LOGGER.debug(String.format("Executing doScroll: %s", request));
		}

		return Flux.from(execute(client -> client.scroll(request))) //
				.map(DocumentAdapters::from).onErrorResume(NoSuchIndexException.class, it -> Mono.empty());
	}

	/**
	 * Customization hook to modify a generated {@link SearchRequest} prior to its execution. E.g. by setting the
	 * {@link SearchRequest#indicesOptions(IndicesOptions) indices options} if applicable.
	 *
	 * @param request the generated {@link SearchRequest}.
	 * @param useScroll
	 * @return never {@literal null}.
	 */
	protected SearchRequest prepareSearchRequest(SearchRequest request, boolean useScroll) {

		if (indicesOptions != null) {
			request = request.indicesOptions(indicesOptions);
		}

		// request_cache is not allowed on scroll requests.
		if (useScroll) {
			request = request.requestCache(null);
		}
		return request;

	}

	// endregion

	// region Helper methods
	protected Mono<String> getClusterVersion() {
		try {
			return Mono.from(execute(ReactiveElasticsearchClient::info))
					.map(mainResponse -> mainResponse.getVersion().toString());
		} catch (Exception ignored) {}
		return Mono.empty();
	}

	/**
	 * @return the vendor name of the used cluster and client library
	 * @since 4.3
	 */
	protected Mono<String> getVendor() {
		return Mono.just("Elasticsearch");
	}

	/**
	 * @return the version of the used client runtime library.
	 * @since 4.3
	 */
	protected Mono<String> getRuntimeLibraryVersion() {
		return Mono.just(Version.CURRENT.toString());
	}

	@Override
	public Query matchAllQuery() {
		return new NativeSearchQueryBuilder().withQuery(QueryBuilders.matchAllQuery()).build();
	}

	@Override
	public Query idsQuery(List<String> ids) {

		Assert.notNull(ids, "ids must not be null");

		return new NativeSearchQueryBuilder().withQuery(QueryBuilders.idsQuery().addIds(ids.toArray(new String[] {})))
				.build();
	}
	// endregion

	@Override
	public <T> Publisher<T> execute(ClientCallback<Publisher<T>> callback) {
		return Flux.defer(() -> callback.doWithClient(getClient())).onErrorMap(this::translateException);
	}

	@Override
	public <T> Publisher<T> executeWithIndicesClient(IndicesClientCallback<Publisher<T>> callback) {
		return Flux.defer(() -> callback.doWithClient(getIndicesClient())).onErrorMap(this::translateException);
	}

	@Override
	public <T> Publisher<T> executeWithClusterClient(ClusterClientCallback<Publisher<T>> callback) {
		return Flux.defer(() -> callback.doWithClient(getClusterClient())).onErrorMap(this::translateException);
	}

	@Override
	public ElasticsearchConverter getElasticsearchConverter() {
		return converter;
	}

	@Override
	public ReactiveIndexOperations indexOps(IndexCoordinates index) {
		return new ReactiveIndexTemplate(this, index);
	}

	@Override
	public ReactiveIndexOperations indexOps(Class<?> clazz) {
		return new ReactiveIndexTemplate(this, clazz);
	}

	@Override
	public ReactiveClusterOperations cluster() {
		return new DefaultReactiveClusterOperations(this);
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

	/**
	 * Obtain the {@link ReactiveElasticsearchClient.Indices} to operate upon.
	 *
	 * @return never {@literal null}.
	 */
	protected ReactiveElasticsearchClient.Indices getIndicesClient() {

		if (client instanceof ReactiveElasticsearchClient.Indices) {
			return (ReactiveElasticsearchClient.Indices) client;
		}

		throw new UncategorizedElasticsearchException("No ReactiveElasticsearchClient.Indices implementation available");
	}

	/**
	 * Obtain the {@link ReactiveElasticsearchClient.Cluster} to operate upon.
	 *
	 * @return never {@literal null}.
	 */
	protected ReactiveElasticsearchClient.Cluster getClusterClient() {

		if (client instanceof ReactiveElasticsearchClient.Cluster) {
			return (ReactiveElasticsearchClient.Cluster) client;
		}

		throw new UncategorizedElasticsearchException("No ReactiveElasticsearchClient.Cluster implementation available");
	}

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

	protected <T> Mono<Document> maybeCallbackAfterLoad(Document document, Class<T> type,
			IndexCoordinates indexCoordinates) {

		if (entityCallbacks != null) {
			return entityCallbacks.callback(ReactiveAfterLoadCallback.class, document, type, indexCoordinates);
		}

		return Mono.just(document);
	}

	// endregion

	// region routing
	private void setRoutingResolver(RoutingResolver routingResolver) {

		Assert.notNull(routingResolver, "routingResolver must not be null");

		this.routingResolver = routingResolver;
	}

	@Override
	public ReactiveElasticsearchOperations withRouting(RoutingResolver routingResolver) {

		Assert.notNull(routingResolver, "routingResolver must not be null");

		ReactiveElasticsearchTemplate copy = copy();
		copy.setRoutingResolver(routingResolver);
		return copy;
	}
	// endregion

	protected interface DocumentCallback<T> {

		@NonNull
		Mono<T> toEntity(@Nullable Document document);
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
		public Mono<T> toEntity(@Nullable Document document) {
			if (document == null) {
				return Mono.empty();
			}

			return maybeCallbackAfterLoad(document, type, index) //
					.flatMap(documentAfterLoad -> {

						T entity = reader.read(type, documentAfterLoad);

						IndexedObjectInformation indexedObjectInformation = IndexedObjectInformation.of( //
								documentAfterLoad.hasId() ? documentAfterLoad.getId() : null, //
								documentAfterLoad.getSeqNo(), //
								documentAfterLoad.getPrimaryTerm(), //
								documentAfterLoad.getVersion()); //
						entity = updateIndexedObject(entity, indexedObjectInformation);

						return maybeCallAfterConvert(entity, documentAfterLoad, index);
					});
		}
	}

	protected interface SearchDocumentCallback<T> {

		Mono<T> toEntity(SearchDocument response);

		Mono<SearchHit<T>> toSearchHit(SearchDocument response);
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
		public Mono<T> toEntity(SearchDocument response) {
			return delegate.toEntity(response);
		}

		@Override
		public Mono<SearchHit<T>> toSearchHit(SearchDocument response) {
			return toEntity(response).map(entity -> SearchHitMapping.mappingFor(type, converter).mapHit(response, entity));
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
			// it's safe to cast to int because the original indexed collection was fitting in memory
			int intIndex = (int) index;
			return entities.get(intIndex);
		}
	}
}
