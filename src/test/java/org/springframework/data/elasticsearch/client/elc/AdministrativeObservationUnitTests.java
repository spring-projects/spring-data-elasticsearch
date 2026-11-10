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
import co.elastic.clients.elasticsearch.indices.DeleteIndexResponse;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import io.micrometer.common.KeyValues;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import io.micrometer.observation.tck.TestObservationRegistry;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.reactivestreams.Publisher;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.ReactiveIndexOperations;
import org.springframework.data.elasticsearch.core.RefreshPolicy;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.index.*;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;

class AdministrativeObservationUnitTests {

	private static final IndexCoordinates INDEX = IndexCoordinates.of("test-index");
	private static final IndexCoordinates OTHER = IndexCoordinates.of("other-index");
	private static final PutTemplateRequest LEGACY = PutTemplateRequest.builder("template", "test-*").build();
	private static final PutIndexTemplateRequest TEMPLATE = PutIndexTemplateRequest.builder().withName("template")
			.withIndexPatterns("test-*").build();
	private static final PutComponentTemplateRequest COMPONENT = PutComponentTemplateRequest.builder().withName("template")
			.withTemplateData(ComponentTemplateRequestData.builder().build()).build();

	private final RuntimeException failure = new IllegalStateException("request failed");
	private final ElasticsearchTransport transport = mock(ElasticsearchTransport.class);
	private final TestObservationRegistry registry = TestObservationRegistry.create();
	private final List<ElasticsearchObservationContext> stopped = new ArrayList<>();
	private ElasticsearchTemplate template;
	private ReactiveElasticsearchTemplate reactiveTemplate;

	@BeforeEach
	void setUp() throws Exception {
		when(transport.jsonpMapper()).thenReturn(new JacksonJsonpMapper());
		doThrow(failure).when(transport).performRequest(any(), any(), any());
		doReturn(CompletableFuture.failedFuture(failure)).when(transport).performRequestAsync(any(), any(), any());
		final var converter = new MappingElasticsearchConverter(new SimpleElasticsearchMappingContext());
		template = new ElasticsearchTemplate(new ElasticsearchClient(transport), converter);
		reactiveTemplate = new ReactiveElasticsearchTemplate(new ReactiveElasticsearchClient(transport), converter);
		template.setObservationRegistry(registry);
		reactiveTemplate.setObservationRegistry(registry);
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
				new Operation("indexCreate", t -> t.create(), t -> t.create()),
				new Operation("indexCreate", t -> t.create(Map.of()), t -> t.create(Map.of())),
				new Operation("indexCreate", t -> t.create(Map.of(), Document.create()),
						t -> t.create(Map.of(), Document.create())),
				new Operation("indexDelete", t -> t.delete(), t -> t.delete()),
				new Operation("indexExists", t -> t.exists(), t -> t.exists()),
				new Operation("indexRefresh", t -> { t.refresh(); return null; }, t -> t.refresh()),
				new Operation("putMapping", t -> t.putMapping(Document.create()),
						t -> t.putMapping(reactor.core.publisher.Mono.just(Document.create()))),
				new Operation("getMapping", t -> t.getMapping(), t -> t.getMapping()),
				new Operation("getSettings", t -> t.getSettings(), t -> t.getSettings()),
				new Operation("getSettings", t -> t.getSettings(true), t -> t.getSettings(true)),
				new Operation("alias", t -> t.alias(new AliasActions()), t -> t.alias(new AliasActions())),
				new Operation("getAliases", t -> t.getAliases("alias"), t -> t.getAliases("alias")),
				new Operation("getAliases", t -> t.getAliasesForIndex("other-index"), t -> t.getAliasesForIndex("other-index")),
				new Operation("getAliases", t -> t.getAliasesForIndex(), t -> t.getAliasesForIndex()),
				new Operation("putTemplate", t -> t.putTemplate(LEGACY), t -> t.putTemplate(LEGACY)),
				new Operation("getTemplate", t -> t.getTemplate("template"), t -> t.getTemplate("template")),
				new Operation("existsTemplate", t -> t.existsTemplate("template"), t -> t.existsTemplate("template")),
				new Operation("deleteTemplate", t -> t.deleteTemplate("template"), t -> t.deleteTemplate("template")),
				new Operation("putIndexTemplate", t -> t.putIndexTemplate(TEMPLATE), t -> t.putIndexTemplate(TEMPLATE)),
				new Operation("getIndexTemplate", t -> t.getIndexTemplate("template"), t -> t.getIndexTemplate("template")),
				new Operation("existsIndexTemplate", t -> t.existsIndexTemplate("template"), t -> t.existsIndexTemplate("template")),
				new Operation("deleteIndexTemplate", t -> t.deleteIndexTemplate("template"), t -> t.deleteIndexTemplate("template")),
				new Operation("putComponentTemplate", t -> t.putComponentTemplate(COMPONENT), t -> t.putComponentTemplate(COMPONENT)),
				new Operation("getComponentTemplate", t -> t.getComponentTemplate(new GetComponentTemplateRequest("template")),
						t -> t.getComponentTemplate(new GetComponentTemplateRequest("template"))),
				new Operation("existsComponentTemplate", t -> t.existsComponentTemplate(new ExistsComponentTemplateRequest("template")),
						t -> t.existsComponentTemplate(new ExistsComponentTemplateRequest("template"))),
				new Operation("deleteComponentTemplate", t -> t.deleteComponentTemplate(new DeleteComponentTemplateRequest("template")),
						t -> t.deleteComponentTemplate(new DeleteComponentTemplateRequest("template"))),
				new Operation("getIndexInformation", t -> t.getInformation(), t -> t.getInformation()),
				new Operation("getIndexInformation", t -> t.getInformation(OTHER), t -> t.getInformation(OTHER)));
	}

	@ParameterizedTest
	@MethodSource("operations")
	void imperativeRecordsOneObservation(final Operation operation) {
		assertThatThrownBy(() -> operation.imperative().apply(template.indexOps(INDEX))).isSameAs(failure);
		assertStopped(operation.name(), 1);
	}

	@ParameterizedTest
	@MethodSource("operations")
	void reactiveRecordsOneObservationPerSubscription(final Operation operation) {
		final var publisher = operation.reactive().apply(reactiveTemplate.indexOps(INDEX));
		assertThat(stopped).isEmpty();
		final var parent = Observation.start("parent", registry);

		for (var subscription = 0; subscription < 2; subscription++) {
			Flux.from(publisher).contextWrite(context -> context.put(ObservationThreadLocalAccessor.KEY, parent))
					.as(StepVerifier::create).expectErrorMatches(error -> error == failure).verify();
		}
		parent.stop();

		assertStopped(operation.name(), 2);
		assertThat(stopped).allSatisfy(context -> assertThat(context.getParentObservation()).isSameAs(parent));
		assertThat(stopped.get(0)).isNotSameAs(stopped.get(1));
	}

	@ParameterizedTest
	@MethodSource("operations")
	void reactiveCancellationStopsObservation(final Operation operation) {
		doAnswer(invocation -> new CompletableFuture<>()).when(transport).performRequestAsync(any(), any(), any());

		Flux.from(operation.reactive().apply(reactiveTemplate.indexOps(INDEX)))
				.as(StepVerifier::create).thenCancel().verify();

		assertThat(stopped).singleElement().satisfies(context -> {
			assertThat(context.getOperationName().getValue()).isEqualTo(operation.name());
			assertThat(context.getError()).isNull();
		});
	}

	@Test
	void deleteIncludesExistenceCheckWithoutSecondObservation() throws Exception {
		doReturn(new BooleanResponse(true), DeleteIndexResponse.of(builder -> builder.acknowledged(true)))
				.when(transport).performRequest(any(), any(), any());
		doReturn(CompletableFuture.completedFuture(new BooleanResponse(true)),
				CompletableFuture.completedFuture(DeleteIndexResponse.of(builder -> builder.acknowledged(true))))
				.when(transport).performRequestAsync(any(), any(), any());

		assertThat(template.indexOps(INDEX).delete()).isTrue();
		reactiveTemplate.indexOps(INDEX).delete().as(StepVerifier::create).expectNext(true).verifyComplete();

		assertThat(stopped).hasSize(2).allSatisfy(context -> {
			assertThat(context.getOperationName()).isEqualTo(ElasticsearchOperationName.INDEX_DELETE);
			assertThat(context.getError()).isNull();
		});
		verify(transport, times(2)).performRequest(any(), any(), any());
		verify(transport, times(2)).performRequestAsync(any(), any(), any());
	}

	@Test
	void clusterHealthUsesParentConfiguration() {
		assertThatThrownBy(() -> template.cluster().health()).isSameAs(failure);
		reactiveTemplate.cluster().health().as(StepVerifier::create)
				.expectErrorMatches(error -> error == failure).verify();
		assertStopped("clusterHealth", 2);
		assertThat(stopped).allSatisfy(context -> assertThat(context.getIndexName()).isNull());
	}

	@Test
	void clusterVersionIsObserved() {
		assertThatThrownBy(() -> template.getClusterVersion()).isSameAs(failure);
		reactiveTemplate.getClusterVersion().as(StepVerifier::create)
				.expectErrorMatches(error -> error == failure).verify();
		assertStopped("clusterInfo", 2);
	}

	@Test
	void existingChildrenUseUpdatedRegistryAndConvention() {
		template.setObservationRegistry(io.micrometer.observation.ObservationRegistry.NOOP);
		reactiveTemplate.setObservationRegistry(io.micrometer.observation.ObservationRegistry.NOOP);
		final var imperative = template.indexOps(INDEX);
		final var reactive = reactiveTemplate.indexOps(INDEX);
		final var custom = new DefaultElasticsearchObservationConvention() {
			@Override
			public KeyValues getLowCardinalityKeyValues(final ElasticsearchObservationContext context) {
				return super.getLowCardinalityKeyValues(context).and("custom", "value");
			}
		};
		template.setObservationConvention(custom);
		reactiveTemplate.setObservationConvention(custom);
		template.setObservationRegistry(registry);
		reactiveTemplate.setObservationRegistry(registry);

		assertThatThrownBy(imperative::exists).isSameAs(failure);
		reactive.exists().as(StepVerifier::create).expectErrorMatches(error -> error == failure).verify();

		assertThat(stopped).hasSize(2).allSatisfy(context -> {
			assertThat(context.getLowCardinalityKeyValue("custom").getValue()).isEqualTo("value");
			assertThat(context.getHighCardinalityKeyValue("spring.data.collection").getValue()).isEqualTo("test-index");
		});
	}

	@Test
	void copiesPropagateConventionToClassBoundIndexAndClusterChildren() {
		final var custom = new DefaultElasticsearchObservationConvention() {
			@Override
			public String getName() {
				return "custom.command";
			}
		};
		template.setObservationConvention(custom);
		reactiveTemplate.setObservationConvention(custom);
		final var imperativeCopy = template.withRefreshPolicy(RefreshPolicy.IMMEDIATE);
		final var reactiveCopy = reactiveTemplate.withRefreshPolicy(RefreshPolicy.IMMEDIATE);

		assertThatThrownBy(() -> imperativeCopy.indexOps(Entity.class).exists()).isSameAs(failure);
		reactiveCopy.indexOps(Entity.class).exists().as(StepVerifier::create)
				.expectErrorMatches(error -> error == failure).verify();
		assertThatThrownBy(() -> imperativeCopy.cluster().health()).isSameAs(failure);
		reactiveCopy.cluster().health().as(StepVerifier::create)
				.expectErrorMatches(error -> error == failure).verify();

		assertThat(stopped).hasSize(4).allSatisfy(context -> assertThat(context.getName()).isEqualTo("custom.command"));
	}

	@Test
	void classBoundCreateAndMappingOverloadsAreObservedOnce() {
		final var imperative = template.indexOps(Entity.class);
		final var reactive = reactiveTemplate.indexOps(Entity.class);

		assertThatThrownBy(imperative::createWithMapping).isSameAs(failure);
		assertThatThrownBy(imperative::putMapping).isSameAs(failure);
		assertThatThrownBy(() -> imperative.putMapping(Entity.class)).isSameAs(failure);
		reactive.createWithMapping().as(StepVerifier::create).expectErrorMatches(error -> error == failure).verify();
		reactive.putMapping().as(StepVerifier::create).expectErrorMatches(error -> error == failure).verify();
		reactive.putMapping(Entity.class).as(StepVerifier::create).expectErrorMatches(error -> error == failure).verify();

		assertThat(stopped).extracting(context -> context.getOperationName().getValue())
				.containsExactly("indexCreate", "putMapping", "putMapping", "indexCreate", "putMapping", "putMapping");
	}

	@Test
	void explicitIndicesOverrideBoundIndexInObservations() {
		assertThatThrownBy(() -> template.indexOps(INDEX).getInformation(OTHER)).isSameAs(failure);
		assertThatThrownBy(() -> template.indexOps(INDEX).getAliasesForIndex("other-index")).isSameAs(failure);
		reactiveTemplate.indexOps(INDEX).getInformation(OTHER).as(StepVerifier::create)
				.expectErrorMatches(error -> error == failure).verify();
		reactiveTemplate.indexOps(INDEX).getAliasesForIndex("other-index").as(StepVerifier::create)
				.expectErrorMatches(error -> error == failure).verify();

		assertThat(stopped).hasSize(4).allSatisfy(context -> assertThat(context.getIndexName()).isEqualTo("other-index"));
	}

	@Test
	void localMethodsDoNotProduceObservations() {
		final var imperative = template.indexOps(Entity.class);
		final var reactive = reactiveTemplate.indexOps(Entity.class);

		imperative.createMapping();
		imperative.createSettings();
		reactive.createMapping().as(StepVerifier::create).expectNextCount(1).verifyComplete();
		reactive.createSettings().as(StepVerifier::create).expectNextCount(1).verifyComplete();

		assertThat(stopped).isEmpty();
	}

	@Test
	void mappingPublisherFailuresAreObserved() {
		reactiveTemplate.indexOps(INDEX).putMapping(Mono.error(failure)).as(StepVerifier::create)
				.expectErrorMatches(error -> error == failure).verify();
		assertStopped("putMapping", 1);
	}

	@Test
	void unboundedSearchStartsBeforePointInTimeChildren() {
		final var started = new ArrayList<ElasticsearchObservationContext>();
		registry.observationConfig().observationHandler(new ObservationHandler<ElasticsearchObservationContext>() {
			@Override
			public boolean supportsContext(final Observation.Context context) {
				return context instanceof ElasticsearchObservationContext;
			}

			@Override
			public void onStart(final ElasticsearchObservationContext context) {
				started.add(context);
			}
		});
		final var response = co.elastic.clients.elasticsearch.core.SearchResponse.<EntityAsMap>of(builder -> builder
				.took(1).timedOut(false).shards(shards -> shards.total(1).successful(1).failed(0))
				.hits(hits -> hits.hits(List.of())));
		doReturn(
				CompletableFuture.completedFuture(co.elastic.clients.elasticsearch.core.OpenPointInTimeResponse
						.of(builder -> builder.id("pit").shards(shards -> shards.total(1).successful(1).failed(0)))),
				CompletableFuture.completedFuture(response),
				CompletableFuture.completedFuture(co.elastic.clients.elasticsearch.core.ClosePointInTimeResponse
						.of(builder -> builder.succeeded(true).numFreed(1))))
				.when(transport).performRequestAsync(any(), any(), any());

		reactiveTemplate.search(NativeQuery.builder().withPageable(org.springframework.data.domain.Pageable.unpaged()).build(),
				Entity.class, INDEX).as(StepVerifier::create).verifyComplete();

		assertThat(started).extracting(context -> context.getOperationName().getValue())
				.containsExactly("search", "openPointInTime", "closePointInTime");
		assertThat(started.subList(1, 3)).allSatisfy(context ->
				assertThat(context.getParentObservation().getContextView()).isSameAs(started.get(0)));
		assertThat(stopped).hasSize(3);
	}

	private void assertStopped(final String operation, final int count) {
		assertThat(stopped).hasSize(count).allSatisfy(context -> {
			assertThat(context.getName()).isEqualTo("spring.data.elasticsearch.command");
			assertThat(context.getOperationName().getValue()).isEqualTo(operation);
			assertThat(context.getLowCardinalityKeyValue("spring.data.operation").getValue()).isEqualTo(operation);
			assertThat(context.getLowCardinalityKeyValue("spring.data.collection")).isNull();
			assertThat(context.getError()).isSameAs(failure);
		});
	}

	record Operation(String name, Function<IndexOperations, Object> imperative,
			Function<ReactiveIndexOperations, Publisher<?>> reactive) {}

	@org.springframework.data.elasticsearch.annotations.Document(indexName = "entity-index")
	record Entity(@org.springframework.data.annotation.Id String id) {}
}
