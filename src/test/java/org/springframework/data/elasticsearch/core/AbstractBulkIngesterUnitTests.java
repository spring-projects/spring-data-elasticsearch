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
package org.springframework.data.elasticsearch.core;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.core.bulk.BulkIngesterListener;
import org.springframework.data.elasticsearch.core.bulk.BulkOperationResult;
import org.springframework.data.elasticsearch.core.bulk.BulkOperationType;
import org.springframework.data.elasticsearch.core.event.AfterSaveCallback;
import org.springframework.data.elasticsearch.core.event.BeforeConvertCallback;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.data.mapping.callback.EntityCallbacks;

/**
 * @author Ralph Ursprung
 */
class AbstractBulkIngesterUnitTests {

	private static final IndexCoordinates INDEX = IndexCoordinates.of("bulk-ingester-test");

	private final RecordingListener listener = new RecordingListener();
	private final List<SampleEntity> afterSaveEntities = new ArrayList<>();
	private TestBulkIngester ingester;

	@BeforeEach
	void setUp() {

		final AbstractElasticsearchTemplate template = mock(AbstractElasticsearchTemplate.class,
				withSettings().useConstructor().defaultAnswer(CALLS_REAL_METHODS));
		template.setEntityCallbacks(EntityCallbacks.create( //
				(BeforeConvertCallback<SampleEntity>) (entity, index) -> {
					entity.text = "converted on " + Thread.currentThread().getName();
					return entity;
				}, //
				(AfterSaveCallback<SampleEntity>) (entity, index) -> {
					afterSaveEntities.add(entity);
					return entity;
				}));

		ingester = new TestBulkIngester(template, listener);
	}

	@Test // #2071
	@DisplayName("should run before convert callback on the calling thread when adding an index query")
	void shouldRunBeforeConvertCallbackOnTheCallingThreadWhenAddingAnIndexQuery() {

		final var entity = new SampleEntity();
		ingester.add(indexQuery(entity), "ctx");

		assertThat(entity.text).isEqualTo("converted on " + Thread.currentThread().getName());
		assertThat(ingester.added).hasSize(1);
		assertThat(ingester.contexts.get(0).getOperationType()).isEqualTo(BulkOperationType.INDEX);
		assertThat(ingester.contexts.get(0).getContext()).isEqualTo("ctx");
	}

	@Test // #2071
	@DisplayName("should map responses to results and update entities only for successful index operations")
	void shouldMapResponsesToResultsAndUpdateEntitiesOnlyForSuccessfulIndexOperations() {

		final var created = new SampleEntity();
		final var failed = new SampleEntity();
		ingester.add(new IndexQueryBuilder().withObject(created).withOpType(IndexQuery.OpType.CREATE).build(), "create");
		ingester.add(indexQuery(failed), "failed");
		ingester.add(updateQuery("2"), "update");
		ingester.addDelete("3", "routing", "delete");

		ingester.respond(7, List.of( //
				new AbstractBulkIngester.ItemResponse("generated", "idx", 201, 1L, 2L, 3L, null), //
				new AbstractBulkIngester.ItemResponse("1", "idx", 409, null, null, null, "conflict"), //
				new AbstractBulkIngester.ItemResponse("2", "idx", 200, 4L, 2L, 5L, null), //
				new AbstractBulkIngester.ItemResponse("3", "idx", 200, 6L, 2L, 7L, null)));

		assertThat(created.id).isEqualTo("generated");
		assertThat(created.version).isEqualTo(3L);
		assertThat(failed.id).isNull();
		assertThat(failed.version).isNull();
		assertThat(afterSaveEntities).containsExactly(created);

		assertThat(listener.executionId).isEqualTo(7);
		final var results = listener.results;
		assertThat(results).extracting(BulkOperationResult::operationType).containsExactly(BulkOperationType.CREATE,
				BulkOperationType.INDEX, BulkOperationType.UPDATE, BulkOperationType.DELETE);
		assertThat(results).extracting(BulkOperationResult::context).containsExactly("create", "failed", "update",
				"delete");
		assertThat(results).extracting(BulkOperationResult::isSuccessful).containsExactly(true, false, true, true);
		assertThat(results.get(0).indexedObjectInformation())
				.isEqualTo(new IndexedObjectInformation("generated", "idx", 1L, 2L, 3L));
		assertThat(results.get(1).indexedObjectInformation()).isNull();
		assertThat(results.get(1).failure().status()).isEqualTo(409);
		assertThat(results.get(1).failure().errorMessage()).isEqualTo("conflict");
		assertThat(results.get(3).indexedObjectInformation().version()).isEqualTo(7L);
	}

	@Test // #2071
	@DisplayName("should pass converted id and routing for delete operations")
	void shouldPassConvertedIdAndRoutingForDeleteOperations() {

		ingester.addDelete("42");
		ingester.addDelete("43", "routing", null);

		assertThat(ingester.added).containsExactly(new Delete("42", null), new Delete("43", "routing"));
		assertThat(ingester.contexts).extracting(AbstractBulkIngester.OperationContext::getOperationType)
				.containsOnly(BulkOperationType.DELETE);
	}

	@Test // #2071
	@DisplayName("should unwrap and translate request failures")
	void shouldUnwrapAndTranslateRequestFailures() {

		ingester.add(updateQuery("1"), "first");
		ingester.addDelete("2");
		final var cause = new IllegalStateException("connection refused");

		final var translated = ingester.fail(3, new CompletionException(new ExecutionException(cause)));

		assertThat(ingester.translatedCause).isSameAs(cause);
		assertThat(translated).isInstanceOf(DataAccessResourceFailureException.class).hasCause(cause);
		assertThat(listener.executionId).isEqualTo(3);
		assertThat(listener.failure).isSameAs(translated);
		assertThat(listener.contexts).containsExactly("first", null);
	}

	@Test // #2071
	@DisplayName("should report a request failure if the number of items does not match")
	void shouldReportARequestFailureIfTheNumberOfItemsDoesNotMatch() {

		ingester.addDelete("1");

		ingester.respond(1, List.of());

		assertThat(listener.results).isNull();
		assertThat(listener.failure).isInstanceOf(IllegalStateException.class);
	}

	@Test // #2071
	@DisplayName("should not propagate exceptions thrown by the listener")
	void shouldNotPropagateExceptionsThrownByTheListener() {

		listener.exceptionToThrow = new RuntimeException("listener failed");
		ingester.addDelete("1");

		assertThatNoException().isThrownBy(() -> ingester.respond(1,
				List.of(new AbstractBulkIngester.ItemResponse("1", "idx", 200, 1L, 1L, 1L, null))));
		assertThatNoException().isThrownBy(() -> ingester.fail(2, new RuntimeException("request failed")));
	}

	@Test // #2071
	@DisplayName("should prevent the listener from using the ingester")
	void shouldPreventTheListenerFromUsingTheIngester() {

		listener.onAfterBulk = () -> ingester.addDelete("2");
		ingester.addDelete("1");

		ingester.respond(1, List.of(new AbstractBulkIngester.ItemResponse("1", "idx", 200, 1L, 1L, 1L, null)));

		assertThat(listener.exceptionInCallback).isInstanceOf(IllegalStateException.class);
		assertThat(ingester.added).hasSize(1);
		assertThatNoException().isThrownBy(() -> ingester.addDelete("3"));
	}

	@Test // #2071
	@DisplayName("should close only once and reject operations after close")
	void shouldCloseOnlyOnceAndRejectOperationsAfterClose() {

		ingester.close();
		ingester.close();

		assertThat(ingester.closeCount).isEqualTo(1);
		assertThatIllegalStateException().isThrownBy(() -> ingester.add(indexQuery(new SampleEntity())));
		assertThatIllegalStateException().isThrownBy(() -> ingester.add(updateQuery("1")));
		assertThatIllegalStateException().isThrownBy(() -> ingester.addDelete("1"));
	}

	@Test // #2071
	@DisplayName("should require a listener")
	void shouldRequireAListener() {

		final var template = mock(AbstractElasticsearchTemplate.class);

		// noinspection DataFlowIssue
		assertThatIllegalArgumentException().isThrownBy(() -> new TestBulkIngester(template, null));
	}

	private static UpdateQuery updateQuery(final String id) {
		return UpdateQuery.builder(id).withDocument(org.springframework.data.elasticsearch.core.document.Document.create())
				.build();
	}

	private static IndexQuery indexQuery(final SampleEntity entity) {
		return new IndexQueryBuilder().withId("1").withObject(entity).build();
	}

	record Delete(String id, @Nullable String routing) {
	}

	static class TestBulkIngester extends AbstractBulkIngester<String> {

		final List<Object> added = new ArrayList<>();
		final List<OperationContext<String>> contexts = new ArrayList<>();
		int closeCount;
		@Nullable Throwable translatedCause;

		TestBulkIngester(final AbstractElasticsearchTemplate template, final BulkIngesterListener<String> listener) {
			super(template, INDEX, listener);
		}

		void respond(final long executionId, final List<ItemResponse> items) {
			onResponse(executionId, List.copyOf(contexts), items);
		}

		RuntimeException fail(final long executionId, final Throwable failure) {
			return onFailure(executionId, List.copyOf(contexts), failure);
		}

		@Override
		protected void doAdd(final IndexQuery query, final OperationContext<String> context) {
			added.add(query);
			contexts.add(context);
		}

		@Override
		protected void doAdd(final UpdateQuery query, final OperationContext<String> context) {
			added.add(query);
			contexts.add(context);
		}

		@Override
		protected void doAddDelete(final String id, final @Nullable String routing,
				final OperationContext<String> context) {
			added.add(new Delete(id, routing));
			contexts.add(context);
		}

		@Override
		protected void doFlush() {}

		@Override
		protected void doClose() {
			closeCount++;
		}

		@Override
		protected RuntimeException translateException(final Throwable throwable) {
			translatedCause = throwable;
			return new DataAccessResourceFailureException("translated", throwable);
		}

		@Override
		public int getPendingOperations() {
			return 0;
		}

		@Override
		public long getPendingOperationsSize() {
			return 0;
		}

		@Override
		public int getPendingRequests() {
			return 0;
		}

		@Override
		public long getOperationsCount() {
			return 0;
		}

		@Override
		public long getOperationContentionsCount() {
			return 0;
		}

		@Override
		public long getRequestCount() {
			return 0;
		}

		@Override
		public long getRequestContentionsCount() {
			return 0;
		}
	}

	static class RecordingListener implements BulkIngesterListener<String> {

		long executionId = -1;
		@Nullable List<BulkOperationResult<String>> results;
		@Nullable List<@Nullable String> contexts;
		@Nullable RuntimeException failure;
		@Nullable RuntimeException exceptionToThrow;
		@Nullable Runnable onAfterBulk;
		@Nullable RuntimeException exceptionInCallback;

		@Override
		public void afterBulk(final long executionId, final List<BulkOperationResult<String>> results) {
			this.executionId = executionId;
			this.results = results;
			runCallback();
		}

		@Override
		public void afterBulk(final long executionId, final List<@Nullable String> contexts,
				final RuntimeException failure) {
			this.executionId = executionId;
			this.contexts = contexts;
			this.failure = failure;
			runCallback();
		}

		private void runCallback() {

			if (onAfterBulk != null) {
				try {
					onAfterBulk.run();
				} catch (RuntimeException e) {
					exceptionInCallback = e;
				}
			}

			if (exceptionToThrow != null) {
				throw exceptionToThrow;
			}
		}
	}

	@Document(indexName = "bulk-ingester-test")
	static class SampleEntity {
		@Nullable @Id String id;
		@Nullable @Version Long version;
		@Nullable String text;
	}
}
