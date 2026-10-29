/*
 * Copyright 2026-present the original author or authors.
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.bulk.OperationType;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.elasticsearch.core.RefreshPolicy;
import org.springframework.data.elasticsearch.core.bulk.BulkIngesterListener;
import org.springframework.data.elasticsearch.core.bulk.BulkIngesterOptions;
import org.springframework.data.elasticsearch.core.bulk.BulkOperationResult;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.BulkOptions;

/**
 * @author Ralph Ursprung
 */
class ElasticsearchBulkIngesterUnitTests {

	private static final IndexCoordinates INDEX = IndexCoordinates.of("bulk-ingester-test");

	private final ElasticsearchTransport transport = mock(ElasticsearchTransport.class);
	private final RecordingListener listener = new RecordingListener();
	private ElasticsearchTemplate template;

	@BeforeEach
	void setUp() {

		when(transport.jsonpMapper()).thenReturn(new JacksonJsonpMapper());
		template = new ElasticsearchTemplate(new ElasticsearchClient(transport));
	}

	@Test // #2071
	@DisplayName("should require a listener")
	void shouldRequireAListener() {

		// noinspection DataFlowIssue
		assertThatIllegalArgumentException()
				.isThrownBy(() -> template.bulkIngester(BulkIngesterOptions.defaultOptions(), INDEX, null));
	}

	@Test // #2071
	@DisplayName("should translate request failures")
	void shouldTranslateRequestFailures() {

		doReturn(CompletableFuture.failedFuture(new IOException("connection refused"))).when(transport)
				.performRequestAsync(any(), any(), any());

		try (final var ingester = template.bulkIngester(INDEX, listener)) {
			ingester.addDelete("1", null, "first");
			ingester.addDelete("2", null, "second");
		}

		assertThat(listener.failures).hasSize(1);
		assertThat(listener.failures.get(0)).isInstanceOf(DataAccessResourceFailureException.class)
				.hasRootCauseInstanceOf(IOException.class);
		assertThat(listener.contexts).containsExactly("first", "second");
	}

	@Test // #2071
	@DisplayName("should apply request settings and map response items")
	void shouldApplyRequestSettingsAndMapResponseItems() {

		final var response = BulkResponse.of(b -> b //
				.errors(true) //
				.took(1) //
				.items( //
						BulkResponseItem.of(i -> i.operationType(OperationType.Delete).index(INDEX.getIndexName()).id("1")
								.status(200).seqNo(3L).primaryTerm(1L).version(2L)), //
						BulkResponseItem.of(i -> i.operationType(OperationType.Delete).index(INDEX.getIndexName()).id("2")
								.status(404).error(e -> e.type("not_found")))));
		doReturn(CompletableFuture.completedFuture(response)).when(transport).performRequestAsync(any(), any(), any());

		final var options = BulkIngesterOptions.builder() //
				.withBulkOptions(BulkOptions.builder().withPipeline("pipeline").build()) //
				.build();
		try (final var ingester = template.withRefreshPolicy(RefreshPolicy.IMMEDIATE).bulkIngester(options, INDEX,
				listener)) {
			ingester.addDelete("1", null, "first");
			ingester.addDelete("2", "routing", "second");
		}

		final var requestCaptor = ArgumentCaptor.forClass(BulkRequest.class);
		verify(transport).performRequestAsync(requestCaptor.capture(), any(), any());
		final var request = requestCaptor.getValue();
		assertThat(request.refresh()).isEqualTo(Refresh.True);
		assertThat(request.pipeline()).isEqualTo("pipeline");
		assertThat(request.operations()).hasSize(2);
		assertThat(request.operations().get(1).delete().routing()).isEqualTo("routing");

		assertThat(listener.results).hasSize(2);
		final var first = listener.results.get(0);
		assertThat(first.isSuccessful()).isTrue();
		assertThat(first.context()).isEqualTo("first");
		assertThat(first.indexedObjectInformation().seqNo()).isEqualTo(3L);
		assertThat(first.indexedObjectInformation().version()).isEqualTo(2L);
		final var second = listener.results.get(1);
		assertThat(second.isSuccessful()).isFalse();
		assertThat(second.status()).isEqualTo(404);
		assertThat(second.failure().errorMessage()).isEqualTo("not_found");
	}

	static class RecordingListener implements BulkIngesterListener<String> {

		final List<BulkOperationResult<String>> results = new CopyOnWriteArrayList<>();
		final List<@Nullable String> contexts = new CopyOnWriteArrayList<>();
		final List<RuntimeException> failures = new CopyOnWriteArrayList<>();

		@Override
		public void afterBulk(final long executionId, final List<BulkOperationResult<String>> results) {
			this.results.addAll(results);
		}

		@Override
		public void afterBulk(final long executionId, final List<@Nullable String> contexts,
				final RuntimeException failure) {
			this.contexts.addAll(contexts);
			this.failures.add(failure);
		}
	}
}
