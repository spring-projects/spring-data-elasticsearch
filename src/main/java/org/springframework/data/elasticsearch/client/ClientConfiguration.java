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
package org.springframework.data.elasticsearch.client;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import javax.net.ssl.SSLContext;

import org.springframework.http.HttpHeaders;

/**
 * Configuration interface exposing common client configuration properties for Elasticsearch clients.
 *
 * @author Mark Paluch
 * @author Peter-Josef Meisch
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
	 * <p/>
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
	 * <p/>
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
	 * <p/>
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
	 * Returns the {@link SSLContext} to use. Can be {@link Optional#empty()} if unconfigured.
	 *
	 * @return the {@link SSLContext} to use. Can be {@link Optional#empty()} if unconfigured.
	 */
	Optional<SSLContext> getSslContext();

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
		 * Configure the {@literal milliseconds} for the connect timeout.
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
		 * Build the {@link ClientConfiguration} object.
		 *
		 * @return the {@link ClientConfiguration} object.
		 */
		ClientConfiguration build();
	}
}
