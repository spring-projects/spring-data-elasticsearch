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
package org.springframework.data.elasticsearch.repository.query.keywords;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.elasticsearch.annotations.FieldType.*;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.data.elasticsearch.repository.ReactiveElasticsearchRepository;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.lang.Nullable;

/**
 * @author Peter-Josef Meisch
 */
@SpringIntegrationTest
public abstract class ReactiveQueryKeywordsIntegrationTests {

	@Autowired private IndexNameProvider indexNameProvider;
	@Autowired private ReactiveElasticsearchOperations operations;
	@Autowired private SampleRepository repository;

	// region setup
	@BeforeEach
	void setUp() {
		indexNameProvider.increment();
		operations.indexOps(SampleEntity.class).createWithMapping().block();
	}

	@Test
	@Order(java.lang.Integer.MAX_VALUE)
	void cleanup() {
		operations.indexOps(IndexCoordinates.of(indexNameProvider.getPrefix() + "*")).delete().block();
	}
	// endregion

	@Test // #1909
	@DisplayName("should find by property exists")
	void shouldFindByPropertyExists() {

		loadEntities();
		repository.findByMessageExists().mapNotNull(SearchHit::getId).collectList() //
				.as(StepVerifier::create) //
				.assertNext(ids -> { //
					assertThat(ids).containsExactlyInAnyOrder("empty-message", "with-message"); //
				}).verifyComplete();
	}

	@Test // #1909
	@DisplayName("should find by property is not null")
	void shouldFindByPropertyIsNotNull() {

		loadEntities();
		repository.findByMessageIsNotNull().mapNotNull(SearchHit::getId).collectList() //
				.as(StepVerifier::create) //
				.assertNext(ids -> { //
					assertThat(ids).containsExactlyInAnyOrder("empty-message", "with-message"); //
				}).verifyComplete();
	}

	@Test // #1909
	@DisplayName("should find by property is null")
	void shouldFindByPropertyIsNull() {

		loadEntities();
		repository.findByMessageIsNull().mapNotNull(SearchHit::getId).collectList() //
				.as(StepVerifier::create) //
				.assertNext(ids -> { //
					assertThat(ids).containsExactlyInAnyOrder("null-message"); //
				}).verifyComplete();
	}

	@Test // #1909
	@DisplayName("should find by empty property ")
	void shouldFindByEmptyProperty() {

		loadEntities();
		repository.findByMessageIsEmpty().mapNotNull(SearchHit::getId).collectList() //
				.as(StepVerifier::create) //
				.assertNext(ids -> { //
					assertThat(ids).containsExactlyInAnyOrder("empty-message"); //
				}).verifyComplete();
	}

	@Test // #1909
	@DisplayName("should find by not empty property ")
	void shouldFindByNotEmptyProperty() {

		loadEntities();
		repository.findByMessageIsNotEmpty().mapNotNull(SearchHit::getId).collectList() //
				.as(StepVerifier::create) //
				.assertNext(ids -> { //
					assertThat(ids).containsExactlyInAnyOrder("with-message"); //
				}).verifyComplete();
	}

	@Test // #2162
	@DisplayName("should run exists query")
	void shouldRunExistsQuery() {

		loadEntities();
		repository.existsByMessage("message") //
				.as(StepVerifier::create) //
				.expectNext(true) //
				.verifyComplete();
		repository.existsByMessage("without") //
				.as(StepVerifier::create) //
				.expectNext(false) //
				.verifyComplete();
	}

	interface SampleRepository extends ReactiveElasticsearchRepository<SampleEntity, String> {
		Flux<SearchHit<SampleEntity>> findByMessageExists();

		Flux<SearchHit<SampleEntity>> findByMessageIsNotNull();

		Flux<SearchHit<SampleEntity>> findByMessageIsNull();

		Flux<SearchHit<SampleEntity>> findByMessageIsNotEmpty();

		Flux<SearchHit<SampleEntity>> findByMessageIsEmpty();

		Mono<Boolean> existsByMessage(String message);
	}

	private void loadEntities() {
		repository.saveAll(Flux.just( //
				new SampleEntity("with-message", "message"), //
				new SampleEntity("empty-message", ""), //
				new SampleEntity("null-message", null)) //
		).blockLast(); //
	}

	// region entities
	@SuppressWarnings("unused")
	@Document(indexName = "#{@indexNameProvider.indexName()}")
	static class SampleEntity {
		@Nullable
		@Id private String id;

		@Nullable
		@Field(type = Text) private String message;

		public SampleEntity() {}

		public SampleEntity(@Nullable String id, @Nullable String message) {
			this.id = id;
			this.message = message;
		}

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
	// endregion
}
