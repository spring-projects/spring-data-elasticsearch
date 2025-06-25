package org.springframework.data.elasticsearch.client.elc.rest5_client;

import co.elastic.clients.transport.TransportOptions;
import co.elastic.clients.transport.TransportUtils;
import co.elastic.clients.transport.rest5_client.Rest5ClientOptions;
import co.elastic.clients.transport.rest5_client.low_level.RequestOptions;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import co.elastic.clients.transport.rest5_client.low_level.Rest5ClientBuilder;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import javax.net.ssl.SSLContext;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.DefaultAuthenticationStrategy;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.client5.http.impl.routing.DefaultProxyRoutePlanner;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.http.nio.ssl.BasicClientTlsStrategy;
import org.apache.hc.core5.util.Timeout;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.support.HttpHeaders;
import org.springframework.data.elasticsearch.support.VersionInfo;
import org.springframework.util.Assert;

/**
 * Utility class containing the functions to create the Elasticsearch Rest5Client used from Elasticsearch 9 on.
 *
 * @since 6.0
 */
public final class Rest5Clients {

	// values copied from Rest5ClientBuilder
	public static final int DEFAULT_CONNECT_TIMEOUT_MILLIS = 1000;
	public static final int DEFAULT_SOCKET_TIMEOUT_MILLIS = 30000;
	public static final int DEFAULT_RESPONSE_TIMEOUT_MILLIS = 0; // meaning infinite
	public static final int DEFAULT_MAX_CONN_PER_ROUTE = 10;
	public static final int DEFAULT_MAX_CONN_TOTAL = 30;

	private Rest5Clients() {}

	/**
	 * Creates a low level {@link Rest5Client} for the given configuration.
	 *
	 * @param clientConfiguration must not be {@literal null}
	 * @return the {@link Rest5Client}
	 */
	public static Rest5Client getRest5Client(ClientConfiguration clientConfiguration) {
		return getRest5ClientBuilder(clientConfiguration).build();
	}

	private static Rest5ClientBuilder getRest5ClientBuilder(ClientConfiguration clientConfiguration) {

		HttpHost[] httpHosts = getHttpHosts(clientConfiguration);
		Rest5ClientBuilder builder = Rest5Client.builder(httpHosts);

		if (clientConfiguration.getPathPrefix() != null) {
			builder.setPathPrefix(clientConfiguration.getPathPrefix());
		}

		HttpHeaders headers = clientConfiguration.getDefaultHeaders();

		if (!headers.isEmpty()) {
			builder.setDefaultHeaders(toHeaderArray(headers));
		}

		// we need to provide our own HttpClient, as the Rest5ClientBuilder
		// does not provide a callback for configuration the http client as the old RestClientBuilder.
		var httpClient = createHttpClient(clientConfiguration);
		builder.setHttpClient(httpClient);

		for (ClientConfiguration.ClientConfigurationCallback<?> clientConfigurationCallback : clientConfiguration
				.getClientConfigurers()) {
			if (clientConfigurationCallback instanceof ElasticsearchRest5ClientConfigurationCallback configurationCallback) {
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
				.map(URI::create)
				.map(HttpHost::create)
				.toArray(HttpHost[]::new);
	}

	private static Header[] toHeaderArray(HttpHeaders headers) {
		return headers.entrySet().stream() //
				.flatMap(entry -> entry.getValue().stream() //
						.map(value -> new BasicHeader(entry.getKey(), value))) //
				.toList().toArray(new Header[0]);
	}

	// the basic logic to create the http client is copied from the Rest5ClientBuilder class, this is taken from the
	// Elasticsearch code, as there is no public usable instance in that
	private static CloseableHttpAsyncClient createHttpClient(ClientConfiguration clientConfiguration) {

		var requestConfigBuilder = RequestConfig.custom();
		var connectionConfigBuilder = ConnectionConfig.custom();

		Duration connectTimeout = clientConfiguration.getConnectTimeout();

		if (!connectTimeout.isNegative()) {
			connectionConfigBuilder.setConnectTimeout(
					Timeout.of(Math.toIntExact(connectTimeout.toMillis()), TimeUnit.MILLISECONDS));
		}

		Duration socketTimeout = clientConfiguration.getSocketTimeout();

		if (!socketTimeout.isNegative()) {
			var soTimeout = Timeout.of(Math.toIntExact(socketTimeout.toMillis()), TimeUnit.MILLISECONDS);
			connectionConfigBuilder.setSocketTimeout(soTimeout);
			requestConfigBuilder.setConnectionRequestTimeout(soTimeout);
		} else {
			connectionConfigBuilder.setSocketTimeout(Timeout.of(DEFAULT_SOCKET_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS));
			requestConfigBuilder
					.setConnectionRequestTimeout(Timeout.of(DEFAULT_RESPONSE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS));
		}

		try {
			SSLContext sslContext = clientConfiguration.getCaFingerprint().isPresent()
					? TransportUtils.sslContextFromCaFingerprint(clientConfiguration.getCaFingerprint().get())
					: (clientConfiguration.getSslContext().isPresent()
							? clientConfiguration.getSslContext().get()
							: SSLContext.getDefault());

			ConnectionConfig connectionConfig = connectionConfigBuilder.build();

			PoolingAsyncClientConnectionManager defaultConnectionManager = PoolingAsyncClientConnectionManagerBuilder.create()
					.setDefaultConnectionConfig(connectionConfig)
					.setMaxConnPerRoute(DEFAULT_MAX_CONN_PER_ROUTE)
					.setMaxConnTotal(DEFAULT_MAX_CONN_TOTAL)
					.setTlsStrategy(new BasicClientTlsStrategy(sslContext))
					.build();

			var requestConfig = requestConfigBuilder.build();

            var immutableRefToHttpClientBuilder = new Object() {
                HttpAsyncClientBuilder httpClientBuilder = HttpAsyncClientBuilder.create()
                        .setDefaultRequestConfig(requestConfig)
                        .setConnectionManager(defaultConnectionManager)
                        .setUserAgent(VersionInfo.clientVersions())
                        .setTargetAuthenticationStrategy(new DefaultAuthenticationStrategy())
                        .setThreadFactory(new RestClientThreadFactory());
            };

			clientConfiguration.getProxy().ifPresent(proxy -> {
				try {
					var proxyRoutePlanner = new DefaultProxyRoutePlanner(HttpHost.create(proxy));
					immutableRefToHttpClientBuilder.httpClientBuilder.setRoutePlanner(proxyRoutePlanner);
				} catch (URISyntaxException e) {
					throw new RuntimeException(e);
				}
			});

			immutableRefToHttpClientBuilder.httpClientBuilder.addRequestInterceptorFirst((request, entity, context) -> {
				clientConfiguration.getHeadersSupplier().get().forEach((header, values) -> {
					// The accept and content-type headers are already put on the request, despite this being the first
					// interceptor.
					if ("Accept".equalsIgnoreCase(header) || " Content-Type".equalsIgnoreCase(header)) {
						request.removeHeaders(header);
					}
					values.forEach(value -> request.addHeader(header, value));
				});
			});

			for (ClientConfiguration.ClientConfigurationCallback<?> clientConfigurer : clientConfiguration
					.getClientConfigurers()) {
				if (clientConfigurer instanceof ElasticsearchHttpClientConfigurationCallback httpClientConfigurer) {
					immutableRefToHttpClientBuilder.httpClientBuilder = httpClientConfigurer.configure(immutableRefToHttpClientBuilder.httpClientBuilder);
				}
			}

			return immutableRefToHttpClientBuilder.httpClientBuilder.build();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("could not create the default ssl context", e);
		}
	}

	/*
	 * Copied from the Elasticsearch code as this class is not public there.
	 */
	private static class RestClientThreadFactory implements ThreadFactory {
		private static final AtomicLong CLIENT_THREAD_POOL_ID_GENERATOR = new AtomicLong();
		private final long clientThreadPoolId;
		private final AtomicLong clientThreadId;

		private RestClientThreadFactory() {
			this.clientThreadPoolId = CLIENT_THREAD_POOL_ID_GENERATOR.getAndIncrement();
			this.clientThreadId = new AtomicLong();
		}

		public Thread newThread(Runnable runnable) {
			return new Thread(runnable, String.format(Locale.ROOT, "elasticsearch-rest-client-%d-thread-%d",
					this.clientThreadPoolId, this.clientThreadId.incrementAndGet()));
		}
	}

	/**
	 * {@link org.springframework.data.elasticsearch.client.ClientConfiguration.ClientConfigurationCallback} to configure
	 * the Elasticsearch Rest5Client's Http client with a {@link HttpAsyncClientBuilder}
	 *
	 * @since 6.0
	 */
	public interface ElasticsearchHttpClientConfigurationCallback
			extends ClientConfiguration.ClientConfigurationCallback<HttpAsyncClientBuilder> {

		static Rest5Clients.ElasticsearchHttpClientConfigurationCallback from(
				Function<HttpAsyncClientBuilder, HttpAsyncClientBuilder> httpClientBuilderCallback) {

			Assert.notNull(httpClientBuilderCallback, "httpClientBuilderCallback must not be null");

			return httpClientBuilderCallback::apply;
		}
	}

	/**
	 * {@link ClientConfiguration.ClientConfigurationCallback} to configure the Rest5Client client with a
	 * {@link Rest5ClientBuilder}
	 *
	 * @since 6.0
	 */
	public interface ElasticsearchRest5ClientConfigurationCallback
			extends ClientConfiguration.ClientConfigurationCallback<Rest5ClientBuilder> {

		static ElasticsearchRest5ClientConfigurationCallback from(
				Function<Rest5ClientBuilder, Rest5ClientBuilder> rest5ClientBuilderCallback) {

			Assert.notNull(rest5ClientBuilderCallback, "rest5ClientBuilderCallback must not be null");

			return rest5ClientBuilderCallback::apply;
		}
	}

	public static Rest5ClientOptions.Builder getRest5ClientOptionsBuilder(@Nullable TransportOptions transportOptions) {

		if (transportOptions instanceof Rest5ClientOptions rest5ClientOptions) {
			return rest5ClientOptions.toBuilder();
		}

		var builder = new Rest5ClientOptions.Builder(RequestOptions.DEFAULT.toBuilder());

		if (transportOptions != null) {
			transportOptions.headers().forEach(header -> builder.addHeader(header.getKey(), header.getValue()));
			transportOptions.queryParameters().forEach(builder::setParameter);
			builder.onWarnings(transportOptions.onWarnings());
		}

		return builder;
	}
}
