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

import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.TransportOptions;
import co.elastic.clients.transport.rest_client.RestClientOptions;

import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.config.ElasticsearchConfigurationSupport;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.util.Assert;

/**
 * Base class for a @{@link org.springframework.context.annotation.Configuration} class to set up the Elasticsearch
 * connection using the {@link ReactiveElasticsearchClient}.
 *
 * @author Peter-Josef Meisch
 * @since 4.4
 */
public abstract class ReactiveElasticsearchConfiguration extends ElasticsearchConfigurationSupport {

	/**
	 * Must be implemented by deriving classes to provide the {@link ClientConfiguration}.
	 *
	 * @return configuration, must not be {@literal null}
	 */
	@Bean(name = "elasticsearchClientConfiguration")
	public abstract ClientConfiguration clientConfiguration();

	/**
	 * Provides the underlying low level RestClient.
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
	 * Provides the {@link ReactiveElasticsearchClient} instance used.
	 *
	 * @param restClient the low level RestClient to use
	 * @return ReactiveElasticsearchClient instance.
	 */
	@Bean
	public ReactiveElasticsearchClient reactiveElasticsearchClient(RestClient restClient, JsonpMapper jsonpMapper) {

		Assert.notNull(restClient, "restClient must not be null");

		return ElasticsearchClients.createReactive(restClient, transportOptions(), jsonpMapper);
	}

	/**
	 * Creates {@link ReactiveElasticsearchOperations}.
	 *
	 * @return never {@literal null}.
	 */
	@Bean(name = { "reactiveElasticsearchOperations", "reactiveElasticsearchTemplate" })
	public ReactiveElasticsearchOperations reactiveElasticsearchOperations(ElasticsearchConverter elasticsearchConverter,
			ReactiveElasticsearchClient reactiveElasticsearchClient) {

		ReactiveElasticsearchTemplate template = new ReactiveElasticsearchTemplate(reactiveElasticsearchClient,
				elasticsearchConverter);
		template.setRefreshPolicy(refreshPolicy());

		return template;
	}

	/**
	 * Provides the JsonpMapper that is used in the {@link #reactiveElasticsearchClient(RestClient, JsonpMapper)} method
	 * and exposes it as a bean.
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
		return new RestClientOptions(RequestOptions.DEFAULT).toBuilder().build();
	}
}
