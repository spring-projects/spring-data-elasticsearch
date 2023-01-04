/*
 * Copyright 2019-2023 the original author or authors.
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
import static org.springframework.data.elasticsearch.core.query.Query.*;
import static org.springframework.data.elasticsearch.utils.IdGenerator.*;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.elasticsearch.NoSuchIndexException;
import org.springframework.data.elasticsearch.annotations.CountQuery;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Highlight;
import org.springframework.data.elasticsearch.annotations.HighlightField;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.annotations.SourceFilters;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.lang.Nullable;

/**
 * @author Christoph Strobl
 * @author Peter-Josef Meisch
 * @author Jens Schauder
 */
@SpringIntegrationTest
abstract class SimpleReactiveElasticsearchRepositoryIntegrationTests {

	@Autowired ReactiveElasticsearchOperations operations;
	@Autowired ReactiveSampleEntityRepository repository;

	@Autowired private IndexNameProvider indexNameProvider;

	@BeforeEach
	void before() {
		indexNameProvider.increment();
		operations.indexOps(SampleEntity.class).createWithMapping().block();
	}

	@Test
	@org.junit.jupiter.api.Order(Integer.MAX_VALUE)
	public void cleanup() {
		operations.indexOps(IndexCoordinates.of(indexNameProvider.getPrefix() + "*")).delete().block();
	}

	@Test // DATAES-519
	void saveShouldSaveSingleEntity() {

		repository.save(new SampleEntity()) //
				.map(SampleEntity::getId) //
				.flatMap(this::documentWithIdExistsInIndex) //
				.as(StepVerifier::create) //
				.expectNext(true).verifyComplete();
	}

	private Mono<Boolean> documentWithIdExistsInIndex(String id) {
		return operations.exists(id, IndexCoordinates.of(indexNameProvider.indexName()));
	}

	@Test // DATAES-519
	void saveShouldComputeMultipleEntities() {

		repository.saveAll(Arrays.asList(new SampleEntity(), new SampleEntity(), new SampleEntity()))
				/**/.map(SampleEntity::getId) //
				.flatMap(this::documentWithIdExistsInIndex) //
				.as(StepVerifier::create) //
				.expectNext(true) //
				.expectNext(true) //
				.expectNext(true) //
				.verifyComplete();
	}

	@Test // DATAES-519, DATAES-767, DATAES-822
	void findByIdShouldErrorIfIndexDoesNotExist() {

		operations.indexOps(SampleEntity.class).delete().block();
		repository.findById("id-two") //
				.as(StepVerifier::create) //
				.expectError(NoSuchIndexException.class) //
				.verify();
	}

	@Test // DATAES-519
	void findShouldRetrieveSingleEntityById() {

		bulkIndex(new SampleEntity("id-one"), //
				new SampleEntity("id-two"), //
				new SampleEntity("id-three")) //
						.block();

		repository.findById("id-two").as(StepVerifier::create)//
				.consumeNextWith(it -> assertThat(it.getId()).isEqualTo("id-two")) //
				.verifyComplete();
	}

	@Test // DATAES-519
	void findByIdShouldCompleteIfNothingFound() {

		bulkIndex(new SampleEntity("id-one"), //
				new SampleEntity("id-two"), //
				new SampleEntity("id-three")) //
						.block();

		repository.findById("does-not-exist").as(StepVerifier::create) //
				.verifyComplete();
	}

	@Test // DATAES-720
	void findAllShouldReturnAllElements() {
		// make sure to be above the default page size of the Query interface
		int count = DEFAULT_PAGE_SIZE * 2;
		bulkIndex(IntStream.range(1, count + 1) //
				.mapToObj(it -> new SampleEntity(String.valueOf(it))) //
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

	@Test // DATAES-519, #2417
	void findAllByIdShouldRetrieveMatchingDocuments() {

		// create more than 10 documents to see that the number of input ids is set as requested size
		int numEntities = 20;
		List<String> ids = new ArrayList<>(numEntities);
		List<SampleEntity> entities = new ArrayList<>(numEntities);
		for (int i = 0; i < numEntities; i++) {
			String documentId = nextIdAsString();
			ids.add(documentId);
			SampleEntity sampleEntity = new SampleEntity();
			sampleEntity.setId(documentId);
			sampleEntity.setMessage("hello world.");
			sampleEntity.setVersion(System.currentTimeMillis());
			entities.add(sampleEntity);
		}
		repository.saveAll(entities).blockLast();

		repository.findAllById(ids) //
				.as(StepVerifier::create)//
				.expectNextCount(numEntities) //
				.verifyComplete();
	}

	@Test // DATAES-519
	void findAllByIdShouldCompleteWhenNothingFound() {

		bulkIndex(new SampleEntity("id-one"), //
				new SampleEntity("id-two"), //
				new SampleEntity("id-three")) //
						.block();

		repository.findAllById(Arrays.asList("can't", "touch", "this")) //
				.as(StepVerifier::create)//
				.verifyComplete();
	}

	@Test // DATAES-717
	void shouldReturnFluxOfSearchHit() {

		bulkIndex(new SampleEntity("id-one", "message"), //
				new SampleEntity("id-two", "message"), //
				new SampleEntity("id-three", "message")) //
						.block();

		repository.queryAllByMessage("message") //
				.as(StepVerifier::create) //
				.expectNextMatches(searchHit -> SearchHit.class.isAssignableFrom(searchHit.getClass()))//
				.expectNextCount(2) //
				.verifyComplete();
	}

	@Test // DATAES-717
	void shouldReturnFluxOfSearchHitForStringQuery() {

		bulkIndex(new SampleEntity("id-one", "message"), //
				new SampleEntity("id-two", "message"), //
				new SampleEntity("id-three", "message")) //
						.block();

		repository.queryByMessageWithString("message") //
				.as(StepVerifier::create) //
				.expectNextMatches(searchHit -> SearchHit.class.isAssignableFrom(searchHit.getClass()))//
				.expectNextCount(2) //
				.verifyComplete();
	}

	@Test // DATAES-372
	void shouldReturnHighlightsOnAnnotatedMethod() {

		bulkIndex(new SampleEntity("id-one", "message"), //
				new SampleEntity("id-two", "message"), //
				new SampleEntity("id-three", "message")) //
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

		bulkIndex(new SampleEntity("id-one", "message"), //
				new SampleEntity("id-two", "message"), //
				new SampleEntity("id-three", "message")) //
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

		operations.indexOps(SampleEntity.class).delete().block();
		repository.count() //
				.as(StepVerifier::create) //
				.expectError(NoSuchIndexException.class) //
				.verify();
	}

	@Test // DATAES-519
	void countShouldCountDocuments() {

		bulkIndex(new SampleEntity("id-one"), //
				new SampleEntity("id-two")) //
						.block();

		repository.count().as(StepVerifier::create).expectNext(2L).verifyComplete();
	}

	@Test // DATAES-519
	void existsByIdShouldReturnTrueIfExists() {

		bulkIndex(new SampleEntity("id-one", "message"), //
				new SampleEntity("id-two", "test message"), //
				new SampleEntity("id-three", "test test")) //
						.block();

		repository.existsById("id-two") //
				.as(StepVerifier::create) //
				.expectNext(true) //
				.verifyComplete();
	}

	@Test // DATAES-519
	void existsByIdShouldReturnFalseIfNotExists() {

		bulkIndex(new SampleEntity("id-one", "message"), //
				new SampleEntity("id-two", "test message"), //
				new SampleEntity("id-three", "test test")) //
						.block();

		repository.existsById("wrecking ball") //
				.as(StepVerifier::create) //
				.expectNext(false) //
				.verifyComplete();
	}

	@Test // DATAES-519
	void countShouldCountMatchingDocuments() {

		bulkIndex(new SampleEntity("id-one", "message"), //
				new SampleEntity("id-two", "test message"), //
				new SampleEntity("id-three", "test test")).block();

		repository.countAllByMessage("test") //
				.as(StepVerifier::create) //
				.expectNext(2L) //
				.verifyComplete();
	}

	@Test // #1156
	@DisplayName("should count with string query")
	void shouldCountWithStringQuery() {

		bulkIndex(new SampleEntity("id-one", "message"), //
				new SampleEntity("id-two", "test message"), //
				new SampleEntity("id-three", "test test")).block();

		repository.retrieveCountByText("test") //
				.as(StepVerifier::create) //
				.expectNext(2L) //
				.verifyComplete();
	}

	@Test // DATAES-519
	void existsShouldReturnTrueIfExists() {

		bulkIndex(new SampleEntity("id-one", "message"), //
				new SampleEntity("id-two", "test message"), //
				new SampleEntity("id-three", "test test")) //
						.block();

		repository.existsAllByMessage("message") //
				.as(StepVerifier::create) //
				.expectNext(true) //
				.verifyComplete();
	}

	@Test // DATAES-519
	void existsShouldReturnFalseIfNotExists() {

		bulkIndex(new SampleEntity("id-one", "message"), //
				new SampleEntity("id-two", "test message"), //
				new SampleEntity("id-three", "test test")) //
						.block();

		repository.existsAllByMessage("these days") //
				.as(StepVerifier::create) //
				.expectNext(false) //
				.verifyComplete();
	}

	@Test // DATAES-519
	void deleteByIdShouldCompleteIfNothingDeleted() {

		bulkIndex(new SampleEntity("id-one"), //
				new SampleEntity("id-two")) //
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

		SampleEntity toBeDeleted = new SampleEntity("id-two");
		bulkIndex(new SampleEntity("id-one"), toBeDeleted) //
				.block();

		repository.deleteById(toBeDeleted.getId()).as(StepVerifier::create).verifyComplete();

		assertThat(documentWithIdExistsInIndex(toBeDeleted.getId()).block()).isFalse();
	}

	@Test // DATAES-976
	void deleteAllByIdShouldDeleteEntry() {

		SampleEntity toBeDeleted = new SampleEntity("id-two");
		bulkIndex(new SampleEntity("id-one"), toBeDeleted) //
				.block();

		repository.deleteAllById(Collections.singletonList(toBeDeleted.getId())).as(StepVerifier::create).verifyComplete();

		assertThat(documentWithIdExistsInIndex(toBeDeleted.getId()).block()).isFalse();
	}

	@Test // DATAES-519
	void deleteShouldDeleteEntry() {

		SampleEntity toBeDeleted = new SampleEntity("id-two");
		bulkIndex(new SampleEntity("id-one"), toBeDeleted) //
				.block();

		repository.delete(toBeDeleted).as(StepVerifier::create).verifyComplete();

		assertThat(documentWithIdExistsInIndex(toBeDeleted.getId()).block()).isFalse();
	}

	@Test // DATAES-519
	void deleteAllShouldDeleteGivenEntries() {

		SampleEntity toBeDeleted = new SampleEntity("id-one");
		SampleEntity hangInThere = new SampleEntity("id-two");
		SampleEntity toBeDeleted2 = new SampleEntity("id-three");

		bulkIndex(toBeDeleted, hangInThere, toBeDeleted2) //
				.block();

		repository.deleteAll(Arrays.asList(toBeDeleted, toBeDeleted2)).as(StepVerifier::create).verifyComplete();

		assertThat(documentWithIdExistsInIndex(toBeDeleted.getId()).block()).isFalse();
		assertThat(documentWithIdExistsInIndex(toBeDeleted2.getId()).block()).isFalse();
		assertThat(documentWithIdExistsInIndex(hangInThere.getId()).block()).isTrue();
	}

	@Test // DATAES-519
	void deleteAllShouldDeleteAllEntries() {

		bulkIndex(new SampleEntity("id-one"), //
				new SampleEntity("id-two"), //
				new SampleEntity("id-three")) //
						.block();

		repository.deleteAll().as(StepVerifier::create).verifyComplete();

		repository.count() //
				.as(StepVerifier::create) //
				.expectNext(0L) //
				.verifyComplete();
	}

	@Test // DATAES-519
	void derivedFinderMethodShouldBeExecutedCorrectly() {

		bulkIndex(new SampleEntity("id-one", "message"), //
				new SampleEntity("id-two", "test message"), //
				new SampleEntity("id-three", "test test")) //
						.block();

		repository.findAllByMessageLike("test") //
				.as(StepVerifier::create) //
				.expectNextCount(2) //
				.verifyComplete();
	}

	@Test // DATAES-519
	void derivedFinderMethodShouldBeExecutedCorrectlyWhenGivenPublisher() {

		bulkIndex(new SampleEntity("id-one", "message"), //
				new SampleEntity("id-two", "test message"), //
				new SampleEntity("id-three", "test test")) //
						.block();

		repository.findAllByMessage(Mono.just("test")) //
				.as(StepVerifier::create) //
				.expectNextCount(2) //
				.verifyComplete();
	}

	@Test // DATAES-519
	void derivedFinderWithDerivedSortMethodShouldBeExecutedCorrectly() {

		bulkIndex(new SampleEntity("id-one", "test", 3), //
				new SampleEntity("id-two", "test test", 1), //
				new SampleEntity("id-three", "test test", 2)) //
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

		bulkIndex(new SampleEntity("id-one", "test", 3), //
				new SampleEntity("id-two", "test test", 1), //
				new SampleEntity("id-three", "test test", 2)) //
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

		bulkIndex(new SampleEntity("id-one", "test", 3), //
				new SampleEntity("id-two", "test test", 1), //
				new SampleEntity("id-three", "test test", 2)) //
						.block();

		repository.findAllByMessage("test", PageRequest.of(0, 2, Sort.by(Order.asc("rate")))) //
				.as(StepVerifier::create) //
				.consumeNextWith(it -> assertThat(it.getId()).isEqualTo("id-two")) //
				.consumeNextWith(it -> assertThat(it.getId()).isEqualTo("id-three")) //
				.verifyComplete();
	}

	@Test // DATAES-519
	void derivedFinderMethodReturningMonoShouldBeExecutedCorrectly() {

		bulkIndex(new SampleEntity("id-one", "message"), //
				new SampleEntity("id-two", "test message"), //
				new SampleEntity("id-three", "test test")) //
						.block();

		repository.findFirstByMessageLike("test") //
				.as(StepVerifier::create) //
				.expectNextCount(1) //
				.verifyComplete();
	}

	@Test // DATAES-519
	void annotatedFinderMethodShouldBeExecutedCorrectly() {

		int count = 30;
		SampleEntity[] sampleEntities = IntStream.range(1, count + 1)
				.mapToObj(i -> new SampleEntity("id-" + i, "test " + i)).collect(Collectors.toList())
				.toArray(new SampleEntity[count]);

		bulkIndex(sampleEntities).block();

		repository.findAllViaAnnotatedQueryByMessageLike("test") //
				.as(StepVerifier::create) //
				.expectNextCount(count) //
				.verifyComplete();
	}

	@Test // #1917
	void annotatedFinderMethodPagedShouldBeExecutedCorrectly() {

		int count = 30;
		SampleEntity[] sampleEntities = IntStream.range(1, count + 1)
				.mapToObj(i -> new SampleEntity("id-" + i, "test " + i)).collect(Collectors.toList())
				.toArray(new SampleEntity[count]);

		bulkIndex(sampleEntities).block();

		repository.findAllViaAnnotatedQueryByMessageLikePaged("test", PageRequest.of(0, 20)) //
				.as(StepVerifier::create) //
				.expectNextCount(20) //
				.verifyComplete();
		repository.findAllViaAnnotatedQueryByMessageLikePaged("test", PageRequest.of(1, 20)) //
				.as(StepVerifier::create) //
				.expectNextCount(10) //
				.verifyComplete();
	}

	@Test // DATAES-519
	void derivedDeleteMethodShouldBeExecutedCorrectly() {

		bulkIndex(new SampleEntity("id-one", "message"), //
				new SampleEntity("id-two", "test message"), //
				new SampleEntity("id-three", "test test")) //
						.block();

		repository.deleteAllByMessage("message") //
				.as(StepVerifier::create) //
				.expectNext(2L) //
				.verifyComplete();

		assertThat(documentWithIdExistsInIndex("id-one").block()).isFalse();
		assertThat(documentWithIdExistsInIndex("id-two").block()).isFalse();
		assertThat(documentWithIdExistsInIndex("id-three").block()).isTrue();
	}

	@Test // #2135
	void FluxOfSearchHitForArrayQuery() {
		bulkIndex(new SampleEntity("id-one", "message1"), //
				new SampleEntity("id-two", "message2"), //
				new SampleEntity("id-three", "message3")) //
						.block();

		repository.findAllViaAnnotatedQueryByMessageIn(List.of("message1", "message3")) //
				.as(StepVerifier::create) //
				.consumeNextWith(it -> assertThat(it.getId()).isEqualTo("id-one")) //
				.consumeNextWith(it -> assertThat(it.getId()).isEqualTo("id-three")) //
				.verifyComplete();

	}

	@Test // #2135
	void FluxOfSearchHitForIntegerArrayQuery() {
		bulkIndex(new SampleEntity("id-one", "test", 3), //
				new SampleEntity("id-two", "test test", 1), //
				new SampleEntity("id-three", "test test", 2)) //
						.block();

		repository.findAllViaAnnotatedQueryByRatesIn(List.of(2, 3)) //
				.as(StepVerifier::create) //
				.consumeNextWith(it -> assertThat(it.getId()).isEqualTo("id-one")) //
				.consumeNextWith(it -> assertThat(it.getId()).isEqualTo("id-three")) //
				.verifyComplete();

	}

	@Test // #2135
	void FluxOfSearchHitForStringAndIntegerArrayQuery() {
		bulkIndex(new SampleEntity("id-one", "message1", 1), //
				new SampleEntity("id-two", "message2", 2), //
				new SampleEntity("id-three", "message3", 3), //
				new SampleEntity("id-four", "message4", 4), //
				new SampleEntity("id-five", "message5", 5)) //
						.block();

		repository.findAllViaAnnotatedQueryByMessageInAndRatesIn(List.of("message5", "message3"), List.of(2, 3)) //
				.as(StepVerifier::create) //
				.consumeNextWith(it -> assertThat(it.getId()).isEqualTo("id-three")) //
				.verifyComplete();

	}

	@Test // #2146
	@DisplayName("should use sourceIncludes from annotation")
	void shouldUseSourceIncludesFromAnnotation() {

		var entity = new SampleEntity();
		entity.setId("42");
		entity.setMessage("message");
		entity.setCustomFieldNameMessage("customFieldNameMessage");
		entity.setType("type");
		entity.setKeyword("keyword");
		repository.save(entity).block();

		repository.searchWithSourceFilterIncludesAnnotation() //
				.as(StepVerifier::create) //
				.consumeNextWith(foundEntity -> { //
					assertThat(foundEntity.getMessage()).isEqualTo("message"); //
					assertThat(foundEntity.getCustomFieldNameMessage()).isEqualTo("customFieldNameMessage"); //
					assertThat(foundEntity.getType()).isNull(); //
					assertThat(foundEntity.getKeyword()).isNull(); //
				}) //
				.verifyComplete();
	}

	@Test // #2146
	@DisplayName("should use sourceIncludes from parameter")
	void shouldUseSourceIncludesFromParameter() {

		var entity = new SampleEntity();
		entity.setId("42");
		entity.setMessage("message");
		entity.setCustomFieldNameMessage("customFieldNameMessage");
		entity.setType("type");
		entity.setKeyword("keyword");
		repository.save(entity).block();

		repository.searchBy(List.of("message", "customFieldNameMessage")) //
				.as(StepVerifier::create) //
				.consumeNextWith(foundEntity -> { //
					assertThat(foundEntity.getMessage()).isEqualTo("message"); //
					assertThat(foundEntity.getCustomFieldNameMessage()).isEqualTo("customFieldNameMessage"); //
					assertThat(foundEntity.getType()).isNull(); //
					assertThat(foundEntity.getKeyword()).isNull(); //
				}) //
				.verifyComplete();
	}

	@Test // #2146
	@DisplayName("should use sourceExcludes from annotation")
	void shouldUseSourceExcludesFromAnnotation() {

		var entity = new SampleEntity();
		entity.setId("42");
		entity.setMessage("message");
		entity.setCustomFieldNameMessage("customFieldNameMessage");
		entity.setType("type");
		entity.setKeyword("keyword");
		repository.save(entity).block();

		repository.searchWithSourceFilterExcludesAnnotation() //
				.as(StepVerifier::create) //
				.consumeNextWith(foundEntity -> { //
					assertThat(foundEntity.getMessage()).isEqualTo("message"); //
					assertThat(foundEntity.getCustomFieldNameMessage()).isEqualTo("customFieldNameMessage"); //
					assertThat(foundEntity.getType()).isNull(); //
					assertThat(foundEntity.getKeyword()).isNull(); //
				}) //
				.verifyComplete();
	}

	@Test // #2146
	@DisplayName("should use source excludes from parameter")
	void shouldUseSourceExcludesFromParameter() {

		var entity = new SampleEntity();
		entity.setId("42");
		entity.setMessage("message");
		entity.setCustomFieldNameMessage("customFieldNameMessage");
		entity.setType("type");
		entity.setKeyword("keyword");
		repository.save(entity).block();

		repository.findBy(List.of("type", "keyword")) //
				.as(StepVerifier::create) //
				.consumeNextWith(foundEntity -> { //
					assertThat(foundEntity.getMessage()).isEqualTo("message"); //
					assertThat(foundEntity.getCustomFieldNameMessage()).isEqualTo("customFieldNameMessage"); //
					assertThat(foundEntity.getType()).isNull(); //
					assertThat(foundEntity.getKeyword()).isNull(); //
				}) //
				.verifyComplete();
	}

	Mono<Void> bulkIndex(SampleEntity... entities) {
		return operations.saveAll(Arrays.asList(entities), IndexCoordinates.of(indexNameProvider.indexName())).then();
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

		@Query("{ \"bool\" : { \"must\" : { \"term\" : { \"message\" : \"?0\" } } } }")
		Flux<SampleEntity> findAllViaAnnotatedQueryByMessageLikePaged(String message, Pageable pageable);

		Mono<SampleEntity> findFirstByMessageLike(String message);

		Mono<Long> countAllByMessage(String message);

		Mono<Boolean> existsAllByMessage(String message);

		Mono<Long> deleteAllByMessage(String message);

		@CountQuery(value = "{\"bool\": {\"must\": [{\"term\": {\"message\": \"?0\"}}]}}")
		Mono<Long> retrieveCountByText(String message);

		@Query("{ \"terms\": { \"message\": ?0 } }")
		Flux<SampleEntity> findAllViaAnnotatedQueryByMessageIn(List<String> messages);

		@Query("{ \"terms\": { \"rate\": ?0 } }")
		Flux<SampleEntity> findAllViaAnnotatedQueryByRatesIn(List<Integer> rates);

		@Query("{\"bool\": {\"must\": [{ \"terms\": { \"message\": ?0 } }, { \"terms\": { \"rate\": ?1 } }] } }")
		Flux<SampleEntity> findAllViaAnnotatedQueryByMessageInAndRatesIn(List<String> messages, List<Integer> rates);

		@Query(query = """
				{
					"match_all": {}
				}
				""")
		@SourceFilters(includes = { "message", "customFieldNameMessage" })
		Flux<SampleEntity> searchWithSourceFilterIncludesAnnotation();

		@SourceFilters(includes = "?0")
		Flux<SampleEntity> searchBy(Collection<String> sourceIncludes);

		@Query("""
				{
					"match_all": {}
				}
				""")
		@SourceFilters(excludes = { "type", "keyword" })
		Flux<SampleEntity> searchWithSourceFilterExcludesAnnotation();

		@SourceFilters(excludes = "?0")
		Flux<SampleEntity> findBy(Collection<String> sourceExcludes);
	}

	@Document(indexName = "#{@indexNameProvider.indexName()}")
	static class SampleEntity {
		@Nullable
		@Id private String id;
		@Nullable
		@Field(type = FieldType.Text, store = true, fielddata = true) private String type;
		@Nullable
		@Field(type = FieldType.Text, store = true, fielddata = true) private String message;
		@Nullable
		@Field(type = FieldType.Keyword) private String keyword;

		@Nullable private int rate;
		@Nullable private boolean available;
		@Nullable
		@Version private Long version;
		@Field(name = "custom_field_name", type = FieldType.Text)
		@Nullable private String customFieldNameMessage;

		public SampleEntity() {}

		public SampleEntity(@Nullable String id) {
			this.id = id;
		}

		public SampleEntity(@Nullable String id, @Nullable String message) {
			this.id = id;
			this.message = message;
		}

		public SampleEntity(@Nullable String id, @Nullable String message, int rate) {
			this.id = id;
			this.message = message;
			this.rate = rate;
		}

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getType() {
			return type;
		}

		public void setType(@Nullable String type) {
			this.type = type;
		}

		@Nullable
		public String getKeyword() {
			return keyword;
		}

		public void setKeyword(@Nullable String keyword) {
			this.keyword = keyword;
		}

		@Nullable
		public String getMessage() {
			return message;
		}

		public void setMessage(@Nullable String message) {
			this.message = message;
		}

		public int getRate() {
			return rate;
		}

		public void setRate(int rate) {
			this.rate = rate;
		}

		public boolean isAvailable() {
			return available;
		}

		public void setAvailable(boolean available) {
			this.available = available;
		}

		@Nullable
		public java.lang.Long getVersion() {
			return version;
		}

		public void setVersion(@Nullable java.lang.Long version) {
			this.version = version;
		}

		@Nullable
		public String getCustomFieldNameMessage() {
			return customFieldNameMessage;
		}

		public void setCustomFieldNameMessage(@Nullable String customFieldNameMessage) {
			this.customFieldNameMessage = customFieldNameMessage;
		}
	}
}
