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

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.net.ssl.SSLContext;

import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;

/**
 * Default {@link ClientConfiguration} implementation.
 *
 * @author Mark Paluch
 * @author Christoph Strobl
 * @since 3.2
 */
class DefaultClientConfiguration implements ClientConfiguration {

	private final List<InetSocketAddress> hosts;
	private final HttpHeaders headers;
	private final boolean useSsl;
	private final @Nullable SSLContext sslContext;
	private final Duration soTimeout;
	private final Duration connectTimeout;

	DefaultClientConfiguration(List<InetSocketAddress> hosts, HttpHeaders headers, boolean useSsl,
			@Nullable SSLContext sslContext, Duration soTimeout, Duration connectTimeout) {

		this.hosts = Collections.unmodifiableList(new ArrayList<>(hosts));
		this.headers = new HttpHeaders(headers);
		this.useSsl = useSsl;
		this.sslContext = sslContext;
		this.soTimeout = soTimeout;
		this.connectTimeout = connectTimeout;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.client.ClientConfiguration#getEndpoints()
	 */
	@Override
	public List<InetSocketAddress> getEndpoints() {
		return this.hosts;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.client.ClientConfiguration#getDefaultHeaders()
	 */
	@Override
	public HttpHeaders getDefaultHeaders() {
		return this.headers;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.client.ClientConfiguration#useSsl()
	 */
	@Override
	public boolean useSsl() {
		return this.useSsl;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.client.ClientConfiguration#getSslContext()
	 */
	@Override
	public Optional<SSLContext> getSslContext() {
		return Optional.ofNullable(this.sslContext);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.client.ClientConfiguration#getConnectTimeout()
	 */
	@Override
	public Duration getConnectTimeout() {
		return this.connectTimeout;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.client.ClientConfiguration#getSocketTimeout()
	 */
	@Override
	public Duration getSocketTimeout() {
		return this.soTimeout;
	}

}
