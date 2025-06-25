package org.springframework.data.elasticsearch.client.elc.rest_client;

import co.elastic.clients.transport.TransportOptions;
import co.elastic.clients.transport.TransportUtils;
import co.elastic.clients.transport.rest_client.RestClientOptions;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HttpContext;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchClients;
import org.springframework.data.elasticsearch.support.HttpHeaders;
import org.springframework.util.Assert;

/**
 * Utility class containing the functions to create the Elasticsearch RestClient used up to Elasticsearch 9.
 *
 * @since 6.0
 * @deprecated since 6.0, use the new Rest5Client the code for that is in the package ../rest_client.
 */
@Deprecated(since = "6.0", forRemoval = true)
public final class RestClients {

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
		HttpHost[] httpHosts = getHttpHosts(clientConfiguration);
		RestClientBuilder builder = RestClient.builder(httpHosts);

		if (clientConfiguration.getPathPrefix() != null) {
			builder.setPathPrefix(clientConfiguration.getPathPrefix());
		}

		HttpHeaders headers = clientConfiguration.getDefaultHeaders();

		if (!headers.isEmpty()) {
			builder.setDefaultHeaders(toHeaderArray(headers));
		}

		builder.setHttpClientConfigCallback(clientBuilder -> {
			if (clientConfiguration.getCaFingerprint().isPresent()) {
				clientBuilder
						.setSSLContext(TransportUtils.sslContextFromCaFingerprint(clientConfiguration.getCaFingerprint().get()));
			}
			clientConfiguration.getSslContext().ifPresent(clientBuilder::setSSLContext);
			clientConfiguration.getHostNameVerifier().ifPresent(clientBuilder::setSSLHostnameVerifier);
			clientBuilder.addInterceptorLast(new CustomHeaderInjector(clientConfiguration.getHeadersSupplier()));

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
				if (clientConfigurer instanceof RestClients.ElasticsearchHttpClientConfigurationCallback restClientConfigurationCallback) {
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

	private static HttpHost @NonNull [] getHttpHosts(ClientConfiguration clientConfiguration) {
		List<InetSocketAddress> hosts = clientConfiguration.getEndpoints();
		boolean useSsl = clientConfiguration.useSsl();
		return hosts.stream()
				.map(it -> (useSsl ? "https" : "http") + "://" + it.getHostString() + ':' + it.getPort())
				.map(HttpHost::create).toArray(HttpHost[]::new);
	}

	private static org.apache.http.Header[] toHeaderArray(HttpHeaders headers) {
		return headers.entrySet().stream() //
				.flatMap(entry -> entry.getValue().stream() //
						.map(value -> new BasicHeader(entry.getKey(), value))) //
				.toArray(org.apache.http.Header[]::new);
	}

	/**
	 * Interceptor to inject custom supplied headers.
	 *
	 * @since 4.4
	 */
	record CustomHeaderInjector(Supplier<HttpHeaders> headersSupplier) implements HttpRequestInterceptor {

		@Override
		public void process(HttpRequest request, HttpContext context) {
			HttpHeaders httpHeaders = headersSupplier.get();

			if (httpHeaders != null && !httpHeaders.isEmpty()) {
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

		static RestClients.ElasticsearchHttpClientConfigurationCallback from(
				Function<HttpAsyncClientBuilder, HttpAsyncClientBuilder> httpClientBuilderCallback) {

			Assert.notNull(httpClientBuilderCallback, "httpClientBuilderCallback must not be null");

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

			return restClientBuilderCallback::apply;
		}
	}

	public static RestClientOptions.Builder getRestClientOptionsBuilder(@Nullable TransportOptions transportOptions) {

		if (transportOptions instanceof RestClientOptions restClientOptions) {
			return restClientOptions.toBuilder();
		}

		var builder = new RestClientOptions.Builder(RequestOptions.DEFAULT.toBuilder());

		if (transportOptions != null) {
			transportOptions.headers().forEach(header -> builder.addHeader(header.getKey(), header.getValue()));
			transportOptions.queryParameters().forEach(builder::setParameter);
			builder.onWarnings(transportOptions.onWarnings());
		}

		return builder;
	}

}
