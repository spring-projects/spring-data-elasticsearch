/*
 * Copyright 2019 the original author or authors.
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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.lang.Boolean;
import java.lang.Long;
import java.lang.Object;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.reactivestreams.Publisher;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.elasticsearch.TestUtils;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.annotations.Score;
import org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient;
import org.springframework.data.elasticsearch.config.AbstractReactiveElasticsearchConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableReactiveElasticsearchRepositories;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.StringUtils;

/**
 * @author Christoph Strobl
 * @currentRead Fool's Fate - Robin Hobb
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class SimpleReactiveElasticsearchRepositoryTests {

	@Configuration
	@EnableReactiveElasticsearchRepositories(considerNestedRepositories = true)
	static class Config extends AbstractReactiveElasticsearchConfiguration {

		@Override
		public ReactiveElasticsearchClient reactiveElasticsearchClient() {
			return TestUtils.reactiveClient();
		}
	}

	static final String INDEX = "test-index-sample-simple-reactive";
	static final String TYPE = "test-type";

	@Autowired ReactiveSampleEntityRepository repository;

	@Before
	public void setUp() {
		TestUtils.deleteIndex(INDEX);
	}

	@Test // DATAES-519
	public void saveShouldSaveSingleEntity() {

		repository.save(SampleEntity.builder().build()) //
				.as(StepVerifier::create) //
				.consumeNextWith(it -> {
					assertThat(TestUtils.documentWithId(it.getId()).ofType(TYPE).existsIn(INDEX)).isTrue();
				}) //
				.verifyComplete();
	}

	@Test // DATAES-519
	public void saveShouldComputeMultipleEntities() {

		repository
				.saveAll(Arrays.asList(SampleEntity.builder().build(), SampleEntity.builder().build(),
						SampleEntity.builder().build())) //
				.as(StepVerifier::create) //
				.consumeNextWith(it -> {
					assertThat(TestUtils.documentWithId(it.getId()).ofType(TYPE).existsIn(INDEX)).isTrue();
				}) //
				.consumeNextWith(it -> {
					assertThat(TestUtils.documentWithId(it.getId()).ofType(TYPE).existsIn(INDEX)).isTrue();
				}) //
				.consumeNextWith(it -> {
					assertThat(TestUtils.documentWithId(it.getId()).ofType(TYPE).existsIn(INDEX)).isTrue();
				}) //
				.verifyComplete();
	}

	@Test // DATAES-519
	public void findByIdShouldCompleteIfIndexDoesNotExist() {
		repository.findById("id-two").as(StepVerifier::create).verifyComplete();
	}

	@Test // DATAES-519
	public void findShouldRetrieveSingleEntityById() {

		bulkIndex(SampleEntity.builder().id("id-one").build(), //
				SampleEntity.builder().id("id-two").build(), //
				SampleEntity.builder().id("id-three").build());

		repository.findById("id-two").as(StepVerifier::create)//
				.consumeNextWith(it -> {
					assertThat(it.getId()).isEqualTo("id-two");
				}) //
				.verifyComplete();
	}

	@Test // DATAES-519
	public void findByIdShouldCompleteIfNothingFound() {

		bulkIndex(SampleEntity.builder().id("id-one").build(), //
				SampleEntity.builder().id("id-two").build(), //
				SampleEntity.builder().id("id-three").build());

		repository.findById("does-not-exist").as(StepVerifier::create) //
				.verifyComplete();
	}

	@Test // DATAES-519
	public void findAllByIdByIdShouldCompleteIfIndexDoesNotExist() {
		repository.findAllById(Arrays.asList("id-two", "id-two")).as(StepVerifier::create).verifyComplete();
	}

	@Test // DATAES-519
	public void findAllByIdShouldRetrieveMatchingDocuments() {

		bulkIndex(SampleEntity.builder().id("id-one").build(), //
				SampleEntity.builder().id("id-two").build(), //
				SampleEntity.builder().id("id-three").build());

		repository.findAllById(Arrays.asList("id-one", "id-two")) //
				.as(StepVerifier::create)//
				.expectNextCount(2) //
				.verifyComplete();
	}

	@Test // DATAES-519
	public void findAllByIdShouldCompleteWhenNothingFound() {

		bulkIndex(SampleEntity.builder().id("id-one").build(), //
				SampleEntity.builder().id("id-two").build(), //
				SampleEntity.builder().id("id-three").build());

		repository.findAllById(Arrays.asList("can't", "touch", "this")) //
				.as(StepVerifier::create)//
				.verifyComplete();
	}

	@Test // DATAES-519
	public void countShouldReturnZeroWhenIndexDoesNotExist() {
		repository.count().as(StepVerifier::create).expectNext(0L).verifyComplete();
	}

	@Test // DATAES-519
	public void countShouldCountDocuments() {

		bulkIndex(SampleEntity.builder().id("id-one").build(), //
				SampleEntity.builder().id("id-two").build());

		repository.count().as(StepVerifier::create).expectNext(2L).verifyComplete();
	}

	@Test // DATAES-519
	public void existsByIdShouldReturnTrueIfExists() {

		bulkIndex(SampleEntity.builder().id("id-one").message("message").build(), //
				SampleEntity.builder().id("id-two").message("test message").build(), //
				SampleEntity.builder().id("id-three").message("test test").build());

		repository.existsById("id-two") //
				.as(StepVerifier::create) //
				.expectNext(true) //
				.verifyComplete();
	}

	@Test // DATAES-519
	public void existsByIdShouldReturnFalseIfNotExists() {

		bulkIndex(SampleEntity.builder().id("id-one").message("message").build(), //
				SampleEntity.builder().id("id-two").message("test message").build(), //
				SampleEntity.builder().id("id-three").message("test test").build());

		repository.existsById("wrecking ball") //
				.as(StepVerifier::create) //
				.expectNext(false) //
				.verifyComplete();
	}

	@Test // DATAES-519
	public void countShouldCountMatchingDocuments() {

		bulkIndex(SampleEntity.builder().id("id-one").message("message").build(), //
				SampleEntity.builder().id("id-two").message("test message").build(), //
				SampleEntity.builder().id("id-three").message("test test").build());

		repository.countAllByMessage("test") //
				.as(StepVerifier::create) //
				.expectNext(2L) //
				.verifyComplete();
	}

	@Test // DATAES-519
	public void existsShouldReturnTrueIfExists() {

		bulkIndex(SampleEntity.builder().id("id-one").message("message").build(), //
				SampleEntity.builder().id("id-two").message("test message").build(), //
				SampleEntity.builder().id("id-three").message("test test").build());

		repository.existsAllByMessage("message") //
				.as(StepVerifier::create) //
				.expectNext(true) //
				.verifyComplete();
	}

	@Test // DATAES-519
	public void existsShouldReturnFalseIfNotExists() {

		bulkIndex(SampleEntity.builder().id("id-one").message("message").build(), //
				SampleEntity.builder().id("id-two").message("test message").build(), //
				SampleEntity.builder().id("id-three").message("test test").build());

		repository.existsAllByMessage("these days") //
				.as(StepVerifier::create) //
				.expectNext(false) //
				.verifyComplete();
	}

	@Test // DATAES-519
	public void deleteByIdShouldCompleteIfNothingDeleted() {

		bulkIndex(SampleEntity.builder().id("id-one").build(), //
				SampleEntity.builder().id("id-two").build());

		repository.deleteById("does-not-exist").as(StepVerifier::create).verifyComplete();
	}

	@Test // DATAES-519
	public void deleteByIdShouldCompleteWhenIndexDoesNotExist() {
		repository.deleteById("does-not-exist").as(StepVerifier::create).verifyComplete();
	}

	@Test // DATAES-519
	public void deleteByIdShouldDeleteEntry() {

		SampleEntity toBeDeleted = SampleEntity.builder().id("id-two").build();
		bulkIndex(SampleEntity.builder().id("id-one").build(), toBeDeleted);

		repository.deleteById(toBeDeleted.getId()).as(StepVerifier::create).verifyComplete();

		assertThat(TestUtils.documentWithId(toBeDeleted.getId()).ofType(TYPE).existsIn(INDEX)).isFalse();
	}

	@Test // DATAES-519
	public void deleteShouldDeleteEntry() {

		SampleEntity toBeDeleted = SampleEntity.builder().id("id-two").build();
		bulkIndex(SampleEntity.builder().id("id-one").build(), toBeDeleted);

		repository.delete(toBeDeleted).as(StepVerifier::create).verifyComplete();

		assertThat(TestUtils.documentWithId(toBeDeleted.getId()).ofType(TYPE).existsIn(INDEX)).isFalse();
	}

	@Test // DATAES-519
	public void deleteAllShouldDeleteGivenEntries() {

		SampleEntity toBeDeleted = SampleEntity.builder().id("id-one").build();
		SampleEntity hangInThere = SampleEntity.builder().id("id-two").build();
		SampleEntity toBeDeleted2 = SampleEntity.builder().id("id-three").build();

		bulkIndex(toBeDeleted, hangInThere, toBeDeleted2);

		repository.deleteAll(Arrays.asList(toBeDeleted, toBeDeleted2)).as(StepVerifier::create).verifyComplete();

		assertThat(TestUtils.documentWithId(toBeDeleted.getId()).ofType(TYPE).existsIn(INDEX)).isFalse();
		assertThat(TestUtils.documentWithId(toBeDeleted2.getId()).ofType(TYPE).existsIn(INDEX)).isFalse();
		assertThat(TestUtils.documentWithId(hangInThere.getId()).ofType(TYPE).existsIn(INDEX)).isTrue();
	}

	@Test // DATAES-519
	public void deleteAllShouldDeleteAllEntries() {

		bulkIndex(SampleEntity.builder().id("id-one").build(), //
				SampleEntity.builder().id("id-two").build(), //
				SampleEntity.builder().id("id-three").build());

		repository.deleteAll().as(StepVerifier::create).verifyComplete();

		assertThat(TestUtils.isEmptyIndex(INDEX)).isTrue();
	}

	@Test // DATAES-519
	public void derivedFinderMethodShouldBeExecutedCorrectly() {

		bulkIndex(SampleEntity.builder().id("id-one").message("message").build(), //
				SampleEntity.builder().id("id-two").message("test message").build(), //
				SampleEntity.builder().id("id-three").message("test test").build());

		repository.findAllByMessageLike("test") //
				.as(StepVerifier::create) //
				.expectNextCount(2) //
				.verifyComplete();
	}

	@Test // DATAES-519
	public void derivedFinderMethodShouldBeExecutedCorrectlyWhenGivenPublisher() {

		bulkIndex(SampleEntity.builder().id("id-one").message("message").build(), //
				SampleEntity.builder().id("id-two").message("test message").build(), //
				SampleEntity.builder().id("id-three").message("test test").build());

		repository.findAllByMessage(Mono.just("test")) //
				.as(StepVerifier::create) //
				.expectNextCount(2) //
				.verifyComplete();
	}

	@Test // DATAES-519
	public void derivedFinderWithDerivedSortMethodShouldBeExecutedCorrectly() {

		bulkIndex(SampleEntity.builder().id("id-one").message("test").rate(3).build(), //
				SampleEntity.builder().id("id-two").message("test test").rate(1).build(), //
				SampleEntity.builder().id("id-three").message("test test").rate(2).build());

		repository.findAllByMessageLikeOrderByRate("test") //
				.as(StepVerifier::create) //
				.consumeNextWith(it -> assertThat(it.getId()).isEqualTo("id-two")) //
				.consumeNextWith(it -> assertThat(it.getId()).isEqualTo("id-three")) //
				.consumeNextWith(it -> assertThat(it.getId()).isEqualTo("id-one")) //
				.verifyComplete();
	}

	@Test // DATAES-519
	public void derivedFinderMethodWithSortParameterShouldBeExecutedCorrectly() {

		bulkIndex(SampleEntity.builder().id("id-one").message("test").rate(3).build(), //
				SampleEntity.builder().id("id-two").message("test test").rate(1).build(), //
				SampleEntity.builder().id("id-three").message("test test").rate(2).build());

		repository.findAllByMessage("test", Sort.by(Order.asc("rate"))) //
				.as(StepVerifier::create) //
				.consumeNextWith(it -> assertThat(it.getId()).isEqualTo("id-two")) //
				.consumeNextWith(it -> assertThat(it.getId()).isEqualTo("id-three")) //
				.consumeNextWith(it -> assertThat(it.getId()).isEqualTo("id-one")) //
				.verifyComplete();
	}

	@Test // DATAES-519
	public void derivedFinderMethodWithPageableParameterShouldBeExecutedCorrectly() {

		bulkIndex(SampleEntity.builder().id("id-one").message("test").rate(3).build(), //
				SampleEntity.builder().id("id-two").message("test test").rate(1).build(), //
				SampleEntity.builder().id("id-three").message("test test").rate(2).build());

		repository.findAllByMessage("test", PageRequest.of(0, 2, Sort.by(Order.asc("rate")))) //
				.as(StepVerifier::create) //
				.consumeNextWith(it -> assertThat(it.getId()).isEqualTo("id-two")) //
				.consumeNextWith(it -> assertThat(it.getId()).isEqualTo("id-three")) //
				.verifyComplete();
	}

	@Test // DATAES-519
	public void derivedFinderMethodReturningMonoShouldBeExecutedCorrectly() {

		bulkIndex(SampleEntity.builder().id("id-one").message("message").build(), //
				SampleEntity.builder().id("id-two").message("test message").build(), //
				SampleEntity.builder().id("id-three").message("test test").build());

		repository.findFirstByMessageLike("test") //
				.as(StepVerifier::create) //
				.expectNextCount(1) //
				.verifyComplete();
	}

	@Test // DATAES-519
	public void annotatedFinderMethodShouldBeExecutedCorrectly() {

		bulkIndex(SampleEntity.builder().id("id-one").message("message").build(), //
				SampleEntity.builder().id("id-two").message("test message").build(), //
				SampleEntity.builder().id("id-three").message("test test").build());

		repository.findAllViaAnnotatedQueryByMessageLike("test") //
				.as(StepVerifier::create) //
				.expectNextCount(2) //
				.verifyComplete();
	}

	@Test // DATAES-519
	public void derivedDeleteMethodShouldBeExecutedCorrectly() {

		bulkIndex(SampleEntity.builder().id("id-one").message("message").build(), //
				SampleEntity.builder().id("id-two").message("test message").build(), //
				SampleEntity.builder().id("id-three").message("test test").build());

		repository.deleteAllByMessage("message") //
				.as(StepVerifier::create) //
				.expectNext(2L) //
				.verifyComplete();

		assertThat(TestUtils.documentWithId("id-one").ofType(TYPE).existsIn(INDEX)).isFalse();
		assertThat(TestUtils.documentWithId("id-two").ofType(TYPE).existsIn(INDEX)).isFalse();
		assertThat(TestUtils.documentWithId("id-three").ofType(TYPE).existsIn(INDEX)).isTrue();
	}

	IndexRequest indexRequest(Map source, String index, String type) {

		return new IndexRequest(index, type) //
				.id(source.containsKey("id") ? source.get("id").toString() : UUID.randomUUID().toString()) //
				.source(source) //
				.create(true);
	}

	IndexRequest indexRequestFrom(SampleEntity entity) {

		Map<String, Object> target = new LinkedHashMap<>();

		if (StringUtils.hasText(entity.getId())) {
			target.put("id", entity.getId());
		}

		if (StringUtils.hasText(entity.getType())) {
			target.put("type", entity.getType());
		}

		if (StringUtils.hasText(entity.getMessage())) {
			target.put("message", entity.getMessage());
		}

		target.put("rate", entity.getRate());
		target.put("available", entity.isAvailable());

		return indexRequest(target, INDEX, TYPE);
	}

	void bulkIndex(SampleEntity... entities) {

		BulkRequest request = new BulkRequest();
		Arrays.stream(entities).forEach(it -> request.add(indexRequestFrom(it)));

		try (RestHighLevelClient client = TestUtils.restHighLevelClient()) {
			client.bulk(request.setRefreshPolicy(RefreshPolicy.IMMEDIATE), RequestOptions.DEFAULT);
		} catch (Exception e) {}
	}

	interface ReactiveSampleEntityRepository extends ReactiveCrudRepository<SampleEntity, String> {

		Flux<SampleEntity> findAllByMessageLike(String message);

		Flux<SampleEntity> findAllByMessageLikeOrderByRate(String message);

		Flux<SampleEntity> findAllByMessage(String message, Sort sort);

		Flux<SampleEntity> findAllByMessage(String message, Pageable pageable);

		Flux<SampleEntity> findAllByMessage(Publisher<String> message);

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
	@Document(indexName = INDEX, type = TYPE, shards = 1, replicas = 0, refreshInterval = "-1")
	static class SampleEntity {

		@Id private String id;
		@Field(type = Text, store = true, fielddata = true) private String type;
		@Field(type = Text, store = true, fielddata = true) private String message;
		private int rate;
		private boolean available;
		@Version private Long version;
		@Score private float score;

	}
}
