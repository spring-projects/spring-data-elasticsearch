/*
 * Copyright 2018-2023 the original author or authors.
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
package org.springframework.data.elasticsearch.client.erhlc;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.reactivestreams.Publisher;
import org.springframework.data.elasticsearch.BulkFailureException;
import org.springframework.data.elasticsearch.NoSuchIndexException;
import org.springframework.data.elasticsearch.UncategorizedElasticsearchException;
import org.springframework.data.elasticsearch.core.AbstractReactiveElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.AggregationContainer;
import org.springframework.data.elasticsearch.core.IndexedObjectInformation;
import org.springframework.data.elasticsearch.core.MultiGetItem;
import org.springframework.data.elasticsearch.core.ReactiveIndexOperations;
import org.springframework.data.elasticsearch.core.RefreshPolicy;
import org.springframework.data.elasticsearch.core.SearchHitMapping;
import org.springframework.data.elasticsearch.core.SearchHitSupport;
import org.springframework.data.elasticsearch.core.SearchPage;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.document.SearchDocument;
import org.springframework.data.elasticsearch.core.document.SearchDocumentResponse;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.BulkOptions;
import org.springframework.data.elasticsearch.core.query.ByQueryResponse;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.data.elasticsearch.core.query.UpdateResponse;
import org.springframework.data.elasticsearch.core.reindex.ReindexRequest;
import org.springframework.data.elasticsearch.core.reindex.ReindexResponse;
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
 * @author Aleksei Arsenev
 * @author Roman Puchkovskiy
 * @author Russell Parry
 * @author Thomas Geese
 * @author Farid Faoudi
 * @author Sijia Liu
 * @since 3.2
 * @deprecated since 5.0
 */
@Deprecated
public class ReactiveElasticsearchTemplate extends AbstractReactiveElasticsearchTemplate {

	private final ReactiveElasticsearchClient client;
	private final ElasticsearchExceptionTranslator exceptionTranslator;
	protected RequestFactory requestFactory;

	private @Nullable IndicesOptions indicesOptions = IndicesOptions.strictExpandOpenAndForbidClosedIgnoreThrottled();

	// region Initialization
	public ReactiveElasticsearchTemplate(ReactiveElasticsearchClient client) {
		this(client, null);
	}

	public ReactiveElasticsearchTemplate(ReactiveElasticsearchClient client, @Nullable ElasticsearchConverter converter) {

		super(converter);

		Assert.notNull(client, "client must not be null");

		this.client = client;
		this.exceptionTranslator = new ElasticsearchExceptionTranslator();
		this.requestFactory = new RequestFactory(this.converter);
	}

	protected ReactiveElasticsearchTemplate doCopy() {

		ReactiveElasticsearchTemplate copy = new ReactiveElasticsearchTemplate(client, converter);
		copy.setIndicesOptions(indicesOptions);
		return copy;
	}

	/**
	 * Set the default {@link IndicesOptions} for {@link SearchRequest search requests}.
	 *
	 * @param indicesOptions can be {@literal null}.
	 */
	public void setIndicesOptions(@Nullable IndicesOptions indicesOptions) {
		this.indicesOptions = indicesOptions;
	}

	// endregion

	// region DocumentOperations

	@Override
	public <T> Flux<T> saveAll(Mono<? extends Collection<? extends T>> entitiesPublisher, IndexCoordinates index) {

		Assert.notNull(entitiesPublisher, "entitiesPublisher must not be null!");

		return entitiesPublisher //
				.flatMapMany(entities -> Flux.fromIterable(entities) //
						.concatMap(entity -> maybeCallbackBeforeConvert(entity, index)) //
				).collectList() //
				.map(Entities::new) //
				.flatMapMany(entities -> {

					if (entities.isEmpty()) {
						return Flux.empty();
					}

					return doBulkOperation(entities.indexQueries(), BulkOptions.defaultOptions(), index) //
							.index() //
							.flatMap(indexAndResponse -> {
								T savedEntity = entities.entityAt(indexAndResponse.getT1());
								BulkItemResponse bulkItemResponse = indexAndResponse.getT2();

								DocWriteResponse response = bulkItemResponse.getResponse();
								updateIndexedObject(savedEntity, new IndexedObjectInformation(response.getId(), response.getIndex(),
										response.getSeqNo(), response.getPrimaryTerm(), response.getVersion()));

								return maybeCallbackAfterSave(savedEntity, index);
							});
				});
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

	@Override
	public Mono<Void> bulkUpdate(List<UpdateQuery> queries, BulkOptions bulkOptions, IndexCoordinates index) {

		Assert.notNull(queries, "List of UpdateQuery must not be null");
		Assert.notNull(bulkOptions, "BulkOptions must not be null");
		Assert.notNull(index, "Index must not be null");

		return doBulkOperation(queries, bulkOptions, index).then();
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

	protected Mono<Boolean> doExists(String id, IndexCoordinates index) {
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

	protected <T> Mono<Tuple2<T, IndexResponseMetaData>> doIndex(T entity, IndexCoordinates index) {

		IndexRequest request = requestFactory.indexRequest(getIndexQuery(entity), index);
		request = prepareIndexRequest(entity, request);
		return Mono.just(entity).zipWith(doIndex(request) //
				.map(indexResponse -> new IndexResponseMetaData( //
						indexResponse.getId(), //
						indexResponse.getIndex(), //
						indexResponse.getSeqNo(), //
						indexResponse.getPrimaryTerm(), //
						indexResponse.getVersion() //
				)));
	}

	@Override
	public <T> Mono<T> get(String id, Class<T> entityType, IndexCoordinates index) {

		Assert.notNull(id, "Id must not be null!");

		GetRequest request = requestFactory.getRequest(id, routingResolver.getRouting(), index);
		Mono<GetResult> getResult = doGet(request);

		DocumentCallback<T> callback = new ReadDocumentCallback<>(converter, entityType, index);
		return getResult.flatMap(response -> callback.toEntity(DocumentAdapters.from(response)));
	}

	/**
	 * Customization hook on the actual execution result {@link Publisher}. <br />
	 *
	 * @param request the already prepared {@link GetRequest} ready to be executed.
	 * @return a {@link Mono} emitting the result of the operation.
	 */
	protected Mono<GetResult> doGet(GetRequest request) {

		return Mono.from(execute(client -> client.get(request)));
	}

	protected Mono<String> doDeleteById(String id, @Nullable String routing, IndexCoordinates index) {

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
	public Mono<ReindexResponse> reindex(ReindexRequest postReindexRequest) {

		Assert.notNull(postReindexRequest, "postReindexRequest must not be null");

		return Mono.defer(() -> {
			org.elasticsearch.index.reindex.ReindexRequest reindexRequest = requestFactory.reindexRequest(postReindexRequest);
			return Mono.from(execute(client -> client.reindex(reindexRequest))).map(ResponseConverter::reindexResponseOf);
		});
	}

	@Override
	public Mono<String> submitReindex(ReindexRequest postReindexRequest) {

		Assert.notNull(postReindexRequest, "postReindexRequest must not be null");

		return Mono.defer(() -> {
			org.elasticsearch.index.reindex.ReindexRequest reindexRequest = requestFactory.reindexRequest(postReindexRequest);
			return Mono.from(execute(client -> client.submitReindex(reindexRequest)));
		});
	}

	protected Mono<BulkByScrollResponse> doDeleteBy(Query query, Class<?> entityType, IndexCoordinates index) {

		return Mono.defer(() -> {
			DeleteByQueryRequest request = requestFactory.deleteByQueryRequest(query, routingResolver.getRouting(),
					entityType, index);
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

	protected Flux<SearchDocument> doFind(Query query, Class<?> clazz, IndexCoordinates index) {

		return Flux.defer(() -> {

			SearchRequest request = requestFactory.searchRequest(query, routingResolver.getRouting(), clazz, index);
			boolean useScroll = !(query.getPageable().isPaged() || query.isLimiting());
			request = prepareSearchRequest(request, useScroll);

			if (useScroll) {
				return doScroll(request);
			} else {
				return doFind(request);
			}
		});
	}

	protected <T> Mono<SearchDocumentResponse> doFindForResponse(Query query, Class<?> clazz, IndexCoordinates index) {

		return Mono.defer(() -> {
			SearchRequest request = requestFactory.searchRequest(query, routingResolver.getRouting(), clazz, index);
			request = prepareSearchRequest(request, false);

			SearchDocumentCallback<?> documentCallback = new ReadSearchDocumentCallback<>(clazz, index);
			// noinspection unchecked
			SearchDocumentResponse.EntityCreator<T> entityCreator = searchDocument -> ((Mono<T>) documentCallback
					.toEntity(searchDocument)).toFuture();

			return doFindForResponse(request, entityCreator);
		});
	}

	@Override
	public Flux<AggregationContainer<?>> aggregate(Query query, Class<?> entityType, IndexCoordinates index) {

		Assert.notNull(query, "query must not be null");
		Assert.notNull(entityType, "entityType must not be null");
		Assert.notNull(index, "index must not be null");

		return Flux.defer(() -> {
			SearchRequest request = requestFactory.searchRequest(query, routingResolver.getRouting(), entityType, index);
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

	protected Mono<Long> doCount(Query query, Class<?> entityType, IndexCoordinates index) {
		return Mono.defer(() -> {

			SearchRequest request = requestFactory.searchRequest(query, routingResolver.getRouting(), entityType, index);
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
	protected <T> Mono<SearchDocumentResponse> doFindForResponse(SearchRequest request,
			SearchDocumentResponse.EntityCreator<T> entityCreator) {

		if (QUERY_LOGGER.isDebugEnabled()) {
			QUERY_LOGGER.debug(String.format("Executing doFindForResponse: %s", request));
		}

		return Mono.from(execute(client -> client.searchForResponse(request)))
				.map(searchResponse -> SearchDocumentResponseBuilder.from(searchResponse, entityCreator));
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

		return Mono.from(execute(client -> client.count(request)));
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
	@Override
	public Mono<String> getClusterVersion() {
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
	@Override
	public Mono<String> getVendor() {
		return Mono.just("Elasticsearch");
	}

	/**
	 * @return the version of the used client runtime library.
	 * @since 4.3
	 */
	@Override
	public Mono<String> getRuntimeLibraryVersion() {
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
}
