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
package org.springframework.data.elasticsearch.client;

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
 * @since 4.0
 */
class DefaultClientConfiguration implements ClientConfiguration {

	private final List<String> hosts;
	private final HttpHeaders headers;
	private final boolean useSsl;
	private final @Nullable SSLContext sslContext;

	DefaultClientConfiguration(List<String> hosts, HttpHeaders headers, boolean useSsl, @Nullable SSLContext sslContext) {

		this.hosts = Collections.unmodifiableList(new ArrayList<>(hosts));
		this.headers = new HttpHeaders(headers);
		this.useSsl = useSsl;
		this.sslContext = sslContext;
	}

	@Override
	public List<String> getHosts() {
		return this.hosts;
	}

	@Override
	public HttpHeaders getDefaultHeaders() {
		return this.headers;
	}

	@Override
	public boolean useSsl() {
		return this.useSsl;
	}

	@Override
	public Optional<SSLContext> getSslContext() {
		return Optional.ofNullable(this.sslContext);
	}
}
