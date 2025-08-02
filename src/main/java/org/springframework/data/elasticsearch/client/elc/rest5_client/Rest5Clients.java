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
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.net.ssl.SSLContext;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;
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
    public static final int DEFAULT_SOCKET_TIMEOUT_MILLIS = 30000;
    public static final int DEFAULT_RESPONSE_TIMEOUT_MILLIS = 0; // meaning infinite

    private Rest5Clients() {
    }

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

        // RestClientBuilder configuration callbacks from the consumer
        for (ClientConfiguration.ClientConfigurationCallback<?> clientConfigurationCallback : clientConfiguration
                .getClientConfigurers()) {
            if (clientConfigurationCallback instanceof ElasticsearchRest5ClientConfigurationCallback configurationCallback) {
                builder = configurationCallback.configure(builder);
            }
        }

        Duration connectTimeout = clientConfiguration.getConnectTimeout();
        Duration socketTimeout = clientConfiguration.getSocketTimeout();

        builder.setHttpClientConfigCallback(httpAsyncClientBuilder -> {

            httpAsyncClientBuilder.setUserAgent(VersionInfo.clientVersions());
            if (clientConfiguration.getProxy().isPresent()) {
                var proxy = clientConfiguration.getProxy().get();
                try {
                    var proxyRoutePlanner = new DefaultProxyRoutePlanner(HttpHost.create(proxy));
                    httpAsyncClientBuilder.setRoutePlanner(proxyRoutePlanner);
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            }
            httpAsyncClientBuilder.addRequestInterceptorFirst((request, entity, context) -> {
                clientConfiguration.getHeadersSupplier().get().forEach((header, values) -> {
                    // The accept and content-type headers are already put on the request, despite this being the first
                    // interceptor.
                    if ("Accept".equalsIgnoreCase(header) || " Content-Type".equalsIgnoreCase(header)) {
                        request.removeHeaders(header);
                    }
                    values.forEach(value -> request.addHeader(header, value));
                });
            });

            // add httpclient configurator callbacks provided by the configuration
            for (ClientConfiguration.ClientConfigurationCallback<?> clientConfigurer : clientConfiguration
                    .getClientConfigurers()) {
                if (clientConfigurer instanceof ElasticsearchHttpClientConfigurationCallback httpClientConfigurer) {
                    httpAsyncClientBuilder = httpClientConfigurer.configure(httpAsyncClientBuilder);
                }
            }
        });

        builder.setConnectionConfigCallback(connectionConfigBuilder -> {

            if (!connectTimeout.isNegative()) {
                connectionConfigBuilder.setConnectTimeout(
                        Timeout.of(Math.toIntExact(connectTimeout.toMillis()), TimeUnit.MILLISECONDS));
            }
            if (!socketTimeout.isNegative()) {
                var soTimeout = Timeout.of(Math.toIntExact(socketTimeout.toMillis()), TimeUnit.MILLISECONDS);
                connectionConfigBuilder.setSocketTimeout(soTimeout);
            } else {
                connectionConfigBuilder.setSocketTimeout(Timeout.of(DEFAULT_SOCKET_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS));
            }

            // add connectionConfig configurator callbacks provided by the configuration
            for (ClientConfiguration.ClientConfigurationCallback<?> clientConfigurer : clientConfiguration
                    .getClientConfigurers()) {
                if (clientConfigurer instanceof ElasticsearchConnectionConfigurationCallback connectionConfigurationCallback) {
                    connectionConfigBuilder = connectionConfigurationCallback.configure(connectionConfigBuilder);
                }
            }
        });

        builder.setConnectionManagerCallback(poolingAsyncClientConnectionManagerBuilder -> {

            SSLContext sslContext = null;
            try {
                sslContext = clientConfiguration.getCaFingerprint().isPresent()
                        ? TransportUtils.sslContextFromCaFingerprint(clientConfiguration.getCaFingerprint().get())
                        : (clientConfiguration.getSslContext().isPresent()
                        ? clientConfiguration.getSslContext().get()
                        : SSLContext.getDefault());
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("could not create the default ssl context", e);
            }
            poolingAsyncClientConnectionManagerBuilder.setTlsStrategy(new BasicClientTlsStrategy(sslContext));

            // add connectionManager configurator callbacks provided by the configuration
            for (ClientConfiguration.ClientConfigurationCallback<?> clientConfigurer : clientConfiguration
                    .getClientConfigurers()) {
                if (clientConfigurer instanceof ElasticsearchConnectionManagerCallback connectionManagerCallback) {
                    poolingAsyncClientConnectionManagerBuilder = connectionManagerCallback.configure(poolingAsyncClientConnectionManagerBuilder);
                }
            }
        });

        builder.setRequestConfigCallback(requestConfigBuilder -> {

            if (!socketTimeout.isNegative()) {
                var soTimeout = Timeout.of(Math.toIntExact(socketTimeout.toMillis()), TimeUnit.MILLISECONDS);
                requestConfigBuilder.setConnectionRequestTimeout(soTimeout);
            } else {
                requestConfigBuilder
                        .setConnectionRequestTimeout(Timeout.of(DEFAULT_RESPONSE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS));
            }
            // add connectionConfig configurator callbacks provided by the configuration
            for (ClientConfiguration.ClientConfigurationCallback<?> clientConfigurer : clientConfiguration
                    .getClientConfigurers()) {
                if (clientConfigurer instanceof ElasticsearchRequestConfigCallback requestConfigCallback) {
                    requestConfigBuilder = requestConfigCallback.configure(requestConfigBuilder);
                }
            }
        });

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
     * {@link org.springframework.data.elasticsearch.client.ClientConfiguration.ClientConfigurationCallback} to configure
     * the Elasticsearch Rest5Client's connection with a {@link ConnectionConfig.Builder}
     *
     * @since 6.0
     */
    public interface ElasticsearchConnectionConfigurationCallback
            extends ClientConfiguration.ClientConfigurationCallback<ConnectionConfig.Builder> {

        static ElasticsearchConnectionConfigurationCallback from(
                Function<ConnectionConfig.Builder, ConnectionConfig.Builder> connectionConfigBuilderCallback) {

            Assert.notNull(connectionConfigBuilderCallback, "connectionConfigBuilderCallback must not be null");

            return connectionConfigBuilderCallback::apply;
        }
    }

    /**
     * {@link org.springframework.data.elasticsearch.client.ClientConfiguration.ClientConfigurationCallback} to configure
     * the Elasticsearch Rest5Client's connection manager with a {@link PoolingAsyncClientConnectionManagerBuilder}
     *
     * @since 6.0
     */
    public interface ElasticsearchConnectionManagerCallback
            extends ClientConfiguration.ClientConfigurationCallback<PoolingAsyncClientConnectionManagerBuilder> {

        static ElasticsearchConnectionManagerCallback from(
                Function<PoolingAsyncClientConnectionManagerBuilder, PoolingAsyncClientConnectionManagerBuilder> connectionManagerBuilderCallback) {

            Assert.notNull(connectionManagerBuilderCallback, "connectionManagerBuilderCallback must not be null");

            return connectionManagerBuilderCallback::apply;
        }
    }

    /**
     * {@link org.springframework.data.elasticsearch.client.ClientConfiguration.ClientConfigurationCallback} to configure
     * the Elasticsearch Rest5Client's connection manager with a {@link RequestConfig.Builder}
     *
     * @since 6.0
     */
    public interface ElasticsearchRequestConfigCallback
            extends ClientConfiguration.ClientConfigurationCallback<RequestConfig.Builder> {

        static ElasticsearchRequestConfigCallback from(
                Function<RequestConfig.Builder, RequestConfig.Builder> requestConfigBuilderCallback) {

            Assert.notNull(requestConfigBuilderCallback, "requestConfigBuilderCallback must not be null");

            return requestConfigBuilderCallback::apply;
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
