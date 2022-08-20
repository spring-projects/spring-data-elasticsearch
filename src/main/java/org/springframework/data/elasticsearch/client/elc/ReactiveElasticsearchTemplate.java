/*
 * Copyright 2021-2022 the original author or authors.
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
package org.springframework.data.elasticsearch.client.elc;

import static co.elastic.clients.util.ApiTypeHelper.*;
import static org.springframework.data.elasticsearch.client.elc.TypeUtils.*;

import co.elastic.clients.elasticsearch._types.Result;
import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.get.GetResult;
import co.elastic.clients.elasticsearch.core.search.ResponseBody;
import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.transport.Version;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.reactivestreams.Publisher;
import org.springframework.data.elasticsearch.BulkFailureException;
import org.springframework.data.elasticsearch.NoSuchIndexException;
import org.springframework.data.elasticsearch.UncategorizedElasticsearchException;
import org.springframework.data.elasticsearch.client.UnsupportedBackendOperation;
import org.springframework.data.elasticsearch.client.erhlc.ReactiveClusterOperations;
import org.springframework.data.elasticsearch.client.util.ScrollState;
import org.springframework.data.elasticsearch.core.AbstractReactiveElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.AggregationContainer;
import org.springframework.data.elasticsearch.core.IndexedObjectInformation;
import org.springframework.data.elasticsearch.core.MultiGetItem;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ReactiveIndexOperations;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.document.Document;
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
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * Implementation of {@link org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations} using the new
 * Elasticsearch client.
 *
 * @author Peter-Josef Meisch
 * @since 4.4
 */
public class ReactiveElasticsearchTemplate extends AbstractReactiveElasticsearchTemplate {

	private final ReactiveElasticsearchClient client;
	private final RequestConverter requestConverter;
	private final ResponseConverter responseConverter;
	private final JsonpMapper jsonpMapper;
	private final ElasticsearchExceptionTranslator exceptionTranslator;

	public ReactiveElasticsearchTemplate(ReactiveElasticsearchClient client, ElasticsearchConverter converter) {
		super(converter);

		Assert.notNull(client, "client must not be null");

		this.client = client;
		this.jsonpMapper = client._transport().jsonpMapper();
		requestConverter = new RequestConverter(converter, jsonpMapper);
		responseConverter = new ResponseConverter(jsonpMapper);
		exceptionTranslator = new ElasticsearchExceptionTranslator(jsonpMapper);
	}

	// region Document operations
	@Override
	protected <T> Mono<Tuple2<T, IndexResponseMetaData>> doIndex(T entity, IndexCoordinates index) {

		IndexRequest<?> indexRequest = requestConverter.documentIndexRequest(getIndexQuery(entity), index,
				getRefreshPolicy());
		return Mono.just(entity) //
				.zipWith(//
						Mono.from(execute((ClientCallback<Publisher<IndexResponse>>) client -> client.index(indexRequest))) //
								.map(indexResponse -> new IndexResponseMetaData(indexResponse.id(), //
										indexResponse.seqNo(), //
										indexResponse.primaryTerm(), //
										indexResponse.version() //
								)));
	}

	@Override
	public <T> Flux<T> saveAll(Mono<? extends Collection<? extends T>> entitiesPublisher, IndexCoordinates index) {

		Assert.notNull(entitiesPublisher, "entitiesPublisher must not be null!");

		return entitiesPublisher //
				.flatMapMany(entities -> Flux.fromIterable(entities) //
						.concatMap(entity -> maybeCallBeforeConvert(entity, index)) //
				).collectList() //
				.map(Entities::new) //
				.flatMapMany(entities -> {

					if (entities.isEmpty()) {
						return Flux.empty();
					}

					return doBulkOperation(entities.indexQueries(), BulkOptions.defaultOptions(), index)//
							.index() //
							.flatMap(indexAndResponse -> {
								T savedEntity = entities.entityAt(indexAndResponse.getT1());
								BulkResponseItem response = indexAndResponse.getT2();
								updateIndexedObject(savedEntity, IndexedObjectInformation.of(response.id(), response.seqNo(),
										response.primaryTerm(), response.version()));
								return maybeCallAfterSave(savedEntity, index);
							});
				});
	}

	@Override
	public <T> Mono<T> get(String id, Class<T> entityType, IndexCoordinates index) {

		Assert.notNull(id, "id must not be null");
		Assert.notNull(entityType, "entityType must not be null");
		Assert.notNull(index, "index must not be null");

		GetRequest getRequest = requestConverter.documentGetRequest(id, routingResolver.getRouting(), index, false);

		Mono<GetResponse<EntityAsMap>> getResponse = Mono.from(execute(
				(ClientCallback<Publisher<GetResponse<EntityAsMap>>>) client -> client.get(getRequest, EntityAsMap.class)));

		ReadDocumentCallback<T> callback = new ReadDocumentCallback<>(converter, entityType, index);
		return getResponse.flatMap(response -> callback.toEntity(DocumentAdapters.from(response)));
	}

	@Override
	public Mono<ReindexResponse> reindex(ReindexRequest reindexRequest) {

		Assert.notNull(reindexRequest, "reindexRequest must not be null");

		co.elastic.clients.elasticsearch.core.ReindexRequest reindexRequestES = requestConverter.reindex(reindexRequest,
				true);

		return Mono.from(execute( //
				(ClientCallback<Publisher<co.elastic.clients.elasticsearch.core.ReindexResponse>>) client -> client
						.reindex(reindexRequestES)))
				.map(responseConverter::reindexResponse);
	}

	@Override
	public Mono<String> submitReindex(ReindexRequest reindexRequest) {

		Assert.notNull(reindexRequest, "reindexRequest must not be null");

		co.elastic.clients.elasticsearch.core.ReindexRequest reindexRequestES = requestConverter.reindex(reindexRequest,
				false);

		return Mono.from(execute( //
				(ClientCallback<Publisher<co.elastic.clients.elasticsearch.core.ReindexResponse>>) client -> client
						.reindex(reindexRequestES)))
				.flatMap(response -> (response.task() == null)
						? Mono.error(
								new UnsupportedBackendOperation("ElasticsearchClient did not return a task id on submit request"))
						: Mono.just(response.task()));
	}

	@Override
	public Mono<Void> bulkUpdate(List<UpdateQuery> queries, BulkOptions bulkOptions, IndexCoordinates index) {

		Assert.notNull(queries, "List of UpdateQuery must not be null");
		Assert.notNull(bulkOptions, "BulkOptions must not be null");
		Assert.notNull(index, "Index must not be null");

		return doBulkOperation(queries, bulkOptions, index).then();
	}

	private Flux<BulkResponseItem> doBulkOperation(List<?> queries, BulkOptions bulkOptions, IndexCoordinates index) {

		BulkRequest bulkRequest = requestConverter.documentBulkRequest(queries, bulkOptions, index, getRefreshPolicy());
		return client.bulk(bulkRequest)
				.onErrorMap(e -> new UncategorizedElasticsearchException("Error executing bulk request", e))
				.flatMap(this::checkForBulkOperationFailure) //
				.flatMapMany(response -> Flux.fromIterable(response.items()));

	}

	private Mono<BulkResponse> checkForBulkOperationFailure(BulkResponse bulkResponse) {

		if (bulkResponse.errors()) {
			Map<String, String> failedDocuments = new HashMap<>();

			for (BulkResponseItem item : bulkResponse.items()) {

				if (item.error() != null) {
					failedDocuments.put(item.id(), item.error().reason());
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
	protected Mono<String> doDeleteById(String id, @Nullable String routing, IndexCoordinates index) {

		Assert.notNull(id, "id must not be null");
		Assert.notNull(index, "index must not be null");

		return Mono.defer(() -> {
			DeleteRequest deleteRequest = requestConverter.documentDeleteRequest(id, routing, index, getRefreshPolicy());
			return doDelete(deleteRequest);
		});
	}

	private Mono<String> doDelete(DeleteRequest request) {

		return Mono.from(execute((ClientCallback<Publisher<DeleteResponse>>) client -> client.delete(request))) //
				.flatMap(deleteResponse -> {
					if (deleteResponse.result() == Result.NotFound) {
						return Mono.empty();
					}
					return Mono.just(deleteResponse.id());
				}).onErrorResume(NoSuchIndexException.class, it -> Mono.empty());
	}

	@Override
	public <T> Flux<MultiGetItem<T>> multiGet(Query query, Class<T> clazz, IndexCoordinates index) {

		Assert.notNull(query, "query must not be null");
		Assert.notNull(clazz, "clazz must not be null");

		MgetRequest request = requestConverter.documentMgetRequest(query, clazz, index);

		ReadDocumentCallback<T> callback = new ReadDocumentCallback<>(converter, clazz, index);

		Publisher<MgetResponse<EntityAsMap>> response = execute(
				(ClientCallback<Publisher<MgetResponse<EntityAsMap>>>) client -> client.mget(request, EntityAsMap.class));

		return Mono.from(response)//
				.flatMapMany(it -> Flux.fromIterable(DocumentAdapters.from(it))) //
				.flatMap(multiGetItem -> {
					if (multiGetItem.isFailed()) {
						return Mono.just(MultiGetItem.of(null, multiGetItem.getFailure()));
					} else {
						return callback.toEntity(multiGetItem.getItem()) //
								.map(t -> MultiGetItem.of(t, multiGetItem.getFailure()));
					}
				});
	}

	// endregion

	@Override
	protected ReactiveElasticsearchTemplate doCopy() {
		return new ReactiveElasticsearchTemplate(client, converter);
	}

	@Override
	protected Mono<Boolean> doExists(String id, IndexCoordinates index) {

		Assert.notNull(id, "id must not be null");
		Assert.notNull(index, "index must not be null");

		GetRequest getRequest = requestConverter.documentGetRequest(id, routingResolver.getRouting(), index, true);

		return Mono.from(execute(
				((ClientCallback<Publisher<GetResponse<EntityAsMap>>>) client -> client.get(getRequest, EntityAsMap.class))))
				.map(GetResult::found) //
				.onErrorReturn(NoSuchIndexException.class, false);
	}

	@Override
	public Mono<ByQueryResponse> delete(Query query, Class<?> entityType, IndexCoordinates index) {

		Assert.notNull(query, "query must not be null");

		DeleteByQueryRequest request = requestConverter.documentDeleteByQueryRequest(query, entityType, index,
				getRefreshPolicy());
		return Mono
				.from(execute((ClientCallback<Publisher<DeleteByQueryResponse>>) client -> client.deleteByQuery(request)))
				.map(responseConverter::byQueryResponse);
	}

	// region search operations

	@Override
	protected Flux<SearchDocument> doFind(Query query, Class<?> clazz, IndexCoordinates index) {

		return Flux.defer(() -> {
			boolean useScroll = !(query.getPageable().isPaged() || query.isLimiting());
			SearchRequest searchRequest = requestConverter.searchRequest(query, clazz, index, false, useScroll);

			if (useScroll) {
				return doScroll(searchRequest);
			} else {
				return doFind(searchRequest);
			}
		});

	}

	private Flux<SearchDocument> doScroll(SearchRequest searchRequest) {

		Time scrollTimeout = searchRequest.scroll() != null ? searchRequest.scroll() : Time.of(t -> t.time("1m"));

		Flux<ResponseBody<EntityAsMap>> searchResponses = Flux.usingWhen(Mono.fromSupplier(ScrollState::new), //
				state -> Mono
						.from(execute((ClientCallback<Publisher<ResponseBody<EntityAsMap>>>) client1 -> client1
								.search(searchRequest, EntityAsMap.class))) //
						.expand(entityAsMapSearchResponse -> {

							state.updateScrollId(entityAsMapSearchResponse.scrollId());

							if (entityAsMapSearchResponse.hits() == null
									|| CollectionUtils.isEmpty(entityAsMapSearchResponse.hits().hits())) {
								return Mono.empty();
							}

							return Mono.from(execute((ClientCallback<Publisher<ScrollResponse<EntityAsMap>>>) client1 -> {
								ScrollRequest scrollRequest = ScrollRequest
										.of(sr -> sr.scrollId(state.getScrollId()).scroll(scrollTimeout));
								return client1.scroll(scrollRequest, EntityAsMap.class);
							}));
						}),
				this::cleanupScroll, (state, ex) -> cleanupScroll(state), this::cleanupScroll);

		return searchResponses.flatMapIterable(entityAsMapSearchResponse -> entityAsMapSearchResponse.hits().hits())
				.map(entityAsMapHit -> DocumentAdapters.from(entityAsMapHit, jsonpMapper));
	}

	private Publisher<?> cleanupScroll(ScrollState state) {

		return execute((ClientCallback<Publisher<ClearScrollResponse>>) client -> client
				.clearScroll(ClearScrollRequest.of(csr -> csr.scrollId(state.getScrollIds()))));
	}

	@Override
	protected Mono<Long> doCount(Query query, Class<?> entityType, IndexCoordinates index) {

		Assert.notNull(query, "query must not be null");
		Assert.notNull(index, "index must not be null");

		SearchRequest searchRequest = requestConverter.searchRequest(query, entityType, index, true, false);

		return Mono
				.from(execute((ClientCallback<Publisher<ResponseBody<EntityAsMap>>>) client -> client.search(searchRequest,
						EntityAsMap.class)))
				.map(searchResponse -> searchResponse.hits().total() != null ? searchResponse.hits().total().value() : 0L);
	}

	private Flux<SearchDocument> doFind(SearchRequest searchRequest) {

		return Mono
				.from(execute((ClientCallback<Publisher<ResponseBody<EntityAsMap>>>) client -> client.search(searchRequest,
						EntityAsMap.class))) //
				.flatMapIterable(entityAsMapSearchResponse -> entityAsMapSearchResponse.hits().hits()) //
				.map(entityAsMapHit -> DocumentAdapters.from(entityAsMapHit, jsonpMapper));
	}

	@Override
	protected <T> Mono<SearchDocumentResponse> doFindForResponse(Query query, Class<?> clazz, IndexCoordinates index) {

		Assert.notNull(query, "query must not be null");
		Assert.notNull(index, "index must not be null");

		SearchRequest searchRequest = requestConverter.searchRequest(query, clazz, index, false, false);

		// noinspection unchecked
		SearchDocumentCallback<T> callback = new ReadSearchDocumentCallback<>((Class<T>) clazz, index);
		SearchDocumentResponse.EntityCreator<T> entityCreator = searchDocument -> callback.toEntity(searchDocument)
				.toFuture();

		return Mono
				.from(execute((ClientCallback<Publisher<ResponseBody<EntityAsMap>>>) client -> client.search(searchRequest,
						EntityAsMap.class)))
				.map(searchResponse -> SearchDocumentResponseBuilder.from(searchResponse, entityCreator, jsonpMapper));
	}

	@Override
	public Flux<? extends AggregationContainer<?>> aggregate(Query query, Class<?> entityType, IndexCoordinates index) {

		return doFindForResponse(query, entityType, index).flatMapMany(searchDocumentResponse -> {
			ElasticsearchAggregations aggregations = (ElasticsearchAggregations) searchDocumentResponse.getAggregations();
			return aggregations == null ? Flux.empty() : Flux.fromIterable(aggregations.aggregations());
		});
	}

	@Override
	public Mono<String> openPointInTime(IndexCoordinates index, Duration keepAlive, Boolean ignoreUnavailable) {

		Assert.notNull(index, "index must not be null");
		Assert.notNull(keepAlive, "keepAlive must not be null");
		Assert.notNull(ignoreUnavailable, "ignoreUnavailable must not be null");

		var request = requestConverter.searchOpenPointInTimeRequest(index, keepAlive, ignoreUnavailable);
		return Mono
				.from(execute((ClientCallback<Publisher<OpenPointInTimeResponse>>) client -> client.openPointInTime(request)))
				.map(OpenPointInTimeResponse::id);
	}

	@Override
	public Mono<Boolean> closePointInTime(String pit) {

		Assert.notNull(pit, "pit must not be null");

		ClosePointInTimeRequest request = requestConverter.searchClosePointInTime(pit);
		return Mono
				.from(execute((ClientCallback<Publisher<ClosePointInTimeResponse>>) client -> client.closePointInTime(request)))
				.map(ClosePointInTimeResponse::succeeded);
	}

	// endregion

	@Override
	public Mono<String> getVendor() {
		return Mono.just("Elasticsearch");
	}

	@Override
	public Mono<String> getRuntimeLibraryVersion() {
		return Mono.just(Version.VERSION != null ? Version.VERSION.toString() : "null");
	}

	@Override
	public Mono<String> getClusterVersion() {
		return Mono.from(execute((ReactiveElasticsearchClient reactiveElasticsearchClient) -> {
			try (var ignored = DANGEROUS_disableRequiredPropertiesCheck(true)) {
				return reactiveElasticsearchClient.info();
			}
		})).map(infoResponse -> infoResponse.version().number());
	}

	@Override
	public Mono<UpdateResponse> update(UpdateQuery updateQuery, IndexCoordinates index) {

		Assert.notNull(updateQuery, "UpdateQuery must not be null");
		Assert.notNull(index, "Index must not be null");

		UpdateRequest<Document, ?> request = requestConverter.documentUpdateRequest(updateQuery, index, getRefreshPolicy(),
				routingResolver.getRouting());

		return Mono.from(execute(
				(ClientCallback<Publisher<co.elastic.clients.elasticsearch.core.UpdateResponse<Document>>>) client -> client
						.update(request, Document.class)))
				.flatMap(response -> {
					UpdateResponse.Result result = result(response.result());
					return result == null ? Mono.empty() : Mono.just(UpdateResponse.of(result));
				});
	}

	@Override
	public Mono<ByQueryResponse> updateByQuery(UpdateQuery updateQuery, IndexCoordinates index) {
		throw new UnsupportedOperationException("not implemented");
	}

	@Override
	@Deprecated
	public <T> Publisher<T> execute(ReactiveElasticsearchOperations.ClientCallback<Publisher<T>> callback) {
		throw new UnsupportedBackendOperation("direct execution on the WebClient is not supported for this class");
	}

	@Override
	public <T> Publisher<T> executeWithIndicesClient(IndicesClientCallback<Publisher<T>> callback) {
		throw new UnsupportedOperationException("not implemented");
	}

	@Override
	public <T> Publisher<T> executeWithClusterClient(ClusterClientCallback<Publisher<T>> callback) {
		throw new UnsupportedOperationException("not implemented");
	}

	@Override
	public ReactiveIndexOperations indexOps(IndexCoordinates index) {
		return new ReactiveIndicesTemplate(client.indices(), converter, index);
	}

	@Override
	public ReactiveIndexOperations indexOps(Class<?> clazz) {
		return new ReactiveIndicesTemplate(client.indices(), converter, clazz);
	}

	@Override
	public ReactiveClusterOperations cluster() {
		return new ReactiveClusterTemplate(client.cluster(), converter);
	}

	@Override
	public Query matchAllQuery() {
		return NativeQuery.builder().withQuery(QueryBuilders.matchAllQueryAsQuery()).build();
	}

	@Override
	public Query idsQuery(List<String> ids) {
		return NativeQuery.builder().withQuery(QueryBuilders.idsQueryAsQuery(ids)).build();
	}

	/**
	 * Callback interface to be used with {@link #execute(ReactiveElasticsearchOperations.ClientCallback)} for operating
	 * directly on {@link ReactiveElasticsearchClient}.
	 *
	 * @param <T>
	 */
	interface ClientCallback<T extends Publisher<?>> {

		T doWithClient(ReactiveElasticsearchClient client);
	}

	/**
	 * Execute a callback with the {@link ReactiveElasticsearchClient} and provide exception translation.
	 *
	 * @param callback the callback to execute, must not be {@literal null}
	 * @param <T> the type returned from the callback
	 * @return the callback result
	 */
	public <T> Publisher<T> execute(ReactiveElasticsearchTemplate.ClientCallback<Publisher<T>> callback) {
		return Flux.defer(() -> callback.doWithClient(client)).onErrorMap(this::translateException);
	}

	/**
	 * translates an Exception if possible. Exceptions that are no {@link RuntimeException}s are wrapped in a
	 * RuntimeException
	 *
	 * @param throwable the Exception to map
	 * @return the potentially translated RuntimeException.
	 */
	private RuntimeException translateException(Throwable throwable) {

		RuntimeException runtimeException = throwable instanceof RuntimeException ? (RuntimeException) throwable
				: new RuntimeException(throwable.getMessage(), throwable);
		RuntimeException potentiallyTranslatedException = exceptionTranslator
				.translateExceptionIfPossible(runtimeException);

		return potentiallyTranslatedException != null ? potentiallyTranslatedException : runtimeException;
	}

}
