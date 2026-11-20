/*
 * Copyright 2021-present the original author or authors.
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

import static org.springframework.data.elasticsearch.client.elc.rest5_client.Rest5Clients.*;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.json.jackson.Jackson3JsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.TransportOptions;
import co.elastic.clients.transport.rest5_client.Rest5ClientOptions;
import co.elastic.clients.transport.rest5_client.Rest5ClientTransport;
import co.elastic.clients.transport.rest5_client.low_level.RequestOptions;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import org.jspecify.annotations.Nullable;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.support.VersionInfo;
import org.springframework.data.util.Lazy;
import org.springframework.util.Assert;

/**
 * Utility class to create the different Elasticsearch clients. The RestClient class is the one used in Elasticsearch
 * until version 9, it is still available, but it's use is deprecated. The Rest5Client class is the one that should be
 * used from Elasticsearch 9 on.
 *
 * @author Peter-Josef Meisch
 * @author Minju Kim
 * @since 4.4
 */
@SuppressWarnings("unused")
public final class ElasticsearchClients {
	/**
	 * Name of whose value can be used to correlate log messages for this request.
	 */
	private static final String X_SPRING_DATA_ELASTICSEARCH_CLIENT = "X-SpringDataElasticsearch-Client";
	public static final String IMPERATIVE_CLIENT = "imperative";
	public static final String REACTIVE_CLIENT = "reactive";

	private static final Lazy<JsonpMapper> DEFAULT_JACKSON3_JSONP_MAPPER = Lazy.of(() -> new Jackson3JsonpMapper());

	// region reactive client
	/**
	 * Creates a new {@link ReactiveElasticsearchClient}.
	 *
	 * @param clientConfiguration configuration options, must not be {@literal null}.
	 * @return the {@link ReactiveElasticsearchClient}
	 */
	public static ReactiveElasticsearchClient createReactive(ClientConfiguration clientConfiguration) {

		Assert.notNull(clientConfiguration, "clientConfiguration must not be null");

		return createReactive(getRest5Client(clientConfiguration), null, DEFAULT_JACKSON3_JSONP_MAPPER.get());
	}

	/**
	 * Creates a new {@link ReactiveElasticsearchClient}.
	 *
	 * @param clientConfiguration configuration options, must not be {@literal null}.
	 * @param transportOptions options to be added to each request.
	 * @return the {@link ReactiveElasticsearchClient}
	 */
	public static ReactiveElasticsearchClient createReactive(ClientConfiguration clientConfiguration,
			@Nullable TransportOptions transportOptions) {

		Assert.notNull(clientConfiguration, "ClientConfiguration must not be null!");

		return createReactive(getRest5Client(clientConfiguration), transportOptions,
				DEFAULT_JACKSON3_JSONP_MAPPER.get());
	}

	/**
	 * Creates a new {@link ReactiveElasticsearchClient}.
	 *
	 * @param clientConfiguration configuration options, must not be {@literal null}.
	 * @param transportOptions options to be added to each request.
	 * @param jsonpMapper the JsonpMapper to use
	 * @return the {@link ReactiveElasticsearchClient}
	 */
	public static ReactiveElasticsearchClient createReactive(ClientConfiguration clientConfiguration,
			@Nullable TransportOptions transportOptions, JsonpMapper jsonpMapper) {

		Assert.notNull(clientConfiguration, "ClientConfiguration must not be null!");
		Assert.notNull(jsonpMapper, "jsonpMapper must not be null");

		return createReactive(getRest5Client(clientConfiguration), transportOptions, jsonpMapper);
	}

	/**
	 * Creates a new {@link ReactiveElasticsearchClient}.
	 *
	 * @param rest5Client the underlying {@link Rest5Client}
	 * @return the {@link ReactiveElasticsearchClient}
	 */
	public static ReactiveElasticsearchClient createReactive(Rest5Client rest5Client) {
		return createReactive(rest5Client, null, DEFAULT_JACKSON3_JSONP_MAPPER.get());
	}

	/**
	 * Creates a new {@link ReactiveElasticsearchClient}.
	 *
	 * @param rest5Client the underlying {@link Rest5Client}
	 * @param transportOptions options to be added to each request.
	 * @return the {@link ReactiveElasticsearchClient}
	 */
	public static ReactiveElasticsearchClient createReactive(Rest5Client rest5Client,
			@Nullable TransportOptions transportOptions, JsonpMapper jsonpMapper) {

		Assert.notNull(rest5Client, "restClient must not be null");

		var transport = getElasticsearchTransport(rest5Client, REACTIVE_CLIENT, transportOptions, jsonpMapper);
		return createReactive(transport);
	}

	/**
	 * Creates a new {@link ReactiveElasticsearchClient} that uses the given {@link ElasticsearchTransport}.
	 *
	 * @param transport the transport to use
	 * @return the {@link ElasticsearchClient
	 */
	public static ReactiveElasticsearchClient createReactive(ElasticsearchTransport transport) {

		Assert.notNull(transport, "transport must not be null");

		return new ReactiveElasticsearchClient(transport);
	}
	// endregion

	// region imperative client
	/**
	 * Creates a new imperative {@link ElasticsearchClient}.
	 *
	 * @param clientConfiguration configuration options, must not be {@literal null}.
	 * @return the {@link ElasticsearchClient}
	 */
	public static ElasticsearchClient createImperative(ClientConfiguration clientConfiguration) {
		return createImperative(getRest5Client(clientConfiguration), null, DEFAULT_JACKSON3_JSONP_MAPPER.get());
	}

	/**
	 * Creates a new imperative {@link ElasticsearchClient}.
	 *
	 * @param clientConfiguration configuration options, must not be {@literal null}.
	 * @param transportOptions options to be added to each request.
	 * @return the {@link ElasticsearchClient}
	 */
	public static ElasticsearchClient createImperative(ClientConfiguration clientConfiguration,
			TransportOptions transportOptions) {
		return createImperative(getRest5Client(clientConfiguration), transportOptions,
				DEFAULT_JACKSON3_JSONP_MAPPER.get());
	}

	/**
	 * Creates a new imperative {@link ElasticsearchClient}
	 *
	 * @param rest5Client the Rest5Client to use
	 * @return the {@link ElasticsearchClient}
	 */
	public static ElasticsearchClient createImperative(Rest5Client rest5Client) {
		return createImperative(rest5Client, null, DEFAULT_JACKSON3_JSONP_MAPPER.get());
	}

	/**
	 * Creates a new imperative {@link ElasticsearchClient}
	 *
	 * @param rest5Client the Rest5Client to use
	 * @param transportOptions options to be added to each request.
	 * @param jsonpMapper the mapper for the transport to use
	 * @return the {@link ElasticsearchClient}
	 * @since 6.0
	 */
	public static ElasticsearchClient createImperative(Rest5Client rest5Client,
			@Nullable TransportOptions transportOptions,
			JsonpMapper jsonpMapper) {

		Assert.notNull(rest5Client, "rest5Client must not be null");

		ElasticsearchTransport transport = getElasticsearchTransport(rest5Client, IMPERATIVE_CLIENT, transportOptions,
				jsonpMapper);

		return createImperative(transport);
	}

	/**
	 * Creates a new {@link ElasticsearchClient} that uses the given {@link ElasticsearchTransport}.
	 *
	 * @param transport the transport to use
	 * @return the {@link ElasticsearchClient
	 */
	public static AutoCloseableElasticsearchClient createImperative(ElasticsearchTransport transport) {

		Assert.notNull(transport, "transport must not be null");

		return new AutoCloseableElasticsearchClient(transport);
	}
	// endregion

	// region Elasticsearch transport

	/**
	 * Creates an {@link ElasticsearchTransport} that will use the given client that additionally is customized with a
	 * header to contain the clientType
	 *
	 * @param rest5Client the client to use
	 * @param clientType the client type to pass in each request as header
	 * @param transportOptions options for the transport
	 * @param jsonpMapper mapper for the transport
	 * @return ElasticsearchTransport
	 */
	public static ElasticsearchTransport getElasticsearchTransport(Rest5Client rest5Client, String clientType,
			@Nullable TransportOptions transportOptions, JsonpMapper jsonpMapper) {

		Assert.notNull(rest5Client, "rest5Client must not be null");
		Assert.notNull(clientType, "clientType must not be null");
		Assert.notNull(jsonpMapper, "jsonpMapper must not be null");

		TransportOptions.Builder transportOptionsBuilder = transportOptions != null ? transportOptions.toBuilder()
				: new Rest5ClientOptions(RequestOptions.DEFAULT, false).toBuilder();

		Rest5ClientOptions.Builder rest5ClientOptionsBuilder = getRest5ClientOptionsBuilder(transportOptions);

		rest5ClientOptionsBuilder.addHeader(X_SPRING_DATA_ELASTICSEARCH_CLIENT,
				VersionInfo.clientVersions() + " / " + clientType);

		return new Rest5ClientTransport(rest5Client, jsonpMapper, rest5ClientOptionsBuilder.build());
	}
	// endregion
}
