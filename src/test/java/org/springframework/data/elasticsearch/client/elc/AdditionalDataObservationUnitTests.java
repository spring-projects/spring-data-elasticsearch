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
import static org.mockito.Mockito.*;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import io.micrometer.observation.tck.TestObservationRegistry;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.data.elasticsearch.core.reindex.ReindexRequest;
import org.springframework.data.elasticsearch.core.script.Script;

class AdditionalDataObservationUnitTests {

	private static final IndexCoordinates INDEX = IndexCoordinates.of("source");
	private static final ReindexRequest REINDEX = ReindexRequest.builder(INDEX, IndexCoordinates.of("destination")).build();
	private static final Script SCRIPT = new Script("script", "painless", "return 1");
	private final RuntimeException failure = new IllegalStateException("request failed");
	private final TestObservationRegistry registry = TestObservationRegistry.create();
	private final List<ElasticsearchObservationContext> stopped = new ArrayList<>();

	AdditionalDataObservationUnitTests() {
		registry.observationConfig().observationHandler(new ObservationHandler<ElasticsearchObservationContext>() {
			@Override
			public boolean supportsContext(final Observation.Context context) {
				return context instanceof ElasticsearchObservationContext;
			}

			@Override
			public void onStop(final ElasticsearchObservationContext context) {
				stopped.add(context);
			}
		});
	}

	static Stream<Operation> operations() {
		return Stream.of(
				new Operation("openPointInTime", "source", t -> t.openPointInTime(INDEX, Duration.ofMinutes(1)),
						t -> t.openPointInTime(INDEX, Duration.ofMinutes(1))),
				new Operation("closePointInTime", null, t -> t.closePointInTime("pit"), t -> t.closePointInTime("pit")),
				new Operation("reindex", "source", t -> t.reindex(REINDEX), t -> t.reindex(REINDEX)),
				new Operation("submitReindex", "source", t -> t.submitReindex(REINDEX), t -> t.submitReindex(REINDEX)),
				new Operation("putScript", null, t -> t.putScript(SCRIPT), t -> t.putScript(SCRIPT)),
				new Operation("getScript", null, t -> t.getScript("script"), t -> t.getScript("script")),
				new Operation("deleteScript", null, t -> t.deleteScript("script"), t -> t.deleteScript("script")));
	}

	@ParameterizedTest
	@MethodSource("operations")
	void imperativeFailuresStopOneObservation(final Operation operation) {
		final var converter = new MappingElasticsearchConverter(new SimpleElasticsearchMappingContext());
		final var template = new ElasticsearchTemplate(mock(ElasticsearchClient.class, RETURNS_DEEP_STUBS), converter) {
			@Override
			public <T> T execute(final ClientCallback<T> callback) {
				throw failure;
			}
		};
		template.setObservationRegistry(registry);

		assertThatThrownBy(() -> operation.imperative().apply(template)).isSameAs(failure);

		assertObservation(operation);
		assertThat(registry.getCurrentObservation()).isNull();
	}

	@ParameterizedTest
	@MethodSource("operations")
	void reactiveFailuresAreLazyAndUseSubscriberParent(final Operation operation) {
		final var converter = new MappingElasticsearchConverter(new SimpleElasticsearchMappingContext());
		final var template = new ReactiveElasticsearchTemplate(
				mock(ReactiveElasticsearchClient.class, RETURNS_DEEP_STUBS), converter) {
			@Override
			public <T> Publisher<T> execute(final ClientCallback<Publisher<T>> callback) {
				assertThat(registry.getCurrentObservation()).isNull();
				return Mono.deferContextual(context -> {
					assertThat(context.hasKey(ObservationThreadLocalAccessor.KEY)).isTrue();
					return Mono.error(failure);
				});
			}
		};
		template.setObservationRegistry(registry);
		final var publisher = operation.reactive().apply(template);
		assertThat(stopped).isEmpty();
		final var parent = Observation.start("parent", registry);

		for (var subscription = 0; subscription < 2; subscription++) {
			Mono.from(publisher).contextWrite(context -> context.put(ObservationThreadLocalAccessor.KEY, parent))
					.as(StepVerifier::create).expectErrorMatches(error -> error == failure).verify();
		}
		parent.stop();

		assertThat(stopped).hasSize(2).allSatisfy(context -> {
			assertThat(context.getOperationName().getValue()).isEqualTo(operation.name());
			assertThat(context.getParentObservation()).isSameAs(parent);
			assertThat(context.getError()).isSameAs(failure);
		});
		assertThat(stopped.get(0)).isNotSameAs(stopped.get(1));
	}

	@ParameterizedTest
	@MethodSource("operations")
	void reactiveCancellationStopsObservation(final Operation operation) {
		final var converter = new MappingElasticsearchConverter(new SimpleElasticsearchMappingContext());
		final var template = new ReactiveElasticsearchTemplate(
				mock(ReactiveElasticsearchClient.class, RETURNS_DEEP_STUBS), converter) {
			@Override
			public <T> Publisher<T> execute(final ClientCallback<Publisher<T>> callback) {
				return Mono.never();
			}
		};
		template.setObservationRegistry(registry);

		Mono.from(operation.reactive().apply(template)).as(StepVerifier::create).thenCancel().verify();

		assertThat(stopped).singleElement().satisfies(context -> {
			assertThat(context.getOperationName().getValue()).isEqualTo(operation.name());
			assertThat(context.getError()).isNull();
		});
	}

	@Test
	void submissionEndsWhenTaskIdArrives() throws Exception {
		final var client = mock(ElasticsearchClient.class, RETURNS_DEEP_STUBS);
		when(client.reindex(any(co.elastic.clients.elasticsearch.core.ReindexRequest.class)))
				.thenReturn(co.elastic.clients.elasticsearch.core.ReindexResponse.of(builder -> builder.task("node:1")));
		final var template = new ElasticsearchTemplate(client,
				new MappingElasticsearchConverter(new SimpleElasticsearchMappingContext()));
		template.setObservationRegistry(registry);

		assertThat(template.submitReindex(REINDEX)).isEqualTo("node:1");

		assertThat(stopped).singleElement().satisfies(context -> assertThat(context.getError()).isNull());
		verify(client).reindex(argThat((co.elastic.clients.elasticsearch.core.ReindexRequest request) ->
				Boolean.FALSE.equals(request.waitForCompletion())));
	}

	@Test
	void reactiveSubmissionEndsWhenTaskIdArrives() {
		final var client = mock(ReactiveElasticsearchClient.class, RETURNS_DEEP_STUBS);
		when(client.reindex(any(co.elastic.clients.elasticsearch.core.ReindexRequest.class))).thenReturn(
				Mono.just(co.elastic.clients.elasticsearch.core.ReindexResponse.of(builder -> builder.task("node:1"))));
		final var template = new ReactiveElasticsearchTemplate(client,
				new MappingElasticsearchConverter(new SimpleElasticsearchMappingContext()));
		template.setObservationRegistry(registry);

		template.submitReindex(REINDEX).as(StepVerifier::create).expectNext("node:1").verifyComplete();

		assertThat(stopped).singleElement().satisfies(context -> assertThat(context.getError()).isNull());
		verify(client).reindex(argThat((co.elastic.clients.elasticsearch.core.ReindexRequest request) ->
				Boolean.FALSE.equals(request.waitForCompletion())));
	}

	private void assertObservation(final Operation operation) {
		assertThat(stopped).singleElement().satisfies(context -> {
			assertThat(context.getName()).isEqualTo("spring.data.elasticsearch.command");
			assertThat(context.getOperationName().getValue()).isEqualTo(operation.name());
			assertThat(context.getIndexName()).isEqualTo(operation.index());
			assertThat(context.getError()).isSameAs(failure);
			assertThat(context.getLowCardinalityKeyValue("spring.data.operation").getValue()).isEqualTo(operation.name());
			assertThat(context.getLowCardinalityKeyValue("spring.data.collection")).isNull();
		});
	}

	record Operation(String name, String index, Function<ElasticsearchTemplate, Object> imperative,
			Function<ReactiveElasticsearchTemplate, Publisher<?>> reactive) {}
}
