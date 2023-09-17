/*
 * Copyright 2021-2023 the original author or authors.
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
package org.springframework.data.elasticsearch.client.elc;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.TransportOptions;
import co.elastic.clients.transport.rest_client.RestClientOptions;

import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.config.ElasticsearchConfigurationSupport;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.util.Assert;

/**
 * Base class for a @{@link org.springframework.context.annotation.Configuration} class to set up the Elasticsearch
 * connection using the Elasticsearch Client. This class exposes different parts of the setup as Spring beans. Deriving
 * classes must provide the {@link ClientConfiguration} to use.
 *
 * @author Peter-Josef Meisch
 * @since 4.4
 */
public abstract class ElasticsearchConfiguration extends ElasticsearchConfigurationSupport {

	/**
	 * Must be implemented by deriving classes to provide the {@link ClientConfiguration}.
	 *
	 * @return configuration, must not be {@literal null}
	 */
	@Bean(name = "elasticsearchClientConfiguration")
	public abstract ClientConfiguration clientConfiguration();

	/**
	 * Provides the underlying low level Elasticsearch RestClient.
	 *
	 * @param clientConfiguration configuration for the client, must not be {@literal null}
	 * @return RestClient
	 */
	@Bean
	public RestClient elasticsearchRestClient(ClientConfiguration clientConfiguration) {

		Assert.notNull(clientConfiguration, "clientConfiguration must not be null");

		return ElasticsearchClients.getRestClient(clientConfiguration);
	}

	/**
	 * Provides the Elasticsearch transport to be used. The default implementation uses the {@link RestClient} bean and
	 * the {@link JsonpMapper} bean provided in this class.
	 *
	 * @return the {@link ElasticsearchTransport}
	 * @since 5.2
	 */
	@Bean
	public ElasticsearchTransport elasticsearchTransport(RestClient restClient, JsonpMapper jsonpMapper) {

		Assert.notNull(restClient, "restClient must not be null");
		Assert.notNull(jsonpMapper, "jsonpMapper must not be null");

		return ElasticsearchClients.getElasticsearchTransport(restClient, ElasticsearchClients.IMPERATIVE_CLIENT,
				transportOptions(), jsonpMapper);
	}

	/**
	 * Provides the {@link ElasticsearchClient} to be used.
	 *
	 * @param transport the {@link ElasticsearchTransport} to use
	 * @return ElasticsearchClient instance
	 */
	@Bean
	public ElasticsearchClient elasticsearchClient(ElasticsearchTransport transport) {

		Assert.notNull(transport, "transport must not be null");

		return ElasticsearchClients.createImperative(transport);
	}

	/**
	 * Creates a {@link ElasticsearchOperations} implementation using an
	 * {@link co.elastic.clients.elasticsearch.ElasticsearchClient}.
	 *
	 * @return never {@literal null}.
	 */
	@Bean(name = { "elasticsearchOperations", "elasticsearchTemplate" })
	public ElasticsearchOperations elasticsearchOperations(ElasticsearchConverter elasticsearchConverter,
			ElasticsearchClient elasticsearchClient) {

		ElasticsearchTemplate template = new ElasticsearchTemplate(elasticsearchClient, elasticsearchConverter);
		template.setRefreshPolicy(refreshPolicy());

		return template;
	}

	/**
	 * Provides the JsonpMapper bean that is used in the {@link #elasticsearchTransport(RestClient, JsonpMapper)} method.
	 *
	 * @return the {@link JsonpMapper} to use
	 * @since 5.2
	 */
	@Bean
	public JsonpMapper jsonpMapper() {
		return new JacksonJsonpMapper();
	}

	/**
	 * @return the options that should be added to every request. Must not be {@literal null}
	 */
	public TransportOptions transportOptions() {
		return new RestClientOptions(RequestOptions.DEFAULT);
	}
}
