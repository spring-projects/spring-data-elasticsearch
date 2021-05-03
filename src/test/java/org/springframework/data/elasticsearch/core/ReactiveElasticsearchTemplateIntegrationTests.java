/*
 * Copyright 2018-2021 the original author or authors.
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

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.lang.Boolean;
import java.lang.Long;
import java.lang.Object;
import java.net.ConnectException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.index.query.IdsQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.json.JSONException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
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
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Mapping;
import org.springframework.data.elasticsearch.annotations.Setting;
import org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient;
import org.springframework.data.elasticsearch.core.document.Explanation;
import org.springframework.data.elasticsearch.core.index.AliasAction;
import org.springframework.data.elasticsearch.core.index.AliasActionParameters;
import org.springframework.data.elasticsearch.core.index.AliasActions;
import org.springframework.data.elasticsearch.core.index.AliasData;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.*;
import org.springframework.data.elasticsearch.junit.jupiter.ReactiveElasticsearchRestTemplateConfiguration;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.lang.Nullable;
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
 * @author George Popides
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

		template.save(sampleEntity) //
				.then(indexOperations.refresh()) //
				.block();

		template
				.search(new CriteriaQuery(Criteria.where("message").is(sampleEntity.getMessage())), SampleEntity.class,
						IndexCoordinates.of(DEFAULT_INDEX)) //
				.as(StepVerifier::create) //
				.expectNextCount(1) //
				.verifyComplete();
	}

	@Test // DATAES-504
	public void insertWithAutogeneratedIdShouldUpdateEntityId() {

		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setMessage("wohoo");

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

		template.exists("foo", IndexCoordinates.of("no-such-index")) //
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
				.consumeNextWith(byQueryResponse -> {
					assertThat(byQueryResponse.getDeleted()).isEqualTo(0L);
				}).verifyComplete();
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
				.map(ByQueryResponse::getDeleted) //
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
				.map(ByQueryResponse::getDeleted) //
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
				.map(ByQueryResponse::getDeleted) //
				.as(StepVerifier::create) //
				.expectNext(2L) //
				.verifyComplete();
	}

	@Test // DATAES-504
	public void deleteByQueryShouldReturnZeroIfNothingDeleted() {

		index(randomEntity("test message"));

		CriteriaQuery query = new CriteriaQuery(new Criteria("message").contains("luke"));

		template.delete(query, SampleEntity.class) //
				.map(ByQueryResponse::getDeleted) //
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

	@Test // DATAES-623, #1678
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
				.map(MultiGetItem::getItem).as(StepVerifier::create) //
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

	@Test // DATAES-623. #1678
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
				.map(MultiGetItem::getItem) //
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
				.then(indexOperations.refresh()) //
				.block();

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

	@Test // DATAES-799, #1678
	void multiGetShouldReturnSeqNoPrimaryTerm() {
		OptimisticEntity original = new OptimisticEntity();
		original.setMessage("It's fine");
		OptimisticEntity saved = template.save(original).block();

		template
				.multiGet(multiGetQueryForOne(saved.getId()), OptimisticEntity.class,
						template.getIndexCoordinatesFor(OptimisticEntity.class)) //
				.map(MultiGetItem::getItem) //
				.as(StepVerifier::create) //
				.assertNext(this::assertThatSeqNoPrimaryTermIsFilled).verifyComplete();
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

	@Test // #1665
	@DisplayName("should be able to process date-math-index names")
	void shouldBeAbleToProcessDateMathIndexNames() {

		String indexName = "foo-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy.MM"));
		String dateMathIndexName = "<foo-{now/M{yyyy.MM}}>";

		template.indexOps(IndexCoordinates.of(dateMathIndexName)) //
				.create() //
				.as(StepVerifier::create) //
				.expectNext(true) //
				.verifyComplete(); //

		template.indexOps(IndexCoordinates.of(indexName)) //
				.exists() //
				.as(StepVerifier::create) //
				.expectNext(true) //
				.verifyComplete(); //

		template.indexOps(IndexCoordinates.of(dateMathIndexName)) //
				.delete() //
				.as(StepVerifier::create) //
				.expectNext(true) //
				.verifyComplete(); //
	}

	@Test // #725
	@DisplayName("should not return explanation when not requested")
	void shouldNotReturnExplanationWhenNotRequested() {

		ElasticsearchTemplateTests.SampleEntity entity = ElasticsearchTemplateTests.SampleEntity.builder().id("42")
				.message("a message with text").build();
		template.save(entity).as(StepVerifier::create).expectNextCount(1).verifyComplete();

		Criteria criteria = new Criteria("message").contains("with");
		CriteriaQuery query = new CriteriaQuery(criteria);

		template.search(query, ElasticsearchTemplateTests.SampleEntity.class).as(StepVerifier::create)
				.consumeNextWith(searchHit -> {
					Explanation explanation = searchHit.getExplanation();
					assertThat(explanation).isNull();
				}).verifyComplete();
	}

	@Test // #725
	@DisplayName("should return explanation when requested")
	void shouldReturnExplanationWhenRequested() {

		ElasticsearchTemplateTests.SampleEntity entity = ElasticsearchTemplateTests.SampleEntity.builder().id("42")
				.message("a message with text").build();
		template.save(entity).as(StepVerifier::create).expectNextCount(1).verifyComplete();

		Criteria criteria = new Criteria("message").contains("with");
		CriteriaQuery query = new CriteriaQuery(criteria);
		query.setExplain(true);

		template.search(query, ElasticsearchTemplateTests.SampleEntity.class).as(StepVerifier::create)
				.consumeNextWith(searchHit -> {
					Explanation explanation = searchHit.getExplanation();
					assertThat(explanation).isNotNull();
				}).verifyComplete();
	}

	@Test // #1646, #1718
	@DisplayName("should return a list of info for specific index")
	void shouldReturnInformationListOfAllIndices() {
		String indexName = "test-index-reactive-information-list";
		String aliasName = "testindexinformationindex";
		ReactiveIndexOperations indexOps = template.indexOps(EntityWithSettingsAndMappingsReactive.class);

		indexOps.createWithMapping().block();

		AliasActionParameters parameters = AliasActionParameters.builder().withAliases(aliasName).withIndices(indexName)
				.withIsHidden(false).withIsWriteIndex(false).withRouting("indexrouting").withSearchRouting("searchrouting")
				.build();
		indexOps.alias(new AliasActions(new AliasAction.Add(parameters))).block();

		indexOps.getInformation().as(StepVerifier::create).consumeNextWith(indexInformation -> {
			assertThat(indexInformation.getName()).isEqualTo(indexName);
			assertThat(indexInformation.getSettings().get("index.number_of_shards")).isEqualTo("1");
			assertThat(indexInformation.getSettings().get("index.number_of_replicas")).isEqualTo("0");
			assertThat(indexInformation.getSettings().get("index.analysis.analyzer.emailAnalyzer.type")).isEqualTo("custom");
			assertThat(indexInformation.getAliases()).hasSize(1);

			AliasData aliasData = indexInformation.getAliases().get(0);

			assertThat(aliasData.getAlias()).isEqualTo(aliasName);
			assertThat(aliasData.isHidden()).isEqualTo(false);
			assertThat(aliasData.isWriteIndex()).isEqualTo(false);
			assertThat(aliasData.getIndexRouting()).isEqualTo("indexrouting");
			assertThat(aliasData.getSearchRouting()).isEqualTo("searchrouting");

			String expectedMappings = "{\"properties\":{\"email\":{\"type\":\"text\",\"analyzer\":\"emailAnalyzer\"}}}";
			try {
				JSONAssert.assertEquals(expectedMappings, indexInformation.getMapping().toJson(), false);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}).verifyComplete();
	}

	@Test // #1800
	@DisplayName("should work with immutable classes")
	void shouldWorkWithImmutableClasses() {

		ImmutableEntity entity = new ImmutableEntity(null, "some text", null);
		AtomicReference<ImmutableEntity> savedEntity = new AtomicReference<>();

		template.save(entity).as(StepVerifier::create).consumeNextWith(saved -> {
			assertThat(saved).isNotNull();
			savedEntity.set(saved);
			assertThat(saved.getId()).isNotEmpty();
			SeqNoPrimaryTerm seqNoPrimaryTerm = saved.getSeqNoPrimaryTerm();
			assertThat(seqNoPrimaryTerm).isNotNull();
		}).verifyComplete();

		template.get(savedEntity.get().getId(), ImmutableEntity.class).as(StepVerifier::create)
				.consumeNextWith(retrieved -> {
					assertThat(retrieved).isEqualTo(savedEntity.get());
				}).verifyComplete();
	}
	// endregion

	// region Helper functions
	private SampleEntity randomEntity(String message) {

		SampleEntity entity = new SampleEntity();
		entity.setId(UUID.randomUUID().toString());
		entity.setMessage(StringUtils.hasText(message) ? message : "test message");
		entity.setVersion(System.currentTimeMillis());
		return entity;
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
	@Document(indexName = "marvel")
	static class Person {
		@Nullable private @Id String id;
		@Nullable private String name;
		@Nullable private int age;

		public Person() {}

		public Person(String name, int age) {
			this.name = name;
			this.age = age;
		}

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getName() {
			return name;
		}

		public void setName(@Nullable String name) {
			this.name = name;
		}

		public int getAge() {
			return age;
		}

		public void setAge(int age) {
			this.age = age;
		}
	}

	static class Message {
		@Nullable String message;

		public Message(String message) {
			this.message = message;
		}

		@Nullable
		public String getMessage() {
			return message;
		}

		public void setMessage(@Nullable String message) {
			this.message = message;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;

			Message message1 = (Message) o;

			return message != null ? message.equals(message1.message) : message1.message == null;
		}

		@Override
		public int hashCode() {
			return message != null ? message.hashCode() : 0;
		}
	}

	@Document(indexName = DEFAULT_INDEX)
	static class SampleEntity {
		@Nullable @Id private String id;
		@Nullable @Field(type = Text, store = true, fielddata = true) private String message;
		@Nullable private int rate;
		@Nullable @Version private Long version;

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

		public int getRate() {
			return rate;
		}

		public void setRate(int rate) {
			this.rate = rate;
		}

		@Nullable
		public java.lang.Long getVersion() {
			return version;
		}

		public void setVersion(@Nullable java.lang.Long version) {
			this.version = version;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;

			SampleEntity that = (SampleEntity) o;

			if (rate != that.rate)
				return false;
			if (id != null ? !id.equals(that.id) : that.id != null)
				return false;
			if (message != null ? !message.equals(that.message) : that.message != null)
				return false;
			return version != null ? version.equals(that.version) : that.version == null;
		}

		@Override
		public int hashCode() {
			int result = id != null ? id.hashCode() : 0;
			result = 31 * result + (message != null ? message.hashCode() : 0);
			result = 31 * result + rate;
			result = 31 * result + (version != null ? version.hashCode() : 0);
			return result;
		}
	}

	@Document(indexName = "test-index-reactive-optimistic-entity-template")
	static class OptimisticEntity {
		@Nullable @Id private String id;
		@Nullable private String message;
		@Nullable private SeqNoPrimaryTerm seqNoPrimaryTerm;

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

		@Nullable
		public SeqNoPrimaryTerm getSeqNoPrimaryTerm() {
			return seqNoPrimaryTerm;
		}

		public void setSeqNoPrimaryTerm(@Nullable SeqNoPrimaryTerm seqNoPrimaryTerm) {
			this.seqNoPrimaryTerm = seqNoPrimaryTerm;
		}
	}

	@Document(indexName = "test-index-reactive-optimistic-and-versioned-entity-template")
	static class OptimisticAndVersionedEntity {
		@Nullable @Id private String id;
		@Nullable private String message;
		@Nullable private SeqNoPrimaryTerm seqNoPrimaryTerm;
		@Nullable @Version private Long version;

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

		@Nullable
		public SeqNoPrimaryTerm getSeqNoPrimaryTerm() {
			return seqNoPrimaryTerm;
		}

		public void setSeqNoPrimaryTerm(@Nullable SeqNoPrimaryTerm seqNoPrimaryTerm) {
			this.seqNoPrimaryTerm = seqNoPrimaryTerm;
		}

		@Nullable
		public java.lang.Long getVersion() {
			return version;
		}

		public void setVersion(@Nullable java.lang.Long version) {
			this.version = version;
		}
	}

	@Document(indexName = "test-index-reactive-versioned-entity-template")
	static class VersionedEntity {
		@Nullable @Id private String id;
		@Nullable @Version private Long version;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public java.lang.Long getVersion() {
			return version;
		}

		public void setVersion(@Nullable java.lang.Long version) {
			this.version = version;
		}
	}

	@Document(indexName = "test-index-reactive-information-list", createIndex = false)
	@Setting(settingPath = "settings/test-settings.json")
	@Mapping(mappingPath = "mappings/test-mappings.json")
	private static class EntityWithSettingsAndMappingsReactive {
		@Nullable @Id String id;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}
	}

	@Document(indexName = "immutable-class")
	private static final class ImmutableEntity {
		@Id private final String id;
		@Field(type = FieldType.Text) private final String text;
		@Nullable private final SeqNoPrimaryTerm seqNoPrimaryTerm;

		public ImmutableEntity(@Nullable String id, String text, @Nullable SeqNoPrimaryTerm seqNoPrimaryTerm) {
			this.id = id;
			this.text = text;
			this.seqNoPrimaryTerm = seqNoPrimaryTerm;
		}

		public String getId() {
			return id;
		}

		public String getText() {
			return text;
		}

		@Nullable
		public SeqNoPrimaryTerm getSeqNoPrimaryTerm() {
			return seqNoPrimaryTerm;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;

			ImmutableEntity that = (ImmutableEntity) o;

			if (!id.equals(that.id))
				return false;
			if (!text.equals(that.text))
				return false;
			return seqNoPrimaryTerm != null ? seqNoPrimaryTerm.equals(that.seqNoPrimaryTerm) : that.seqNoPrimaryTerm == null;
		}

		@Override
		public int hashCode() {
			int result = id.hashCode();
			result = 31 * result + text.hashCode();
			result = 31 * result + (seqNoPrimaryTerm != null ? seqNoPrimaryTerm.hashCode() : 0);
			return result;
		}

		@Override
		public String toString() {
			return "ImmutableEntity{" + "id='" + id + '\'' + ", text='" + text + '\'' + ", seqNoPrimaryTerm="
					+ seqNoPrimaryTerm + '}';
		}
	}
	// endregion
}
