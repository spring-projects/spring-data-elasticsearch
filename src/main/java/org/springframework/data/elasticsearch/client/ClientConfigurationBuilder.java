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
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import org.springframework.data.elasticsearch.client.ClientConfiguration.ClientConfigurationBuilderWithRequiredEndpoint;
import org.springframework.data.elasticsearch.client.ClientConfiguration.MaybeSecureClientConfigurationBuilder;
import org.springframework.data.elasticsearch.client.ClientConfiguration.TerminalClientConfigurationBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Default builder implementation for {@link ClientConfiguration}.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @author Peter-Josef Meisch
 * @author Huw Ayling-Miller
 * @author Henrique Amaral
 * @since 3.2
 */
class ClientConfigurationBuilder
		implements ClientConfigurationBuilderWithRequiredEndpoint, MaybeSecureClientConfigurationBuilder {

	private List<InetSocketAddress> hosts = new ArrayList<>();
	private HttpHeaders headers = HttpHeaders.EMPTY;
	private boolean useSsl;
	private @Nullable SSLContext sslContext;
	private @Nullable HostnameVerifier hostnameVerifier;
	private Duration connectTimeout = Duration.ofSeconds(10);
	private Duration soTimeout = Duration.ofSeconds(5);
	private @Nullable String username;
	private @Nullable String password;
	private @Nullable String pathPrefix;
	private @Nullable String proxy;
	private Function<WebClient, WebClient> webClientConfigurer = Function.identity();
	private Supplier<HttpHeaders> headersSupplier = () -> HttpHeaders.EMPTY;

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.client.ClientConfiguration.ClientConfigurationBuilderWithRequiredEndpoint#connectedTo(java.lang.String[])
	 */
	@Override
	public MaybeSecureClientConfigurationBuilder connectedTo(String... hostAndPorts) {

		Assert.notEmpty(hostAndPorts, "At least one host is required");

		this.hosts.addAll(Arrays.stream(hostAndPorts).map(ClientConfigurationBuilder::parse).collect(Collectors.toList()));
		return this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.client.ClientConfiguration.ClientConfigurationBuilderWithRequiredEndpoint#connectedTo(java.net.InetSocketAddress[])
	 */
	@Override
	public MaybeSecureClientConfigurationBuilder connectedTo(InetSocketAddress... endpoints) {

		Assert.notEmpty(endpoints, "At least one endpoint is required");

		this.hosts.addAll(Arrays.asList(endpoints));

		return this;
	}

	@Override
	public MaybeSecureClientConfigurationBuilder withProxy(String proxy) {
		Assert.hasLength(proxy, "proxy must not be null or empty");
		this.proxy = proxy;
		return this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.client.ClientConfiguration.MaybeSecureClientConfigurationBuilder#usingSsl()
	 */
	@Override
	public TerminalClientConfigurationBuilder usingSsl() {

		this.useSsl = true;
		return this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.client.ClientConfiguration.MaybeSecureClientConfigurationBuilder#usingSsl(javax.net.ssl.SSLContext)
	 */
	@Override
	public TerminalClientConfigurationBuilder usingSsl(SSLContext sslContext) {

		Assert.notNull(sslContext, "SSL Context must not be null");

		this.useSsl = true;
		this.sslContext = sslContext;
		return this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.client.ClientConfiguration.MaybeSecureClientConfigurationBuilder#usingSsl(javax.net.ssl.SSLContext, javax.net.ssl.HostnameVerifier)
	 */
	@Override
	public TerminalClientConfigurationBuilder usingSsl(SSLContext sslContext, HostnameVerifier hostnameVerifier) {

		Assert.notNull(sslContext, "SSL Context must not be null");
		Assert.notNull(hostnameVerifier, "Host Name Verifier must not be null");

		this.useSsl = true;
		this.sslContext = sslContext;
		this.hostnameVerifier = hostnameVerifier;
		return this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.client.ClientConfiguration.TerminalClientConfigurationBuilder#withDefaultHeaders(org.springframework.http.HttpHeaders)
	 */
	@Override
	public TerminalClientConfigurationBuilder withDefaultHeaders(HttpHeaders defaultHeaders) {

		Assert.notNull(defaultHeaders, "Default HTTP headers must not be null");

		this.headers = new HttpHeaders();
		this.headers.addAll(defaultHeaders);

		return this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.client.ClientConfiguration.TerminalClientConfigurationBuilder#withConnectTimeout(java.time.Duration)
	 */
	@Override
	public TerminalClientConfigurationBuilder withConnectTimeout(Duration timeout) {

		Assert.notNull(timeout, "I/O timeout must not be null!");

		this.connectTimeout = timeout;
		return this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.client.ClientConfiguration.TerminalClientConfigurationBuilder#withTimeout(java.time.Duration)
	 */
	@Override
	public TerminalClientConfigurationBuilder withSocketTimeout(Duration timeout) {

		Assert.notNull(timeout, "Socket timeout must not be null!");

		this.soTimeout = timeout;
		return this;
	}

	@Override
	public TerminalClientConfigurationBuilder withBasicAuth(String username, String password) {

		Assert.notNull(username, "username must not be null");
		Assert.notNull(password, "password must not be null");

		this.username = username;
		this.password = password;

		return this;
	}

	@Override
	public TerminalClientConfigurationBuilder withPathPrefix(String pathPrefix) {
		this.pathPrefix = pathPrefix;

		return this;
	}

	@Override
	public TerminalClientConfigurationBuilder withWebClientConfigurer(
			Function<WebClient, WebClient> webClientConfigurer) {

		Assert.notNull(webClientConfigurer, "webClientConfigurer must not be null");

		this.webClientConfigurer = webClientConfigurer;
		return this;
	}

	@Override
	public TerminalClientConfigurationBuilder withHeaders(Supplier<HttpHeaders> headers) {

		Assert.notNull(headers, "headersSupplier must not be null");

		this.headersSupplier = headers;
		return this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.client.ClientConfiguration.ClientConfigurationBuilderWithOptionalDefaultHeaders#build()
	 */
	@Override
	public ClientConfiguration build() {

		if (username != null && password != null) {
			if (HttpHeaders.EMPTY.equals(headers)) {
				headers = new HttpHeaders();
			}
			headers.setBasicAuth(username, password);
		}

		return new DefaultClientConfiguration(hosts, headers, useSsl, sslContext, soTimeout, connectTimeout, pathPrefix,
				hostnameVerifier, proxy, webClientConfigurer, headersSupplier);
	}

	private static InetSocketAddress parse(String hostAndPort) {
		return InetSocketAddressParser.parse(hostAndPort, ElasticsearchHost.DEFAULT_PORT);
	}

}
