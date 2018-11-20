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

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.net.ssl.SSLContext;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;

/**
 * Utility class for common access to Elasticsearch clients. {@link RestClients} consolidates set up routines for the
 * various drivers into a single place.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 4.0
 */
public final class RestClients {

	private RestClients() {}

	/**
	 * Start here to create a new client tailored to your needs.
	 *
	 * @return new instance of {@link ElasticsearchRestClient}.
	 */
	public static ElasticsearchRestClient create(ClientConfiguration clientConfiguration) {

		Assert.notNull(clientConfiguration, "ClientConfiguration must not be null!");

		HttpHost[] httpHosts = formattedHosts(clientConfiguration.getHosts(), clientConfiguration.useSsl()).stream()
				.map(HttpHost::create).toArray(HttpHost[]::new);
		RestClientBuilder builder = RestClient.builder(httpHosts);
		HttpHeaders headers = clientConfiguration.getDefaultHeaders();

		if (!headers.isEmpty()) {

			Header[] httpHeaders = headers.toSingleValueMap().entrySet().stream()
					.map(it -> new BasicHeader(it.getKey(), it.getValue())).toArray(Header[]::new);
			builder.setDefaultHeaders(httpHeaders);
		}

		builder.setHttpClientConfigCallback(clientBuilder -> {
			Optional<SSLContext> sslContext = clientConfiguration.getSslContext();
			sslContext.ifPresent(clientBuilder::setSSLContext);

			return clientBuilder;
		});

		RestHighLevelClient client = new RestHighLevelClient(builder);
		return () -> client;
	}

	private static List<String> formattedHosts(List<String> hosts, boolean useSsl) {
		return hosts.stream().map(it -> it.startsWith("http") ? it : (useSsl ? "https" : "http") + "://" + it)
				.collect(Collectors.toList());
	}

	/**
	 * @author Christoph Strobl
	 */
	@FunctionalInterface
	public interface ElasticsearchRestClient extends Closeable {

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

		@Override
		default void close() throws IOException {
			rest().close();
		}
	}
}
