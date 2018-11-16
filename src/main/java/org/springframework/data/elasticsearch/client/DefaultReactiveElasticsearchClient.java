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
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

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
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.reactivestreams.Publisher;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.elasticsearch.client.ClientProvider.HostState;
import org.springframework.data.elasticsearch.client.ClientProvider.VerificationMode;
import org.springframework.data.elasticsearch.client.util.RequestConverters;
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
import org.springframework.util.StreamUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.BodyExtractor;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;
import org.springframework.web.reactive.function.client.WebClientException;

/**
 * A {@link WebClient} based {@link ReactiveElasticsearchClient} that connects to an Elasticsearch cluster through HTTP.
 * 
 * @author Christoph Strobl
 * @since 4.0
 */
public class DefaultReactiveElasticsearchClient implements ReactiveElasticsearchClient {

	private final ClientProvider clientProvider;

	/**
	 * Create a new {@link DefaultReactiveElasticsearchClient} using the given clientProvider to obtain server
	 * connections.
	 *
	 * @param clientProvider must not be {@literal null}.
	 */
	public DefaultReactiveElasticsearchClient(ClientProvider clientProvider) {
		this.clientProvider = clientProvider;
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

		Assert.notEmpty(hosts, "Elasticsearch Cluster needs to consist of at least one host");

		ClientProvider clientProvider = ClientProvider.provider(hosts);
		return new DefaultReactiveElasticsearchClient(
				headers.isEmpty() ? clientProvider : clientProvider.withDefaultHeaders(headers));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.client.ReactiveElasticsearchClient#ping(org.springframework.http.HttpHeaders)
	 */
	@Override
	public Mono<Boolean> ping(HttpHeaders headers) {

		return sendRequest(new MainRequest(), RequestCreator.ping(), RawActionResponse.class, headers) //
				.map(response -> response.statusCode().is2xxSuccessful()) //
				.next();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.client.ReactiveElasticsearchClient#info(org.springframework.http.HttpHeaders)
	 */
	@Override
	public Mono<MainResponse> info(HttpHeaders headers) {

		return sendRequest(new MainRequest(), RequestCreator.info(), MainResponse.class, headers) //
				.next();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.client.ReactiveElasticsearchClient#get(org.springframework.http.HttpHeaderss, org.elasticsearch.action.get.GetRequest)
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
	 * @see org.springframework.data.elasticsearch.client.ReactiveElasticsearchClient#multiGet(org.springframework.http.HttpHeaders, org.elasticsearch.action.get.MultiGetRequest)
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
	 * @see org.springframework.data.elasticsearch.client.ReactiveElasticsearchClient#exists(org.springframework.http.HttpHeaders, org.elasticsearch.action.get.GetRequest)
	 */
	@Override
	public Mono<Boolean> exists(HttpHeaders headers, GetRequest getRequest) {

		return sendRequest(getRequest, RequestCreator.exists(), RawActionResponse.class, headers) //
				.map(response -> {

					if (response.statusCode().is2xxSuccessful()) {
						return true;
					}

					if (response.statusCode().is5xxServerError()) {

						throw new HttpClientErrorException(response.statusCode(), String.format(
								"Exists request (%s) returned error code %s.", getRequest.toString(), response.statusCode().value()));
					}

					return false;
				}) //
				.next();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.client.ReactiveElasticsearchClient#ping(org.springframework.http.HttpHeaders, org.elasticsearch.action.index.IndexRequest)
	 */
	@Override
	public Mono<IndexResponse> index(HttpHeaders headers, IndexRequest indexRequest) {
		return sendRequest(indexRequest, RequestCreator.index(), IndexResponse.class, headers).publishNext();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.client.ReactiveElasticsearchClient#ping(org.springframework.http.HttpHeaders, org.elasticsearch.action.update.UpdateRequest)
	 */
	@Override
	public Mono<UpdateResponse> update(HttpHeaders headers, UpdateRequest updateRequest) {
		return sendRequest(updateRequest, RequestCreator.update(), UpdateResponse.class, headers).publishNext();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.client.ReactiveElasticsearchClient#ping(org.springframework.http.HttpHeaders, org.elasticsearch.action.delete.DeleteRequest)
	 */
	@Override
	public Mono<DeleteResponse> delete(HttpHeaders headers, DeleteRequest deleteRequest) {
		return sendRequest(deleteRequest, RequestCreator.delete(), DeleteResponse.class, headers).publishNext();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.client.ReactiveElasticsearchClient#ping(org.springframework.http.HttpHeaders, org.elasticsearch.action.search.SearchRequest)
	 */
	@Override
	public Flux<SearchHit> search(HttpHeaders headers, SearchRequest searchRequest) {

		return sendRequest(searchRequest, RequestCreator.search(), SearchResponse.class, headers) //
				.map(SearchResponse::getHits) //
				.flatMap(Flux::fromIterable);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.client.ReactiveElasticsearchClient#ping(org.springframework.data.elasticsearch.client.ReactiveElasticsearchClient.ReactiveElasticsearchClientCallback)
	 */
	@Override
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

	@Override
	public Mono<Status> status() {

		return clientProvider.refresh() //
				.then(Mono.just(new ClientStatus(clientProvider.status())));
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
			return Mono.just((T) new RawActionResponse(response));
		}

		if (response.statusCode().is5xxServerError()) {

			throw new HttpClientErrorException(response.statusCode(),
					String.format("%s request to %s returned error code %s.", request.getMethod(), request.getEndpoint(),
							response.statusCode().value()));
		}

		return response.body(BodyExtractors.toDataBuffers()).flatMap(it -> {
			try {

				String content = StreamUtils.copyToString(it.asInputStream(true), StandardCharsets.UTF_8);

				try {

					XContentParser contentParser = XContentType
							.fromMediaTypeOrFormat(
									response.headers().contentType().map(MediaType::toString).orElse(XContentType.JSON.mediaType()))
							.xContent()
							.createParser(NamedXContentRegistry.EMPTY, DeprecationHandler.THROW_UNSUPPORTED_OPERATION, content);

					Method fromXContent = ReflectionUtils.findMethod(responseType, "fromXContent", XContentParser.class);
					return Mono.just((T) ReflectionUtils.invokeMethod(fromXContent, responseType, contentParser));
				} catch (Exception parseFailure) {

					try {

						XContentParser errorParser = XContentType
								.fromMediaTypeOrFormat(
										response.headers().contentType().map(MediaType::toString).orElse(XContentType.JSON.mediaType()))
								.xContent()
								.createParser(NamedXContentRegistry.EMPTY, DeprecationHandler.THROW_UNSUPPORTED_OPERATION, content);

						// return Mono.error to avoid ElasticsearchStatusException to be caught by outer catch.
						return Mono.error(BytesRestResponse.errorFromXContent(errorParser));

					} catch (Exception errorParseFailure) {

						// return Mono.error to avoid ElasticsearchStatusException to be caught by outer catch.
						return Mono.error(new ElasticsearchStatusException("Unable to parse response body",
								RestStatus.fromCode(response.statusCode().value())));
					}
				}
			} catch (IOException e) {
				throw new DataAccessResourceFailureException("Error parsing XContent.", e);
			}
		});
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

	/**
	 * Reactive client {@link org.springframework.data.elasticsearch.client.ReactiveElasticsearchClient.Status}
	 * implementation.
	 * 
	 * @author Christoph Strobl
	 */
	class ClientStatus implements Status {

		private final Collection<HostState> connectedHosts;

		ClientStatus(Collection<HostState> connectedHosts) {
			this.connectedHosts = connectedHosts;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.elasticsearch.client.ReactiveElasticsearchClient.Status#hosts()
		 */
		@Override
		public Collection<HostState> hosts() {
			return connectedHosts;
		}
	}
}
