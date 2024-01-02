/*
 * Copyright 2021-2024 the original author or authors.
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
package org.springframework.data.elasticsearch.client.elc;

import static org.mockito.Mockito.*;

import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;

import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Peter-Josef Meisch
 * @since 4.4
 */
@ExtendWith(MockitoExtension.class)
class AutoCloseableElasticsearchClientTest {

	@Mock private RestClient restClient;
	@Mock private JsonpMapper jsonMapper;

	@Test // #1973
	@DisplayName("should close the RestClient")
	void shouldCloseTheRestClient() throws Exception {

		ElasticsearchTransport transport = new RestClientTransport(restClient, jsonMapper);
		// noinspection EmptyTryBlock
		try (AutoCloseableElasticsearchClient ignored = new AutoCloseableElasticsearchClient(transport)) {}

		verify(restClient).close();
	}
}
