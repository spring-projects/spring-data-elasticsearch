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
package org.springframework.data.elasticsearch.client;

import java.util.List;
import java.util.Optional;

import javax.net.ssl.SSLContext;

import org.springframework.http.HttpHeaders;

/**
 * Configuration interface exposing common client configuration properties for Elasticsearch clients.
 *
 * @author Mark Paluch
 * @since 4.0
 */
public interface ClientConfiguration {

	/**
	 * Creates a new {@link ClientConfigurationBuilder} instance.
	 *
	 * @return a new {@link ClientConfigurationBuilder} instance.
	 */
	static ClientConfigurationBuilderWithRequiredHost builder() {
		return new ClientConfigurationBuilder();
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
	 * Returns the configured hosts. Hosts contain the hostname and port.
	 *
	 * @return the configured hosts. Hosts contain the hostname and port.
	 */
	List<String> getHosts();

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
	 * @author Christoph Strobl
	 */
	interface ClientConfigurationBuilderWithRequiredHost {

		/**
		 * @param hostAndPort the {@literal host} and {@literal port} formatted as String {@literal host:port}. You may
		 *          leave out {@literal http / https} and use {@link MaybeSecureClientConfigurationBuilder#usingSsl()
		 *          viaSsl}.
		 * @return the {@link MaybeSecureClientConfigurationBuilder}.
		 */
		default MaybeSecureClientConfigurationBuilder connectedTo(String hostAndPort) {
			return connectedTo(new String[] { hostAndPort });
		}

		/**
		 * @param hostAndPorts the list of {@literal host} and {@literal port} combinations formatted as String
		 *          {@literal host:port}. You may leave out {@literal http / https} and use
		 *          {@link MaybeSecureClientConfigurationBuilder#usingSsl() viaSsl}.
		 * @return the {@link MaybeSecureClientConfigurationBuilder}.
		 */
		MaybeSecureClientConfigurationBuilder connectedTo(String... hostAndPorts);

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
	interface MaybeSecureClientConfigurationBuilder extends ClientConfigurationBuilderWithOptionalDefaultHeaders {

		/**
		 * Connect via {@literal https} <br />
		 * <strong>NOTE</strong> You need to leave out the protocol in
		 * {@link ClientConfigurationBuilderWithRequiredHost#connectedTo(String)}.
		 *
		 * @return the {@link ClientConfigurationBuilderWithOptionalDefaultHeaders}.
		 */
		ClientConfigurationBuilderWithOptionalDefaultHeaders usingSsl();

		/**
		 * Connect via {@literal https} using the given {@link SSLContext}.<br />
		 * <strong>NOTE</strong> You need to leave out the protocol in
		 * {@link ClientConfigurationBuilderWithRequiredHost#connectedTo(String)}.
		 *
		 * @return the {@link ClientConfigurationBuilderWithOptionalDefaultHeaders}.
		 */
		ClientConfigurationBuilderWithOptionalDefaultHeaders usingSsl(SSLContext sslContext);
	}

	/**
	 * @author Christoph Strobl
	 * @author Mark Paluch
	 */
	interface ClientConfigurationBuilderWithOptionalDefaultHeaders {

		/**
		 * @param defaultHeaders
		 * @return the {@link ClientConfigurationBuilderWithOptionalDefaultHeaders}
		 */
		ClientConfigurationBuilderWithOptionalDefaultHeaders withDefaultHeaders(HttpHeaders defaultHeaders);

		/**
		 * Build the {@link ClientConfiguration} object.
		 *
		 * @return the {@link ClientConfiguration} object.
		 */
		ClientConfiguration build();
	}
}
