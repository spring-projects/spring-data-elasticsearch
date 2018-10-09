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
import java.util.function.Function;

import org.apache.http.util.EntityUtils;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
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
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.elasticsearch.client.ClientProvider.VerificationMode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;
import org.springframework.web.reactive.function.client.WebClientException;

/**
 * A {@link WebClient} based client that connects to an Elasticsearch cluster through HTTP.
 * 
 * @author Christoph Strobl
 * @since 3.2
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
	 * Execute the given {@link GetRequest} against the {@literal get} API.
	 *
	 * @param getRequest must not be {@literal null}.
	 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-get.html">Get API on
	 *      elastic.co</a>
	 * @return the {@link Mono} emittig the {@link GetResult result}.
	 */
	public Mono<GetResult> get(GetRequest getRequest) {
		return get(HttpHeaders.EMPTY, getRequest);
	}

	/**
	 * Execute the given {@link GetRequest} against the {@literal get} API.
	 *
	 * @param headers Use {@link HttpHeaders} to provide eg. authentication data. Must not be {@literal null}.
	 * @param getRequest must not be {@literal null}.
	 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-get.html">Get API on
	 *      elastic.co</a>
	 * @return the {@link Mono} emittig the {@link GetResult result}.
	 */
	public Mono<GetResult> get(HttpHeaders headers, GetRequest getRequest) {

		return request(getRequest, RequestCreator.get(), GetResponse.class, headers) //
				.map(it -> new GetResult(it.getIndex(), it.getType(), it.getId(), it.getVersion(), it.isExists(),
						it.getSourceAsBytesRef(), it.getFields())) //
				.next();
	}

	/**
	 * Execute the given {@link SearchRequest} against the {@literal search} API.
	 * 
	 * @param searchRequest must not be {@literal null}.
	 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/search-search.html">Search API on
	 *      elastic.co</a>
	 * @return the {@link Flux} emittig {@link SearchHit hits} one by one.
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

		return request(searchRequest, RequestCreator.search(), SearchResponse.class, headers) //
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
		return request(indexRequest, RequestCreator.index(), IndexResponse.class, headers).publishNext();
	}

	private <Req extends ActionRequest, Resp extends ActionResponse> Flux<Resp> request(Req request,
			Function<Req, Request> converter, Class<Resp> responseType, HttpHeaders headers) {
		return request(converter.apply(request), responseType, headers);
	}

	private <AR extends ActionResponse> Flux<AR> request(Request request, Class<AR> responseType, HttpHeaders headers) {

		return this.clientProvider.getActive(VerificationMode.LAZY, headers) //
				.flatMapMany(webClient -> sendRequest(webClient, request, responseType, headers)) //
				.onErrorResume(throwable -> {

					if (throwable instanceof ConnectException) {

						return clientProvider.getActive(VerificationMode.ALWAYS) //
								.flatMapMany(webClient -> sendRequest(webClient, request, responseType, headers));
					}

					return Mono.error(throwable);
				});
	}

	private <T> Flux<T> sendRequest(WebClient webClient, Request request, Class<T> responseType, HttpHeaders headers) {

		RequestBodySpec requestBodySpec = webClient.method(HttpMethod.valueOf(request.getMethod().toUpperCase())) //
				.uri(request.getEndpoint(), request.getParameters()) //
				.headers(theHeaders -> theHeaders.addAll(headers));

		if (request.getEntity() != null) {

			requestBodySpec.contentType(MediaType.valueOf(request.getEntity().getContentType().getValue()));
			requestBodySpec.body(bodyExtractor(request), String.class);
		}

		return requestBodySpec //
				.exchange() //
				.flatMapMany(response -> readResponseBody(request, response, responseType));
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

	static class RequestCreator {

		static final Method SEARCH_METHOD = ReflectionUtils.findMethod(Request.class, "search", SearchRequest.class);
		static final Method INDEX_METHOD = ReflectionUtils.findMethod(Request.class, "index", IndexRequest.class);
		static final Method GET_METHOD = ReflectionUtils.findMethod(Request.class, "get", GetRequest.class);

		static {
			SEARCH_METHOD.setAccessible(true);
			INDEX_METHOD.setAccessible(true);
			GET_METHOD.setAccessible(true);
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
	}

	public static class RequestBodyEncodingException extends WebClientException {

		RequestBodyEncodingException(String msg, Throwable ex) {
			super(msg, ex);
		}
	}

}
