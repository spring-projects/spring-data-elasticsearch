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
package org.springframework.data.elasticsearch.client;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import org.elasticsearch.client.RestClientBuilder.HttpClientConfigCallback;
import org.springframework.data.elasticsearch.client.reactive.ReactiveRestClients;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration interface exposing common client configuration properties for Elasticsearch clients.
 *
 * @author Mark Paluch
 * @author Peter-Josef Meisch
 * @author Huw Ayling-Miller
 * @author Henrique Amaral
 * @since 3.2
 */
public interface ClientConfiguration {

	/**
	 * Creates a new {@link ClientConfigurationBuilder} instance.
	 *
	 * @return a new {@link ClientConfigurationBuilder} instance.
	 */
	static ClientConfigurationBuilderWithRequiredEndpoint builder() {
		return new ClientConfigurationBuilder();
	}

	/**
	 * Creates a new {@link ClientConfiguration} instance configured to localhost.
	 *
	 * <pre class="code">
	 * // "localhost:9200"
	 * ClientConfiguration configuration = ClientConfiguration.localhost();
	 * </pre>
	 *
	 * @return a new {@link ClientConfiguration} instance
	 * @see ClientConfigurationBuilder#connectedToLocalhost()
	 */
	static ClientConfiguration localhost() {
		return new ClientConfigurationBuilder().connectedToLocalhost().build();
	}

	/**
	 * Creates a new {@link ClientConfiguration} instance configured to a single host given {@code hostAndPort}.
	 * For example given the endpoint http://localhost:9200
	 *
	 * <pre class="code">
	 * ClientConfiguration configuration = ClientConfiguration.create("localhost:9200");
	 * </pre>
	 *
	 * @return a new {@link ClientConfigurationBuilder} instance.
	 */
	static ClientConfiguration create(String hostAndPort) {
		return new ClientConfigurationBuilder().connectedTo(hostAndPort).build();
	}

	/**
	 * Creates a new {@link ClientConfiguration} instance configured to a single host given {@link InetSocketAddress}.
	 * For example given the endpoint http://localhost:9200
	 *
	 * <pre class="code">
	 * ClientConfiguration configuration = ClientConfiguration
	 * 		.create(InetSocketAddress.createUnresolved("localhost", 9200));
	 * </pre>
	 *
	 * @return a new {@link ClientConfigurationBuilder} instance.
	 */
	static ClientConfiguration create(InetSocketAddress socketAddress) {
		return new ClientConfigurationBuilder().connectedTo(socketAddress).build();
	}

	/**
	 * Returns the configured endpoints.
	 *
	 * @return the configured endpoints.
	 */
	List<InetSocketAddress> getEndpoints();

	/**
	 * Obtain the {@link HttpHeaders} to be used by default.
	 *
	 * @return the {@link HttpHeaders} to be used by default.
	 */
	HttpHeaders getDefaultHeaders();

	/**
	 * Returns {@literal true} when the client should use SSL.
	 *
	 * @return {@literal true} when the client should use SSL.
	 */
	boolean useSsl();

	/**
	 * Returns the {@link SSLContext} to use. Can be {@link Optional#empty()} if not configured.
	 *
	 * @return the {@link SSLContext} to use. Can be {@link Optional#empty()} if not configured.
	 */
	Optional<SSLContext> getSslContext();

	/**
	 * Returns the {@link HostnameVerifier} to use. Can be {@link Optional#empty()} if not configured.
	 *
	 * @return the {@link HostnameVerifier} to use. Can be {@link Optional#empty()} if not configured.
	 */
	Optional<HostnameVerifier> getHostNameVerifier();

	/**
	 * Returns the {@link java.time.Duration connect timeout}.
	 *
	 * @see java.net.Socket#connect(SocketAddress, int)
	 * @see io.netty.channel.ChannelOption#CONNECT_TIMEOUT_MILLIS
	 */
	Duration getConnectTimeout();

	/**
	 * Returns the {@link java.time.Duration socket timeout} which is typically applied as SO-timeout/read timeout.
	 *
	 * @see java.net.Socket#setSoTimeout(int)
	 * @see io.netty.handler.timeout.ReadTimeoutHandler
	 * @see io.netty.handler.timeout.WriteTimeoutHandler
	 */
	Duration getSocketTimeout();

	/**
	 * Returns the path prefix that should be prepended to HTTP(s) requests for Elasticsearch behind a proxy.
	 *
	 * @return the path prefix.
	 * @since 4.0
	 */
	@Nullable
	String getPathPrefix();

	/**
	 * returns an optionally set proxy in the form host:port
	 *
	 * @return the optional proxy
	 * @since 4.0
	 */
	Optional<String> getProxy();

	/**
	 * @return the function for configuring a WebClient.
	 */
	Function<WebClient, WebClient> getWebClientConfigurer();

	/**
	 * @return the Rest Client configuration callback.
	 * @since 4.2
	 * @deprecated since 4.3 use {@link #getClientConfigurers()}
	 */
	@Deprecated
	HttpClientConfigCallback getHttpClientConfigurer();

	/**
	 * @return the client configuration callbacks
	 * @since 4.3
	 */
	<T> List<ClientConfigurationCallback<?>> getClientConfigurers();

	/**
	 * @return the supplier for custom headers.
	 */
	Supplier<HttpHeaders> getHeadersSupplier();

	/**
	 * @author Christoph Strobl
	 */
	interface ClientConfigurationBuilderWithRequiredEndpoint {

		/**
		 * @param hostAndPort the {@literal host} and {@literal port} formatted as String {@literal host:port}.
		 * @return the {@link MaybeSecureClientConfigurationBuilder}.
		 */
		default MaybeSecureClientConfigurationBuilder connectedTo(String hostAndPort) {
			return connectedTo(new String[] { hostAndPort });
		}

		/**
		 * @param hostAndPorts the list of {@literal host} and {@literal port} combinations formatted as String
		 *          {@literal host:port}.
		 * @return the {@link MaybeSecureClientConfigurationBuilder}.
		 */
		MaybeSecureClientConfigurationBuilder connectedTo(String... hostAndPorts);

		/**
		 * @param endpoint the {@literal host} and {@literal port}.
		 * @return the {@link MaybeSecureClientConfigurationBuilder}.
		 */
		default MaybeSecureClientConfigurationBuilder connectedTo(InetSocketAddress endpoint) {
			return connectedTo(new InetSocketAddress[] { endpoint });
		}

		/**
		 * @param endpoints the list of {@literal host} and {@literal port} combinations.
		 * @return the {@link MaybeSecureClientConfigurationBuilder}.
		 */
		MaybeSecureClientConfigurationBuilder connectedTo(InetSocketAddress... endpoints);

		/**
		 * Obviously for testing.
		 *
		 * @return the {@link MaybeSecureClientConfigurationBuilder}.
		 */
		default MaybeSecureClientConfigurationBuilder connectedToLocalhost() {
			return connectedTo("localhost:9200");
		}
	}

	/**
	 * @author Christoph Strobl
	 */
	interface MaybeSecureClientConfigurationBuilder extends TerminalClientConfigurationBuilder {

		/**
		 * Connect via {@literal https} <br />
		 * <strong>NOTE</strong> You need to leave out the protocol in
		 * {@link ClientConfigurationBuilderWithRequiredEndpoint#connectedTo(String)}.
		 *
		 * @return the {@link TerminalClientConfigurationBuilder}.
		 */
		TerminalClientConfigurationBuilder usingSsl();

		/**
		 * Connect via {@literal https} using the given {@link SSLContext}.<br />
		 * <strong>NOTE</strong> You need to leave out the protocol in
		 * {@link ClientConfigurationBuilderWithRequiredEndpoint#connectedTo(String)}.
		 *
		 * @return the {@link TerminalClientConfigurationBuilder}.
		 */
		TerminalClientConfigurationBuilder usingSsl(SSLContext sslContext);

		/**
		 * Connect via {@literal https} using the givens {@link SSLContext} and HostnameVerifier {@link HostnameVerifier}
		 * .<br />
		 * <strong>NOTE</strong> You need to leave out the protocol in
		 * {@link ClientConfigurationBuilderWithRequiredEndpoint#connectedTo(String)}.
		 *
		 * @return the {@link TerminalClientConfigurationBuilder}.
		 */
		TerminalClientConfigurationBuilder usingSsl(SSLContext sslContext, HostnameVerifier hostnameVerifier);
	}

	/**
	 * @author Christoph Strobl
	 * @author Mark Paluch
	 */
	interface TerminalClientConfigurationBuilder {

		/**
		 * @param defaultHeaders must not be {@literal null}.
		 * @return the {@link TerminalClientConfigurationBuilder}
		 */
		TerminalClientConfigurationBuilder withDefaultHeaders(HttpHeaders defaultHeaders);

		/**
		 * Configure the {@literal milliseconds} for the connect-timeout.
		 *
		 * @param millis the timeout to use.
		 * @return the {@link TerminalClientConfigurationBuilder}
		 * @see #withConnectTimeout(Duration)
		 */
		default TerminalClientConfigurationBuilder withConnectTimeout(long millis) {
			return withConnectTimeout(Duration.ofMillis(millis));
		}

		/**
		 * Configure a {@link java.time.Duration} connect timeout.
		 *
		 * @param timeout the timeout to use. Must not be {@literal null}.
		 * @return the {@link TerminalClientConfigurationBuilder}
		 * @see java.net.Socket#connect(SocketAddress, int)
		 * @see io.netty.channel.ChannelOption#CONNECT_TIMEOUT_MILLIS
		 */
		TerminalClientConfigurationBuilder withConnectTimeout(Duration timeout);

		/**
		 * Configure the {@literal milliseconds} for the socket timeout.
		 *
		 * @param millis the timeout to use.
		 * @return the {@link TerminalClientConfigurationBuilder}
		 * @see #withSocketTimeout(Duration)
		 */
		default TerminalClientConfigurationBuilder withSocketTimeout(long millis) {
			return withSocketTimeout(Duration.ofMillis(millis));
		}

		/**
		 * Configure a {@link java.time.Duration socket timeout} which is typically applied as SO-timeout/read timeout.
		 *
		 * @param timeout the timeout to use. Must not be {@literal null}.
		 * @return the {@link TerminalClientConfigurationBuilder}
		 * @see java.net.Socket#setSoTimeout(int)
		 * @see io.netty.handler.timeout.ReadTimeoutHandler
		 * @see io.netty.handler.timeout.WriteTimeoutHandler
		 */
		TerminalClientConfigurationBuilder withSocketTimeout(Duration timeout);

		/**
		 * Configure the username and password to be sent as a Basic Authentication header
		 *
		 * @param username the username. Must not be {@literal null}.
		 * @param password the password. Must not be {@literal null}.
		 * @return the {@link TerminalClientConfigurationBuilder}
		 */
		TerminalClientConfigurationBuilder withBasicAuth(String username, String password);

		/**
		 * Configure the path prefix that will be prepended to any HTTP(s) requests
		 *
		 * @param pathPrefix the pathPrefix.
		 * @return the {@link TerminalClientConfigurationBuilder}
		 * @since 4.0
		 */
		TerminalClientConfigurationBuilder withPathPrefix(String pathPrefix);

		/**
		 * @param proxy a proxy formatted as String {@literal host:port}.
		 * @return the {@link TerminalClientConfigurationBuilder}.
		 */
		TerminalClientConfigurationBuilder withProxy(String proxy);

		/**
		 * set customization hook in case of a reactive configuration
		 *
		 * @param webClientConfigurer function to configure the WebClient
		 * @return the {@link TerminalClientConfigurationBuilder}.
		 * @deprecated since 4.3, use {@link #withClientConfigurer(ClientConfigurationCallback)} with
		 *             {@link ReactiveRestClients.WebClientConfigurationCallback}
		 */
		@Deprecated
		TerminalClientConfigurationBuilder withWebClientConfigurer(Function<WebClient, WebClient> webClientConfigurer);

		/**
		 * Register a {HttpClientConfigCallback} to configure the non-reactive REST client.
		 *
		 * @param httpClientConfigurer configuration callback, must not be null.
		 * @return the {@link TerminalClientConfigurationBuilder}.
		 * @since 4.2
		 * @deprecated since 4.3, use {@link #withClientConfigurer(ClientConfigurationCallback)} with
		 *             {@link RestClients.RestClientConfigurationCallback}
		 */
		@Deprecated
		TerminalClientConfigurationBuilder withHttpClientConfigurer(HttpClientConfigCallback httpClientConfigurer);

		/**
		 * Register a {@link ClientConfigurationCallback} to configure the client.
		 *
		 * @param clientConfigurer configuration callback, must not be {@literal null}.
		 * @return the {@link TerminalClientConfigurationBuilder}.
		 * @since 4.3
		 */
		TerminalClientConfigurationBuilder withClientConfigurer(ClientConfigurationCallback<?> clientConfigurer);

		/**
		 * set a supplier for custom headers. This is invoked for every HTTP request to Elasticsearch to retrieve headers
		 * that should be sent with the request. A common use case is passing in authentication headers that may change.
		 * <br/>
		 * Note: When used in a reactive environment, the calling of {@link Supplier#get()} function must not do any
		 * blocking operations. It may return {@literal null}.
		 *
		 * @param headers supplier function for headers, must not be {@literal null}
		 * @return the {@link TerminalClientConfigurationBuilder}.
		 * @since 4.0
		 */
		TerminalClientConfigurationBuilder withHeaders(Supplier<HttpHeaders> headers);

		/**
		 * Build the {@link ClientConfiguration} object.
		 *
		 * @return the {@link ClientConfiguration} object.
		 */
		ClientConfiguration build();
	}

	/**
	 * Callback to be executed to configure a client.
	 *
	 * @param <T> the type of the client configuration class.
	 * @since 4.3
	 */
	@FunctionalInterface
	interface ClientConfigurationCallback<T> {
		T configure(T clientConfigurer);
	}
}
