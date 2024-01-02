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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import co.elastic.clients.elasticsearch.indices.RefreshRequest;
import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;

/**
 * @author Urs Keller
 * @since 5.0
 */
@ExtendWith(MockitoExtension.class)
class ReactiveIndicesTemplateTest {
	@Mock private ElasticsearchConverter elasticsearchConverter;
	@Mock private ReactiveElasticsearchIndicesClient client;
	@Mock private ReactiveClusterTemplate cluster;
	@Mock private ElasticsearchTransport transport;
	@Mock private JsonpMapper jsonpMapper;
	@Captor ArgumentCaptor<RefreshRequest> refreshRequest;

	@BeforeEach
	void setup() {
		doReturn(transport).when(client)._transport();
		doReturn(jsonpMapper).when(transport).jsonpMapper();
	}

	/**
	 * Tests that refresh is called with the indices bound to the template
	 */
	@Test
	void refresh() {
		IndexCoordinates indexCoordinate = IndexCoordinates.of("i1", "i2");
		ReactiveIndicesTemplate template = new ReactiveIndicesTemplate(client, cluster, elasticsearchConverter,
				indexCoordinate);
		doReturn(Mono.empty()).when(client).refresh(any(RefreshRequest.class));

		template.refresh().as(StepVerifier::create).verifyComplete();

		verify(client).refresh(refreshRequest.capture());
		assertEquals(List.of("i1", "i2"), refreshRequest.getValue().index());

		verifyNoMoreInteractions(elasticsearchConverter, client, transport, jsonpMapper);
	}
}
