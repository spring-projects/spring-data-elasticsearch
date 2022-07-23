/*
 * Copyright 2021-2022 the original author or authors.
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
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.TransportOptions;
import co.elastic.clients.transport.Version;
import co.elastic.clients.transport.rest_client.RestClientOptions;
import co.elastic.clients.transport.rest_client.RestClientTransport;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.ClientLogger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Utility class to create the different Elasticsearch clients
 *
 * @author Peter-Josef Meisch
 * @since 4.4
 */
public final class ElasticsearchClients {
	/**
	 * Name of whose value can be used to correlate log messages for this request.
	 */
	private static final String LOG_ID_ATTRIBUTE = ElasticsearchClients.class.getName() + ".LOG_ID";
	private static final String X_SPRING_DATA_ELASTICSEARCH_CLIENT = "X-SpringDataElasticsearch-Client";
	private static final String IMPERATIVE_CLIENT = "imperative";
	private static final String REACTIVE_CLIENT = "reactive";

	/**
	 * Creates a new {@link ReactiveElasticsearchClient}
	 *
	 * @param clientConfiguration configuration options, must not be {@literal null}.
	 * @return the {@link ReactiveElasticsearchClient}
	 */
	public static ReactiveElasticsearchClient createReactive(ClientConfiguration clientConfiguration) {

		Assert.notNull(clientConfiguration, "clientConfiguration must not be null");

		return createReactive(getRestClient(clientConfiguration), null);
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

		return createReactive(getRestClient(clientConfiguration), transportOptions);
	}

	/**
	 * Creates a new {@link ReactiveElasticsearchClient}.
	 *
	 * @param restClient the underlying {@link RestClient}
	 * @return the {@link ReactiveElasticsearchClient}
	 */
	public static ReactiveElasticsearchClient createReactive(RestClient restClient) {
		return createReactive(restClient, null);
	}

	/**
	 * Creates a new {@link ReactiveElasticsearchClient}.
	 *
	 * @param restClient the underlying {@link RestClient}
	 * @param transportOptions options to be added to each request.
	 * @return the {@link ReactiveElasticsearchClient}
	 */
	public static ReactiveElasticsearchClient createReactive(RestClient restClient,
			@Nullable TransportOptions transportOptions) {
		return new ReactiveElasticsearchClient(getElasticsearchTransport(restClient, REACTIVE_CLIENT, transportOptions));
	}

	/**
	 * Creates a new imperative {@link ElasticsearchClient}
	 *
	 * @param clientConfiguration configuration options, must not be {@literal null}.
	 * @return the {@link ElasticsearchClient}
	 */
	public static ElasticsearchClient createImperative(ClientConfiguration clientConfiguration) {
		return createImperative(getRestClient(clientConfiguration), null);
	}

	/**
	 * Creates a new imperative {@link ElasticsearchClient}
	 *
	 * @param clientConfiguration configuration options, must not be {@literal null}.
	 * @param transportOptions options to be added to each request.
	 * @return the {@link ElasticsearchClient}
	 */
	public static ElasticsearchClient createImperative(ClientConfiguration clientConfiguration,
			TransportOptions transportOptions) {
		return createImperative(getRestClient(clientConfiguration), transportOptions);
	}

	/**
	 * Creates a new imperative {@link ElasticsearchClient}
	 *
	 * @param restClient the RestClient to use
	 * @return the {@link ElasticsearchClient}
	 */
	public static ElasticsearchClient createImperative(RestClient restClient) {
		return createImperative(restClient, null);
	}

	/**
	 * Creates a new imperative {@link ElasticsearchClient}
	 *
	 * @param restClient the RestClient to use
	 * @param transportOptions options to be added to each request.
	 * @return the {@link ElasticsearchClient}
	 */
	public static ElasticsearchClient createImperative(RestClient restClient,
			@Nullable TransportOptions transportOptions) {

		Assert.notNull(restClient, "restClient must not be null");

		ElasticsearchTransport transport = getElasticsearchTransport(restClient, IMPERATIVE_CLIENT, transportOptions);

		return new AutoCloseableElasticsearchClient(transport);
	}

	/**
	 * Creates a low level {@link RestClient} for the given configuration.
	 *
	 * @param clientConfiguration must not be {@literal null}
	 * @return the {@link RestClient}
	 */
	public static RestClient getRestClient(ClientConfiguration clientConfiguration) {
		return getRestClientBuilder(clientConfiguration).build();
	}

	private static RestClientBuilder getRestClientBuilder(ClientConfiguration clientConfiguration) {
		HttpHost[] httpHosts = formattedHosts(clientConfiguration.getEndpoints(), clientConfiguration.useSsl()).stream()
				.map(HttpHost::create).toArray(HttpHost[]::new);
		RestClientBuilder builder = RestClient.builder(httpHosts);

		if (clientConfiguration.getPathPrefix() != null) {
			builder.setPathPrefix(clientConfiguration.getPathPrefix());
		}

		HttpHeaders headers = clientConfiguration.getDefaultHeaders();

		if (!headers.isEmpty()) {
			builder.setDefaultHeaders(toHeaderArray(headers));
		}

		builder.setHttpClientConfigCallback(clientBuilder -> {
			clientConfiguration.getSslContext().ifPresent(clientBuilder::setSSLContext);
			clientConfiguration.getHostNameVerifier().ifPresent(clientBuilder::setSSLHostnameVerifier);
			clientBuilder.addInterceptorLast(new CustomHeaderInjector(clientConfiguration.getHeadersSupplier()));

			if (ClientLogger.isEnabled()) {
				HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();

				clientBuilder.addInterceptorLast((HttpRequestInterceptor) interceptor);
				clientBuilder.addInterceptorLast((HttpResponseInterceptor) interceptor);
			}

			RequestConfig.Builder requestConfigBuilder = RequestConfig.custom();
			Duration connectTimeout = clientConfiguration.getConnectTimeout();

			if (!connectTimeout.isNegative()) {
				requestConfigBuilder.setConnectTimeout(Math.toIntExact(connectTimeout.toMillis()));
			}

			Duration socketTimeout = clientConfiguration.getSocketTimeout();

			if (!socketTimeout.isNegative()) {
				requestConfigBuilder.setSocketTimeout(Math.toIntExact(socketTimeout.toMillis()));
				requestConfigBuilder.setConnectionRequestTimeout(Math.toIntExact(socketTimeout.toMillis()));
			}

			clientBuilder.setDefaultRequestConfig(requestConfigBuilder.build());

			clientConfiguration.getProxy().map(HttpHost::create).ifPresent(clientBuilder::setProxy);

			for (ClientConfiguration.ClientConfigurationCallback<?> clientConfigurer : clientConfiguration
					.getClientConfigurers()) {
				if (clientConfigurer instanceof ElasticsearchHttpClientConfigurationCallback restClientConfigurationCallback) {
					clientBuilder = restClientConfigurationCallback.configure(clientBuilder);
				}
			}

			return clientBuilder;
		});

		for (ClientConfiguration.ClientConfigurationCallback<?> clientConfigurationCallback : clientConfiguration
				.getClientConfigurers()) {
			if (clientConfigurationCallback instanceof ElasticsearchRestClientConfigurationCallback configurationCallback) {
				builder = configurationCallback.configure(builder);
			}
		}
		return builder;
	}

	private static ElasticsearchTransport getElasticsearchTransport(RestClient restClient, String clientType,
			@Nullable TransportOptions transportOptions) {

		TransportOptions.Builder transportOptionsBuilder = transportOptions != null ? transportOptions.toBuilder()
				: new RestClientOptions(RequestOptions.DEFAULT).toBuilder();

		// need to add the compatibility header, this is only done automatically when not passing in custom options.
		// code copied from RestClientTransport as it is not available outside the package
		ContentType jsonContentType = null;
		if (Version.VERSION == null) {
			jsonContentType = ContentType.APPLICATION_JSON;
		} else {
			jsonContentType = ContentType.create("application/vnd.elasticsearch+json",
					new BasicNameValuePair("compatible-with", String.valueOf(Version.VERSION.major())));
		}
		transportOptionsBuilder.addHeader("Accept", jsonContentType.toString());

		TransportOptions transportOptionsWithHeader = transportOptionsBuilder
				.addHeader(X_SPRING_DATA_ELASTICSEARCH_CLIENT, clientType).build();

		ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper(),
				transportOptionsWithHeader);
		return transport;
	}

	private static List<String> formattedHosts(List<InetSocketAddress> hosts, boolean useSsl) {
		return hosts.stream().map(it -> (useSsl ? "https" : "http") + "://" + it.getHostString() + ":" + it.getPort())
				.collect(Collectors.toList());
	}

	private static org.apache.http.Header[] toHeaderArray(HttpHeaders headers) {
		return headers.entrySet().stream() //
				.flatMap(entry -> entry.getValue().stream() //
						.map(value -> new BasicHeader(entry.getKey(), value))) //
				.toArray(org.apache.http.Header[]::new);
	}

	/**
	 * Logging interceptors for Elasticsearch client logging.
	 *
	 * @see ClientLogger
	 * @since 4.4
	 */
	private static class HttpLoggingInterceptor implements HttpResponseInterceptor, HttpRequestInterceptor {

		@Override
		public void process(HttpRequest request, HttpContext context) throws IOException {

			String logId = (String) context.getAttribute(LOG_ID_ATTRIBUTE);

			if (logId == null) {
				logId = ClientLogger.newLogId();
				context.setAttribute(LOG_ID_ATTRIBUTE, logId);
			}

			String headers = Arrays.stream(request.getAllHeaders())
					.map(header -> header.getName()
							+ ((header.getName().equals("Authorization")) ? ": *****" : ": " + header.getValue()))
					.collect(Collectors.joining(", ", "[", "]"));

			if (request instanceof HttpEntityEnclosingRequest && ((HttpEntityEnclosingRequest) request).getEntity() != null) {

				HttpEntityEnclosingRequest entityRequest = (HttpEntityEnclosingRequest) request;
				HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
				ByteArrayOutputStream buffer = new ByteArrayOutputStream();
				entity.writeTo(buffer);

				if (!entity.isRepeatable()) {
					entityRequest.setEntity(new ByteArrayEntity(buffer.toByteArray()));
				}

				ClientLogger.logRequest(logId, request.getRequestLine().getMethod(), request.getRequestLine().getUri(), "",
						headers, buffer::toString);
			} else {
				ClientLogger.logRequest(logId, request.getRequestLine().getMethod(), request.getRequestLine().getUri(), "",
						headers);
			}
		}

		@Override
		public void process(HttpResponse response, HttpContext context) throws IOException {

			String logId = (String) context.getAttribute(LOG_ID_ATTRIBUTE);

			String headers = Arrays.stream(response.getAllHeaders())
					.map(header -> header.getName()
							+ ((header.getName().equals("Authorization")) ? ": *****" : ": " + header.getValue()))
					.collect(Collectors.joining(", ", "[", "]"));

			// no way of logging the body, in this callback, it is not read yset, later there is no callback possibility in
			// RestClient or RestClientTransport
			ClientLogger.logRawResponse(logId, HttpStatus.resolve(response.getStatusLine().getStatusCode()), headers);
		}
	}

	/**
	 * Interceptor to inject custom supplied headers.
	 *
	 * @since 4.4
	 */
	private static class CustomHeaderInjector implements HttpRequestInterceptor {

		public CustomHeaderInjector(Supplier<HttpHeaders> headersSupplier) {
			this.headersSupplier = headersSupplier;
		}

		private final Supplier<HttpHeaders> headersSupplier;

		@Override
		public void process(HttpRequest request, HttpContext context) {
			HttpHeaders httpHeaders = headersSupplier.get();

			if (httpHeaders != null && httpHeaders != HttpHeaders.EMPTY) {
				Arrays.stream(toHeaderArray(httpHeaders)).forEach(request::addHeader);
			}
		}
	}

	/**
	 * {@link org.springframework.data.elasticsearch.client.ClientConfiguration.ClientConfigurationCallback} to configure
	 * the Elasticsearch RestClient's Http client with a {@link HttpAsyncClientBuilder}
	 *
	 * @since 4.4
	 */
	public interface ElasticsearchHttpClientConfigurationCallback
			extends ClientConfiguration.ClientConfigurationCallback<HttpAsyncClientBuilder> {

		static ElasticsearchHttpClientConfigurationCallback from(
				Function<HttpAsyncClientBuilder, HttpAsyncClientBuilder> httpClientBuilderCallback) {

			Assert.notNull(httpClientBuilderCallback, "httpClientBuilderCallback must not be null");

			// noinspection NullableProblems
			return httpClientBuilderCallback::apply;
		}
	}

	/**
	 * {@link org.springframework.data.elasticsearch.client.ClientConfiguration.ClientConfigurationCallback} to configure
	 * the RestClient client with a {@link RestClientBuilder}
	 *
	 * @since 5.0
	 */
	public interface ElasticsearchRestClientConfigurationCallback
			extends ClientConfiguration.ClientConfigurationCallback<RestClientBuilder> {

		static ElasticsearchRestClientConfigurationCallback from(
				Function<RestClientBuilder, RestClientBuilder> restClientBuilderCallback) {

			Assert.notNull(restClientBuilderCallback, "restClientBuilderCallback must not be null");

			// noinspection NullableProblems
			return restClientBuilderCallback::apply;
		}
	}
}
