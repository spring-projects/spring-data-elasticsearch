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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import javax.net.ssl.SSLContext;

import org.junit.Test;
import org.springframework.http.HttpHeaders;

/**
 * Unit tests for {@link ClientConfiguration}.
 *
 * @author Mark Paluch
 */
public class ClientConfigurationUnitTests {

	@Test // DATAES-488
	public void shouldCreateSimpleConfiguration() {

		ClientConfiguration clientConfiguration = ClientConfiguration.create("localhost:9200");

		assertThat(clientConfiguration.getHosts()).containsOnly("localhost:9200");
	}

	@Test // DATAES-488
	public void shouldCreateCustomizedConfiguration() {

		HttpHeaders headers = new HttpHeaders();
		headers.set("foo", "bar");

		ClientConfiguration clientConfiguration = ClientConfiguration.builder() //
				.connectedTo("foo", "bar") //
				.usingSsl() //
				.withDefaultHeaders(headers) //
				.build();

		assertThat(clientConfiguration.getHosts()).containsOnly("foo", "bar");
		assertThat(clientConfiguration.useSsl()).isTrue();
		assertThat(clientConfiguration.getDefaultHeaders().get("foo")).containsOnly("bar");
	}

	@Test // DATAES-488
	public void shouldCreateSslConfiguration() {

		SSLContext sslContext = mock(SSLContext.class);

		ClientConfiguration clientConfiguration = ClientConfiguration.builder() //
				.connectedTo("foo", "bar") //
				.usingSsl(sslContext) //
				.build();

		assertThat(clientConfiguration.getHosts()).containsOnly("foo", "bar");
		assertThat(clientConfiguration.useSsl()).isTrue();
		assertThat(clientConfiguration.getSslContext()).contains(sslContext);
	}
}
