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

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import org.elasticsearch.action.ActionResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyExtractor;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeStrategies;

/**
 * Extension to {@link ActionResponse} that also implements {@link ClientResponse}.
 */
class RawActionResponse extends ActionResponse implements ClientResponse {

	private final ClientResponse delegate;

	private RawActionResponse(ClientResponse delegate) {
		this.delegate = delegate;
	}

	public static RawActionResponse create(ClientResponse response) {
		return new RawActionResponse(response);
	}

	public static Builder builder(ClientResponse other) {
		return ClientResponse.from(other);
	}

	public static Builder builder(HttpStatus statusCode) {
		return ClientResponse.create(statusCode);
	}

	public static Builder builder(HttpStatus statusCode, ExchangeStrategies strategies) {
		return ClientResponse.create(statusCode, strategies);
	}

	public static Builder builder(HttpStatus statusCode, List<HttpMessageReader<?>> messageReaders) {
		return ClientResponse.create(statusCode, messageReaders);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.reactive.function.client.ClientResponse#statusCode()
	 */
	public HttpStatus statusCode() {
		return delegate.statusCode();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.reactive.function.client.ClientResponse#rawStatusCode()
	 */
	public int rawStatusCode() {
		return delegate.rawStatusCode();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.reactive.function.client.ClientResponse#headers()
	 */
	public Headers headers() {
		return delegate.headers();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.reactive.function.client.ClientResponse#cookies()
	 */
	public MultiValueMap<String, ResponseCookie> cookies() {
		return delegate.cookies();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.reactive.function.client.ClientResponse#strategies()
	 */
	public ExchangeStrategies strategies() {
		return delegate.strategies();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.reactive.function.client.ClientResponse#body(org.springframework.web.reactive.function.BodyExtractor)
	 */
	public <T> T body(BodyExtractor<T, ? super ClientHttpResponse> extractor) {
		return delegate.body(extractor);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.reactive.function.client.ClientResponse#bodyToMono(java.lang.Class)
	 */
	public <T> Mono<T> bodyToMono(Class<? extends T> elementClass) {
		return delegate.bodyToMono(elementClass);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.reactive.function.client.ClientResponse#bodyToMono(org.springframework.core.ParameterizedTypeReference)
	 */
	public <T> Mono<T> bodyToMono(ParameterizedTypeReference<T> typeReference) {
		return delegate.bodyToMono(typeReference);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.reactive.function.client.ClientResponse#bodyToFlux(java.lang.Class)
	 */
	public <T> Flux<T> bodyToFlux(Class<? extends T> elementClass) {
		return delegate.bodyToFlux(elementClass);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.reactive.function.client.ClientResponse#bodyToFlux(org.springframework.core.ParameterizedTypeReference)
	 */
	public <T> Flux<T> bodyToFlux(ParameterizedTypeReference<T> typeReference) {
		return delegate.bodyToFlux(typeReference);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.reactive.function.client.ClientResponse#toEntity(java.lang.Class)
	 */
	public <T> Mono<ResponseEntity<T>> toEntity(Class<T> bodyType) {
		return delegate.toEntity(bodyType);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.reactive.function.client.ClientResponse#toEntity(org.springframework.core.ParameterizedTypeReference)
	 */
	public <T> Mono<ResponseEntity<T>> toEntity(ParameterizedTypeReference<T> typeReference) {
		return delegate.toEntity(typeReference);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.reactive.function.client.ClientResponse#toEntityList(java.lang.Class)
	 */
	public <T> Mono<ResponseEntity<List<T>>> toEntityList(Class<T> elementType) {
		return delegate.toEntityList(elementType);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.reactive.function.client.ClientResponse#toEntityList(org.springframework.core.ParameterizedTypeReference)
	 */
	public <T> Mono<ResponseEntity<List<T>>> toEntityList(ParameterizedTypeReference<T> typeReference) {
		return delegate.toEntityList(typeReference);
	}
}
