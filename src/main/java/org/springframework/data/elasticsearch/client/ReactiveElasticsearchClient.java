/*
 * Copyright 2018. the original author or authors.
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

package org.springframework.data.elasticsearch.client;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.ConnectException;
import java.util.List;
import java.util.function.Function;

import org.apache.http.util.EntityUtils;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionResponse;
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
import org.elasticsearch.client.Request;
import org.elasticsearch.common.xcontent.DeprecationHandler;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.get.GetResult;
import org.elasticsearch.search.SearchHit;
import org.reactivestreams.Publisher;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.elasticsearch.client.ClientProvider.VerificationMode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.BodyExtractor;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;
import org.springframework.web.reactive.function.client.WebClientException;

/**
 * A {@link WebClient} based client that connects to an Elasticsearch cluster through HTTP.
 * 
 * @author Christoph Strobl
 * @since 4.0
 */
public class ReactiveElasticsearchClient {

	private final ClientProvider clientProvider;

	/**
	 * Create a new {@link ReactiveElasticsearchClient} using the given clientProvider to obtain server connections.
	 *
	 * @param clientProvider must not be {@literal null}.
	 */
	ReactiveElasticsearchClient(ClientProvider clientProvider) {
		this.clientProvider = clientProvider;
	}

	/**
	 * Create a new {@link ReactiveElasticsearchClient} aware of the given nodes in the cluster. <br />
	 * <strong>NOTE</strong> If the cluster requires authentication be sure to provide {@link HttpHeaders} via
	 * {@link #create(HttpHeaders, String...)}.
	 * 
	 * @param hosts must not be {@literal null} nor empty!
	 * @return new instance of {@link ReactiveElasticsearchClient}.
	 */
	public static ReactiveElasticsearchClient create(String... hosts) {
		return create(HttpHeaders.EMPTY, hosts);
	}

	/**
	 * Create a new {@link ReactiveElasticsearchClient} aware of the given nodes in the cluster. <br />
	 * <strong>NOTE</strong> If the cluster requires authentication be sure to provide the according {@link HttpHeaders}
	 * correctly.
	 *
	 * @param headers Use {@link HttpHeaders} to provide eg. authentication data. Must not be {@literal null}.
	 * @param hosts must not be {@literal null} nor empty!
	 * @return new instance of {@link ReactiveElasticsearchClient}.
	 */
	public static ReactiveElasticsearchClient create(HttpHeaders headers, String... hosts) {

		Assert.notEmpty(hosts, "Elasticsearch Cluster needs to consist of at least one host");

		ClientProvider clientProvider = ClientProvider.provider(hosts);
		return new ReactiveElasticsearchClient(
				headers.isEmpty() ? clientProvider : clientProvider.withDefaultHeaders(headers));
	}

	/**
	 * Pings the remote Elasticsearch cluster and emits {@literal true} if the ping succeeded, {@literal false} otherwise.
	 *
	 * @return the {@link Mono} emitting the result of the ping attempt.
	 */
	public Mono<Boolean> ping() {
		return ping(HttpHeaders.EMPTY);
	}

	/**
	 * Pings the remote Elasticsearch cluster and emits {@literal true} if the ping succeeded, {@literal false} otherwise.
	 *
	 * @param headers Use {@link HttpHeaders} to provide eg. authentication data. Must not be {@literal null}.
	 * @return the {@link Mono} emitting the result of the ping attempt.
	 */
	public Mono<Boolean> ping(HttpHeaders headers) {

		return sendRequest(new MainRequest(), RequestCreator.ping(), RawActionResponse.class, headers) //
				.map(response -> response.statusCode().is2xxSuccessful()) //
				.next();
	}

	public Mono<MainResponse> info() {
		return info(HttpHeaders.EMPTY);
	}

	public Mono<MainResponse> info(HttpHeaders headers) {

		return sendRequest(new MainRequest(), RequestCreator.info(), MainResponse.class, headers) //
				.next();
	}

	/**
	 * Execute the given {@link GetRequest} against the {@literal get} API to retrieve a document by id.
	 *
	 * @param getRequest must not be {@literal null}.
	 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-get.html">Get API on
	 *      elastic.co</a>
	 * @return the {@link Mono} emitting the {@link GetResult result}.
	 */
	public Mono<GetResult> get(GetRequest getRequest) {
		return get(HttpHeaders.EMPTY, getRequest);
	}

	/**
	 * Execute the given {@link GetRequest} against the {@literal get} API to retrieve a document by id.
	 *
	 * @param headers Use {@link HttpHeaders} to provide eg. authentication data. Must not be {@literal null}.
	 * @param getRequest must not be {@literal null}.
	 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-get.html">Get API on
	 *      elastic.co</a>
	 * @return the {@link Mono} emitting the {@link GetResult result}.
	 */
	public Mono<GetResult> get(HttpHeaders headers, GetRequest getRequest) {

		return sendRequest(getRequest, RequestCreator.get(), GetResponse.class, headers) //
				.map(ReactiveElasticsearchClient::getResponseToGetResult) //
				.next();
	}

	/**
	 * Execute the given {@link MultiGetRequest} against the {@literal multi-get} API to retrieve multiple documents by
	 * id.
	 *
	 * @param multiGetRequest must not be {@literal null}.
	 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-multi-get.html">Multi Get API on
	 *      elastic.co</a>
	 * @return the {@link Flux} emitting the {@link GetResult result}.
	 */
	public Flux<GetResult> multiGet(MultiGetRequest multiGetRequest) {
		return multiGet(HttpHeaders.EMPTY, multiGetRequest);
	}

	/**
	 * Execute the given {@link MultiGetRequest} against the {@literal multi-get} API to retrieve multiple documents by
	 * id.
	 *
	 * @param headers Use {@link HttpHeaders} to provide eg. authentication data. Must not be {@literal null}.
	 * @param multiGetRequest must not be {@literal null}.
	 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-multi-get.html">Multi Get API on
	 *      elastic.co</a>
	 * @return the {@link Flux} emitting the {@link GetResult result}.
	 */
	public Flux<GetResult> multiGet(HttpHeaders headers, MultiGetRequest multiGetRequest) {

		return sendRequest(multiGetRequest, RequestCreator.multiGet(), MultiGetResponse.class, headers)
				.map(MultiGetResponse::getResponses) //
				.flatMap(Flux::fromArray) //
				.filter(it -> !it.isFailed()) //
				.map(it -> ReactiveElasticsearchClient.getResponseToGetResult(it.getResponse()));
	}

	/**
	 * Execute the given {@link SearchRequest} against the {@literal search} API.
	 * 
	 * @param searchRequest must not be {@literal null}.
	 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/search-search.html">Search API on
	 *      elastic.co</a>
	 * @return the {@link Flux} emitting {@link SearchHit hits} one by one.
	 */
	public Flux<SearchHit> search(SearchRequest searchRequest) {
		return search(HttpHeaders.EMPTY, searchRequest);
	}

	/**
	 * Execute the given {@link SearchRequest} against the {@literal search} API.
	 *
	 * @param headers Use {@link HttpHeaders} to provide eg. authentication data. Must not be {@literal null}.
	 * @param searchRequest must not be {@literal null}.
	 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/search-search.html">Search API on
	 *      elastic.co</a>
	 * @return the {@link Flux} emitting {@link SearchHit hits} one by one.
	 */
	public Flux<SearchHit> search(HttpHeaders headers, SearchRequest searchRequest) {

		return sendRequest(searchRequest, RequestCreator.search(), SearchResponse.class, headers) //
				.map(SearchResponse::getHits) //
				.flatMap(Flux::fromIterable);
	}

	/**
	 * Execute the given {@link IndexRequest} against the {@literal index} API.
	 * 
	 * @param indexRequest must not be {@literal null}.
	 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-index_.html">Index API on
	 *      elastic.co</a>
	 * @return the {@link Mono} emitting the {@link IndexResponse}.
	 */
	public Mono<IndexResponse> index(IndexRequest indexRequest) {
		return index(HttpHeaders.EMPTY, indexRequest);
	}

	/**
	 * Execute the given {@link IndexRequest} against the {@literal index} API.
	 *
	 * @param headers Use {@link HttpHeaders} to provide eg. authentication data. Must not be {@literal null}.
	 * @param indexRequest must not be {@literal null}.
	 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-index_.html">Index API on
	 *      elastic.co</a>
	 * @return the {@link Mono} emitting the {@link IndexResponse}.
	 */
	public Mono<IndexResponse> index(HttpHeaders headers, IndexRequest indexRequest) {
		return sendRequest(indexRequest, RequestCreator.index(), IndexResponse.class, headers).publishNext();
	}

	/**
	 * Compose the actual command/s to run against Elasticsearch using the underlying {@link WebClient connection}.
	 * {@link #execute(ReactiveElasticsearchClientCallback) Execute} selects an active server from the available ones and
	 * retries operations that fail with a {@link ConnectException} on another node if the previously selected one becomes
	 * unavailable.
	 *
	 * @param callback the {@link ReactiveElasticsearchClientCallback callback} wielding the actual command to run.
	 * @return the {@link Mono} emitting the {@link ClientResponse} once subscribed.
	 */
	public Mono<ClientResponse> execute(ReactiveElasticsearchClientCallback callback) {

		return this.clientProvider.getActive(VerificationMode.LAZY) //
				.flatMap(it -> callback.doWithClient(it)) //
				.onErrorResume(throwable -> {

					if (throwable instanceof ConnectException) {

						return clientProvider.getActive(VerificationMode.ALWAYS) //
								.flatMap(webClient -> callback.doWithClient(webClient));
					}

					return Mono.error(throwable);
				});
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
				.exchange();
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
			return Mono.just((T) new RawActionResponse(response));
		}

		if (response.statusCode().isError()) {

			throw new HttpClientErrorException(response.statusCode(),
					String.format("%s request to %s returned error code %s.", request.getMethod(), request.getEndpoint(),
							response.statusCode().value()));
		}

		return response.body(BodyExtractors.toDataBuffers()).flatMap(it -> {
			try {

				XContentParser parser = XContentType
						.fromMediaTypeOrFormat(
								response.headers().contentType().map(MediaType::toString).orElse(XContentType.JSON.mediaType()))
						.xContent().createParser(NamedXContentRegistry.EMPTY, DeprecationHandler.THROW_UNSUPPORTED_OPERATION,
								it.asInputStream(true));

				// TODO: read findAndInvokeFromXContent cut response into pieces (at least for search)

				Method fromXContent = ReflectionUtils.findMethod(responseType, "fromXContent", XContentParser.class);
				return Mono.just((T) ReflectionUtils.invokeMethod(fromXContent, responseType, parser));
			} catch (IOException e) {
				throw new DataAccessResourceFailureException("Error parsing XContent.", e);
			}
		});
	}

	public interface ReactiveElasticsearchClientCallback {
		Mono<ClientResponse> doWithClient(WebClient client);
	}

	static class RequestCreator {

		static final Method SEARCH_METHOD = ReflectionUtils.findMethod(Request.class, "search", SearchRequest.class);
		static final Method INDEX_METHOD = ReflectionUtils.findMethod(Request.class, "index", IndexRequest.class);
		static final Method GET_METHOD = ReflectionUtils.findMethod(Request.class, "get", GetRequest.class);
		static final Method PING_METHOD = ReflectionUtils.findMethod(Request.class, "ping");
		static final Method INFO_METHOD = ReflectionUtils.findMethod(Request.class, "info");
		static final Method MULTI_GET_METHOD = ReflectionUtils.findMethod(Request.class, "multiGet", MultiGetRequest.class);

		static {

			PING_METHOD.setAccessible(true);
			SEARCH_METHOD.setAccessible(true);
			INDEX_METHOD.setAccessible(true);
			GET_METHOD.setAccessible(true);
			INFO_METHOD.setAccessible(true);
			MULTI_GET_METHOD.setAccessible(true);
		}

		static Function<SearchRequest, Request> search() {
			return (request) -> (Request) ReflectionUtils.invokeMethod(SEARCH_METHOD, Request.class, request);
		}

		static Function<IndexRequest, Request> index() {
			return (request) -> (Request) ReflectionUtils.invokeMethod(INDEX_METHOD, Request.class, request);
		}

		static Function<GetRequest, Request> get() {
			return (request) -> (Request) ReflectionUtils.invokeMethod(GET_METHOD, Request.class, request);
		}

		static Function<MainRequest, Request> ping() {
			return (request) -> (Request) ReflectionUtils.invokeMethod(PING_METHOD, Request.class);
		}

		static Function<MainRequest, Request> info() {
			return (request) -> (Request) ReflectionUtils.invokeMethod(INFO_METHOD, Request.class);
		}

		static Function<MultiGetRequest, Request> multiGet() {
			return (request) -> (Request) ReflectionUtils.invokeMethod(MULTI_GET_METHOD, Request.class, request);
		}
	}

	public static class RequestBodyEncodingException extends WebClientException {

		RequestBodyEncodingException(String msg, Throwable ex) {
			super(msg, ex);
		}
	}

	static class RawActionResponse extends ActionResponse implements ClientResponse {

		final ClientResponse delegate;

		RawActionResponse(ClientResponse delegate) {
			this.delegate = delegate;
		}

		public HttpStatus statusCode() {
			return delegate.statusCode();
		}

		public int rawStatusCode() {
			return delegate.rawStatusCode();
		}

		public Headers headers() {
			return delegate.headers();
		}

		public MultiValueMap<String, ResponseCookie> cookies() {
			return delegate.cookies();
		}

		public ExchangeStrategies strategies() {
			return delegate.strategies();
		}

		public <T> T body(BodyExtractor<T, ? super ClientHttpResponse> extractor) {
			return delegate.body(extractor);
		}

		public <T> Mono<T> bodyToMono(Class<? extends T> elementClass) {
			return delegate.bodyToMono(elementClass);
		}

		public <T> Mono<T> bodyToMono(ParameterizedTypeReference<T> typeReference) {
			return delegate.bodyToMono(typeReference);
		}

		public <T> Flux<T> bodyToFlux(Class<? extends T> elementClass) {
			return delegate.bodyToFlux(elementClass);
		}

		public <T> Flux<T> bodyToFlux(ParameterizedTypeReference<T> typeReference) {
			return delegate.bodyToFlux(typeReference);
		}

		public <T> Mono<ResponseEntity<T>> toEntity(Class<T> bodyType) {
			return delegate.toEntity(bodyType);
		}

		public <T> Mono<ResponseEntity<T>> toEntity(ParameterizedTypeReference<T> typeReference) {
			return delegate.toEntity(typeReference);
		}

		public <T> Mono<ResponseEntity<List<T>>> toEntityList(Class<T> elementType) {
			return delegate.toEntityList(elementType);
		}

		public <T> Mono<ResponseEntity<List<T>>> toEntityList(ParameterizedTypeReference<T> typeReference) {
			return delegate.toEntityList(typeReference);
		}

		public static Builder from(ClientResponse other) {
			return ClientResponse.from(other);
		}

		public static Builder create(HttpStatus statusCode) {
			return ClientResponse.create(statusCode);
		}

		public static Builder create(HttpStatus statusCode, ExchangeStrategies strategies) {
			return ClientResponse.create(statusCode, strategies);
		}

		public static Builder create(HttpStatus statusCode, List<HttpMessageReader<?>> messageReaders) {
			return ClientResponse.create(statusCode, messageReaders);
		}

	}

}
