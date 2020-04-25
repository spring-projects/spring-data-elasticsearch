/*
 * Copyright 2018-2020 the original author or authors.
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
package org.springframework.data.elasticsearch.client;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Default {@link ClientConfiguration} implementation.
 *
 * @author Mark Paluch
 * @author Christoph Strobl
 * @author Huw Ayling-Miller
 * @author Peter-Josef Meisch
 * @since 3.2
 */
class DefaultClientConfiguration implements ClientConfiguration {

	private final List<InetSocketAddress> hosts;
	private final HttpHeaders headers;
	private final boolean useSsl;
	private final @Nullable SSLContext sslContext;
	private final Duration soTimeout;
	private final Duration connectTimeout;
	private final @Nullable String pathPrefix;
	private final @Nullable HostnameVerifier hostnameVerifier;
	private final @Nullable String proxy;
	private final Function<WebClient, WebClient> webClientConfigurer;
	private final Supplier<HttpHeaders> headersSupplier;

	DefaultClientConfiguration(List<InetSocketAddress> hosts, HttpHeaders headers, boolean useSsl,
			@Nullable SSLContext sslContext, Duration soTimeout, Duration connectTimeout, @Nullable String pathPrefix,
			@Nullable HostnameVerifier hostnameVerifier, @Nullable String proxy,
			Function<WebClient, WebClient> webClientConfigurer, Supplier<HttpHeaders> headersSupplier) {

		this.hosts = Collections.unmodifiableList(new ArrayList<>(hosts));
		this.headers = new HttpHeaders(headers);
		this.useSsl = useSsl;
		this.sslContext = sslContext;
		this.soTimeout = soTimeout;
		this.connectTimeout = connectTimeout;
		this.pathPrefix = pathPrefix;
		this.hostnameVerifier = hostnameVerifier;
		this.proxy = proxy;
		this.webClientConfigurer = webClientConfigurer;
		this.headersSupplier = headersSupplier;
	}

	@Override
	public List<InetSocketAddress> getEndpoints() {
		return this.hosts;
	}

	@Override
	public HttpHeaders getDefaultHeaders() {
		return this.headers;
	}

	@Override
	public boolean useSsl() {
		return this.useSsl;
	}

	@Override
	public Optional<SSLContext> getSslContext() {
		return Optional.ofNullable(this.sslContext);
	}

	@Override
	public Optional<HostnameVerifier> getHostNameVerifier() {
		return Optional.ofNullable(this.hostnameVerifier);
	}

	@Override
	public Duration getConnectTimeout() {
		return this.connectTimeout;
	}

	@Override
	public Duration getSocketTimeout() {
		return this.soTimeout;
	}

	@Nullable
	@Override
	public String getPathPrefix() {
		return this.pathPrefix;
	}

	@Override
	public Optional<String> getProxy() {
		return Optional.ofNullable(proxy);
	}

	@Override
	public Function<WebClient, WebClient> getWebClientConfigurer() {
		return webClientConfigurer;
	}

	@Override
	public Supplier<HttpHeaders> getHeadersSupplier() {
		return headersSupplier;
	}
}
