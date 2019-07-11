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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.net.InetSocketAddress;
import java.time.Duration;

import javax.net.ssl.SSLContext;

import org.junit.Test;
import org.springframework.http.HttpHeaders;

/**
 * Unit tests for {@link ClientConfiguration}.
 *
 * @author Mark Paluch
 * @author Peter-Josef Meisch
 */
public class ClientConfigurationUnitTests {

	private static final String AUTHORIZATION_HEADER = "Authorization";

	@Test // DATAES-488
	public void shouldCreateSimpleConfiguration() {

		ClientConfiguration clientConfiguration = ClientConfiguration.create("localhost:9200");

		assertThat(clientConfiguration.getEndpoints()).containsOnly(InetSocketAddress.createUnresolved("localhost", 9200));
	}

	@Test // DATAES-488, DATAES-504
	public void shouldCreateCustomizedConfiguration() {

		HttpHeaders headers = new HttpHeaders();
		headers.set("foo", "bar");

		ClientConfiguration clientConfiguration = ClientConfiguration.builder() //
				.connectedTo("foo", "bar") //
				.usingSsl() //
				.withDefaultHeaders(headers) //
				.withConnectTimeout(Duration.ofDays(1)).withSocketTimeout(Duration.ofDays(2)).build();

		assertThat(clientConfiguration.getEndpoints()).containsOnly(InetSocketAddress.createUnresolved("foo", 9200),
				InetSocketAddress.createUnresolved("bar", 9200));
		assertThat(clientConfiguration.useSsl()).isTrue();
		assertThat(clientConfiguration.getDefaultHeaders().get("foo")).containsOnly("bar");
		assertThat(clientConfiguration.getConnectTimeout()).isEqualTo(Duration.ofDays(1));
		assertThat(clientConfiguration.getSocketTimeout()).isEqualTo(Duration.ofDays(2));
	}

	@Test // DATAES-488, DATAES-504
	public void shouldCreateSslConfiguration() {

		SSLContext sslContext = mock(SSLContext.class);

		ClientConfiguration clientConfiguration = ClientConfiguration.builder() //
				.connectedTo("foo", "bar") //
				.usingSsl(sslContext) //
				.build();

		assertThat(clientConfiguration.getEndpoints()).containsOnly(InetSocketAddress.createUnresolved("foo", 9200),
				InetSocketAddress.createUnresolved("bar", 9200));
		assertThat(clientConfiguration.useSsl()).isTrue();
		assertThat(clientConfiguration.getSslContext()).contains(sslContext);
		assertThat(clientConfiguration.getConnectTimeout()).isEqualTo(Duration.ofSeconds(10));
		assertThat(clientConfiguration.getSocketTimeout()).isEqualTo(Duration.ofSeconds(5));
	}

	@Test // DATAES-607
	public void shouldAddBasicAuthenticationHeaderWhenNoHeadersAreSet() {

		final String username = "secretUser";
		final String password = "secretPassword";

		ClientConfiguration clientConfiguration = ClientConfiguration.builder() //
				.connectedTo("foo", "bar") //
				.withBasicAuth(username, password) //
				.build();

		assertThat(clientConfiguration.getDefaultHeaders().get(AUTHORIZATION_HEADER))
				.containsOnly(buildBasicAuth(username, password));
	}

	@Test // DATAES-607
	public void shouldAddBasicAuthenticationHeaderAndKeepHeaders() {

		final String username = "secretUser";
		final String password = "secretPassword";

		HttpHeaders defaultHeaders = new HttpHeaders();
		defaultHeaders.set("foo", "bar");

		ClientConfiguration clientConfiguration = ClientConfiguration.builder() //
				.connectedTo("foo", "bar") //
				.withBasicAuth(username, password) //
				.withDefaultHeaders(defaultHeaders).build();
		final HttpHeaders httpHeaders = clientConfiguration.getDefaultHeaders();

		assertThat(httpHeaders.get(AUTHORIZATION_HEADER)).containsOnly(buildBasicAuth(username, password));
		assertThat(httpHeaders.get("foo")).containsOnly("bar");
	}

	private String buildBasicAuth(String username, String password) {

		HttpHeaders headers = new HttpHeaders();
		headers.setBasicAuth(username, password);
		return headers.get(AUTHORIZATION_HEADER).get(0);
	}
}
