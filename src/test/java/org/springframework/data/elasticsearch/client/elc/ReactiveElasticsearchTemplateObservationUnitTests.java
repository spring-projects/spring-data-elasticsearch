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

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import io.micrometer.observation.tck.TestObservationRegistry;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.context.ContextView;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;

/**
 * Tests the propagation of the parent observation in {@link ReactiveElasticsearchTemplate}.
 */
class ReactiveElasticsearchTemplateObservationUnitTests {

	private static final IndexCoordinates INDEX = IndexCoordinates.of("test-index");

	private final TestObservationRegistry registry = TestObservationRegistry.create();
	private final List<Observation.Context> started = new ArrayList<>();
	private ReactiveElasticsearchTemplate template;
	private volatile ContextView downstreamContext;

	@BeforeEach
	void setUp() {
		template = new ReactiveElasticsearchTemplate(mock(ReactiveElasticsearchClient.class, RETURNS_DEEP_STUBS),
				new MappingElasticsearchConverter(new SimpleElasticsearchMappingContext())) {
			@Override
			protected Mono<Boolean> doExists(String id, IndexCoordinates index) {
				return Mono.deferContextual(ctx -> {
					downstreamContext = ctx;
					return Mono.just(true);
				});
			}
		};
		registry.observationConfig().observationHandler(new ObservationHandler<>() {
			@Override
			public boolean supportsContext(Observation.Context context) {
				return true;
			}

			@Override
			public void onStart(Observation.Context context) {
				started.add(context);
			}
		});
		template.setObservationRegistry(registry);
	}

	@Test
	@DisplayName("should use the observation from the Reactor context as parent")
	void shouldUseContextObservationAsParent() {

		var parent = Observation.createNotStarted("parent", registry).start();

		template.exists("1", INDEX).contextWrite(ctx -> ctx.put(ObservationThreadLocalAccessor.KEY, parent))
				.as(StepVerifier::create).expectNext(true).verifyComplete();
		parent.stop();

		var child = started.stream().filter(c -> c.getName().equals("spring.data.elasticsearch.command")).findFirst()
				.orElseThrow();
		assertThat(child.getParentObservation()).isSameAs(parent);
	}

	@Test
	@DisplayName("should be a root observation without a parent in the context")
	void shouldBeRootWithoutParent() {

		template.exists("1", INDEX).as(StepVerifier::create).expectNext(true).verifyComplete();

		assertThat(started).hasSize(1);
		assertThat(started.get(0).getParentObservation()).isNull();
	}

	@Test
	@DisplayName("should publish the observation under ObservationThreadLocalAccessor.KEY")
	void shouldWriteObservationUnderAccessorKey() {

		template.exists("1", INDEX).as(StepVerifier::create).expectNext(true).verifyComplete();

		assertThat(started).hasSize(1);
		assertThat(downstreamContext.hasKey(ObservationThreadLocalAccessor.KEY)).isTrue();
		assertThat(downstreamContext.hasKey(Observation.class)).isFalse();
	}

	@Test
	@DisplayName("sibling operations should share the callers observation as parent")
	void siblingsShouldShareCallerParent() {

		var parent = Observation.createNotStarted("parent", registry).start();

		template.exists("1", INDEX).then(template.exists("2", INDEX))
				.contextWrite(ctx -> ctx.put(ObservationThreadLocalAccessor.KEY, parent)).as(StepVerifier::create)
				.expectNext(true).verifyComplete();
		parent.stop();

		var commands = started.stream().filter(c -> c.getName().equals("spring.data.elasticsearch.command")).toList();
		assertThat(commands).hasSize(2);
		assertThat(commands).allSatisfy(c -> assertThat(c.getParentObservation()).isSameAs(parent));
	}
}
