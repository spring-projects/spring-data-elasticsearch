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

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.http.util.EntityUtils;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.admin.indices.close.CloseIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.flush.FlushRequest;
import org.elasticsearch.action.admin.indices.flush.FlushResponse;
import org.elasticsearch.action.admin.indices.open.OpenIndexRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshResponse;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsRequest;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsResponse;
import org.elasticsearch.action.admin.indices.template.delete.DeleteIndexTemplateRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetItemResponse;
import org.elasticsearch.action.get.MultiGetRequest;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.main.MainRequest;
import org.elasticsearch.action.main.MainResponse;
import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.action.search.ClearScrollResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.GetAliasesResponse;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.indices.*;
import org.elasticsearch.client.tasks.TaskSubmissionResponse;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.index.get.GetResult;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.index.reindex.ReindexRequest;
import org.elasticsearch.index.reindex.UpdateByQueryRequest;
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.script.mustache.SearchTemplateRequest;
import org.elasticsearch.script.mustache.SearchTemplateResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.xcontent.DeprecationHandler;
import org.elasticsearch.xcontent.NamedXContentRegistry;
import org.elasticsearch.xcontent.XContentParser;
import org.elasticsearch.xcontent.XContentType;
import org.reactivestreams.Publisher;
import org.springframework.data.elasticsearch.RestStatusException;
import org.springframework.data.elasticsearch.UncategorizedElasticsearchException;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.ClientLogger;
import org.springframework.data.elasticsearch.client.ElasticsearchHost;
import org.springframework.data.elasticsearch.client.NoReachableHostException;
import org.springframework.data.elasticsearch.client.erhlc.HostProvider.Verification;
import org.springframework.data.elasticsearch.client.erhlc.ReactiveElasticsearchClient.Cluster;
import org.springframework.data.elasticsearch.client.erhlc.ReactiveElasticsearchClient.Indices;
import org.springframework.data.elasticsearch.client.util.ScrollState;
import org.springframework.data.elasticsearch.core.query.ByQueryResponse;
import org.springframework.data.util.Lazy;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;

/**
 * A {@link WebClient} based {@link ReactiveElasticsearchClient} that connects to an Elasticsearch cluster using HTTP.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @author Peter-Josef Meisch
 * @author Huw Ayling-Miller
 * @author Henrique Amaral
 * @author Roman Puchkovskiy
 * @author Russell Parry
 * @author Thomas Geese
 * @author Brian Clozel
 * @author Farid Faoudi
 * @author George Popides
 * @author Sijia Liu
 * @since 3.2
 * @see ClientConfiguration
 * @see ReactiveRestClients
 * @deprecated since 5.0
 */
@Deprecated
public class DefaultReactiveElasticsearchClient implements ReactiveElasticsearchClient, Indices, Cluster {

	private final HostProvider<?> hostProvider;
	private final RequestCreator requestCreator;
	private Supplier<org.springframework.data.elasticsearch.support.HttpHeaders> headersSupplier = org.springframework.data.elasticsearch.support.HttpHeaders::new;

	/**
	 * Create a new {@link DefaultReactiveElasticsearchClient} using the given {@link HostProvider} to obtain server
	 * connections.
	 *
	 * @param hostProvider must not be {@literal null}.
	 */
	public DefaultReactiveElasticsearchClient(HostProvider<?> hostProvider) {
		this(hostProvider, new DefaultRequestCreator());
	}

	/**
	 * Create a new {@link DefaultReactiveElasticsearchClient} using the given {@link HostProvider} to obtain server
	 * connections and the given {@link RequestCreator}.
	 *
	 * @param hostProvider must not be {@literal null}.
	 * @param requestCreator must not be {@literal null}.
	 */
	public DefaultReactiveElasticsearchClient(HostProvider<?> hostProvider, RequestCreator requestCreator) {

		Assert.notNull(hostProvider, "HostProvider must not be null");
		Assert.notNull(requestCreator, "RequestCreator must not be null");

		this.hostProvider = hostProvider;
		this.requestCreator = requestCreator;
	}

	/**
	 * Create a new {@link DefaultReactiveElasticsearchClient} aware of the given nodes in the cluster. <br />
	 * <strong>NOTE</strong> If the cluster requires authentication be sure to provide the according {@link HttpHeaders}
	 * correctly.
	 *
	 * @param headers Use {@link HttpHeaders} to provide eg. authentication data. Must not be {@literal null}.
	 * @param hosts must not be {@literal null} nor empty!
	 * @return new instance of {@link DefaultReactiveElasticsearchClient}.
	 */
	public static ReactiveElasticsearchClient create(HttpHeaders headers, String... hosts) {

		Assert.notNull(headers, "HttpHeaders must not be null");
		Assert.notEmpty(hosts, "Elasticsearch Cluster needs to consist of at least one host");

		var httpHeaders = new org.springframework.data.elasticsearch.support.HttpHeaders();
		httpHeaders.addAll(headers);
		ClientConfiguration clientConfiguration = ClientConfiguration.builder().connectedTo(hosts)
				.withDefaultHeaders(httpHeaders).build();
		return create(clientConfiguration);
	}

	/**
	 * Create a new {@link DefaultReactiveElasticsearchClient} given {@link ClientConfiguration}. <br />
	 * <strong>NOTE</strong> If the cluster requires authentication be sure to provide the according {@link HttpHeaders}
	 * correctly.
	 *
	 * @param clientConfiguration Client configuration. Must not be {@literal null}.
	 * @return new instance of {@link DefaultReactiveElasticsearchClient}.
	 */
	public static ReactiveElasticsearchClient create(ClientConfiguration clientConfiguration) {
		return create(clientConfiguration, new DefaultRequestCreator());
	}

	/**
	 * Create a new {@link DefaultReactiveElasticsearchClient} given {@link ClientConfiguration} and
	 * {@link RequestCreator}. <br />
	 * <strong>NOTE</strong> If the cluster requires authentication be sure to provide the according {@link HttpHeaders}
	 * correctly.
	 *
	 * @param clientConfiguration Client configuration. Must not be {@literal null}.
	 * @param requestCreator Request creator. Must not be {@literal null}.
	 * @return new instance of {@link DefaultReactiveElasticsearchClient}.
	 */
	public static ReactiveElasticsearchClient create(ClientConfiguration clientConfiguration,
			RequestCreator requestCreator) {

		Assert.notNull(clientConfiguration, "ClientConfiguration must not be null");
		Assert.notNull(requestCreator, "RequestCreator must not be null");

		WebClientProvider provider = WebClientProvider.getWebClientProvider(clientConfiguration);

		HostProvider<?> hostProvider = HostProvider.provider(provider, clientConfiguration.getHeadersSupplier(),
				clientConfiguration.getEndpoints().toArray(new InetSocketAddress[0]));

		DefaultReactiveElasticsearchClient client = new DefaultReactiveElasticsearchClient(hostProvider, requestCreator);

		client.setHeadersSupplier(clientConfiguration.getHeadersSupplier());

		return client;
	}

	public void setHeadersSupplier(Supplier<org.springframework.data.elasticsearch.support.HttpHeaders> headersSupplier) {

		Assert.notNull(headersSupplier, "headersSupplier must not be null");

		this.headersSupplier = headersSupplier;
	}

	@Override
	public Mono<Boolean> ping(HttpHeaders headers) {

		return sendRequest(new MainRequest(), requestCreator.ping(), RawActionResponse.class, headers) //
				.flatMap(response -> response.releaseBody().thenReturn(response.statusCode().is2xxSuccessful())) //
				.onErrorResume(NoReachableHostException.class, error -> Mono.just(false)).next();
	}

	@Override
	public Mono<MainResponse> info(HttpHeaders headers) {

		return sendRequest(new MainRequest(), requestCreator.info(), MainResponse.class, headers) //
				.next();
	}

	@Override
	public Mono<GetResult> get(HttpHeaders headers, GetRequest getRequest) {

		return sendRequest(getRequest, requestCreator.get(), GetResponse.class, headers) //
				.filter(GetResponse::isExists) //
				.map(DefaultReactiveElasticsearchClient::getResponseToGetResult) //
				.next();
	}

	@Override
	public Flux<MultiGetItemResponse> multiGet(HttpHeaders headers, MultiGetRequest multiGetRequest) {

		return sendRequest(multiGetRequest, requestCreator.multiGet(), MultiGetResponse.class, headers)
				.map(MultiGetResponse::getResponses) //
				.flatMap(Flux::fromArray); //
	}

	@Override
	public Mono<Boolean> exists(HttpHeaders headers, GetRequest getRequest) {

		return sendRequest(getRequest, requestCreator.exists(), RawActionResponse.class, headers) //
				.flatMap(response -> response.releaseBody().thenReturn(response.statusCode().is2xxSuccessful())) //
				.next();
	}

	@Override
	public Mono<IndexResponse> index(HttpHeaders headers, IndexRequest indexRequest) {
		return sendRequest(indexRequest, requestCreator.index(), IndexResponse.class, headers).next();
	}

	@Override
	public Indices indices() {
		return this;
	}

	@Override
	public Cluster cluster() {
		return this;
	}

	@Override
	public Mono<UpdateResponse> update(HttpHeaders headers, UpdateRequest updateRequest) {
		return sendRequest(updateRequest, requestCreator.update(), UpdateResponse.class, headers).next();
	}

	@Override
	public Mono<DeleteResponse> delete(HttpHeaders headers, DeleteRequest deleteRequest) {

		return sendRequest(deleteRequest, requestCreator.delete(), DeleteResponse.class, headers) //
				.next();
	}

	@Override
	public Mono<Long> count(HttpHeaders headers, SearchRequest searchRequest) {
		searchRequest.source().trackTotalHits(true);
		searchRequest.source().size(0);
		searchRequest.source().fetchSource(false);
		return sendRequest(searchRequest, requestCreator.search(), SearchResponse.class, headers) //
				.map(SearchResponse::getHits) //
				.map(searchHits -> searchHits.getTotalHits().value) //
				.next();
	}

	@Override
	public Flux<SearchHit> searchTemplate(HttpHeaders headers, SearchTemplateRequest searchTemplateRequest) {
		return sendRequest(searchTemplateRequest, requestCreator.searchTemplate(), SearchTemplateResponse.class, headers)
				.map(response -> response.getResponse().getHits()).flatMap(Flux::fromIterable);
	}

	@Override
	public Flux<SearchHit> search(HttpHeaders headers, SearchRequest searchRequest) {

		return sendRequest(searchRequest, requestCreator.search(), SearchResponse.class, headers) //
				.map(SearchResponse::getHits) //
				.flatMap(Flux::fromIterable);
	}

	@Override
	public Mono<SearchResponse> searchForResponse(HttpHeaders headers, SearchRequest searchRequest) {
		return sendRequest(searchRequest, requestCreator.search(), SearchResponse.class, headers).next();
	}

	@Override
	public Flux<Suggest> suggest(HttpHeaders headers, SearchRequest searchRequest) {
		return sendRequest(searchRequest, requestCreator.search(), SearchResponse.class, headers) //
				.map(SearchResponse::getSuggest);
	}

	@Override
	public Flux<Aggregation> aggregate(HttpHeaders headers, SearchRequest searchRequest) {

		Assert.notNull(headers, "headers must not be null");
		Assert.notNull(searchRequest, "searchRequest must not be null");

		searchRequest.source().size(0);
		searchRequest.source().trackTotalHits(false);

		return sendRequest(searchRequest, requestCreator.search(), SearchResponse.class, headers) //
				.map(SearchResponse::getAggregations) //
				.flatMap(Flux::fromIterable);
	}

	@Override
	public Flux<SearchHit> scroll(HttpHeaders headers, SearchRequest searchRequest) {

		TimeValue scrollTimeout = searchRequest.scroll() != null ? searchRequest.scroll().keepAlive()
				: TimeValue.timeValueMinutes(1);

		if (searchRequest.scroll() == null) {
			searchRequest.scroll(scrollTimeout);
		}

		return Flux.usingWhen(Mono.fromSupplier(ScrollState::new),

				state -> sendRequest(searchRequest, requestCreator.search(), SearchResponse.class, headers)
						.expand(searchResponse -> {

							state.updateScrollId(searchResponse.getScrollId());
							if (isEmpty(searchResponse.getHits())) {
								return Mono.empty();
							}

							return sendRequest(new SearchScrollRequest(searchResponse.getScrollId()).scroll(scrollTimeout),
									requestCreator.scroll(), SearchResponse.class, headers);

						}),
				state -> cleanupScroll(headers, state), //
				(state, ex) -> cleanupScroll(headers, state), //
				state -> cleanupScroll(headers, state)) //
				.filter(it -> !isEmpty(it.getHits())) //
				.map(SearchResponse::getHits) //
				.flatMapIterable(Function.identity()); //
	}

	private static boolean isEmpty(@Nullable SearchHits hits) {
		return hits != null && hits.getHits() != null && hits.getHits().length == 0;
	}

	private Publisher<?> cleanupScroll(HttpHeaders headers, ScrollState state) {

		if (state.getScrollIds().isEmpty()) {
			return Mono.empty();
		}

		ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
		clearScrollRequest.scrollIds(state.getScrollIds());

		// just send the request, resources get cleaned up anyways after scrollTimeout has been reached.
		return sendRequest(clearScrollRequest, requestCreator.clearScroll(), ClearScrollResponse.class, headers);
	}

	@Override
	public Mono<BulkByScrollResponse> deleteBy(HttpHeaders headers, DeleteByQueryRequest deleteRequest) {

		return sendRequest(deleteRequest, requestCreator.deleteByQuery(), BulkByScrollResponse.class, headers) //
				.next();
	}

	@Override
	public Mono<ByQueryResponse> updateBy(HttpHeaders headers, UpdateByQueryRequest updateRequest) {
		return sendRequest(updateRequest, requestCreator.updateByQuery(), BulkByScrollResponse.class, headers) //
				.next() //
				.map(ResponseConverter::byQueryResponseOf);
	}

	@Override
	public Mono<BulkResponse> bulk(HttpHeaders headers, BulkRequest bulkRequest) {
		return sendRequest(bulkRequest, requestCreator.bulk(), BulkResponse.class, headers) //
				.next();
	}

	@Override
	public Mono<BulkByScrollResponse> reindex(HttpHeaders headers, ReindexRequest reindexRequest) {
		return sendRequest(reindexRequest, requestCreator.reindex(), BulkByScrollResponse.class, headers).next();
	}

	@Override
	public Mono<String> submitReindex(HttpHeaders headers, ReindexRequest reindexRequest) {
		return sendRequest(reindexRequest, requestCreator.submitReindex(), TaskSubmissionResponse.class, headers).next()
				.map(TaskSubmissionResponse::getTask);
	}

	@Override
	public <T> Mono<T> execute(ReactiveElasticsearchClientCallback<T> callback) {

		return this.hostProvider.getActive(Verification.LAZY) //
				.flatMap(callback::doWithClient) //
				.onErrorResume(throwable -> {

					if (isCausedByConnectionException(throwable)) {
						return hostProvider.getActive(Verification.ACTIVE) //
								.flatMap(callback::doWithClient);
					}

					return Mono.error(throwable);
				});
	}

	/**
	 * checks if the given throwable is a {@link ConnectException} or has one in it's cause chain
	 *
	 * @param throwable the throwable to check
	 * @return true if throwable is caused by a {@link ConnectException}
	 */
	private boolean isCausedByConnectionException(Throwable throwable) {

		Throwable t = throwable;
		do {

			if (t instanceof ConnectException) {
				return true;
			}

			t = t.getCause();
		} while (t != null);

		return false;
	}

	@Override
	public Mono<Status> status() {

		return hostProvider.clusterInfo() //
				.map(it -> new ClientStatus(it.getNodes()));
	}

	// --> Private Response helpers

	private static GetResult getResponseToGetResult(GetResponse response) {

		return new GetResult(response.getIndex(), response.getType(), response.getId(), response.getSeqNo(),
				response.getPrimaryTerm(), response.getVersion(), response.isExists(), response.getSourceAsBytesRef(),
				response.getFields(), null);
	}

	// -->

	private <REQ, RESP> Flux<RESP> sendRequest(REQ request, Function<REQ, Request> converter, Class<RESP> responseType,
			HttpHeaders headers) {
		return sendRequest(converter.apply(request), responseType, headers);
	}

	private <Resp> Flux<Resp> sendRequest(Request request, Class<Resp> responseType, HttpHeaders headers) {

		String logId = ClientLogger.newLogId();

		return Flux
				.from(execute(webClient -> sendRequest(webClient, logId, request, headers).exchangeToMono(clientResponse -> {
					Publisher<? extends Resp> publisher = readResponseBody(logId, request, clientResponse, responseType);
					return Mono.from(publisher);
				})));
	}

	private RequestBodySpec sendRequest(WebClient webClient, String logId, Request request, HttpHeaders headers) {

		RequestBodySpec requestBodySpec = webClient.method(HttpMethod.valueOf(request.getMethod().toUpperCase())) //
				.uri(builder -> {

					builder = builder.path(request.getEndpoint());

					if (!ObjectUtils.isEmpty(request.getParameters())) {
						for (Entry<String, String> entry : request.getParameters().entrySet()) {
							builder = builder.queryParam(entry.getKey(), entry.getValue());
						}
					}
					return builder.build();
				}) //
				.attribute(ClientRequest.LOG_ID_ATTRIBUTE, logId) //
				.headers(theHeaders -> {

					// add all the headers explicitly set
					theHeaders.addAll(headers);

					// and now those that might be set on the request.
					if (request.getOptions() != null) {

						if (!ObjectUtils.isEmpty(request.getOptions().getHeaders())) {
							request.getOptions().getHeaders().forEach(it -> theHeaders.add(it.getName(), it.getValue()));
						}
					}
				});

		if (request.getEntity() != null) {

			Lazy<String> body = bodyExtractor(request);

			ClientLogger.logRequest(logId, request.getMethod().toUpperCase(), request.getEndpoint(), request.getParameters(),
					body::get);

			requestBodySpec.contentType(MediaType.valueOf(request.getEntity().getContentType().getValue()))
					.body(Mono.fromSupplier(body), String.class);
		} else {
			ClientLogger.logRequest(logId, request.getMethod().toUpperCase(), request.getEndpoint(), request.getParameters());
		}

		return requestBodySpec;
	}

	// region indices operations
	@Override
	public Mono<Boolean> createIndex(HttpHeaders headers,
			org.elasticsearch.action.admin.indices.create.CreateIndexRequest createIndexRequest) {

		return sendRequest(createIndexRequest, requestCreator.indexCreate(), AcknowledgedResponse.class, headers) //
				.map(AcknowledgedResponse::isAcknowledged) //
				.next();
	}

	@Override
	public Mono<Boolean> createIndex(HttpHeaders headers, CreateIndexRequest createIndexRequest) {

		return sendRequest(createIndexRequest, requestCreator.createIndexRequest(), AcknowledgedResponse.class, headers) //
				.map(AcknowledgedResponse::isAcknowledged) //
				.next();
	}

	@Override
	public Mono<Void> closeIndex(HttpHeaders headers, CloseIndexRequest closeIndexRequest) {

		return sendRequest(closeIndexRequest, requestCreator.indexClose(), AcknowledgedResponse.class, headers) //
				.then();
	}

	@Override
	public Mono<Boolean> existsIndex(HttpHeaders headers,
			org.elasticsearch.action.admin.indices.get.GetIndexRequest request) {

		return sendRequest(request, requestCreator.indexExists(), RawActionResponse.class, headers) //
				.flatMap(response -> response.releaseBody().thenReturn(response.statusCode().is2xxSuccessful())) //
				.next();
	}

	@Override
	public Mono<Boolean> existsIndex(HttpHeaders headers, GetIndexRequest request) {
		return sendRequest(request, requestCreator.indexExistsRequest(), RawActionResponse.class, headers) //
				.flatMap(response -> response.releaseBody().thenReturn(response.statusCode().is2xxSuccessful())) //
				.next();
	}

	@Override
	public Mono<Boolean> deleteIndex(HttpHeaders headers, DeleteIndexRequest request) {

		return sendRequest(request, requestCreator.indexDelete(), AcknowledgedResponse.class, headers) //
				.map(AcknowledgedResponse::isAcknowledged) //
				.next();
	}

	@Override
	public Mono<Void> flushIndex(HttpHeaders headers, FlushRequest flushRequest) {

		return sendRequest(flushRequest, requestCreator.flushIndex(), FlushResponse.class, headers) //
				.then();
	}

	@Override
	@Deprecated
	public Mono<org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse> getMapping(HttpHeaders headers,
			org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequest getMappingsRequest) {
		return sendRequest(getMappingsRequest, requestCreator.getMapping(),
				org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse.class, headers).next();
	}

	@Override
	public Mono<GetMappingsResponse> getMapping(HttpHeaders headers, GetMappingsRequest getMappingsRequest) {
		return sendRequest(getMappingsRequest, requestCreator.getMappingRequest(), GetMappingsResponse.class, headers) //
				.next();
	}

	@Override
	public Mono<GetFieldMappingsResponse> getFieldMapping(HttpHeaders headers,
			GetFieldMappingsRequest getFieldMappingsRequest) {
		return sendRequest(getFieldMappingsRequest, requestCreator.getFieldMapping(), GetFieldMappingsResponse.class,
				headers).next();
	}

	@Override
	public Mono<GetSettingsResponse> getSettings(HttpHeaders headers, GetSettingsRequest getSettingsRequest) {
		return sendRequest(getSettingsRequest, requestCreator.getSettings(), GetSettingsResponse.class, headers).next();
	}

	@Override
	public Mono<Boolean> putMapping(HttpHeaders headers,
			org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest putMappingRequest) {

		return sendRequest(putMappingRequest, requestCreator.putMapping(), AcknowledgedResponse.class, headers) //
				.map(AcknowledgedResponse::isAcknowledged) //
				.next();
	}

	@Override
	public Mono<Boolean> putMapping(HttpHeaders headers, PutMappingRequest putMappingRequest) {
		return sendRequest(putMappingRequest, requestCreator.putMappingRequest(), AcknowledgedResponse.class, headers) //
				.map(AcknowledgedResponse::isAcknowledged) //
				.next();
	}

	@Override
	public Mono<Void> openIndex(HttpHeaders headers, OpenIndexRequest request) {

		return sendRequest(request, requestCreator.indexOpen(), AcknowledgedResponse.class, headers) //
				.then();
	}

	@Override
	public Mono<Void> refreshIndex(HttpHeaders headers, RefreshRequest refreshRequest) {

		return sendRequest(refreshRequest, requestCreator.indexRefresh(), RefreshResponse.class, headers) //
				.then();
	}

	@Override
	public Mono<Boolean> updateAliases(HttpHeaders headers, IndicesAliasesRequest indicesAliasesRequest) {
		return sendRequest(indicesAliasesRequest, requestCreator.updateAlias(), AcknowledgedResponse.class, headers)
				.map(AcknowledgedResponse::isAcknowledged).next();
	}

	@Override
	public Mono<GetAliasesResponse> getAliases(HttpHeaders headers, GetAliasesRequest getAliasesRequest) {
		return sendRequest(getAliasesRequest, requestCreator.getAlias(), GetAliasesResponse.class, headers).next();
	}

	@Override
	public Mono<Boolean> putTemplate(HttpHeaders headers, PutIndexTemplateRequest putIndexTemplateRequest) {
		return sendRequest(putIndexTemplateRequest, requestCreator.putTemplate(), AcknowledgedResponse.class, headers)
				.map(AcknowledgedResponse::isAcknowledged).next();
	}

	@Override
	public Mono<GetIndexTemplatesResponse> getTemplate(HttpHeaders headers,
			GetIndexTemplatesRequest getIndexTemplatesRequest) {
		return (sendRequest(getIndexTemplatesRequest, requestCreator.getTemplates(), GetIndexTemplatesResponse.class,
				headers)).next();
	}

	@Override
	public Mono<Boolean> existsTemplate(HttpHeaders headers, IndexTemplatesExistRequest indexTemplatesExistRequest) {
		return sendRequest(indexTemplatesExistRequest, requestCreator.templatesExist(), RawActionResponse.class, headers) //
				.flatMap(response -> response.releaseBody().thenReturn(response.statusCode().is2xxSuccessful())) //
				.next();
	}

	@Override
	public Mono<Boolean> deleteTemplate(HttpHeaders headers, DeleteIndexTemplateRequest deleteIndexTemplateRequest) {
		return sendRequest(deleteIndexTemplateRequest, requestCreator.deleteTemplate(), AcknowledgedResponse.class, headers)
				.map(AcknowledgedResponse::isAcknowledged).next();
	}

	@Override
	public Mono<GetIndexResponse> getIndex(HttpHeaders headers, GetIndexRequest getIndexRequest) {
		return sendRequest(getIndexRequest, requestCreator.getIndex(), GetIndexResponse.class, headers).next();
	}

	// endregion

	// region cluster operations
	@Override
	public Mono<ClusterHealthResponse> health(HttpHeaders headers, ClusterHealthRequest clusterHealthRequest) {
		return sendRequest(clusterHealthRequest, requestCreator.clusterHealth(), ClusterHealthResponse.class, headers)
				.next();
	}
	// endregion

	// region helper functions
	private <T> Publisher<? extends T> readResponseBody(String logId, Request request, ClientResponse response,
			Class<T> responseType) {

		if (RawActionResponse.class.equals(responseType)) {

			ClientLogger.logRawResponse(logId, response.statusCode().value());
			return Mono.just(responseType.cast(RawActionResponse.create(response)));
		}

		if (response.statusCode().is5xxServerError()) {

			ClientLogger.logRawResponse(logId, response.statusCode().value());
			return handleServerError(request, response);
		}

		if (response.statusCode().is4xxClientError()) {

			ClientLogger.logRawResponse(logId, response.statusCode().value());
			return handleClientError(logId, response, responseType);
		}

		return response.body(BodyExtractors.toMono(byte[].class)) //
				.map(it -> new String(it, StandardCharsets.UTF_8)) //
				.doOnNext(it -> ClientLogger.logResponse(logId, response.statusCode().value(), it)) //
				.flatMap(content -> doDecode(response, responseType, content));
	}

	private static <T> Mono<T> doDecode(ClientResponse response, Class<T> responseType, String content) {

		String mediaType = response.headers().contentType().map(MediaType::toString).orElse(XContentType.JSON.mediaType());

		try {

			Method fromXContent = ReflectionUtils.findMethod(responseType, "fromXContent", XContentParser.class);

			if (fromXContent == null) {
				return Mono.error(new UncategorizedElasticsearchException(
						"No method named fromXContent found in " + responseType.getCanonicalName()));
			}
			return Mono.justOrEmpty(responseType
					.cast(ReflectionUtils.invokeMethod(fromXContent, responseType, createParser(mediaType, content))));

		} catch (Throwable errorParseFailure) { // cause elasticsearch also uses AssertionError

			try {
				return Mono.error(BytesRestResponse.errorFromXContent(createParser(mediaType, content)));
			} catch (Exception e) {

				return Mono.error(new RestStatusException(response.statusCode().value(), content));
			}
		}
	}

	private static XContentParser createParser(String mediaType, String content) throws IOException {
		return XContentType.fromMediaTypeOrFormat(mediaType) //
				.xContent() //
				.createParser(new NamedXContentRegistry(NamedXContents.getDefaultNamedXContents()),
						DeprecationHandler.THROW_UNSUPPORTED_OPERATION, content);
	}

	private Lazy<String> bodyExtractor(Request request) {

		return Lazy.of(() -> {

			try {
				return EntityUtils.toString(request.getEntity());
			} catch (IOException e) {
				throw new RequestBodyEncodingException("Error encoding request", e);
			}
		});
	}
	// endregion

	// region error and exception handling
	private <T> Publisher<? extends T> handleServerError(Request request, ClientResponse response) {

		int statusCode = response.statusCode().value();
		RestStatus status = RestStatus.fromCode(statusCode);
		String mediaType = response.headers().contentType().map(MediaType::toString).orElse(XContentType.JSON.mediaType());

		return response.body(BodyExtractors.toMono(byte[].class)) //
				.switchIfEmpty(Mono.error(new RestStatusException(status.getStatus(),
						String.format("%s request to %s returned error code %s and no body.", request.getMethod(),
								request.getEndpoint(), statusCode))))
				.map(bytes -> new String(bytes, StandardCharsets.UTF_8)) //
				.flatMap(content -> contentOrError(content, mediaType, status))
				.flatMap(unused -> Mono
						.error(new RestStatusException(status.getStatus(), String.format("%s request to %s returned error code %s.",
								request.getMethod(), request.getEndpoint(), statusCode))));
	}

	private <T> Publisher<? extends T> handleClientError(String logId, ClientResponse response, Class<T> responseType) {

		int statusCode = response.statusCode().value();
		RestStatus status = RestStatus.fromCode(statusCode);
		String mediaType = response.headers().contentType().map(MediaType::toString).orElse(XContentType.JSON.mediaType());

		return response.body(BodyExtractors.toMono(byte[].class)) //
				.map(bytes -> new String(bytes, StandardCharsets.UTF_8)) //
				.flatMap(content -> contentOrError(content, mediaType, status)) //
				.doOnNext(content -> ClientLogger.logResponse(logId, response.statusCode().value(), content)) //
				.flatMap(content -> doDecode(response, responseType, content));
	}

	/**
	 * checks if the given content body contains an {@link ElasticsearchException}, if yes it is returned in a Mono.error.
	 * Otherwise the content is returned in the Mono
	 *
	 * @param content the content to analyze
	 * @param mediaType the returned media type
	 * @param status the response status
	 * @return a Mono with the content or an Mono.error
	 */
	private static Mono<String> contentOrError(String content, String mediaType, RestStatus status) {

		ElasticsearchException exception = getElasticsearchException(content, mediaType, status);

		if (exception != null) {
			StringBuilder sb = new StringBuilder();
			buildExceptionMessages(sb, exception);
			return Mono.error(new RestStatusException(status.getStatus(), sb.toString(), exception));
		}

		return Mono.just(content);
	}

	/**
	 * tries to parse an {@link ElasticsearchException} from the given body content
	 *
	 * @param content the content to analyse
	 * @param mediaType the type of the body content
	 * @return an {@link ElasticsearchException} or {@literal null}.
	 */
	@Nullable
	private static ElasticsearchException getElasticsearchException(String content, String mediaType, RestStatus status) {

		try {
			XContentParser parser = createParser(mediaType, content);
			// we have a JSON object with an error and a status field
			parser.nextToken(); // Skip START_OBJECT

			XContentParser.Token token;
			do {
				token = parser.nextToken();

				if ("error".equals(parser.currentName())) {
					return ElasticsearchException.failureFromXContent(parser);
				}
			} while (token == XContentParser.Token.FIELD_NAME);

			return null;
		} catch (Exception e) {
			return new ElasticsearchStatusException(content, status);
		}
	}

	private static void buildExceptionMessages(StringBuilder sb, Throwable t) {

		sb.append(t.getMessage());
		for (Throwable throwable : t.getSuppressed()) {
			sb.append(", ");
			buildExceptionMessages(sb, throwable);
		}
	}
	// endregion

	// region internal classes
	/**
	 * Reactive client {@link ReactiveElasticsearchClient.Status} implementation.
	 *
	 * @author Christoph Strobl
	 */
	static class ClientStatus implements Status {

		private final Collection<ElasticsearchHost> connectedHosts;

		ClientStatus(Collection<ElasticsearchHost> connectedHosts) {
			this.connectedHosts = connectedHosts;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.elasticsearch.client.erhlc.ReactiveElasticsearchClient.Status#hosts()
		 */
		@Override
		public Collection<ElasticsearchHost> hosts() {
			return connectedHosts;
		}
	}

	// endregion
}
