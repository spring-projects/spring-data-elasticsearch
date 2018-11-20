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
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.SSLContext;

import org.springframework.data.elasticsearch.client.ClientConfiguration.ClientConfigurationBuilderWithOptionalDefaultHeaders;
import org.springframework.data.elasticsearch.client.ClientConfiguration.ClientConfigurationBuilderWithRequiredHost;
import org.springframework.data.elasticsearch.client.ClientConfiguration.MaybeSecureClientConfigurationBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Default builder implementation for {@link ClientConfiguration}.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 4.0
 */
class ClientConfigurationBuilder
		implements ClientConfigurationBuilderWithRequiredHost, MaybeSecureClientConfigurationBuilder {

	private List<String> hosts = new ArrayList<>();
	private HttpHeaders headers = HttpHeaders.EMPTY;
	private boolean useSsl;
	private @Nullable SSLContext sslContext;

	@Override
	public MaybeSecureClientConfigurationBuilder connectedTo(String... hostAndPorts) {

		Assert.notEmpty(hostAndPorts, "At least one host is required");

		this.hosts.addAll(Arrays.asList(hostAndPorts));
		return this;
	}

	@Override
	public ClientConfigurationBuilderWithOptionalDefaultHeaders usingSsl() {

		this.useSsl = true;
		return this;
	}

	@Override
	public ClientConfigurationBuilderWithOptionalDefaultHeaders usingSsl(SSLContext sslContext) {

		Assert.notNull(sslContext, "SSL Context must not be null");

		this.useSsl = true;
		this.sslContext = sslContext;
		return this;
	}

	@Override
	public ClientConfigurationBuilderWithOptionalDefaultHeaders withDefaultHeaders(HttpHeaders defaultHeaders) {

		Assert.notNull(defaultHeaders, "Default HTTP headers must not be null");

		this.headers = defaultHeaders;
		return this;
	}

	@Override
	public ClientConfiguration build() {
		return new DefaultClientConfiguration(this.hosts, this.headers, this.useSsl, this.sslContext);
	}
}
