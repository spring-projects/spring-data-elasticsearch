/*
 * Copyright 2018-2019 the original author or authors.
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

import java.lang.Long;
import java.lang.Object;
import java.net.ConnectException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.TestUtils;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.Score;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.data.elasticsearch.junit.junit4.ElasticsearchVersion;
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
 */
@SpringIntegrationTest
public class ReactiveElasticsearchTemplateTests {

	static final String DEFAULT_INDEX = "reactive-template-test-index";
	static final String ALTERNATE_INDEX = "reactive-template-tests-alternate-index";

	private ElasticsearchRestTemplate restTemplate;
	private ReactiveElasticsearchTemplate template;

	@BeforeEach
	public void setUp() {

		deleteIndices();

		restTemplate = new ElasticsearchRestTemplate(TestUtils.restHighLevelClient());
		restTemplate.createIndex(SampleEntity.class);
		restTemplate.putMapping(SampleEntity.class);
		restTemplate.refresh(SampleEntity.class);

		template = new ReactiveElasticsearchTemplate(TestUtils.reactiveClient(), restTemplate.getElasticsearchConverter());
	}

	@AfterEach
	public void tearDown() {
		deleteIndices();
	}

	private void deleteIndices() {
		TestUtils.deleteIndex(DEFAULT_INDEX, ALTERNATE_INDEX, "rx-template-test-index-this", "rx-template-test-index-that");
	}

	@Test // DATAES-504
	public void executeShouldProvideResource() {

		Mono.from(template.execute(client -> client.ping())) //
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

		restTemplate.refresh(SampleEntity.class);

		SearchHits<SampleEntity> result = restTemplate.search(
				new CriteriaQuery(Criteria.where("message").is(sampleEntity.getMessage())), SampleEntity.class,
				IndexCoordinates.of(DEFAULT_INDEX));
		assertThat(result).hasSize(1);
	}

	@Test // DATAES-504
	public void insertWithAutogeneratedIdShouldUpdateEntityId() {

		SampleEntity sampleEntity = SampleEntity.builder().message("wohoo").build();

		template.save(sampleEntity) //
				.as(StepVerifier::create) //
				.consumeNextWith(it -> {

					assertThat(it.getId()).isNotNull();

					restTemplate.refresh(SampleEntity.class);
					assertThat(TestUtils.documentWithId(it.getId()).existsIn(DEFAULT_INDEX)).isTrue();
				}) //
				.verifyComplete();
	}

	@Test // DATAES-504
	public void insertWithExplicitIndexNameShouldOverwriteMetadata() {

		SampleEntity sampleEntity = randomEntity("in another index");
		IndexCoordinates alternateIndex = IndexCoordinates.of(ALTERNATE_INDEX);

		template.save(sampleEntity, alternateIndex) //
				.as(StepVerifier::create)//
				.expectNextCount(1)//
				.verifyComplete();

		restTemplate.refresh(IndexCoordinates.of(DEFAULT_INDEX));
		restTemplate.refresh(alternateIndex);

		assertThat(TestUtils.documentWithId(sampleEntity.getId()).existsIn(DEFAULT_INDEX)).isFalse();
		assertThat(TestUtils.documentWithId(sampleEntity.getId()).existsIn(ALTERNATE_INDEX)).isTrue();
	}

	@Test // DATAES-504
	public void insertShouldAcceptPlainMapStructureAsSource() {

		Map<String, Object> map = new LinkedHashMap<>(Collections.singletonMap("foo", "bar"));

		template.save(map, IndexCoordinates.of(ALTERNATE_INDEX).withTypes("singleton-map")) //
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

	@Test // DATAES-519
	public void findByIdShouldCompleteWhenIndexDoesNotExist() {

		template.findById("foo", SampleEntity.class, IndexCoordinates.of("no-such-index").withTypes("test-type")) //
				.as(StepVerifier::create) //
				.verifyComplete();
	}

	@Test // DATAES-504
	public void findByIdShouldReturnEntity() {

		SampleEntity sampleEntity = randomEntity("some message");
		index(sampleEntity);

		template.findById(sampleEntity.getId(), SampleEntity.class) //
				.as(StepVerifier::create) //
				.expectNext(sampleEntity) //
				.verifyComplete();
	}

	@Test // DATAES-504
	public void findByIdWhenIdIsAutogeneratedShouldHaveIdSetCorrectly() {

		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setMessage("some message");

		index(sampleEntity);

		assertThat(sampleEntity.getId()).isNotNull();

		template.findById(sampleEntity.getId(), SampleEntity.class) //
				.as(StepVerifier::create) //
				.consumeNextWith(it -> assertThat(it.getId()).isEqualTo(sampleEntity.getId())) //
				.verifyComplete();
	}

	@Test // DATAES-504
	public void findByIdShouldCompleteWhenNotingFound() {

		SampleEntity sampleEntity = randomEntity("some message");
		index(sampleEntity);

		template.findById("foo", SampleEntity.class) //
				.as(StepVerifier::create) //
				.verifyComplete();
	}

	@Test // DATAES-504
	public void findByIdShouldErrorForNullId() {
		assertThatThrownBy(() -> {
			template.findById(null, SampleEntity.class);
		}).isInstanceOf(IllegalArgumentException.class);
	}

	@Test // DATAES-504
	public void findByIdWithExplicitIndexNameShouldOverwriteMetadata() {

		SampleEntity sampleEntity = randomEntity("some message");

		IndexQuery indexQuery = getIndexQuery(sampleEntity);

		IndexCoordinates defaultIndex = IndexCoordinates.of(DEFAULT_INDEX).withTypes("test-type");
		IndexCoordinates alternateIndex = IndexCoordinates.of(ALTERNATE_INDEX).withTypes("test-type");

		restTemplate.index(indexQuery, alternateIndex);
		restTemplate.refresh(SampleEntity.class);

		restTemplate.refresh(defaultIndex);
		restTemplate.refresh(alternateIndex);

		template.findById(sampleEntity.getId(), SampleEntity.class, defaultIndex) //
				.as(StepVerifier::create) //
				.verifyComplete();

		template.findById(sampleEntity.getId(), SampleEntity.class, alternateIndex) //
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

	@Test // DATAES-519
	public void searchShouldCompleteWhenIndexDoesNotExist() {

		template
				.search(new CriteriaQuery(Criteria.where("message").is("some message")), SampleEntity.class,
						IndexCoordinates.of("no-such-index")) //
				.as(StepVerifier::create) //
				.verifyComplete();
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
				.expectNext(shouldMatch) //
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

	@Test // DATAES-595
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
				.expectError(ElasticsearchStatusException.class).verify();
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

		List<SampleEntity> source = IntStream.range(0, 100).mapToObj(it -> randomEntity("entity - " + it))
				.collect(Collectors.toList());

		index(source.toArray(new SampleEntity[0]));

		CriteriaQuery query = new CriteriaQuery(new Criteria("message").contains("entity")) //
				.addSort(Sort.by("message"))//
				.setPageable(PageRequest.of(0, 20));

		template.search(query, SampleEntity.class).as(StepVerifier::create) //
				.expectNextCount(20) //
				.verifyComplete();
	}

	@Test // DATAES-518
	public void findWithoutPagingShouldReadAll() {

		List<SampleEntity> source = IntStream.range(0, 100).mapToObj(it -> randomEntity("entity - " + it))
				.collect(Collectors.toList());

		index(source.toArray(new SampleEntity[0]));

		CriteriaQuery query = new CriteriaQuery(new Criteria("message").contains("entity")) //
				.addSort(Sort.by("message"))//
				.setPageable(Pageable.unpaged());

		template.search(query, SampleEntity.class).as(StepVerifier::create) //
				.expectNextCount(100) //
				.verifyComplete();
	}

	@Test // DATAES-519
	public void countShouldReturnZeroWhenIndexDoesNotExist() {

		template.count(SampleEntity.class) //
				.as(StepVerifier::create) //
				.expectNext(0L) //
				.verifyComplete();
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

	@Test // DATAES-519
	public void deleteByIdShouldCompleteWhenIndexDoesNotExist() {

		template.deleteById("does-not-exists", SampleEntity.class, IndexCoordinates.of("no-such-index")) //
				.as(StepVerifier::create)//
				.verifyComplete();
	}

	@Test // DATAES-504
	public void deleteByIdShouldRemoveExistingDocumentById() {

		SampleEntity sampleEntity = randomEntity("test message");
		index(sampleEntity);

		template.deleteById(sampleEntity.getId(), SampleEntity.class) //
				.as(StepVerifier::create)//
				.expectNext(sampleEntity.getId()) //
				.verifyComplete();
	}

	@Test // DATAES-504
	public void deleteShouldRemoveExistingDocumentByIdUsingIndexName() {

		SampleEntity sampleEntity = randomEntity("test message");
		index(sampleEntity);

		template.deleteById(sampleEntity.getId(), IndexCoordinates.of(DEFAULT_INDEX).withTypes("test-type")) //
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
	public void deleteByIdShouldCompleteWhenNothingDeleted() {

		SampleEntity sampleEntity = randomEntity("test message");

		template.delete(sampleEntity) //
				.as(StepVerifier::create)//
				.verifyComplete();
	}

	@Test // DATAES-519
	@ElasticsearchVersion(asOf = "6.5.0")
	public void deleteByQueryShouldReturnZeroWhenIndexDoesNotExist() {

		CriteriaQuery query = new CriteriaQuery(new Criteria("message").contains("test"));

		template.deleteBy(query, SampleEntity.class) //
				.as(StepVerifier::create) //
				.expectNext(0L) //
				.verifyComplete();
	}

	@Test // DATAES-547
	@ElasticsearchVersion(asOf = "6.5.0")
	public void shouldDeleteAcrossIndex() {

		String indexPrefix = "rx-template-test-index";
		IndexCoordinates thisIndex = IndexCoordinates.of(indexPrefix + "-this");
		IndexCoordinates thatIndex = IndexCoordinates.of(indexPrefix + "-that");

		template.save(randomEntity("test"), thisIndex) //
				.then(template.save(randomEntity("test"), thatIndex)) //
				.then() //
				.as(StepVerifier::create)//
				.verifyComplete();

		restTemplate.refresh(thisIndex);
		restTemplate.refresh(thatIndex);

		NativeSearchQuery searchQuery = new NativeSearchQueryBuilder() //
				.withQuery(termQuery("message", "test")) //
				.build();

		template.deleteBy(searchQuery, SampleEntity.class, IndexCoordinates.of(indexPrefix + '*')) //
				.as(StepVerifier::create) //
				.expectNext(2L) //
				.verifyComplete();

		TestUtils.deleteIndex(thisIndex.getIndexName(), thatIndex.getIndexName());
	}

	@Test // DATAES-547
	@ElasticsearchVersion(asOf = "6.5.0")
	public void shouldDeleteAcrossIndexWhenNoMatchingDataPresent() {

		String indexPrefix = "rx-template-test-index";
		IndexCoordinates thisIndex = IndexCoordinates.of(indexPrefix + "-this");
		IndexCoordinates thatIndex = IndexCoordinates.of(indexPrefix + "-that");

		template.save(randomEntity("positive"), thisIndex) //
				.then(template.save(randomEntity("positive"), thatIndex)) //
				.then() //
				.as(StepVerifier::create)//
				.verifyComplete();

		restTemplate.refresh(thisIndex);
		restTemplate.refresh(thatIndex);

		NativeSearchQuery searchQuery = new NativeSearchQueryBuilder() //
				.withQuery(termQuery("message", "negative")) //
				.build();

		template.deleteBy(searchQuery, SampleEntity.class, IndexCoordinates.of(indexPrefix + '*')) //
				.as(StepVerifier::create) //
				.expectNext(0L) //
				.verifyComplete();

		TestUtils.deleteIndex(thisIndex.getIndexName(), thatIndex.getIndexName());
	}

	@Test // DATAES-504
	@ElasticsearchVersion(asOf = "6.5.0")
	public void deleteByQueryShouldReturnNumberOfDeletedDocuments() {

		index(randomEntity("test message"), randomEntity("test test"), randomEntity("some message"));

		CriteriaQuery query = new CriteriaQuery(new Criteria("message").contains("test"));

		template.deleteBy(query, SampleEntity.class) //
				.as(StepVerifier::create) //
				.expectNext(2L) //
				.verifyComplete();
	}

	@Test // DATAES-504
	@ElasticsearchVersion(asOf = "6.5.0")
	public void deleteByQueryShouldReturnZeroIfNothingDeleted() {

		index(randomEntity("test message"));

		CriteriaQuery query = new CriteriaQuery(new Criteria("message").contains("luke"));

		template.deleteBy(query, SampleEntity.class) //
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

	@Data
	@Document(indexName = "marvel", type = "characters")
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

	// TODO: check field mapping !!!

	// --> JUST some helpers

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

		IndexCoordinates indexCoordinates = IndexCoordinates.of(DEFAULT_INDEX).withTypes("test-type");

		if (entities.length == 1) {
			restTemplate.index(getIndexQuery(entities[0]), indexCoordinates);
		} else {
			restTemplate.bulkIndex(getIndexQueries(entities), indexCoordinates);
		}

		restTemplate.refresh(SampleEntity.class);
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
	@Document(indexName = DEFAULT_INDEX, type = "test-type", shards = 1, replicas = 0, refreshInterval = "-1")
	static class SampleEntity {

		@Id private String id;
		@Field(type = Text, store = true, fielddata = true) private String message;
		private int rate;
		@Version private Long version;
		@Score private float score;
	}
}
