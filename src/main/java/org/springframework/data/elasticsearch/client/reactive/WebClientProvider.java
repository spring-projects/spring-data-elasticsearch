/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.elasticsearch.client.reactive;

import java.util.function.Consumer;

import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Provider for {@link WebClient} instances for a specific {@code host}.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 4.0 TODO: Encapsulate host (scheme, host, port) within a value object.
 */
public interface WebClientProvider {

	/**
	 * Creates a new {@link WebClientProvider} using a default {@link ClientHttpConnector}.
	 *
	 * @return the resulting {@link WebClientProvider}.
	 */
	static WebClientProvider create() {
		return new DefaultWebClientProvider(null);
	}

	/**
	 * Creates a new {@link WebClientProvider} given {@link ClientHttpConnector}.
	 *
	 * @param connector the HTTP connector to use.
	 * @return the resulting {@link WebClientProvider}.
	 */
	static WebClientProvider create(ClientHttpConnector connector) {
		return new DefaultWebClientProvider(connector);
	}

	/**
	 * Obtain the {@link WebClient} configured with {@link #withDefaultHeaders(HttpHeaders) default HTTP headers} and
	 * {@link Consumer} error callback for a given {@code baseUrl}.
	 *
	 * @return the {@link WebClient} for the given {@code baseUrl}.
	 */
	WebClient get(String baseUrl);

	/**
	 * Obtain the {@link HttpHeaders} to be used by default.
	 *
	 * @return never {@literal null}. {@link HttpHeaders#EMPTY} by default.
	 */
	HttpHeaders getDefaultHeaders();

	/**
	 * Create a new instance of {@link WebClientProvider} applying the given headers by default.
	 *
	 * @param headers must not be {@literal null}.
	 * @return new instance of {@link WebClientProvider}.
	 */
	WebClientProvider withDefaultHeaders(HttpHeaders headers);

	/**
	 * Obtain the {@link Consumer error listener} to be used;
	 *
	 * @return never {@literal null}.
	 */
	Consumer<Throwable> getErrorListener();

	/**
	 * Create a new instance of {@link WebClientProvider} calling the given {@link Consumer} on error.
	 *
	 * @param errorListener must not be {@literal null}.
	 * @return new instance of {@link WebClientProvider}.
	 */
	WebClientProvider withErrorListener(Consumer<Throwable> errorListener);
}
