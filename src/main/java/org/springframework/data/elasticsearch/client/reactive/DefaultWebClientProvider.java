/*
 * Copyright 2018-2019 the original author or authors.
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
package org.springframework.data.elasticsearch.client.reactive;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.Builder;

/**
 * Default {@link WebClientProvider} that uses cached {@link WebClient} instances per {@code hostAndPort}.
 *
 * @author Mark Paluch
 * @author Christoph Strobl
 * @since 3.2
 */
class DefaultWebClientProvider implements WebClientProvider {

	private final Map<InetSocketAddress, WebClient> cachedClients;

	private final String scheme;
	private final @Nullable ClientHttpConnector connector;
	private final Consumer<Throwable> errorListener;
	private final HttpHeaders headers;

	/**
	 * Create new {@link DefaultWebClientProvider} with empty {@link HttpHeaders} and no-op {@literal error listener}.
	 *
	 * @param scheme must not be {@literal null}.
	 * @param connector can be {@literal null}.
	 */
	DefaultWebClientProvider(String scheme, @Nullable ClientHttpConnector connector) {
		this(scheme, connector, e -> {}, HttpHeaders.EMPTY);
	}

	/**
	 * Create new {@link DefaultWebClientProvider} with empty {@link HttpHeaders} and no-op {@literal error listener}.
	 * 
	 * @param scheme must not be {@literal null}.
	 * @param connector can be {@literal null}.
	 * @param errorListener must not be {@literal null}.
	 * @param headers must not be {@literal null}.
	 */
	private DefaultWebClientProvider(String scheme, @Nullable ClientHttpConnector connector,
			Consumer<Throwable> errorListener, HttpHeaders headers) {

		Assert.notNull(scheme, "Scheme must not be null! A common scheme would be 'http'.");
		Assert.notNull(errorListener, "ErrorListener must not be null! You may want use a no-op one 'e -> {}' instead.");
		Assert.notNull(headers, "headers must not be null! Think about using 'HttpHeaders.EMPTY' as an alternative.");

		this.cachedClients = new ConcurrentHashMap<>();
		this.scheme = scheme;
		this.connector = connector;
		this.errorListener = errorListener;
		this.headers = headers;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.client.reactive.WebClientProvider#get(java.net.InetSocketAddress)
	 */
	@Override
	public WebClient get(InetSocketAddress endpoint) {

		Assert.notNull(endpoint, "Endpoint must not be empty!");

		return this.cachedClients.computeIfAbsent(endpoint, this::createWebClientForSocketAddress);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.client.reactive.WebClientProvider#getDefaultHeaders()
	 */
	@Override
	public HttpHeaders getDefaultHeaders() {
		return headers;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.client.reactive.WebClientProvider#withDefaultHeaders(org.springframework.http.HttpHeaders)
	 */
	@Override
	public WebClientProvider withDefaultHeaders(HttpHeaders headers) {

		Assert.notNull(headers, "HttpHeaders must not be null.");

		HttpHeaders merged = new HttpHeaders();
		merged.addAll(this.headers);
		merged.addAll(headers);

		return new DefaultWebClientProvider(this.scheme, this.connector, errorListener, merged);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.client.reactive.WebClientProvider#getErrorListener()
	 */
	@Override
	public Consumer<Throwable> getErrorListener() {
		return this.errorListener;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.client.reactive.WebClientProvider#withErrorListener(java.util.function.Consumer)
	 */
	@Override
	public WebClientProvider withErrorListener(Consumer<Throwable> errorListener) {

		Assert.notNull(errorListener, "Error listener must not be null.");

		Consumer<Throwable> listener = this.errorListener.andThen(errorListener);
		return new DefaultWebClientProvider(this.scheme, this.connector, listener, this.headers);
	}

	protected WebClient createWebClientForSocketAddress(InetSocketAddress socketAddress) {

		Builder builder = WebClient.builder().defaultHeaders(it -> it.addAll(getDefaultHeaders()));

		if (connector != null) {
			builder = builder.clientConnector(connector);
		}

		String baseUrl = String.format("%s://%s:%d", this.scheme, socketAddress.getHostString(), socketAddress.getPort());
		return builder.baseUrl(baseUrl).filter((request, next) -> next.exchange(request).doOnError(errorListener)).build();
	}
}
