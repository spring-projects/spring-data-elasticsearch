/*
 * Copyright 2018-2020 the original author or authors.
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
package org.springframework.data.elasticsearch.core;

import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.*;
import static org.elasticsearch.index.query.QueryBuilders.*;
import static org.springframework.data.elasticsearch.annotations.FieldType.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.lang.Boolean;
import java.lang.Long;
import java.lang.Object;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.index.query.IdsQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.UncategorizedElasticsearchException;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.Score;
import org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.*;
import org.springframework.data.elasticsearch.junit.jupiter.ReactiveElasticsearchRestTemplateConfiguration;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.util.StringUtils;

/**
 * Integration tests for {@link ReactiveElasticsearchTemplate}.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @author Peter-Josef Meisch
 * @author Farid Azaza
 * @author Martin Choraine
 * @author Aleksei Arsenev
 * @author Russell Parry
 * @author Roman Puchkovskiy
 */
@SpringIntegrationTest
public class ReactiveElasticsearchTemplateIntegrationTests {

	@Configuration
	@Import({ ReactiveElasticsearchRestTemplateConfiguration.class })
	static class Config {}

	static final String DEFAULT_INDEX = "reactive-template-test-index";
	static final String ALTERNATE_INDEX = "reactive-template-tests-alternate-index";

	@Autowired private ReactiveElasticsearchTemplate template;
	private ReactiveIndexOperations indexOperations;

	// region Setup
	@BeforeEach
	public void setUp() {
		indexOperations = template.indexOps(SampleEntity.class);

		deleteIndices();

		indexOperations.create() //
				.then(indexOperations.putMapping(SampleEntity.class)) //
				.then(indexOperations.refresh()) //
				.block(); //
	}

	@AfterEach
	public void after() {
		deleteIndices();
	}

	private void deleteIndices() {
		template.indexOps(IndexCoordinates.of(DEFAULT_INDEX)).delete().block();
		template.indexOps(IndexCoordinates.of(ALTERNATE_INDEX)).delete().block();
		template.indexOps(IndexCoordinates.of("rx-template-test-index-this")).delete().block();
		template.indexOps(IndexCoordinates.of("rx-template-test-index-that")).delete().block();
		template.indexOps(IndexCoordinates.of("test-index-reactive-optimistic-entity-template")).delete().block();
		template.indexOps(IndexCoordinates.of("test-index-reactive-optimistic-and-versioned-entity-template")).delete()
				.block();
	}
	// endregion

	// region Tests
	@Test // DATAES-504
	public void executeShouldProvideResource() {

		Mono.from(template.execute(ReactiveElasticsearchClient::ping)) //
				.as(StepVerifier::create) //
				.expectNext(true) //
				.verifyComplete();
	}

	@Test // DATAES-504
	public void executeShouldConvertExceptions() {

		Mono.from(template.execute(client -> {
			throw new RuntimeException(new ConnectException("we're doomed"));
		})) //
				.as(StepVerifier::create) //
				.expectError(DataAccessResourceFailureException.class) //
				.verify();
	}

	@Test // DATAES-504
	public void insertWithIdShouldWork() {

		SampleEntity sampleEntity = randomEntity("foo bar");

		template.save(sampleEntity)//
				.as(StepVerifier::create)//
				.expectNextCount(1)//
				.verifyComplete();

		indexOperations.refresh();

		template
				.search(new CriteriaQuery(Criteria.where("message").is(sampleEntity.getMessage())), SampleEntity.class,
						IndexCoordinates.of(DEFAULT_INDEX)) //
				.as(StepVerifier::create) //
				.expectNextCount(1) //
				.verifyComplete();
	}

	@Test // DATAES-504
	public void insertWithAutogeneratedIdShouldUpdateEntityId() {

		SampleEntity sampleEntity = SampleEntity.builder().message("wohoo").build();

		template.save(sampleEntity) //
				.map(SampleEntity::getId) //
				.flatMap(id -> indexOperations.refresh().thenReturn(id)) //
				.flatMap(id -> documentWithIdExistsInIndex(id, DEFAULT_INDEX)).as(StepVerifier::create) //
				.expectNext(true) //
				.verifyComplete();
	}

	private Mono<Boolean> documentWithIdExistsInIndex(String id, String index) {
		return template.exists(id, IndexCoordinates.of(index));
	}

	@Test // DATAES-504
	public void insertWithExplicitIndexNameShouldOverwriteMetadata() {

		SampleEntity sampleEntity = randomEntity("in another index");
		IndexCoordinates alternateIndex = IndexCoordinates.of(ALTERNATE_INDEX);

		template.save(sampleEntity, alternateIndex) //
				.as(StepVerifier::create)//
				.expectNextCount(1)//
				.verifyComplete();

		template.indexOps(IndexCoordinates.of(DEFAULT_INDEX)).refresh().block();
		template.indexOps(alternateIndex).refresh().block();

		assertThat(documentWithIdExistsInIndex(sampleEntity.getId(), DEFAULT_INDEX).block()).isFalse();
		assertThat(documentWithIdExistsInIndex(sampleEntity.getId(), ALTERNATE_INDEX).block()).isTrue();
	}

	@Test // DATAES-504
	public void insertShouldAcceptPlainMapStructureAsSource() {

		Map<String, Object> map = new LinkedHashMap<>(Collections.singletonMap("foo", "bar"));

		template.save(map, IndexCoordinates.of(ALTERNATE_INDEX)) //
				.as(StepVerifier::create) //
				.consumeNextWith(actual -> {
					assertThat(map).containsKey("id");
				}).verifyComplete();
	}

	@Test // DATAES-504
	public void insertShouldErrorOnNullEntity() {
		assertThatThrownBy(() -> {
			template.save(null);
		}).isInstanceOf(IllegalArgumentException.class);
	}

	@Test // DATAES-519, DATAES-767, DATAES-822
	public void getByIdShouldErrorWhenIndexDoesNotExist() {

		template.get("foo", SampleEntity.class, IndexCoordinates.of("no-such-index")) //
				.as(StepVerifier::create) //
				.expectError(ElasticsearchStatusException.class);
	}

	@Test // DATAES-504
	public void getByIdShouldReturnEntity() {

		SampleEntity sampleEntity = randomEntity("some message");
		index(sampleEntity);

		template.get(sampleEntity.getId(), SampleEntity.class) //
				.as(StepVerifier::create) //
				.expectNext(sampleEntity) //
				.verifyComplete();
	}

	@Test // DATAES-504
	public void getByIdWhenIdIsAutogeneratedShouldHaveIdSetCorrectly() {

		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setMessage("some message");

		index(sampleEntity);

		assertThat(sampleEntity.getId()).isNotNull();

		template.get(sampleEntity.getId(), SampleEntity.class) //
				.as(StepVerifier::create) //
				.consumeNextWith(it -> assertThat(it.getId()).isEqualTo(sampleEntity.getId())) //
				.verifyComplete();
	}

	@Test // DATAES-504
	public void getByIdShouldCompleteWhenNotingFound() {

		SampleEntity sampleEntity = randomEntity("some message");
		index(sampleEntity);

		template.get("foo", SampleEntity.class) //
				.as(StepVerifier::create) //
				.verifyComplete();
	}

	@Test // DATAES-504
	public void getByIdShouldErrorForNullId() {
		assertThatThrownBy(() -> {
			template.get(null, SampleEntity.class);
		}).isInstanceOf(IllegalArgumentException.class);
	}

	@Test // DATAES-504
	public void getByIdWithExplicitIndexNameShouldOverwriteMetadata() {

		SampleEntity sampleEntity = randomEntity("some message");

		IndexCoordinates defaultIndex = IndexCoordinates.of(DEFAULT_INDEX);
		IndexCoordinates alternateIndex = IndexCoordinates.of(ALTERNATE_INDEX);

		template.save(sampleEntity, alternateIndex) //
				.then(indexOperations.refresh()) //
				.then(template.indexOps(defaultIndex).refresh()) //
				.then(template.indexOps(alternateIndex).refresh()) //
				.block();

		template.get(sampleEntity.getId(), SampleEntity.class, defaultIndex) //
				.as(StepVerifier::create) //
				.verifyComplete();

		template.get(sampleEntity.getId(), SampleEntity.class, alternateIndex) //
				.as(StepVerifier::create)//
				.expectNextCount(1) //
				.verifyComplete();
	}

	@Test // DATAES-519
	public void existsShouldReturnFalseWhenIndexDoesNotExist() {

		template.exists("foo", SampleEntity.class, IndexCoordinates.of("no-such-index")) //
				.as(StepVerifier::create) //
				.expectNext(false) //
				.verifyComplete();
	}

	@Test // DATAES-504
	public void existsShouldReturnTrueWhenFound() {

		SampleEntity sampleEntity = randomEntity("some message");
		index(sampleEntity);

		template.exists(sampleEntity.getId(), SampleEntity.class) //
				.as(StepVerifier::create) //
				.expectNext(true) //
				.verifyComplete();
	}

	@Test // DATAES-504
	public void existsShouldReturnFalseWhenNotFound() {

		SampleEntity sampleEntity = randomEntity("some message");
		index(sampleEntity);

		template.exists("foo", SampleEntity.class) //
				.as(StepVerifier::create) //
				.expectNext(false) //
				.verifyComplete();
	}

	@Test // DATAES-519, DATAES-767
	public void searchShouldCompleteWhenIndexDoesNotExist() {

		template
				.search(new CriteriaQuery(Criteria.where("message").is("some message")), SampleEntity.class,
						IndexCoordinates.of("no-such-index")) //
				.as(StepVerifier::create) //
				.expectError(ElasticsearchStatusException.class);
	}

	@Test // DATAES-504
	public void searchShouldApplyCriteria() {

		SampleEntity sampleEntity = randomEntity("some message");
		index(sampleEntity);

		CriteriaQuery criteriaQuery = new CriteriaQuery(Criteria.where("message").is("some message"));

		template.search(criteriaQuery, SampleEntity.class) //
				.map(SearchHit::getContent) //
				.as(StepVerifier::create) //
				.expectNext(sampleEntity) //
				.verifyComplete();
	}

	@Test // DATAES-504
	public void searchShouldReturnEmptyFluxIfNothingFound() {

		SampleEntity sampleEntity = randomEntity("some message");
		index(sampleEntity);

		CriteriaQuery criteriaQuery = new CriteriaQuery(Criteria.where("message").is("foo"));

		template.search(criteriaQuery, SampleEntity.class) //
				.as(StepVerifier::create) //
				.verifyComplete();
	}

	@Test // DATAES-504
	public void shouldAllowStringBasedQuery() {

		index(randomEntity("test message"), randomEntity("test test"), randomEntity("some message"));

		template.search(new StringQuery(matchAllQuery().toString()), SampleEntity.class) //
				.as(StepVerifier::create) //
				.expectNextCount(3) //
				.verifyComplete();
	}

	@Test // DATAES-504
	public void shouldExecuteGivenCriteriaQuery() {

		SampleEntity shouldMatch = randomEntity("test message");
		SampleEntity shouldNotMatch = randomEntity("the dog ate my homework");
		index(shouldMatch, shouldNotMatch);

		CriteriaQuery query = new CriteriaQuery(new Criteria("message").contains("test"));

		template.search(query, SampleEntity.class) //
				.map(SearchHit::getContent) //
				.as(StepVerifier::create) //
				.assertNext(next -> {
					assertThat(next.getMessage()).isEqualTo("test message");
				}) //
				.verifyComplete();
	}

	@Test // DATAES-504
	public void shouldReturnListForGivenCriteria() {

		SampleEntity sampleEntity1 = randomEntity("test message");
		SampleEntity sampleEntity2 = randomEntity("test test");
		SampleEntity sampleEntity3 = randomEntity("some message");

		index(sampleEntity1, sampleEntity2, sampleEntity3);

		CriteriaQuery query = new CriteriaQuery(
				new Criteria("message").contains("some").and("message").contains("message"));

		template.search(query, SampleEntity.class) //
				.map(SearchHit::getContent) //
				.as(StepVerifier::create) //
				.expectNext(sampleEntity3) //
				.verifyComplete();
	}

	@Test // DATAES-595
	public void shouldReturnListUsingLocalPreferenceForGivenCriteria() {

		SampleEntity sampleEntity1 = randomEntity("test message");
		SampleEntity sampleEntity2 = randomEntity("test test");
		SampleEntity sampleEntity3 = randomEntity("some message");

		index(sampleEntity1, sampleEntity2, sampleEntity3);

		CriteriaQuery queryWithValidPreference = new CriteriaQuery(
				new Criteria("message").contains("some").and("message").contains("message"));
		queryWithValidPreference.setPreference("_local");

		template.search(queryWithValidPreference, SampleEntity.class) //
				.map(SearchHit::getContent) //
				.as(StepVerifier::create) //
				.expectNext(sampleEntity3) //
				.verifyComplete();
	}

	@Test // DATAES-595, DATAES-767
	public void shouldThrowElasticsearchStatusExceptionWhenInvalidPreferenceForGivenCriteria() {

		SampleEntity sampleEntity1 = randomEntity("test message");
		SampleEntity sampleEntity2 = randomEntity("test test");
		SampleEntity sampleEntity3 = randomEntity("some message");

		index(sampleEntity1, sampleEntity2, sampleEntity3);

		CriteriaQuery queryWithInvalidPreference = new CriteriaQuery(
				new Criteria("message").contains("some").and("message").contains("message"));
		queryWithInvalidPreference.setPreference("_only_nodes:oops");

		template.search(queryWithInvalidPreference, SampleEntity.class) //
				.as(StepVerifier::create) //
				.expectError(UncategorizedElasticsearchException.class).verify();
	}

	@Test // DATAES-504
	public void shouldReturnProjectedTargetEntity() {

		SampleEntity sampleEntity1 = randomEntity("test message");
		SampleEntity sampleEntity2 = randomEntity("test test");
		SampleEntity sampleEntity3 = randomEntity("some message");

		index(sampleEntity1, sampleEntity2, sampleEntity3);

		CriteriaQuery query = new CriteriaQuery(
				new Criteria("message").contains("some").and("message").contains("message"));

		template.search(query, SampleEntity.class, Message.class) //
				.map(SearchHit::getContent) //
				.as(StepVerifier::create) //
				.expectNext(new Message(sampleEntity3.getMessage())) //
				.verifyComplete();
	}

	@Test // DATAES-518
	public void searchShouldApplyPagingCorrectly() {

		index(IntStream.range(0, 100).mapToObj(it -> randomEntity("entity - " + it)).toArray(SampleEntity[]::new));

		CriteriaQuery query = new CriteriaQuery(new Criteria("message").contains("entity")) //
				.addSort(Sort.by("message"))//
				.setPageable(PageRequest.of(0, 20));

		template.search(query, SampleEntity.class).as(StepVerifier::create) //
				.expectNextCount(20) //
				.verifyComplete();
	}

	@Test // DATAES-518
	public void findWithoutPagingShouldReadAll() {

		index(IntStream.range(0, 100).mapToObj(it -> randomEntity("entity - " + it)).toArray(SampleEntity[]::new));

		CriteriaQuery query = new CriteriaQuery(new Criteria("message").contains("entity")) //
				.addSort(Sort.by("message"))//
				.setPageable(Pageable.unpaged());

		template.search(query, SampleEntity.class).as(StepVerifier::create) //
				.expectNextCount(100) //
				.verifyComplete();
	}

	@Test // DATAES-567
	public void aggregateShouldReturnAggregations() {

		SampleEntity sampleEntity1 = randomEntity("some message");
		SampleEntity sampleEntity2 = randomEntity("some message");
		SampleEntity sampleEntity3 = randomEntity("other message");

		index(sampleEntity1, sampleEntity2, sampleEntity3);

		NativeSearchQuery query = new NativeSearchQueryBuilder().withQuery(matchAllQuery())
				.addAggregation(AggregationBuilders.terms("messages").field("message")).build();

		template.aggregate(query, SampleEntity.class) //
				.as(StepVerifier::create) //
				.consumeNextWith(aggregation -> {
					assertThat(aggregation.getName()).isEqualTo("messages");
					assertThat(aggregation instanceof ParsedStringTerms);
					ParsedStringTerms parsedStringTerms = (ParsedStringTerms) aggregation;
					assertThat(parsedStringTerms.getBuckets().size()).isEqualTo(3);
					assertThat(parsedStringTerms.getBucketByKey("message").getDocCount()).isEqualTo(3);
					assertThat(parsedStringTerms.getBucketByKey("some").getDocCount()).isEqualTo(2);
					assertThat(parsedStringTerms.getBucketByKey("other").getDocCount()).isEqualTo(1);
				}).verifyComplete();
	}

	@Test // DATAES-567, DATAES-767
	public void aggregateShouldErrorWhenIndexDoesNotExist() {
		template
				.aggregate(new CriteriaQuery(Criteria.where("message").is("some message")), SampleEntity.class,
						IndexCoordinates.of("no-such-index")) //
				.as(StepVerifier::create) //
				.expectError(ElasticsearchStatusException.class);
	}

	@Test // DATAES-519, DATAES-767
	public void countShouldReturnZeroWhenIndexDoesNotExist() {

		template.count(SampleEntity.class) //
				.as(StepVerifier::create) //
				.expectError(ElasticsearchStatusException.class);
	}

	@Test // DATAES-504
	public void countShouldReturnCountAllWhenGivenNoQuery() {

		index(randomEntity("test message"), randomEntity("test test"), randomEntity("some message"));

		template.count(SampleEntity.class) //
				.as(StepVerifier::create) //
				.expectNext(3L) //
				.verifyComplete();
	}

	@Test // DATAES-504
	public void countShouldReturnCountMatchingDocuments() {

		index(randomEntity("test message"), randomEntity("test test"), randomEntity("some message"));

		CriteriaQuery query = new CriteriaQuery(new Criteria("message").contains("test"));

		template.count(query, SampleEntity.class) //
				.as(StepVerifier::create) //
				.expectNext(2L) //
				.verifyComplete();
	}

	@Test // DATAES-519, DATAES-767
	public void deleteShouldErrorWhenIndexDoesNotExist() {

		template.delete("does-not-exists", IndexCoordinates.of("no-such-index")) //
				.as(StepVerifier::create)//
				.expectError(ElasticsearchStatusException.class);
	}

	@Test // DATAES-504
	public void deleteShouldRemoveExistingDocumentById() {

		SampleEntity sampleEntity = randomEntity("test message");
		index(sampleEntity);

		template.delete(sampleEntity.getId(), SampleEntity.class) //
				.as(StepVerifier::create)//
				.expectNext(sampleEntity.getId()) //
				.verifyComplete();
	}

	@Test // DATAES-504
	public void deleteShouldRemoveExistingDocumentByIdUsingIndexName() {

		SampleEntity sampleEntity = randomEntity("test message");
		index(sampleEntity);

		template.delete(sampleEntity.getId(), IndexCoordinates.of(DEFAULT_INDEX)) //
				.as(StepVerifier::create)//
				.expectNext(sampleEntity.getId()) //
				.verifyComplete();
	}

	@Test // DATAES-504
	public void deleteShouldRemoveExistingDocument() {

		SampleEntity sampleEntity = randomEntity("test message");
		index(sampleEntity);

		template.delete(sampleEntity) //
				.as(StepVerifier::create)//
				.expectNext(sampleEntity.getId()) //
				.verifyComplete();
	}

	@Test // DATAES-504
	public void deleteShouldCompleteWhenNothingDeleted() {

		SampleEntity sampleEntity = randomEntity("test message");

		template.delete(sampleEntity) //
				.as(StepVerifier::create)//
				.verifyComplete();
	}

	@Test // DATAES-519
	public void deleteByQueryShouldReturnZeroWhenIndexDoesNotExist() {

		CriteriaQuery query = new CriteriaQuery(new Criteria("message").contains("test"));

		template.delete(query, SampleEntity.class) //
				.as(StepVerifier::create) //
				.expectNext(0L) //
				.verifyComplete();
	}

	@Test // DATAES-547
	public void shouldDeleteAcrossIndex() {

		String indexPrefix = "rx-template-test-index";
		IndexCoordinates thisIndex = IndexCoordinates.of(indexPrefix + "-this");
		IndexCoordinates thatIndex = IndexCoordinates.of(indexPrefix + "-that");

		template.save(randomEntity("test"), thisIndex) //
				.then(template.save(randomEntity("test"), thatIndex)) //
				.then() //
				.as(StepVerifier::create)//
				.verifyComplete();

		template.indexOps(thisIndex).refresh().then(template.indexOps(thatIndex).refresh()).block();

		NativeSearchQuery searchQuery = new NativeSearchQueryBuilder() //
				.withQuery(termQuery("message", "test")) //
				.build();

		template.delete(searchQuery, SampleEntity.class, IndexCoordinates.of(indexPrefix + '*')) //
				.as(StepVerifier::create) //
				.expectNext(2L) //
				.verifyComplete();

		template.indexOps(thisIndex).delete().then(template.indexOps(thatIndex).delete()).block();
	}

	@Test // DATAES-547
	public void shouldDeleteAcrossIndexWhenNoMatchingDataPresent() {

		String indexPrefix = "rx-template-test-index";
		IndexCoordinates thisIndex = IndexCoordinates.of(indexPrefix + "-this");
		IndexCoordinates thatIndex = IndexCoordinates.of(indexPrefix + "-that");

		template.save(randomEntity("positive"), thisIndex) //
				.then(template.save(randomEntity("positive"), thatIndex)) //
				.then() //
				.as(StepVerifier::create)//
				.verifyComplete();

		template.indexOps(thisIndex).refresh().then(template.indexOps(thatIndex).refresh()).block();

		NativeSearchQuery searchQuery = new NativeSearchQueryBuilder() //
				.withQuery(termQuery("message", "negative")) //
				.build();

		template.delete(searchQuery, SampleEntity.class, IndexCoordinates.of(indexPrefix + '*')) //
				.as(StepVerifier::create) //
				.expectNext(0L) //
				.verifyComplete();

		template.indexOps(thisIndex).delete().then(template.indexOps(thatIndex).delete()).block();
	}

	@Test // DATAES-504
	public void deleteByQueryShouldReturnNumberOfDeletedDocuments() {

		index(randomEntity("test message"), randomEntity("test test"), randomEntity("some message"));

		CriteriaQuery query = new CriteriaQuery(new Criteria("message").contains("test"));

		template.delete(query, SampleEntity.class) //
				.as(StepVerifier::create) //
				.expectNext(2L) //
				.verifyComplete();
	}

	@Test // DATAES-504
	public void deleteByQueryShouldReturnZeroIfNothingDeleted() {

		index(randomEntity("test message"));

		CriteriaQuery query = new CriteriaQuery(new Criteria("message").contains("luke"));

		template.delete(query, SampleEntity.class) //
				.as(StepVerifier::create) //
				.expectNext(0L) //
				.verifyComplete();
	}

	@Test // DATAES-593
	public void shouldReturnDocumentWithCollapsedField() {

		SampleEntity entity1 = randomEntity("test message");
		entity1.setRate(1);
		SampleEntity entity2 = randomEntity("test another message");
		entity2.setRate(2);
		SampleEntity entity3 = randomEntity("test message again");
		entity3.setRate(1);
		index(entity1, entity2, entity3);

		NativeSearchQuery query = new NativeSearchQueryBuilder() //
				.withQuery(matchAllQuery()) //
				.withCollapseField("rate") //
				.withPageable(PageRequest.of(0, 25)) //
				.build();

		template.search(query, SampleEntity.class, IndexCoordinates.of(DEFAULT_INDEX)) //
				.as(StepVerifier::create) //
				.expectNextCount(2) //
				.verifyComplete();
	}

	@Test
	void shouldReturnSortFields() {
		SampleEntity entity = randomEntity("test message");
		entity.rate = 42;
		index(entity);

		NativeSearchQuery query = new NativeSearchQueryBuilder() //
				.withQuery(matchAllQuery()) //
				.withSort(new FieldSortBuilder("rate").order(SortOrder.DESC)) //
				.build();

		template.search(query, SampleEntity.class) //
				.as(StepVerifier::create) //
				.consumeNextWith(it -> {
					List<Object> sortValues = it.getSortValues();
					assertThat(sortValues).hasSize(1);
					assertThat(sortValues.get(0)).isEqualTo(42);
				}) //
				.verifyComplete();
	}

	@Test // DATAES-623
	public void shouldReturnObjectsForGivenIdsUsingMultiGet() {
		SampleEntity entity1 = randomEntity("test message 1");
		entity1.rate = 1;
		index(entity1);
		SampleEntity entity2 = randomEntity("test message 2");
		entity2.rate = 2;
		index(entity2);

		NativeSearchQuery query = new NativeSearchQueryBuilder() //
				.withIds(Arrays.asList(entity1.getId(), entity2.getId())) //
				.build();

		template.multiGet(query, SampleEntity.class, IndexCoordinates.of(DEFAULT_INDEX)) //
				.as(StepVerifier::create) //
				.expectNext(entity1, entity2) //
				.verifyComplete();
	}

	@Test // DATAES-623
	public void shouldReturnObjectsForGivenIdsUsingMultiGetWithFields() {
		SampleEntity entity1 = randomEntity("test message 1");
		entity1.rate = 1;
		index(entity1);
		SampleEntity entity2 = randomEntity("test message 2");
		entity2.rate = 2;
		index(entity2);

		NativeSearchQuery query = new NativeSearchQueryBuilder() //
				.withIds(Arrays.asList(entity1.getId(), entity2.getId())) //
				.withFields("message") //
				.build();

		template.multiGet(query, SampleEntity.class, IndexCoordinates.of(DEFAULT_INDEX)) //
				.as(StepVerifier::create) //
				.expectNextCount(2) //
				.verifyComplete();
	}

	@Test // DATAES-623
	public void shouldDoBulkUpdate() {
		SampleEntity entity1 = randomEntity("test message 1");
		entity1.rate = 1;
		index(entity1);
		SampleEntity entity2 = randomEntity("test message 2");
		entity2.rate = 2;
		index(entity2);

		org.springframework.data.elasticsearch.core.document.Document document1 = org.springframework.data.elasticsearch.core.document.Document
				.create();
		document1.put("message", "updated 1");
		UpdateQuery updateQuery1 = UpdateQuery.builder(entity1.getId()) //
				.withDocument(document1) //
				.build();

		org.springframework.data.elasticsearch.core.document.Document document2 = org.springframework.data.elasticsearch.core.document.Document
				.create();
		document2.put("message", "updated 2");
		UpdateQuery updateQuery2 = UpdateQuery.builder(entity2.getId()) //
				.withDocument(document2) //
				.build();

		List<UpdateQuery> queries = Arrays.asList(updateQuery1, updateQuery2);
		template.bulkUpdate(queries, IndexCoordinates.of(DEFAULT_INDEX)).block();

		NativeSearchQuery getQuery = new NativeSearchQueryBuilder() //
				.withIds(Arrays.asList(entity1.getId(), entity2.getId())) //
				.build();
		template.multiGet(getQuery, SampleEntity.class, IndexCoordinates.of(DEFAULT_INDEX)) //
				.as(StepVerifier::create) //
				.expectNextMatches(entity -> entity.getMessage().equals("updated 1")) //
				.expectNextMatches(entity -> entity.getMessage().equals("updated 2")) //
				.verifyComplete();
	}

	@Test // DATAES-623
	void shouldSaveAll() {
		SampleEntity entity1 = randomEntity("test message 1");
		entity1.rate = 1;
		SampleEntity entity2 = randomEntity("test message 2");
		entity2.rate = 2;

		template.saveAll(Mono.just(Arrays.asList(entity1, entity2)), IndexCoordinates.of(DEFAULT_INDEX)) //
				.as(StepVerifier::create) //
				.expectNext(entity1) //
				.expectNext(entity2) //
				.verifyComplete();
		indexOperations.refresh();

		NativeSearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery()).build();
		template.search(searchQuery, SampleEntity.class, IndexCoordinates.of(DEFAULT_INDEX)) //
				.as(StepVerifier::create) //
				.expectNextMatches(hit -> entity1.equals(hit.getContent()) || entity2.equals(hit.getContent())) //
				.expectNextMatches(hit -> entity1.equals(hit.getContent()) || entity2.equals(hit.getContent())) //
				.verifyComplete();
	}

	@Test // DATAES-753
	void shouldReturnEmptyFluxOnSaveAllWithEmptyInput() {
		template.saveAll(Collections.emptyList(), IndexCoordinates.of(DEFAULT_INDEX)) //
				.as(StepVerifier::create) //
				.verifyComplete();
	}

	@Test // DATAES-799
	void getShouldReturnSeqNoPrimaryTerm() {
		OptimisticEntity original = new OptimisticEntity();
		original.setMessage("It's fine");
		OptimisticEntity saved = template.save(original).block();

		template.get(saved.getId(), OptimisticEntity.class).as(StepVerifier::create)
				.assertNext(this::assertThatSeqNoPrimaryTermIsFilled).verifyComplete();
	}

	private void assertThatSeqNoPrimaryTermIsFilled(OptimisticEntity retrieved) {
		assertThat(retrieved.seqNoPrimaryTerm).isNotNull();
		assertThat(retrieved.seqNoPrimaryTerm.getSequenceNumber()).isNotNull();
		assertThat(retrieved.seqNoPrimaryTerm.getSequenceNumber()).isNotNegative();
		assertThat(retrieved.seqNoPrimaryTerm.getPrimaryTerm()).isNotNull();
		assertThat(retrieved.seqNoPrimaryTerm.getPrimaryTerm()).isPositive();
	}

	@Test // DATAES-799
	void multiGetShouldReturnSeqNoPrimaryTerm() {
		OptimisticEntity original = new OptimisticEntity();
		original.setMessage("It's fine");
		OptimisticEntity saved = template.save(original).block();

		template
				.multiGet(multiGetQueryForOne(saved.getId()), OptimisticEntity.class,
						template.getIndexCoordinatesFor(OptimisticEntity.class))
				.as(StepVerifier::create).assertNext(this::assertThatSeqNoPrimaryTermIsFilled).verifyComplete();
	}

	private Query multiGetQueryForOne(String id) {
		return new NativeSearchQueryBuilder().withIds(singletonList(id)).build();
	}

	@Test // DATAES-799
	void searchShouldReturnSeqNoPrimaryTerm() {
		OptimisticEntity original = new OptimisticEntity();
		original.setMessage("It's fine");
		OptimisticEntity saved = template.save(original).block();

		template.indexOps(OptimisticEntity.class).refresh().block();

		template
				.search(searchQueryForOne(saved.getId()), OptimisticEntity.class,
						template.getIndexCoordinatesFor(OptimisticEntity.class))
				.map(SearchHit::getContent).as(StepVerifier::create).assertNext(this::assertThatSeqNoPrimaryTermIsFilled)
				.verifyComplete();
	}

	private Query searchQueryForOne(String id) {
		return new NativeSearchQueryBuilder().withFilter(new IdsQueryBuilder().addIds(id)).build();
	}

	@Test // DATAES-799
	void shouldThrowOptimisticLockingFailureExceptionWhenConcurrentUpdateOccursOnEntityWithSeqNoPrimaryTermProperty() {
		OptimisticEntity original = new OptimisticEntity();
		original.setMessage("It's fine");
		OptimisticEntity saved = template.save(original).block();

		OptimisticEntity forEdit1 = template.get(saved.getId(), OptimisticEntity.class).block();
		OptimisticEntity forEdit2 = template.get(saved.getId(), OptimisticEntity.class).block();

		forEdit1.setMessage("It'll be ok");
		template.save(forEdit1).block();

		forEdit2.setMessage("It'll be great");
		template.save(forEdit2) //
				.as(StepVerifier::create) //
				.expectError(OptimisticLockingFailureException.class) //
				.verify();
	}

	@Test // DATAES-799
	void shouldThrowOptimisticLockingFailureExceptionWhenConcurrentUpdateOccursOnVersionedEntityWithSeqNoPrimaryTermProperty() {
		OptimisticAndVersionedEntity original = new OptimisticAndVersionedEntity();
		original.setMessage("It's fine");
		OptimisticAndVersionedEntity saved = template.save(original).block();

		OptimisticAndVersionedEntity forEdit1 = template.get(saved.getId(), OptimisticAndVersionedEntity.class).block();
		OptimisticAndVersionedEntity forEdit2 = template.get(saved.getId(), OptimisticAndVersionedEntity.class).block();

		forEdit1.setMessage("It'll be ok");
		template.save(forEdit1).block();

		forEdit2.setMessage("It'll be great");
		template.save(forEdit2).as(StepVerifier::create).expectError(OptimisticLockingFailureException.class).verify();
	}

	@Test // DATAES-799
	void shouldAllowFullReplaceOfEntityWithBothSeqNoPrimaryTermAndVersion() {
		OptimisticAndVersionedEntity original = new OptimisticAndVersionedEntity();
		original.setMessage("It's fine");
		OptimisticAndVersionedEntity saved = template.save(original).block();

		OptimisticAndVersionedEntity forEdit = template.get(saved.getId(), OptimisticAndVersionedEntity.class).block();

		forEdit.setMessage("It'll be ok");
		template.save(forEdit).as(StepVerifier::create).expectNextCount(1).verifyComplete();
	}

	@Test // DATAES-909
	void shouldDoUpdate() {
		SampleEntity entity = randomEntity("test message");
		entity.rate = 1;
		index(entity);

		org.springframework.data.elasticsearch.core.document.Document document = org.springframework.data.elasticsearch.core.document.Document
				.create();
		document.put("message", "updated");
		UpdateQuery updateQuery = UpdateQuery.builder(entity.getId()) //
				.withDocument(document) //
				.build();

		UpdateResponse updateResponse = template.update(updateQuery, IndexCoordinates.of(DEFAULT_INDEX)).block();
		assertThat(updateResponse).isNotNull();
		assertThat(updateResponse.getResult()).isEqualTo(UpdateResponse.Result.UPDATED);

		template.get(entity.getId(), SampleEntity.class, IndexCoordinates.of(DEFAULT_INDEX)) //
				.as(StepVerifier::create) //
				.expectNextMatches(foundEntity -> foundEntity.getMessage().equals("updated")) //
				.verifyComplete();
	}

	@Test // DATAES-908
	void shouldFillVersionOnSaveOne() {
		VersionedEntity saved = template.save(new VersionedEntity()).block();

		assertThat(saved.getVersion()).isNotNull();
	}

	@Test // DATAES-908
	void shouldFillVersionOnSaveAll() {
		VersionedEntity saved = template.saveAll(singletonList(new VersionedEntity()), VersionedEntity.class).blockLast();

		assertThat(saved.getVersion()).isNotNull();
	}

	@Test // DATAES-908
	void shouldFillSeqNoPrimaryTermOnSaveOne() {
		OptimisticEntity saved = template.save(new OptimisticEntity()).block();

		assertThatSeqNoPrimaryTermIsFilled(saved);
	}

	@Test // DATAES-908
	void shouldFillSeqNoPrimaryTermOnSaveAll() {
		OptimisticEntity saved = template.saveAll(singletonList(new OptimisticEntity()), OptimisticEntity.class)
				.blockLast();

		assertThatSeqNoPrimaryTermIsFilled(saved);
	}

	@Test // DATAES-796
	@DisplayName("should return Mono of SearchPage")
	void shouldReturnMonoOfSearchPage() {
		List<SampleEntity> entities = new ArrayList<>();
		for (int i = 1; i <= 10; i++) {
			entities.add(randomEntity("message " + i));
		}

		Query query = Query.findAll().setPageable(PageRequest.of(0, 5));

		template.saveAll(Mono.just(entities), SampleEntity.class).then(indexOperations.refresh()).block();

		Mono<SearchPage<SampleEntity>> searchPageMono = template.searchForPage(query, SampleEntity.class);

		searchPageMono.as(StepVerifier::create) //
				.consumeNextWith(searchPage -> {
					assertThat(searchPage.hasNext()).isTrue();
					SearchHits<SampleEntity> searchHits = searchPage.getSearchHits();
					assertThat(searchHits.getTotalHits()).isEqualTo(10);
					assertThat(searchHits.getSearchHits().size()).isEqualTo(5);
				}).verifyComplete();
	}
	// endregion

	// region Helper functions
	private SampleEntity randomEntity(String message) {

		return SampleEntity.builder() //
				.id(UUID.randomUUID().toString()) //
				.message(StringUtils.hasText(message) ? message : "test message") //
				.version(System.currentTimeMillis()).build();
	}

	private IndexQuery getIndexQuery(SampleEntity sampleEntity) {

		return new IndexQueryBuilder().withId(sampleEntity.getId()).withObject(sampleEntity)
				.withVersion(sampleEntity.getVersion()).build();
	}

	private List<IndexQuery> getIndexQueries(SampleEntity... sampleEntities) {
		return Arrays.stream(sampleEntities).map(this::getIndexQuery).collect(Collectors.toList());
	}

	private void index(SampleEntity... entities) {

		IndexCoordinates indexCoordinates = IndexCoordinates.of(DEFAULT_INDEX);

		if (entities.length == 1) {
			template.save(entities[0], indexCoordinates).then(indexOperations.refresh()).block();
		} else {
			template.saveAll(Mono.just(Arrays.asList(entities)), indexCoordinates).then(indexOperations.refresh()).block();
		}
	}
	// endregion

	// region Entities
	@Data
	@Document(indexName = "marvel")
	static class Person {

		private @Id String id;
		private String name;
		private int age;

		public Person() {}

		public Person(String name, int age) {

			this.name = name;
			this.age = age;
		}
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	static class Message {

		String message;
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	@EqualsAndHashCode(exclude = "score")
	@Document(indexName = DEFAULT_INDEX, replicas = 0, refreshInterval = "-1")
	static class SampleEntity {

		@Id private String id;
		@Field(type = Text, store = true, fielddata = true) private String message;
		private int rate;
		@Version private Long version;
		@Score private float score;
	}

	@Data
	@Document(indexName = "test-index-reactive-optimistic-entity-template")
	static class OptimisticEntity {
		@Id private String id;
		private String message;
		private SeqNoPrimaryTerm seqNoPrimaryTerm;
	}

	@Data
	@Document(indexName = "test-index-reactive-optimistic-and-versioned-entity-template")
	static class OptimisticAndVersionedEntity {
		@Id private String id;
		private String message;
		private SeqNoPrimaryTerm seqNoPrimaryTerm;
		@Version private Long version;
	}

	@Data
	@Document(indexName = "test-index-reactive-versioned-entity-template")
	static class VersionedEntity {
		@Id private String id;
		@Version private Long version;
	}
	// endregion
}
