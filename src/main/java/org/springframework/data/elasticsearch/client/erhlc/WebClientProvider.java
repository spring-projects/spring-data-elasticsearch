/*
 * Copyright 2018-2022 the original author or authors.
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

import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.IdentityCipherSuiteFilter;
import io.netty.handler.ssl.JdkSslContext;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.ProxyProvider;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.net.ssl.SSLContext;

import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Provider for {@link WebClient}s using a pre-configured {@code scheme}. This class returns {@link WebClient} for a
 * specific {@link InetSocketAddress endpoint} and encapsulates common configuration aspects of {@link WebClient} so
 * that code using {@link WebClient} is not required to apply further configuration to the actual client. <br/>
 * Client instances are typically cached allowing reuse of pooled connections if configured on the
 * {@link ClientHttpConnector}.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @author Huw Ayling-Miller
 * @author Peter-Josef Meisch
 * @since 3.2
 * @deprecated since 5.0
 */
@Deprecated
public interface WebClientProvider {

	/**
	 * Creates a new {@link WebClientProvider} using the {@code http} scheme and a default {@link ClientHttpConnector}.
	 *
	 * @return the resulting {@link WebClientProvider}.
	 */
	static WebClientProvider http() {
		return create("http");
	}

	/**
	 * Creates a new {@link WebClientProvider} using the given {@code scheme} and a default {@link ClientHttpConnector}.
	 *
	 * @param scheme protocol scheme such as {@literal http} or {@literal https}.
	 * @return the resulting {@link WebClientProvider}.
	 */
	static WebClientProvider create(String scheme) {

		Assert.hasText(scheme, "Protocol scheme must not be empty");

		return new DefaultWebClientProvider(scheme, null);
	}

	/**
	 * Creates a new {@link WebClientProvider} given {@code scheme} and {@link ClientHttpConnector}.
	 *
	 * @param scheme protocol scheme such as {@literal http} or {@literal https}.
	 * @param connector the HTTP connector to use. Can be {@literal null}.
	 * @return the resulting {@link WebClientProvider}.
	 */
	static WebClientProvider create(String scheme, @Nullable ClientHttpConnector connector) {

		Assert.hasText(scheme, "Protocol scheme must not be empty");

		return new DefaultWebClientProvider(scheme, connector);
	}

	/**
	 * Obtain the {@link WebClient} configured with {@link #withDefaultHeaders(HttpHeaders) default HTTP headers} and
	 * {@link Consumer} error callback for a given {@link InetSocketAddress endpoint}.
	 *
	 * @return the {@link WebClient} for the given {@link InetSocketAddress endpoint}.
	 */
	WebClient get(InetSocketAddress endpoint);

	/**
	 * Obtain the {@link HttpHeaders} to be used by default.
	 *
	 * @return never {@literal null}. {@link HttpHeaders#EMPTY} by default.
	 */
	HttpHeaders getDefaultHeaders();

	/**
	 * Obtain the {@link Consumer error listener} to be used;
	 *
	 * @return never {@literal null}.
	 */
	Consumer<Throwable> getErrorListener();

	/**
	 * Obtain the {@link String pathPrefix} to be used.
	 *
	 * @return the pathPrefix if set.
	 * @since 4.0
	 */
	@Nullable
	String getPathPrefix();

	/**
	 * Create a new instance of {@link WebClientProvider} applying the given headers by default.
	 *
	 * @param headers must not be {@literal null}.
	 * @return new instance of {@link WebClientProvider}.
	 */
	WebClientProvider withDefaultHeaders(HttpHeaders headers);

	/**
	 * Create a new instance of {@link WebClientProvider} calling the given {@link Consumer} on error.
	 *
	 * @param errorListener must not be {@literal null}.
	 * @return new instance of {@link WebClientProvider}.
	 */
	WebClientProvider withErrorListener(Consumer<Throwable> errorListener);

	/**
	 * Create a new instance of {@link WebClientProvider} where HTTP requests are called with the given path prefix.
	 *
	 * @param pathPrefix Path prefix to add to requests
	 * @return new instance of {@link WebClientProvider}
	 * @since 4.0
	 */
	WebClientProvider withPathPrefix(String pathPrefix);

	/**
	 * Create a new instance of {@link WebClientProvider} calling the given {@link Function} to configure the
	 * {@link WebClient}.
	 *
	 * @param webClientConfigurer configuration function
	 * @return new instance of {@link WebClientProvider}
	 * @since 4.0
	 */
	WebClientProvider withWebClientConfigurer(Function<WebClient, WebClient> webClientConfigurer);

	/**
	 * Create a new instance of {@link WebClientProvider} calling the given {@link Consumer} to configure the requests of
	 * this {@link WebClient}.
	 *
	 * @param requestConfigurer request configuration callback
	 * @return new instance of {@link WebClientProvider}
	 * @since 4.3
	 */
	WebClientProvider withRequestConfigurer(Consumer<WebClient.RequestHeadersSpec<?>> requestConfigurer);

	/**
	 * Creates a {@link WebClientProvider} for the given configuration
	 *
	 * @param clientConfiguration must not be {@literal} null
	 * @return the {@link WebClientProvider}
	 * @since 4.3
	 */
	static WebClientProvider getWebClientProvider(ClientConfiguration clientConfiguration) {

		Assert.notNull(clientConfiguration, "clientConfiguration must not be null");

		Duration connectTimeout = clientConfiguration.getConnectTimeout();
		Duration soTimeout = clientConfiguration.getSocketTimeout();

		HttpClient httpClient = HttpClient.create().compress(true);

		if (!connectTimeout.isNegative()) {
			httpClient = httpClient.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Math.toIntExact(connectTimeout.toMillis()));
		}

		if (!soTimeout.isNegative()) {
			httpClient = httpClient.doOnConnected(connection -> connection //
					.addHandlerLast(new ReadTimeoutHandler(soTimeout.toMillis(), TimeUnit.MILLISECONDS))
					.addHandlerLast(new WriteTimeoutHandler(soTimeout.toMillis(), TimeUnit.MILLISECONDS)));
		}

		if (clientConfiguration.getProxy().isPresent()) {
			String proxy = clientConfiguration.getProxy().get();
			String[] hostPort = proxy.split(":");

			if (hostPort.length != 2) {
				throw new IllegalArgumentException("invalid proxy configuration " + proxy + ", should be \"host:port\"");
			}
			httpClient = httpClient.proxy(proxyOptions -> proxyOptions.type(ProxyProvider.Proxy.HTTP).host(hostPort[0])
					.port(Integer.parseInt(hostPort[1])));
		}

		String scheme = "http";

		if (clientConfiguration.useSsl()) {

			Optional<SSLContext> sslContext = clientConfiguration.getSslContext();

			if (sslContext.isPresent()) {
				httpClient = httpClient
						.secure(sslContextSpec -> sslContextSpec.sslContext(new JdkSslContext(sslContext.get(), true, null,
								IdentityCipherSuiteFilter.INSTANCE, ApplicationProtocolConfig.DISABLED, ClientAuth.NONE, null, false)));
			} else {
				httpClient = httpClient.secure();
			}

			scheme = "https";
		}

		WebClientProvider provider = WebClientProvider.create(scheme, new ReactorClientHttpConnector(httpClient));

		if (clientConfiguration.getPathPrefix() != null) {
			provider = provider.withPathPrefix(clientConfiguration.getPathPrefix());
		}

		Function<WebClient, WebClient> webClientConfigurer = webClient -> {
			for (ClientConfiguration.ClientConfigurationCallback<?> clientConfigurer : clientConfiguration
					.getClientConfigurers()) {

				if (clientConfigurer instanceof ReactiveRestClients.WebClientConfigurationCallback) {
					ReactiveRestClients.WebClientConfigurationCallback webClientConfigurationCallback = (ReactiveRestClients.WebClientConfigurationCallback) clientConfigurer;
					webClient = webClientConfigurationCallback.configure(webClient);
				}
			}
			return webClient;
		};

		provider = provider //
				.withDefaultHeaders(clientConfiguration.getDefaultHeaders()) //
				.withWebClientConfigurer(webClientConfigurer) //
				.withRequestConfigurer(requestHeadersSpec -> requestHeadersSpec //
						.headers(httpHeaders -> {
							HttpHeaders suppliedHeaders = clientConfiguration.getHeadersSupplier().get();

							if (suppliedHeaders != null && suppliedHeaders != HttpHeaders.EMPTY) {
								httpHeaders.addAll(suppliedHeaders);
							}

							// this WebClientProvider is built with ES 7 and not used on 8 anymore
							httpHeaders.add("Accept", "application/vnd.elasticsearch+json;compatible-with=7");

							var contentTypeHeader = "Content-Type";
							if (httpHeaders.containsKey(contentTypeHeader)) {
								httpHeaders.remove(contentTypeHeader);
							}
							httpHeaders.add(contentTypeHeader, "application/vnd.elasticsearch+json;compatible-with=7");

						}));

		return provider;
	}
}
