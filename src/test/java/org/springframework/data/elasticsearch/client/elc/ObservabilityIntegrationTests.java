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

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.tck.TestObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistryAssert;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.bulk.BulkIngesterListener;
import org.springframework.data.elasticsearch.core.bulk.BulkIngesterOptions;
import org.springframework.data.elasticsearch.core.bulk.BulkOperationResult;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SqlQuery;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.data.elasticsearch.junit.jupiter.ElasticsearchTemplateConfiguration;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.test.context.ContextConfiguration;

/**
 * Integration tests verifying that Micrometer observations are recorded for Spring Data Elasticsearch template
 * operations when a {@link TestObservationRegistry} is wired into the application context.
 *
 * @author maryantocinn
 * @author Ralph Ursprung
 * @since 6.1
 */
@SpringIntegrationTest
@ContextConfiguration(classes = { ObservabilityIntegrationTests.Config.class })
@DisplayName("Observability Integration Tests")
class ObservabilityIntegrationTests {

	private static final List<Observation.Context> STOPPED = new CopyOnWriteArrayList<>();

	@Configuration
	@Import({ ElasticsearchTemplateConfiguration.class })
	static class Config {

		@Bean
		IndexNameProvider indexNameProvider() {
			return new IndexNameProvider("observability-it");
		}

		@Bean
		TestObservationRegistry observationRegistry() {
			var registry = TestObservationRegistry.create();
			registry.observationConfig().observationHandler(new ObservationHandler<Observation.Context>() {
				@Override
				public boolean supportsContext(Observation.Context context) {
					return true;
				}

				@Override
				public void onStop(Observation.Context context) {
					STOPPED.add(context);
				}
			});
			return registry;
		}
	}

	@Autowired private ElasticsearchOperations operations;

	@Autowired private IndexNameProvider indexNameProvider;
	@Autowired private TestObservationRegistry observationRegistry;

	@BeforeEach
	void setUp() {
		indexNameProvider.increment();
		operations.indexOps(SampleEntity.class).createWithMapping();
		observationRegistry.clear();
		STOPPED.clear();
	}

	@Test
	@Order(Integer.MAX_VALUE)
	void cleanup() {
		operations.indexOps(IndexCoordinates.of(indexNameProvider.getPrefix() + "*")).delete();
	}

	@Test
	@DisplayName("should record observation for save operation")
	void shouldRecordObservationForSave() {

		SampleEntity entity = new SampleEntity();
		entity.setId("1");
		entity.setMessage("hello");

		operations.save(entity);

		TestObservationRegistryAssert.assertThat(observationRegistry)
				.hasObservationWithNameEqualTo("spring.data.elasticsearch.command")
				.that()
				.hasLowCardinalityKeyValue("spring.data.operation", "save")
				.hasBeenStopped();
	}

	@Test
	@DisplayName("should record observation for search operation")
	void shouldRecordObservationForSearch() {

		SampleEntity entity = new SampleEntity();
		entity.setId("1");
		entity.setMessage("hello");
		operations.save(entity);
		observationRegistry.clear();
		STOPPED.clear();

		SearchHits<SampleEntity> hits = operations.search(operations.matchAllQuery(), SampleEntity.class);

		assertThat(hits.getTotalHits()).isGreaterThanOrEqualTo(1);
		TestObservationRegistryAssert.assertThat(observationRegistry)
				.hasObservationWithNameEqualTo("spring.data.elasticsearch.command")
				.that()
				.hasLowCardinalityKeyValue("spring.data.operation", "search")
				.hasBeenStopped();
	}

	@Test
	@DisplayName("should record observation with collection name")
	void shouldRecordObservationWithCollectionName() {

		SampleEntity entity = new SampleEntity();
		entity.setId("1");
		entity.setMessage("hello");

		operations.save(entity);

		TestObservationRegistryAssert.assertThat(observationRegistry)
				.hasObservationWithNameEqualTo("spring.data.elasticsearch.command")
				.that()
				.doesNotHaveLowCardinalityKeyValue("spring.data.collection", indexNameProvider.indexName())
				.hasHighCardinalityKeyValue("spring.data.collection", indexNameProvider.indexName());
	}

	@Test
	@DisplayName("should record bulk observation with batch size for bulkUpdate")
	void shouldRecordObservationForBulkUpdate() {

		operations.save(sample("1"), sample("2"));
		observationRegistry.clear();
		STOPPED.clear();

		var queries = List.of(
				UpdateQuery.builder("1").withDocument(org.springframework.data.elasticsearch.core.document.Document
						.from(Map.of("message", "u1"))).build(),
				UpdateQuery.builder("2").withDocument(org.springframework.data.elasticsearch.core.document.Document
						.from(Map.of("message", "u2"))).build());
		operations.bulkUpdate(queries, IndexCoordinates.of(indexNameProvider.indexName()));

		assertOperation("bulk");
		assertThat(operations("bulk")).singleElement().satisfies(ctx -> {
			assertThat(ctx.getLowCardinalityKeyValue("spring.data.collection").getValue())
					.isEqualTo(indexNameProvider.indexName());
			assertThat(ctx.getHighCardinalityKeyValue("spring.data.batch.size").getValue()).isEqualTo("2");
		});
	}

	@Test
	@DisplayName("should record observation for multiSearch")
	void shouldRecordObservationForMultiSearch() {

		operations.save(sample("1"));
		observationRegistry.clear();
		STOPPED.clear();

		var result = operations.multiSearch(List.of(operations.matchAllQuery(), operations.matchAllQuery()),
				SampleEntity.class, IndexCoordinates.of(indexNameProvider.indexName()));

		assertThat(result).hasSize(2);
		assertOperation("multiSearch");
		assertThat(operations("multiSearch")).singleElement().satisfies(ctx -> assertThat(
				ctx.getLowCardinalityKeyValue("spring.data.collection").getValue())
				.isEqualTo(indexNameProvider.indexName()));
	}

	@Test
	@DisplayName("should record observations for scroll operations of searchForStream")
	void shouldRecordObservationsForSearchForStream() {

		operations.save(sample("1"), sample("2"));
		operations.indexOps(SampleEntity.class).refresh();
		observationRegistry.clear();
		STOPPED.clear();

		try (var stream = operations.searchForStream(operations.matchAllQuery(), SampleEntity.class)) {
			assertThat(stream.stream().count()).isEqualTo(2);
		}

		assertOperation("scrollStart");
		assertOperation("scrollContinue");
		assertOperation("scrollClear");
	}

	@Test
	@DisplayName("should record observation for SQL search")
	void shouldRecordObservationForSqlSearch() {

		operations.save(sample("1"));
		operations.indexOps(SampleEntity.class).refresh();
		observationRegistry.clear();
		STOPPED.clear();

		var response = operations.search(
				SqlQuery.builder("SELECT * FROM \"" + indexNameProvider.indexName() + "\"").build());

		assertThat(response.getRows()).hasSize(1);
		assertOperation("sql");
	}

	@Test // #2071
	@DisplayName("should record one bulk observation per bulk ingester request")
	void shouldRecordOneBulkObservationPerBulkIngesterRequest() {

		final var options = BulkIngesterOptions.builder().withMaxOperations(2).build();
		try (final var ingester = operations.bulkIngester(options, SampleEntity.class, new NoOpListener())) {
			for (var i = 1; i <= 3; i++) {
				final var entity = new SampleEntity();
				entity.setId(String.valueOf(i));
				entity.setMessage("hello");
				ingester.add(new IndexQueryBuilder().withId(entity.getId()).withObject(entity).build());
			}
		}

		TestObservationRegistryAssert.assertThat(observationRegistry)
				.hasNumberOfObservationsWithNameEqualTo("spring.data.elasticsearch.command", 2)
				.forAllObservationsWithNameEqualTo("spring.data.elasticsearch.command", observation -> observation
						.hasLowCardinalityKeyValue("spring.data.operation", "bulk")
						.hasHighCardinalityKeyValue("spring.data.collection", indexNameProvider.indexName()))
				.hasAnObservationWithAKeyValue("spring.data.batch.size", "2")
				.hasAnObservationWithAKeyValue("spring.data.batch.size", "1")
				.hasObservationWithNameEqualTo("spring.data.elasticsearch.command")
				.that()
				.hasBeenStopped();
	}

	static class NoOpListener implements BulkIngesterListener<Object> {

		@Override
		public void afterBulk(final long executionId, final List<BulkOperationResult<Object>> results) {}

		@Override
		public void afterBulk(final long executionId, final List<@Nullable Object> contexts,
				final RuntimeException failure) {}

	}

	private void assertOperation(String operation) {
		assertThat(operations(operation)).isNotEmpty();
	}

	private List<Observation.Context> operations(String operation) {
		return STOPPED.stream().filter(context -> {
			var keyValue = context.getLowCardinalityKeyValue("spring.data.operation");
			return keyValue != null && operation.equals(keyValue.getValue());
		}).toList();
	}

	private SampleEntity sample(String id) {

		var entity = new SampleEntity();
		entity.setId(id);
		entity.setMessage("hello");
		return entity;
	}

	@Document(indexName = "#{@indexNameProvider.indexName()}")
	static class SampleEntity {
		@Nullable
		@Id private String id;
		@Nullable
		@Field(type = FieldType.Text) private String message;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getMessage() {
			return message;
		}

		public void setMessage(@Nullable String message) {
			this.message = message;
		}
	}
}
