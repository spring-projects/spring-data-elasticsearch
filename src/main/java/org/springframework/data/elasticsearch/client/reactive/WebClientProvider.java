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
package org.springframework.data.elasticsearch.client.reactive;

import java.net.InetSocketAddress;
import java.util.function.Consumer;

import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Provider for {@link WebClient}s using a pre-configured {@code scheme}. This class returns {@link WebClient} for a
 * specific {@link InetSocketAddress endpoint} and encapsulates common configuration aspects of {@link WebClient} so
 * that code using {@link WebClient} is not required to apply further configuration to the actual client.
 * <p/>
 * Client instances are typically cached allowing reuse of pooled connections if configured on the
 * {@link ClientHttpConnector}.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 3.2
 */
public interface WebClientProvider {

	/**
	 * Creates a new {@link WebClientProvider} using the {@code http} scheme and a default {@link ClientHttpConnector}.
	 *
	 * @return the resulting {@link WebClientProvider}.
	 */
	static WebClientProvider http() {
		return create("http");
	}

	/**
	 * Creates a new {@link WebClientProvider} using the given {@code scheme} and a default {@link ClientHttpConnector}.
	 *
	 * @param scheme protocol scheme such as {@literal http} or {@literal https}.
	 * @return the resulting {@link WebClientProvider}.
	 */
	static WebClientProvider create(String scheme) {

		Assert.hasText(scheme, "Protocol scheme must not be empty");

		return new DefaultWebClientProvider(scheme, null);
	}

	/**
	 * Creates a new {@link WebClientProvider} given {@code scheme} and {@link ClientHttpConnector}.
	 *
	 * @param scheme protocol scheme such as {@literal http} or {@literal https}.
	 * @param connector the HTTP connector to use. Can be {@literal null}.
	 * @return the resulting {@link WebClientProvider}.
	 */
	static WebClientProvider create(String scheme, @Nullable ClientHttpConnector connector) {

		Assert.hasText(scheme, "Protocol scheme must not be empty");

		return new DefaultWebClientProvider(scheme, connector);
	}

	/**
	 * Obtain the {@link WebClient} configured with {@link #withDefaultHeaders(HttpHeaders) default HTTP headers} and
	 * {@link Consumer} error callback for a given {@link InetSocketAddress endpoint}.
	 *
	 * @return the {@link WebClient} for the given {@link InetSocketAddress endpoint}.
	 */
	WebClient get(InetSocketAddress endpoint);

	/**
	 * Obtain the {@link HttpHeaders} to be used by default.
	 *
	 * @return never {@literal null}. {@link HttpHeaders#EMPTY} by default.
	 */
	HttpHeaders getDefaultHeaders();

	/**
	 * Obtain the {@link Consumer error listener} to be used;
	 *
	 * @return never {@literal null}.
	 */
	Consumer<Throwable> getErrorListener();

	/**
	 * Create a new instance of {@link WebClientProvider} applying the given headers by default.
	 *
	 * @param headers must not be {@literal null}.
	 * @return new instance of {@link WebClientProvider}.
	 */
	WebClientProvider withDefaultHeaders(HttpHeaders headers);

	/**
	 * Create a new instance of {@link WebClientProvider} calling the given {@link Consumer} on error.
	 *
	 * @param errorListener must not be {@literal null}.
	 * @return new instance of {@link WebClientProvider}.
	 */
	WebClientProvider withErrorListener(Consumer<Throwable> errorListener);
}
