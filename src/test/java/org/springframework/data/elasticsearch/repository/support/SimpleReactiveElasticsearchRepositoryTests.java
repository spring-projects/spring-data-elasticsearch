/*
 * Copyright 2019-2020 the original author or authors.
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
package org.springframework.data.elasticsearch.repository.support;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.elasticsearch.annotations.FieldType.*;
import static org.springframework.data.elasticsearch.core.query.Query.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.lang.Boolean;
import java.lang.Long;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import org.elasticsearch.ElasticsearchStatusException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.Highlight;
import org.springframework.data.elasticsearch.annotations.HighlightField;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.junit.jupiter.ReactiveElasticsearchRestTemplateConfiguration;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.data.elasticsearch.repository.config.EnableReactiveElasticsearchRepositories;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Christoph Strobl
 * @author Peter-Josef Meisch
 * @author Jens Schauder
 */
@SpringIntegrationTest
@ContextConfiguration(classes = { SimpleReactiveElasticsearchRepositoryTests.Config.class })
class SimpleReactiveElasticsearchRepositoryTests {

	@Configuration
	@Import({ ReactiveElasticsearchRestTemplateConfiguration.class })
	@EnableReactiveElasticsearchRepositories(considerNestedRepositories = true)
	static class Config {}

	static final String INDEX = "test-index-sample-simple-reactive";

	@Autowired ReactiveElasticsearchOperations operations;
	@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") @Autowired ReactiveSampleEntityRepository repository;

	@BeforeEach
	void setUp() {
		operations.indexOps(IndexCoordinates.of(INDEX)).delete().block();
	}

	@AfterEach
	void after() {
		operations.indexOps(IndexCoordinates.of(INDEX)).delete().block();
	}

	@Test // DATAES-519
	void saveShouldSaveSingleEntity() {

		repository.save(SampleEntity.builder().build()) //
				.map(SampleEntity::getId) //
				.flatMap(this::documentWithIdExistsInIndex) //
				.as(StepVerifier::create) //
				.expectNext(true).verifyComplete();
	}

	private Mono<Boolean> documentWithIdExistsInIndex(String id) {
		return operations.exists(id, IndexCoordinates.of(INDEX));
	}

	@Test // DATAES-519
	void saveShouldComputeMultipleEntities() {

		repository
				.saveAll(Arrays.asList(SampleEntity.builder().build(), SampleEntity.builder().build(),
						SampleEntity.builder().build())) //
				.map(SampleEntity::getId) //
				.flatMap(this::documentWithIdExistsInIndex) //
				.as(StepVerifier::create) //
				.expectNext(true) //
				.expectNext(true) //
				.expectNext(true) //
				.verifyComplete();
	}

	@Test // DATAES-519, DATAES-767, DATAES-822
	void findByIdShouldErrorIfIndexDoesNotExist() {
		repository.findById("id-two") //
				.as(StepVerifier::create) //
				.expectError(ElasticsearchStatusException.class);
	}

	@Test // DATAES-519
	void findShouldRetrieveSingleEntityById() {

		bulkIndex(SampleEntity.builder().id("id-one").build(), //
				SampleEntity.builder().id("id-two").build(), //
				SampleEntity.builder().id("id-three").build()) //
						.block();

		repository.findById("id-two").as(StepVerifier::create)//
				.consumeNextWith(it -> assertThat(it.getId()).isEqualTo("id-two")) //
				.verifyComplete();
	}

	@Test // DATAES-519
	void findByIdShouldCompleteIfNothingFound() {

		bulkIndex(SampleEntity.builder().id("id-one").build(), //
				SampleEntity.builder().id("id-two").build(), //
				SampleEntity.builder().id("id-three").build()) //
						.block();

		repository.findById("does-not-exist").as(StepVerifier::create) //
				.verifyComplete();
	}

	@Test // DATAES-720
	void findAllShouldReturnAllElements() {
		// make sure to be above the default page size of the Query interface
		int count = DEFAULT_PAGE_SIZE * 2;
		bulkIndex(IntStream.range(1, count + 1) //
				.mapToObj(it -> SampleEntity.builder().id(String.valueOf(it)).build()) //
				.toArray(SampleEntity[]::new)) //
						.block();

		repository.findAll() //
				.as(StepVerifier::create) //
				.expectNextCount(count) //
				.verifyComplete();
	}

	@Test // DATAES-519
	void findAllByIdByIdShouldCompleteIfIndexDoesNotExist() {
		repository.findAllById(Arrays.asList("id-two", "id-two")).as(StepVerifier::create).verifyComplete();
	}

	@Test // DATAES-519
	void findAllByIdShouldRetrieveMatchingDocuments() {

		bulkIndex(SampleEntity.builder().id("id-one").build(), //
				SampleEntity.builder().id("id-two").build(), //
				SampleEntity.builder().id("id-three").build()) //
						.block();

		repository.findAllById(Arrays.asList("id-one", "id-two")) //
				.as(StepVerifier::create)//
				.expectNextMatches(entity -> entity.getId().equals("id-one") || entity.getId().equals("id-two")) //
				.expectNextMatches(entity -> entity.getId().equals("id-one") || entity.getId().equals("id-two")) //
				.verifyComplete();
	}

	@Test // DATAES-519
	void findAllByIdShouldCompleteWhenNothingFound() {

		bulkIndex(SampleEntity.builder().id("id-one").build(), //
				SampleEntity.builder().id("id-two").build(), //
				SampleEntity.builder().id("id-three").build()) //
						.block();

		repository.findAllById(Arrays.asList("can't", "touch", "this")) //
				.as(StepVerifier::create)//
				.verifyComplete();
	}

	@Test // DATAES-717
	void shouldReturnFluxOfSearchHit() {

		bulkIndex(SampleEntity.builder().id("id-one").message("message").build(), //
				SampleEntity.builder().id("id-two").message("message").build(), //
				SampleEntity.builder().id("id-three").message("message").build()) //
						.block();

		repository.queryAllByMessage("message") //
				.as(StepVerifier::create) //
				.expectNextMatches(searchHit -> SearchHit.class.isAssignableFrom(searchHit.getClass()))//
				.expectNextCount(2) //
				.verifyComplete();
	}

	@Test // DATAES-717
	void shouldReturnFluxOfSearchHitForStringQuery() {

		bulkIndex(SampleEntity.builder().id("id-one").message("message").build(), //
				SampleEntity.builder().id("id-two").message("message").build(), //
				SampleEntity.builder().id("id-three").message("message").build()) //
						.block();

		repository.queryByMessageWithString("message") //
				.as(StepVerifier::create) //
				.expectNextMatches(searchHit -> SearchHit.class.isAssignableFrom(searchHit.getClass()))//
				.expectNextCount(2) //
				.verifyComplete();
	}

	@Test // DATAES-372
	void shouldReturnHighlightsOnAnnotatedMethod() {

		bulkIndex(SampleEntity.builder().id("id-one").message("message").build(), //
				SampleEntity.builder().id("id-two").message("message").build(), //
				SampleEntity.builder().id("id-three").message("message").build()) //
						.block();

		repository.queryAllByMessage("message") //
				.as(StepVerifier::create) //
				.expectNextMatches(searchHit -> {
					List<String> hitHighlightField = searchHit.getHighlightField("message");
					return hitHighlightField.size() == 1 && hitHighlightField.get(0).equals("<em>message</em>");
				}) //
				.expectNextCount(2) //
				.verifyComplete();
	}

	@Test // DATAES-372
	void shouldReturnHighlightsOnAnnotatedStringQueryMethod() {

		bulkIndex(SampleEntity.builder().id("id-one").message("message").build(), //
				SampleEntity.builder().id("id-two").message("message").build(), //
				SampleEntity.builder().id("id-three").message("message").build()) //
						.block();

		repository.queryByMessageWithString("message") //
				.as(StepVerifier::create) //
				.expectNextMatches(searchHit -> {
					List<String> hitHighlightField = searchHit.getHighlightField("message");
					return hitHighlightField.size() == 1 && hitHighlightField.get(0).equals("<em>message</em>");
				}) //
				.expectNextCount(2) //
				.verifyComplete();
	}

	@Test // DATAES-519, DATAES-767, DATAES-822
	void countShouldErrorWhenIndexDoesNotExist() {
		repository.count() //
				.as(StepVerifier::create) //
				.expectError(ElasticsearchStatusException.class);
	}

	@Test // DATAES-519
	void countShouldCountDocuments() {

		bulkIndex(SampleEntity.builder().id("id-one").build(), //
				SampleEntity.builder().id("id-two").build()) //
						.block();

		repository.count().as(StepVerifier::create).expectNext(2L).verifyComplete();
	}

	@Test // DATAES-519
	void existsByIdShouldReturnTrueIfExists() {

		bulkIndex(SampleEntity.builder().id("id-one").message("message").build(), //
				SampleEntity.builder().id("id-two").message("test message").build(), //
				SampleEntity.builder().id("id-three").message("test test").build()) //
						.block();

		repository.existsById("id-two") //
				.as(StepVerifier::create) //
				.expectNext(true) //
				.verifyComplete();
	}

	@Test // DATAES-519
	void existsByIdShouldReturnFalseIfNotExists() {

		bulkIndex(SampleEntity.builder().id("id-one").message("message").build(), //
				SampleEntity.builder().id("id-two").message("test message").build(), //
				SampleEntity.builder().id("id-three").message("test test").build()) //
						.block();

		repository.existsById("wrecking ball") //
				.as(StepVerifier::create) //
				.expectNext(false) //
				.verifyComplete();
	}

	@Test // DATAES-519
	void countShouldCountMatchingDocuments() {

		bulkIndex(SampleEntity.builder().id("id-one").message("message").build(), //
				SampleEntity.builder().id("id-two").message("test message").build(), //
				SampleEntity.builder().id("id-three").message("test test").build()) //
						.block();

		repository.countAllByMessage("test") //
				.as(StepVerifier::create) //
				.expectNext(2L) //
				.verifyComplete();
	}

	@Test // DATAES-519
	void existsShouldReturnTrueIfExists() {

		bulkIndex(SampleEntity.builder().id("id-one").message("message").build(), //
				SampleEntity.builder().id("id-two").message("test message").build(), //
				SampleEntity.builder().id("id-three").message("test test").build()) //
						.block();

		repository.existsAllByMessage("message") //
				.as(StepVerifier::create) //
				.expectNext(true) //
				.verifyComplete();
	}

	@Test // DATAES-519
	void existsShouldReturnFalseIfNotExists() {

		bulkIndex(SampleEntity.builder().id("id-one").message("message").build(), //
				SampleEntity.builder().id("id-two").message("test message").build(), //
				SampleEntity.builder().id("id-three").message("test test").build()) //
						.block();

		repository.existsAllByMessage("these days") //
				.as(StepVerifier::create) //
				.expectNext(false) //
				.verifyComplete();
	}

	@Test // DATAES-519
	void deleteByIdShouldCompleteIfNothingDeleted() {

		bulkIndex(SampleEntity.builder().id("id-one").build(), //
				SampleEntity.builder().id("id-two").build()) //
						.block();

		repository.deleteById("does-not-exist").as(StepVerifier::create).verifyComplete();
	}

	@Test // DATAES-519, DATAES-767, DATAES-822, DATAES-678
	void deleteByIdShouldCompleteWhenIndexDoesNotExist() {
		repository.deleteById("does-not-exist") //
				.as(StepVerifier::create) //
				.verifyComplete();
	}

	@Test // DATAES-519
	void deleteByIdShouldDeleteEntry() {

		SampleEntity toBeDeleted = SampleEntity.builder().id("id-two").build();
		bulkIndex(SampleEntity.builder().id("id-one").build(), toBeDeleted) //
				.block();

		repository.deleteById(toBeDeleted.getId()).as(StepVerifier::create).verifyComplete();

		assertThat(documentWithIdExistsInIndex(toBeDeleted.getId()).block()).isFalse();
	}

	@Test // DATAES-976
	void deleteAllByIdShouldDeleteEntry() {

		SampleEntity toBeDeleted = SampleEntity.builder().id("id-two").build();
		bulkIndex(SampleEntity.builder().id("id-one").build(), toBeDeleted) //
				.block();

		repository.deleteAllById(Collections.singletonList(toBeDeleted.getId())).as(StepVerifier::create).verifyComplete();

		assertThat(documentWithIdExistsInIndex(toBeDeleted.getId()).block()).isFalse();
	}

	@Test // DATAES-519
	void deleteShouldDeleteEntry() {

		SampleEntity toBeDeleted = SampleEntity.builder().id("id-two").build();
		bulkIndex(SampleEntity.builder().id("id-one").build(), toBeDeleted) //
				.block();

		repository.delete(toBeDeleted).as(StepVerifier::create).verifyComplete();

		assertThat(documentWithIdExistsInIndex(toBeDeleted.getId()).block()).isFalse();
	}

	@Test // DATAES-519
	void deleteAllShouldDeleteGivenEntries() {

		SampleEntity toBeDeleted = SampleEntity.builder().id("id-one").build();
		SampleEntity hangInThere = SampleEntity.builder().id("id-two").build();
		SampleEntity toBeDeleted2 = SampleEntity.builder().id("id-three").build();

		bulkIndex(toBeDeleted, hangInThere, toBeDeleted2) //
				.block();

		repository.deleteAll(Arrays.asList(toBeDeleted, toBeDeleted2)).as(StepVerifier::create).verifyComplete();

		assertThat(documentWithIdExistsInIndex(toBeDeleted.getId()).block()).isFalse();
		assertThat(documentWithIdExistsInIndex(toBeDeleted2.getId()).block()).isFalse();
		assertThat(documentWithIdExistsInIndex(hangInThere.getId()).block()).isTrue();
	}

	@Test // DATAES-519
	void deleteAllShouldDeleteAllEntries() {

		bulkIndex(SampleEntity.builder().id("id-one").build(), //
				SampleEntity.builder().id("id-two").build(), //
				SampleEntity.builder().id("id-three").build()) //
						.block();

		repository.deleteAll().as(StepVerifier::create).verifyComplete();

		repository.count() //
				.as(StepVerifier::create) //
				.expectNext(0L) //
				.verifyComplete();
	}

	@Test // DATAES-519
	void derivedFinderMethodShouldBeExecutedCorrectly() {

		bulkIndex(SampleEntity.builder().id("id-one").message("message").build(), //
				SampleEntity.builder().id("id-two").message("test message").build(), //
				SampleEntity.builder().id("id-three").message("test test").build()) //
						.block();

		repository.findAllByMessageLike("test") //
				.as(StepVerifier::create) //
				.expectNextCount(2) //
				.verifyComplete();
	}

	@Test // DATAES-519
	void derivedFinderMethodShouldBeExecutedCorrectlyWhenGivenPublisher() {

		bulkIndex(SampleEntity.builder().id("id-one").message("message").build(), //
				SampleEntity.builder().id("id-two").message("test message").build(), //
				SampleEntity.builder().id("id-three").message("test test").build()) //
						.block();

		repository.findAllByMessage(Mono.just("test")) //
				.as(StepVerifier::create) //
				.expectNextCount(2) //
				.verifyComplete();
	}

	@Test // DATAES-519
	void derivedFinderWithDerivedSortMethodShouldBeExecutedCorrectly() {

		bulkIndex(SampleEntity.builder().id("id-one").message("test").rate(3).build(), //
				SampleEntity.builder().id("id-two").message("test test").rate(1).build(), //
				SampleEntity.builder().id("id-three").message("test test").rate(2).build()) //
						.block();

		repository.findAllByMessageLikeOrderByRate("test") //
				.as(StepVerifier::create) //
				.consumeNextWith(it -> assertThat(it.getId()).isEqualTo("id-two")) //
				.consumeNextWith(it -> assertThat(it.getId()).isEqualTo("id-three")) //
				.consumeNextWith(it -> assertThat(it.getId()).isEqualTo("id-one")) //
				.verifyComplete();
	}

	@Test // DATAES-519
	void derivedFinderMethodWithSortParameterShouldBeExecutedCorrectly() {

		bulkIndex(SampleEntity.builder().id("id-one").message("test").rate(3).build(), //
				SampleEntity.builder().id("id-two").message("test test").rate(1).build(), //
				SampleEntity.builder().id("id-three").message("test test").rate(2).build()) //
						.block();

		repository.findAllByMessage("test", Sort.by(Order.asc("rate"))) //
				.as(StepVerifier::create) //
				.consumeNextWith(it -> assertThat(it.getId()).isEqualTo("id-two")) //
				.consumeNextWith(it -> assertThat(it.getId()).isEqualTo("id-three")) //
				.consumeNextWith(it -> assertThat(it.getId()).isEqualTo("id-one")) //
				.verifyComplete();
	}

	@Test // DATAES-519
	void derivedFinderMethodWithPageableParameterShouldBeExecutedCorrectly() {

		bulkIndex(SampleEntity.builder().id("id-one").message("test").rate(3).build(), //
				SampleEntity.builder().id("id-two").message("test test").rate(1).build(), //
				SampleEntity.builder().id("id-three").message("test test").rate(2).build()) //
						.block();

		repository.findAllByMessage("test", PageRequest.of(0, 2, Sort.by(Order.asc("rate")))) //
				.as(StepVerifier::create) //
				.consumeNextWith(it -> assertThat(it.getId()).isEqualTo("id-two")) //
				.consumeNextWith(it -> assertThat(it.getId()).isEqualTo("id-three")) //
				.verifyComplete();
	}

	@Test // DATAES-519
	void derivedFinderMethodReturningMonoShouldBeExecutedCorrectly() {

		bulkIndex(SampleEntity.builder().id("id-one").message("message").build(), //
				SampleEntity.builder().id("id-two").message("test message").build(), //
				SampleEntity.builder().id("id-three").message("test test").build()) //
						.block();

		repository.findFirstByMessageLike("test") //
				.as(StepVerifier::create) //
				.expectNextCount(1) //
				.verifyComplete();
	}

	@Test // DATAES-519
	void annotatedFinderMethodShouldBeExecutedCorrectly() {

		bulkIndex(SampleEntity.builder().id("id-one").message("message").build(), //
				SampleEntity.builder().id("id-two").message("test message").build(), //
				SampleEntity.builder().id("id-three").message("test test").build()) //
						.block();

		repository.findAllViaAnnotatedQueryByMessageLike("test") //
				.as(StepVerifier::create) //
				.expectNextCount(2) //
				.verifyComplete();
	}

	@Test // DATAES-519
	void derivedDeleteMethodShouldBeExecutedCorrectly() {

		bulkIndex(SampleEntity.builder().id("id-one").message("message").build(), //
				SampleEntity.builder().id("id-two").message("test message").build(), //
				SampleEntity.builder().id("id-three").message("test test").build()) //
						.block();

		repository.deleteAllByMessage("message") //
				.as(StepVerifier::create) //
				.expectNext(2L) //
				.verifyComplete();

		assertThat(documentWithIdExistsInIndex("id-one").block()).isFalse();
		assertThat(documentWithIdExistsInIndex("id-two").block()).isFalse();
		assertThat(documentWithIdExistsInIndex("id-three").block()).isTrue();
	}

	Mono<Void> bulkIndex(SampleEntity... entities) {
		return operations.saveAll(Arrays.asList(entities), IndexCoordinates.of(INDEX)).then();
	}

	interface ReactiveSampleEntityRepository extends ReactiveCrudRepository<SampleEntity, String> {

		Flux<SampleEntity> findAllByMessageLike(String message);

		Flux<SampleEntity> findAllByMessageLikeOrderByRate(String message);

		Flux<SampleEntity> findAllByMessage(String message, Sort sort);

		Flux<SampleEntity> findAllByMessage(String message, Pageable pageable);

		Flux<SampleEntity> findAllByMessage(Publisher<String> message);

		@Highlight(fields = { @HighlightField(name = "message") })
		Flux<SearchHit<SampleEntity>> queryAllByMessage(String message);

		@Query("{\"bool\": {\"must\": [{\"term\": {\"message\": \"?0\"}}]}}")
		@Highlight(fields = { @HighlightField(name = "message") })
		Flux<SearchHit<SampleEntity>> queryByMessageWithString(String message);

		@Query("{ \"bool\" : { \"must\" : { \"term\" : { \"message\" : \"?0\" } } } }")
		Flux<SampleEntity> findAllViaAnnotatedQueryByMessageLike(String message);

		Mono<SampleEntity> findFirstByMessageLike(String message);

		Mono<Long> countAllByMessage(String message);

		Mono<Boolean> existsAllByMessage(String message);

		Mono<Long> deleteAllByMessage(String message);
	}

	/**
	 * @author Rizwan Idrees
	 * @author Mohsin Husen
	 * @author Chris White
	 * @author Sascha Woo
	 */
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	@Document(indexName = INDEX, replicas = 0, refreshInterval = "-1")
	static class SampleEntity {

		@Id private String id;
		@Field(type = Text, store = true, fielddata = true) private String type;
		@Field(type = Text, store = true, fielddata = true) private String message;
		private int rate;
		private boolean available;
		@Version private Long version;

	}
}
