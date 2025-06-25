/*
 * Copyright 2021-2025 the original author or authors.
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
import static org.springframework.data.elasticsearch.client.elc.rest_client.RestClients.*;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.TransportOptions;
import co.elastic.clients.transport.Version;
import co.elastic.clients.transport.rest5_client.Rest5ClientOptions;
import co.elastic.clients.transport.rest5_client.Rest5ClientTransport;
import co.elastic.clients.transport.rest5_client.low_level.RequestOptions;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import co.elastic.clients.transport.rest_client.RestClientOptions;
import co.elastic.clients.transport.rest_client.RestClientTransport;

import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.http.entity.ContentType;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.elasticsearch.client.RestClient;
import org.jspecify.annotations.Nullable;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.support.VersionInfo;
import org.springframework.util.Assert;

/**
 * Utility class to create the different Elasticsearch clients. The RestClient class is the one used in Elasticsearch
 * until version 9, it is still available, but it's use is deprecated. The Rest5Client class is the one that should be
 * used from Elasticsearch 9 on.
 *
 * @author Peter-Josef Meisch
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

	private static final JsonpMapper DEFAULT_JSONP_MAPPER = new JacksonJsonpMapper();

	// region reactive client
	/**
	 * Creates a new {@link ReactiveElasticsearchClient}
	 *
	 * @param clientConfiguration configuration options, must not be {@literal null}.
	 * @return the {@link ReactiveElasticsearchClient}
	 */
	public static ReactiveElasticsearchClient createReactive(ClientConfiguration clientConfiguration) {

		Assert.notNull(clientConfiguration, "clientConfiguration must not be null");

		return createReactive(getRestClient(clientConfiguration), null, DEFAULT_JSONP_MAPPER);
	}

	/**
	 * Creates a new {@link ReactiveElasticsearchClient}
	 *
	 * @param clientConfiguration configuration options, must not be {@literal null}.
	 * @param transportOptions options to be added to each request.
	 * @return the {@link ReactiveElasticsearchClient}
	 */
	public static ReactiveElasticsearchClient createReactive(ClientConfiguration clientConfiguration,
			@Nullable TransportOptions transportOptions) {

		Assert.notNull(clientConfiguration, "ClientConfiguration must not be null!");

		return createReactive(getRestClient(clientConfiguration), transportOptions, DEFAULT_JSONP_MAPPER);
	}

	/**
	 * Creates a new {@link ReactiveElasticsearchClient}
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

		return createReactive(getRestClient(clientConfiguration), transportOptions, jsonpMapper);
	}

	/**
	 * Creates a new {@link ReactiveElasticsearchClient}.
	 *
	 * @param restClient the underlying {@link RestClient}
	 * @return the {@link ReactiveElasticsearchClient}
	 * @deprecated since 6.0, use the version with a Rest5Client.
	 */
	@Deprecated(since = "6.0", forRemoval = true)
	public static ReactiveElasticsearchClient createReactive(RestClient restClient) {
		return createReactive(restClient, null, DEFAULT_JSONP_MAPPER);
	}

	/**
	 * Creates a new {@link ReactiveElasticsearchClient}.
	 *
	 * @param rest5Client the underlying {@link RestClient}
	 * @return the {@link ReactiveElasticsearchClient}
	 */
	public static ReactiveElasticsearchClient createReactive(Rest5Client rest5Client) {
		return createReactive(rest5Client, null, DEFAULT_JSONP_MAPPER);
	}

	/**
	 * Creates a new {@link ReactiveElasticsearchClient}.
	 *
	 * @param restClient the underlying {@link RestClient}
	 * @param transportOptions options to be added to each request.
	 * @return the {@link ReactiveElasticsearchClient}
	 * @deprecated since 6.0, use the version with a Rest5Client.
	 */
	@Deprecated(since = "6.0", forRemoval = true)
	public static ReactiveElasticsearchClient createReactive(RestClient restClient,
			@Nullable TransportOptions transportOptions, JsonpMapper jsonpMapper) {

		Assert.notNull(restClient, "restClient must not be null");

		var transport = getElasticsearchTransport(restClient, REACTIVE_CLIENT, transportOptions, jsonpMapper);
		return createReactive(transport);
	}
	/**
	 * Creates a new {@link ReactiveElasticsearchClient}.
	 *
	 * @param rest5Client the underlying {@link RestClient}
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
	 * Creates a new imperative {@link ElasticsearchClient}. This uses a RestClient, if the old RestClient is needed, this
	 * must be created with the {@link org.springframework.data.elasticsearch.client.elc.rest_client.RestClients} class
	 * and passed in as parameter.
	 *
	 * @param clientConfiguration configuration options, must not be {@literal null}.
	 * @return the {@link ElasticsearchClient}
	 */
	public static ElasticsearchClient createImperative(ClientConfiguration clientConfiguration) {
		return createImperative(getRest5Client(clientConfiguration), null, DEFAULT_JSONP_MAPPER);
	}

	/**
	 * Creates a new imperative {@link ElasticsearchClient}. This uses a RestClient, if the old RestClient is needed, this
	 * must be created with the {@link org.springframework.data.elasticsearch.client.elc.rest_client.RestClients} class
	 * and passed in as parameter.
	 *
	 * @param clientConfiguration configuration options, must not be {@literal null}.
	 * @param transportOptions options to be added to each request.
	 * @return the {@link ElasticsearchClient}
	 */
	public static ElasticsearchClient createImperative(ClientConfiguration clientConfiguration,
			TransportOptions transportOptions) {
		return createImperative(getRest5Client(clientConfiguration), transportOptions, DEFAULT_JSONP_MAPPER);
	}

	/**
	 * Creates a new imperative {@link ElasticsearchClient}
	 *
	 * @param restClient the RestClient to use
	 * @return the {@link ElasticsearchClient}
	 * @deprecated since 6.0, use the version with a Rest5Client.
	 */
	@Deprecated(since = "6.0", forRemoval = true)
	public static ElasticsearchClient createImperative(RestClient restClient) {
		return createImperative(restClient, null, DEFAULT_JSONP_MAPPER);
	}

	/**
	 * Creates a new imperative {@link ElasticsearchClient}
	 *
	 * @param rest5Client the Rest5Client to use
	 * @return the {@link ElasticsearchClient}
	 */
	public static ElasticsearchClient createImperative(Rest5Client rest5Client) {
		return createImperative(rest5Client, null, DEFAULT_JSONP_MAPPER);
	}

	/**
	 * Creates a new imperative {@link ElasticsearchClient}
	 *
	 * @param restClient the RestClient to use
	 * @param transportOptions options to be added to each request.
	 * @param jsonpMapper the mapper for the transport to use
	 * @return the {@link ElasticsearchClient}
	 * @deprecated since 6.0, use the version with a Rest5Client.
	 */
	@Deprecated(since = "6.0", forRemoval = true)
	public static ElasticsearchClient createImperative(RestClient restClient, @Nullable TransportOptions transportOptions,
			JsonpMapper jsonpMapper) {

		Assert.notNull(restClient, "restClient must not be null");

		ElasticsearchTransport transport = getElasticsearchTransport(restClient, IMPERATIVE_CLIENT, transportOptions,
				jsonpMapper);

		return createImperative(transport);
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

		Assert.notNull(rest5Client, "restClient must not be null");

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
	 * @param restClient the client to use
	 * @param clientType the client type to pass in each request as header
	 * @param transportOptions options for the transport
	 * @param jsonpMapper mapper for the transport
	 * @return ElasticsearchTransport
	 * @deprecated since 6.0, use the version taking a Rest5Client
	 */
	@Deprecated(since = "6.0", forRemoval = true)
	public static ElasticsearchTransport getElasticsearchTransport(RestClient restClient, String clientType,
			@Nullable TransportOptions transportOptions, JsonpMapper jsonpMapper) {

		Assert.notNull(restClient, "restClient must not be null");
		Assert.notNull(clientType, "clientType must not be null");
		Assert.notNull(jsonpMapper, "jsonpMapper must not be null");

		TransportOptions.Builder transportOptionsBuilder = transportOptions != null ? transportOptions.toBuilder()
				: new RestClientOptions(org.elasticsearch.client.RequestOptions.DEFAULT, false).toBuilder();

		RestClientOptions.Builder restClientOptionsBuilder = getRestClientOptionsBuilder(transportOptions);

		ContentType jsonContentType = Version.VERSION == null ? ContentType.APPLICATION_JSON
				: ContentType.create("application/vnd.elasticsearch+json",
						new BasicNameValuePair("compatible-with", String.valueOf(Version.VERSION.major())));

		Consumer<String> setHeaderIfNotPresent = header -> {
			if (restClientOptionsBuilder.build().headers().stream() //
					.noneMatch((h) -> h.getKey().equalsIgnoreCase(header))) {
				// need to add the compatibility header, this is only done automatically when not passing in custom options.
				// code copied from RestClientTransport as it is not available outside the package
				restClientOptionsBuilder.addHeader(header, jsonContentType.toString());
			}
		};

		setHeaderIfNotPresent.accept("Content-Type");
		setHeaderIfNotPresent.accept("Accept");

		restClientOptionsBuilder.addHeader(X_SPRING_DATA_ELASTICSEARCH_CLIENT, clientType);

		return new RestClientTransport(restClient, jsonpMapper, restClientOptionsBuilder.build());
	}

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

		Assert.notNull(rest5Client, "restClient must not be null");
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

	// todo #3117 remove and document that ElasticsearchHttpClientConfigurationCallback has been move to RestClients.
}
