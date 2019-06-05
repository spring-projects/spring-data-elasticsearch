/*
 * Copyright 2018-2019 the original author or authors.
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
package org.springframework.data.elasticsearch.client;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.net.ssl.SSLContext;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.config.RequestConfig.Builder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HttpContext;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;

/**
 * Utility class for common access to Elasticsearch clients. {@link RestClients} consolidates set up routines for the
 * various drivers into a single place.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 3.2
 */
public final class RestClients {

	/**
	 * Name of whose value can be used to correlate log messages for this request.
	 */
	private static final String LOG_ID_ATTRIBUTE = RestClients.class.getName() + ".LOG_ID";

	private RestClients() {}

	/**
	 * Start here to create a new client tailored to your needs.
	 *
	 * @return new instance of {@link ElasticsearchRestClient}.
	 */
	public static ElasticsearchRestClient create(ClientConfiguration clientConfiguration) {

		Assert.notNull(clientConfiguration, "ClientConfiguration must not be null!");

		HttpHost[] httpHosts = formattedHosts(clientConfiguration.getEndpoints(), clientConfiguration.useSsl()).stream()
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

			if (ClientLogger.isEnabled()) {

				HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();

				clientBuilder.addInterceptorLast((HttpRequestInterceptor) interceptor);
				clientBuilder.addInterceptorLast((HttpResponseInterceptor) interceptor);
			}

			Duration connectTimeout = clientConfiguration.getConnectTimeout();
			Duration timeout = clientConfiguration.getSocketTimeout();

			Builder requestConfigBuilder = RequestConfig.custom();

			if (!connectTimeout.isNegative()) {

				requestConfigBuilder.setConnectTimeout(Math.toIntExact(connectTimeout.toMillis()));
				requestConfigBuilder.setConnectionRequestTimeout(Math.toIntExact(connectTimeout.toMillis()));
			}

			if (!timeout.isNegative()) {
				requestConfigBuilder.setSocketTimeout(Math.toIntExact(timeout.toMillis()));
			}

			clientBuilder.setDefaultRequestConfig(requestConfigBuilder.build());

			return clientBuilder;
		});

		RestHighLevelClient client = new RestHighLevelClient(builder);
		return () -> client;
	}

	private static List<String> formattedHosts(List<InetSocketAddress> hosts, boolean useSsl) {
		return hosts.stream().map(it -> (useSsl ? "https" : "http") + "://" + it).collect(Collectors.toList());
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

	/**
	 * Logging interceptors for Elasticsearch client logging.
	 *
	 * @see ClientLogger
	 * @since 3.2
	 */
	private static class HttpLoggingInterceptor implements HttpResponseInterceptor, HttpRequestInterceptor {

		@Override
		public void process(HttpRequest request, HttpContext context) throws IOException {

			String logId = (String) context.getAttribute(RestClients.LOG_ID_ATTRIBUTE);

			if (logId == null) {

				logId = ClientLogger.newLogId();
				context.setAttribute(RestClients.LOG_ID_ATTRIBUTE, logId);
			}

			if (request instanceof HttpEntityEnclosingRequest && ((HttpEntityEnclosingRequest) request).getEntity() != null) {

				HttpEntityEnclosingRequest entityRequest = (HttpEntityEnclosingRequest) request;
				HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
				ByteArrayOutputStream buffer = new ByteArrayOutputStream();
				entity.writeTo(buffer);

				if (!entity.isRepeatable()) {
					entityRequest.setEntity(new ByteArrayEntity(buffer.toByteArray()));
				}

				ClientLogger.logRequest(logId, request.getRequestLine().getMethod(), request.getRequestLine().getUri(), "",
						() -> new String(buffer.toByteArray()));
			} else {
				ClientLogger.logRequest(logId, request.getRequestLine().getMethod(), request.getRequestLine().getUri(), "");
			}
		}

		@Override
		public void process(HttpResponse response, HttpContext context) {

			String logId = (String) context.getAttribute(RestClients.LOG_ID_ATTRIBUTE);

			ClientLogger.logRawResponse(logId, HttpStatus.resolve(response.getStatusLine().getStatusCode()));
		}
	}
}
