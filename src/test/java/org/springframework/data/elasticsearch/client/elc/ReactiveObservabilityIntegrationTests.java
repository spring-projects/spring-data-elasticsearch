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
import static org.springframework.data.elasticsearch.core.IndexOperationsAdapter.*;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.tck.TestObservationRegistry;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.SqlQuery;
import org.springframework.data.elasticsearch.junit.jupiter.ReactiveElasticsearchTemplateConfiguration;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.test.context.ContextConfiguration;

/**
 * Integration tests verifying that Micrometer observations are recorded for the reactive template operations.
 *
 * @since 6.1
 */
@SpringIntegrationTest
@ContextConfiguration(classes = { ReactiveObservabilityIntegrationTests.Config.class })
@DisplayName("Reactive Observability Integration Tests")
class ReactiveObservabilityIntegrationTests {

	private static final List<Observation.Context> STOPPED = new CopyOnWriteArrayList<>();

	@Configuration
	@Import({ ReactiveElasticsearchTemplateConfiguration.class })
	static class Config {

		@Bean
		IndexNameProvider indexNameProvider() {
			return new IndexNameProvider("reactive-observability-it");
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

	@Autowired private ReactiveElasticsearchOperations operations;

	@Autowired private IndexNameProvider indexNameProvider;
	@Autowired private TestObservationRegistry observationRegistry;

	@BeforeEach
	void setUp() {
		indexNameProvider.increment();
		blocking(operations.indexOps(SampleEntity.class)).createWithMapping();
		observationRegistry.clear();
		STOPPED.clear();
	}

	@AfterEach
	void tearDown() {
		blocking(operations.indexOps(SampleEntity.class)).delete();
	}

	@Test
	@DisplayName("should record bulk observation with batch size for saveAll")
	void shouldRecordBatchSizeForSaveAll() {

		operations.saveAll(List.of(sample("1"), sample("2"), sample("3")), SampleEntity.class).blockLast();

		assertThat(awaitOperations("bulk")).singleElement().satisfies(ctx -> {
						assertThat(ctx.getHighCardinalityKeyValue("spring.data.batch.size").getValue()).isEqualTo("3");
		});
	}

	@Test
	@DisplayName("should record search observation for searchForPage")
	void shouldRecordObservationForSearchForPage() {

		saveAndRefresh();

		operations.searchForPage(operations.matchAllQuery(), SampleEntity.class, SampleEntity.class)
				.as(StepVerifier::create).expectNextCount(1).verifyComplete();

		assertOperation("search");
	}

	@Test
	@DisplayName("should record search observation for searchForHits")
	void shouldRecordObservationForSearchForHits() {

		saveAndRefresh();

		operations.searchForHits(operations.matchAllQuery(), SampleEntity.class, SampleEntity.class)
				.as(StepVerifier::create).expectNextCount(1).verifyComplete();

		assertOperation("search");
	}

	@Test
	@DisplayName("should record search observation for aggregate")
	void shouldRecordObservationForAggregate() {

		saveAndRefresh();

		operations.aggregate(operations.matchAllQuery(), SampleEntity.class).as(StepVerifier::create).verifyComplete();

		assertOperation("search");
	}

	@Test
	@DisplayName("should record observation for SQL search")
	void shouldRecordObservationForSqlSearch() {

		saveAndRefresh();

		operations.search(SqlQuery.builder("SELECT * FROM \"" + indexNameProvider.indexName() + "\"").build())
				.as(StepVerifier::create).expectNextCount(1).verifyComplete();

		assertOperation("sql");
	}

	private void saveAndRefresh() {

		operations.save(sample("1")).block();
		blocking(operations.indexOps(SampleEntity.class)).refresh();
		observationRegistry.clear();
		STOPPED.clear();
	}

	private void assertOperation(String operation) {
		assertThat(awaitOperations(operation)).isNotEmpty();
	}

	// the observation is stopped in doFinally, which may run after the subscriber has been completed
	private List<Observation.Context> awaitOperations(String operation) {

		var deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
		var result = operations(operation);
		while (result.isEmpty() && System.nanoTime() < deadline) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
			result = operations(operation);
		}
		return result;
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
