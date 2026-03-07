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

import io.micrometer.observation.tck.TestObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistryAssert;

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
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.junit.jupiter.ElasticsearchTemplateConfiguration;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.test.context.ContextConfiguration;

/**
 * Integration tests verifying that Micrometer observations are recorded for Spring Data Elasticsearch template
 * operations when a {@link TestObservationRegistry} is wired into the application context.
 *
 * @author maryantocinn
 * @since 6.1
 */
@SpringIntegrationTest
@ContextConfiguration(classes = { ObservabilityIntegrationTests.Config.class })
@DisplayName("Observability Integration Tests")
class ObservabilityIntegrationTests {

	@Configuration
	@Import({ ElasticsearchTemplateConfiguration.class })
	static class Config {

		@Bean
		IndexNameProvider indexNameProvider() {
			return new IndexNameProvider("observability-it");
		}

		@Bean
		TestObservationRegistry observationRegistry() {
			return TestObservationRegistry.create();
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
				.hasLowCardinalityKeyValue("spring.data.collection", indexNameProvider.indexName());
	}

	@Document(indexName = "#{@indexNameProvider.indexName()}")
	static class SampleEntity {
		@Nullable @Id private String id;
		@Nullable @Field(type = FieldType.Text) private String message;

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
