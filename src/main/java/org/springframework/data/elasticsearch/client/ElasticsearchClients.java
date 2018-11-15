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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;

/**
 * Utility class for common access to Elasticsearch clients. {@link ElasticsearchClients} consolidates set up routines
 * for the various drivers into a single place.
 *
 * @author Christoph Strobl
 * @since 4.0
 */
public final class ElasticsearchClients {

	private ElasticsearchClients() {}

	/**
	 * Start here to create a new client tailored to your needs.
	 *
	 * @return new instance of {@link ClientBuilderWithRequiredHost}.
	 */
	public static ClientBuilderWithRequiredHost createClient() {
		return new ElasticsearchClientBuilderImpl();
	}

	/**
	 * @author Christoph Strobl
	 */
	public interface ElasticsearchClientBuilder {

		/**
		 * Apply the configuration to create a {@link ReactiveElasticsearchClient}.
		 *
		 * @return new instance of {@link ReactiveElasticsearchClient}.
		 */
		ReactiveElasticsearchClient reactive();

		/**
		 * Apply the configuration to create a {@link RestHighLevelClient}.
		 *
		 * @return new instance of {@link RestHighLevelClient}.
		 */
		RestHighLevelClient rest();

		/**
		 * Apply the configuration to create a {@link RestClient}.
		 *
		 * @return new instance of {@link RestClient}.
		 */
		default RestClient lowLevelRest() {
			return rest().getLowLevelClient();
		}
	}

	/**
	 * @author Christoph Strobl
	 */
	public interface ClientBuilderWithRequiredHost {

		/**
		 * @param host the {@literal host} and {@literal port} formatted as String {@literal host:port}. You may leave out
		 *          {@literal http / https} and use {@link MaybeSecureClientBuilder#viaSsl() viaSsl}.
		 * @return the {@link MaybeSecureClientBuilder}.
		 */
		default MaybeSecureClientBuilder connectedTo(String host) {
			return connectedTo(new String[] { host });
		}

		/**
		 * @param hosts the list of {@literal host} and {@literal port} combinations formatted as String
		 *          {@literal host:port}. You may leave out {@literal http / https} and use
		 *          {@link MaybeSecureClientBuilder#viaSsl() viaSsl}.
		 * @return the {@link MaybeSecureClientBuilder}.
		 */
		MaybeSecureClientBuilder connectedTo(String... hosts);

		/**
		 * Obviously for testing.
		 *
		 * @return the {@link MaybeSecureClientBuilder}.
		 */
		default MaybeSecureClientBuilder connectedToLocalhost() {
			return connectedTo("localhost:9200");
		}
	}

	/**
	 * @author Christoph Strobl
	 */
	public interface MaybeSecureClientBuilder extends ClientBuilderWithOptionalDefaultHeaders {

		/**
		 * Connect via {@literal https} <br />
		 * <strong>NOTE</strong> You need to leave out the protocol in
		 * {@link ClientBuilderWithRequiredHost#connectedTo(String)}.
		 * 
		 * @return the {@link ClientBuilderWithOptionalDefaultHeaders}.
		 */
		ClientBuilderWithOptionalDefaultHeaders viaSsl();
	}

	/**
	 * @author Christoph Strobl
	 */
	public interface ClientBuilderWithOptionalDefaultHeaders extends ElasticsearchClientBuilder {

		/**
		 * @param defaultHeaders
		 * @return the {@link ElasticsearchClientBuilder}
		 */
		ElasticsearchClientBuilder withDefaultHeaders(HttpHeaders defaultHeaders);
	}

	static class ElasticsearchClientBuilderImpl
			implements ElasticsearchClientBuilder, ClientBuilderWithRequiredHost, MaybeSecureClientBuilder {

		private List<String> hosts = new ArrayList<>();
		private HttpHeaders headers = HttpHeaders.EMPTY;
		private String protocoll = "http";

		@Override
		public ReactiveElasticsearchClient reactive() {
			return DefaultReactiveElasticsearchClient.create(headers, formattedHosts().toArray(new String[0]));
		}

		@Override
		public RestHighLevelClient rest() {

			HttpHost[] httpHosts = formattedHosts().stream().map(HttpHost::create).toArray(HttpHost[]::new);
			RestClientBuilder builder = RestClient.builder(httpHosts);

			if (!headers.isEmpty()) {

				Header[] httpHeaders = headers.toSingleValueMap().entrySet().stream()
						.map(it -> new BasicHeader(it.getKey(), it.getValue())).toArray(Header[]::new);
				builder = builder.setDefaultHeaders(httpHeaders);
			}

			return new RestHighLevelClient(builder);
		}

		@Override
		public MaybeSecureClientBuilder connectedTo(String... hosts) {

			Assert.notEmpty(hosts, "At least one host is required.");
			this.hosts.addAll(Arrays.asList(hosts));
			return this;
		}

		@Override
		public ClientBuilderWithOptionalDefaultHeaders withDefaultHeaders(HttpHeaders defaultHeaders) {

			Assert.notNull(defaultHeaders, "DefaultHeaders must not be null!");
			this.headers = defaultHeaders;
			return this;
		}

		List<String> formattedHosts() {
			return hosts.stream().map(it -> it.startsWith("http") ? it : protocoll + "://" + it).collect(Collectors.toList());
		}

		@Override
		public ClientBuilderWithOptionalDefaultHeaders viaSsl() {
			this.protocoll = "https";
			return this;
		}
	}
}
