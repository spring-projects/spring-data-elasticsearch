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
package org.springframework.data.elasticsearch.client.reactive;

import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.JdkSslContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

import javax.net.ssl.SSLContext;

import org.apache.http.util.EntityUtils;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetRequest;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.main.MainRequest;
import org.elasticsearch.action.main.MainResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Request;
import org.elasticsearch.common.xcontent.DeprecationHandler;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.get.GetResult;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.reactivestreams.Publisher;
import org.springframework.data.elasticsearch.ElasticsearchException;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.ElasticsearchHost;
import org.springframework.data.elasticsearch.client.NoReachableHostException;
import org.springframework.data.elasticsearch.client.reactive.HostProvider.Verification;
import org.springframework.data.elasticsearch.client.util.RequestConverters;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;

/**
 * A {@link WebClient} based {@link ReactiveElasticsearchClient} that connects to an Elasticsearch cluster using HTTP.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 4.0
 * @see ClientConfiguration
 * @see ReactiveRestClients
 */
public class DefaultReactiveElasticsearchClient implements ReactiveElasticsearchClient {

	private final HostProvider hostProvider;

	/**
	 * Create a new {@link DefaultReactiveElasticsearchClient} using the given {@link HostProvider} to obtain server
	 * connections.
	 *
	 * @param hostProvider must not be {@literal null}.
	 */
	public DefaultReactiveElasticsearchClient(HostProvider hostProvider) {

		Assert.notNull(hostProvider, "HostProvider must not be null");

		this.hostProvider = hostProvider;
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

		ClientConfiguration clientConfiguration = ClientConfiguration.builder().connectedTo(hosts)
				.withDefaultHeaders(headers).build();
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

		Assert.notNull(clientConfiguration, "ClientConfiguration must not be null");

		WebClientProvider provider = getWebClientProvider(clientConfiguration);

		HostProvider hostProvider = HostProvider.provider(provider,
				clientConfiguration.getEndpoints().toArray(new InetSocketAddress[0]));
		return new DefaultReactiveElasticsearchClient(hostProvider);
	}

	private static WebClientProvider getWebClientProvider(ClientConfiguration clientConfiguration) {

		WebClientProvider provider;

		if (clientConfiguration.useSsl()) {

			ReactorClientHttpConnector connector = new ReactorClientHttpConnector(HttpClient.create().secure(sslConfig -> {

				Optional<SSLContext> sslContext = clientConfiguration.getSslContext();

				sslContext.ifPresent(it -> sslConfig.sslContext(new JdkSslContext(it, true, ClientAuth.NONE)));
			}));
			provider = WebClientProvider.create("https", connector);
		} else {
			provider = WebClientProvider.create("http");
		}

		return provider.withDefaultHeaders(clientConfiguration.getDefaultHeaders());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient#ping(org.springframework.http.HttpHeaders)
	 */
	@Override
	public Mono<Boolean> ping(HttpHeaders headers) {

		return sendRequest(new MainRequest(), RequestCreator.ping(), RawActionResponse.class, headers) //
				.map(response -> response.statusCode().is2xxSuccessful()) //
				.onErrorResume(NoReachableHostException.class, error -> Mono.just(false)).next();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient#info(org.springframework.http.HttpHeaders)
	 */
	@Override
	public Mono<MainResponse> info(HttpHeaders headers) {

		return sendRequest(new MainRequest(), RequestCreator.info(), MainResponse.class, headers) //
				.next();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient#get(org.springframework.http.HttpHeaders, org.elasticsearch.action.get.GetRequest)
	 */
	@Override
	public Mono<GetResult> get(HttpHeaders headers, GetRequest getRequest) {

		return sendRequest(getRequest, RequestCreator.get(), GetResponse.class, headers) //
				.filter(GetResponse::isExists) //
				.map(DefaultReactiveElasticsearchClient::getResponseToGetResult) //
				.next();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient#multiGet(org.springframework.http.HttpHeaders, org.elasticsearch.action.get.MultiGetRequest)
	 */
	@Override
	public Flux<GetResult> multiGet(HttpHeaders headers, MultiGetRequest multiGetRequest) {

		return sendRequest(multiGetRequest, RequestCreator.multiGet(), MultiGetResponse.class, headers)
				.map(MultiGetResponse::getResponses) //
				.flatMap(Flux::fromArray) //
				.filter(it -> !it.isFailed() && it.getResponse().isExists()) //
				.map(it -> DefaultReactiveElasticsearchClient.getResponseToGetResult(it.getResponse()));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient#exists(org.springframework.http.HttpHeaders, org.elasticsearch.action.get.GetRequest)
	 */
	@Override
	public Mono<Boolean> exists(HttpHeaders headers, GetRequest getRequest) {

		return sendRequest(getRequest, RequestCreator.exists(), RawActionResponse.class, headers) //
				.map(response -> response.statusCode().is2xxSuccessful()) //
				.next();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient#ping(org.springframework.http.HttpHeaders, org.elasticsearch.action.index.IndexRequest)
	 */
	@Override
	public Mono<IndexResponse> index(HttpHeaders headers, IndexRequest indexRequest) {
		return sendRequest(indexRequest, RequestCreator.index(), IndexResponse.class, headers).publishNext();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient#ping(org.springframework.http.HttpHeaders, org.elasticsearch.action.update.UpdateRequest)
	 */
	@Override
	public Mono<UpdateResponse> update(HttpHeaders headers, UpdateRequest updateRequest) {
		return sendRequest(updateRequest, RequestCreator.update(), UpdateResponse.class, headers).publishNext();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient#ping(org.springframework.http.HttpHeaders, org.elasticsearch.action.delete.DeleteRequest)
	 */
	@Override
	public Mono<DeleteResponse> delete(HttpHeaders headers, DeleteRequest deleteRequest) {

		return sendRequest(deleteRequest, RequestCreator.delete(), DeleteResponse.class, headers) //
				.publishNext();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient#ping(org.springframework.http.HttpHeaders, org.elasticsearch.action.search.SearchRequest)
	 */
	@Override
	public Flux<SearchHit> search(HttpHeaders headers, SearchRequest searchRequest) {

		return sendRequest(searchRequest, RequestCreator.search(), SearchResponse.class, headers) //
				.map(SearchResponse::getHits) //
				.flatMap(Flux::fromIterable);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient#ping(org.springframework.http.HttpHeaders, org.elasticsearch.index.reindex.DeleteByQueryRequest)
	 */
	public Mono<BulkByScrollResponse> deleteBy(HttpHeaders headers, DeleteByQueryRequest deleteRequest) {

		return sendRequest(deleteRequest, RequestCreator.deleteByQuery(), BulkByScrollResponse.class, headers) //
				.publishNext();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient#ping(org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient.ReactiveElasticsearchClientCallback)
	 */
	@Override
	public Mono<ClientResponse> execute(ReactiveElasticsearchClientCallback callback) {

		return this.hostProvider.getActive(Verification.LAZY) //
				.flatMap(callback::doWithClient) //
				.onErrorResume(throwable -> {

					if (throwable instanceof ConnectException) {

						return hostProvider.getActive(Verification.ACTIVE) //
								.flatMap(callback::doWithClient);
					}

					return Mono.error(throwable);
				});
	}

	@Override
	public Mono<Status> status() {

		return hostProvider.clusterInfo() //
				.map(it -> new ClientStatus(it.getNodes()));
	}

	// --> Private Response helpers

	private static GetResult getResponseToGetResult(GetResponse response) {

		return new GetResult(response.getIndex(), response.getType(), response.getId(), response.getVersion(),
				response.isExists(), response.getSourceAsBytesRef(), response.getFields());
	}

	// -->

	private <Req extends ActionRequest, Resp extends ActionResponse> Flux<Resp> sendRequest(Req request,
			Function<Req, Request> converter, Class<Resp> responseType, HttpHeaders headers) {
		return sendRequest(converter.apply(request), responseType, headers);
	}

	private <AR extends ActionResponse> Flux<AR> sendRequest(Request request, Class<AR> responseType,
			HttpHeaders headers) {

		return execute(webClient -> sendRequest(webClient, request, headers))
				.flatMapMany(response -> readResponseBody(request, response, responseType));
	}

	private Mono<ClientResponse> sendRequest(WebClient webClient, Request request, HttpHeaders headers) {

		RequestBodySpec requestBodySpec = webClient.method(HttpMethod.valueOf(request.getMethod().toUpperCase())) //
				.uri(request.getEndpoint(), request.getParameters()) //
				.headers(theHeaders -> theHeaders.addAll(headers));

		if (request.getEntity() != null) {

			requestBodySpec.contentType(MediaType.valueOf(request.getEntity().getContentType().getValue()));
			requestBodySpec.body(bodyExtractor(request), String.class);
		}

		return requestBodySpec //
				.exchange() //
				.onErrorReturn(ConnectException.class, ClientResponse.create(HttpStatus.SERVICE_UNAVAILABLE).build());
	}

	private Publisher<String> bodyExtractor(Request request) {

		return Mono.fromSupplier(() -> {

			try {
				return EntityUtils.toString(request.getEntity());
			} catch (IOException e) {
				throw new RequestBodyEncodingException("Error encoding request", e);
			}
		});
	}

	private <T> Publisher<? extends T> readResponseBody(Request request, ClientResponse response, Class<T> responseType) {

		if (RawActionResponse.class.equals(responseType)) {
			return Mono.just(responseType.cast(RawActionResponse.create(response)));
		}

		if (response.statusCode().is5xxServerError()) {
			return handleServerError(request, response);
		}

		return response.body(BodyExtractors.toMono(byte[].class)) //
				.map(it -> new String(it, StandardCharsets.UTF_8)) //
				.flatMap(content -> doDecode(response, responseType, content));
	}

	private static <T> Mono<T> doDecode(ClientResponse response, Class<T> responseType, String content) {

		String mediaType = response.headers().contentType().map(MediaType::toString).orElse(XContentType.JSON.mediaType());

		try {

			Method fromXContent = ReflectionUtils.findMethod(responseType, "fromXContent", XContentParser.class);

			return Mono.justOrEmpty(responseType
					.cast(ReflectionUtils.invokeMethod(fromXContent, responseType, createParser(mediaType, content))));

		} catch (Exception errorParseFailure) {

			try {
				return Mono.error(BytesRestResponse.errorFromXContent(createParser(mediaType, content)));
			} catch (Exception e) {

				return Mono
						.error(new ElasticsearchStatusException(content, RestStatus.fromCode(response.statusCode().value())));
			}
		}

	}

	private static XContentParser createParser(String mediaType, String content) throws IOException {

		return XContentType.fromMediaTypeOrFormat(mediaType) //
				.xContent() //
				.createParser(NamedXContentRegistry.EMPTY, DeprecationHandler.THROW_UNSUPPORTED_OPERATION, content);
	}

	private static <T> Publisher<? extends T> handleServerError(Request request, ClientResponse response) {
		return Mono.error(
				new HttpServerErrorException(response.statusCode(), String.format("%s request to %s returned error code %s.",
						request.getMethod(), request.getEndpoint(), response.statusCode().value())));
	}

	static class RequestCreator {

		static Function<SearchRequest, Request> search() {
			return RequestConverters::search;
		}

		static Function<IndexRequest, Request> index() {
			return RequestConverters::index;
		}

		static Function<GetRequest, Request> get() {
			return RequestConverters::get;
		}

		static Function<MainRequest, Request> ping() {
			return (request) -> RequestConverters.ping();
		}

		static Function<MainRequest, Request> info() {
			return (request) -> RequestConverters.info();
		}

		static Function<MultiGetRequest, Request> multiGet() {
			return RequestConverters::multiGet;
		}

		static Function<GetRequest, Request> exists() {
			return RequestConverters::exists;
		}

		static Function<UpdateRequest, Request> update() {
			return RequestConverters::update;
		}

		static Function<DeleteRequest, Request> delete() {
			return RequestConverters::delete;
		}

		static Function<DeleteByQueryRequest, Request> deleteByQuery() {

			return request -> {

				try {
					return RequestConverters.deleteByQuery(request);
				} catch (IOException e) {
					throw new ElasticsearchException("Could not parse request", e);
				}
			};
		}
	}

	/**
	 * Reactive client {@link ReactiveElasticsearchClient.Status} implementation.
	 *
	 * @author Christoph Strobl
	 */
	class ClientStatus implements Status {

		private final Collection<ElasticsearchHost> connectedHosts;

		ClientStatus(Collection<ElasticsearchHost> connectedHosts) {
			this.connectedHosts = connectedHosts;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient.Status#hosts()
		 */
		@Override
		public Collection<ElasticsearchHost> hosts() {
			return connectedHosts;
		}
	}
}
