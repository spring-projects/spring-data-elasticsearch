/*
 * Copyright 2013-2019 the original author or authors.
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
package org.springframework.data.elasticsearch.core.query;

import static org.apache.commons.lang.RandomStringUtils.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.elasticsearch.annotations.FieldType.*;
import static org.springframework.data.elasticsearch.utils.IndexBuilder.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.lang.Long;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.Score;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.junit.jupiter.ElasticsearchRestTemplateConfiguration;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Peter-Josef Meisch
 * @author James Bodkin
 */
@SpringIntegrationTest
@ContextConfiguration(classes = { CriteriaQueryTests.Config.class })
public class CriteriaQueryTests {

	@Configuration
	@Import({ ElasticsearchRestTemplateConfiguration.class })
	static class Config {}

	private final IndexCoordinates index = IndexCoordinates.of("test-index-sample-core-query").withTypes("test-type");

	@Autowired private ElasticsearchOperations operations;

	@BeforeEach
	public void before() {

		operations.deleteIndex(SampleEntity.class);
		operations.createIndex(SampleEntity.class);
		operations.putMapping(SampleEntity.class);
		operations.refresh(SampleEntity.class);
	}

	@Test
	public void shouldPerformAndOperation() {

		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("some test message");
		sampleEntity.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery = new IndexQuery();
		indexQuery.setId(documentId);
		indexQuery.setObject(sampleEntity);
		operations.index(indexQuery, index);
		operations.refresh(SampleEntity.class);
		CriteriaQuery criteriaQuery = new CriteriaQuery(
				new Criteria("message").contains("test").and("message").contains("some"));

		// when
		SearchHit<SampleEntity> sampleEntity1 = operations.searchOne(criteriaQuery, SampleEntity.class, index);

		// then
		assertThat(sampleEntity1).isNotNull();
	}

	// @Ignore("DATAES-30")
	@Test
	public void shouldPerformOrOperation() {

		// given
		List<IndexQuery> indexQueries = new ArrayList<>();

		// first document
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity1 = new SampleEntity();
		sampleEntity1.setId(documentId);
		sampleEntity1.setMessage("some message");
		sampleEntity1.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery1 = new IndexQuery();
		indexQuery1.setId(documentId);
		indexQuery1.setObject(sampleEntity1);
		indexQueries.add(indexQuery1);

		// second document
		String documentId2 = randomNumeric(5);
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setMessage("test message");
		sampleEntity2.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery2 = new IndexQuery();
		indexQuery2.setId(documentId2);
		indexQuery2.setObject(sampleEntity2);

		indexQueries.add(indexQuery2);
		operations.bulkIndex(indexQueries, index);
		operations.refresh(SampleEntity.class);
		CriteriaQuery criteriaQuery = new CriteriaQuery(
				new Criteria("message").contains("some").or("message").contains("test"));

		// when
		SearchHits<SampleEntity> searchHits = operations.search(criteriaQuery, SampleEntity.class, index);

		// then
		assertThat(searchHits).isNotNull();
		assertThat(searchHits.getTotalHits()).isGreaterThanOrEqualTo(1);
	}

	@Test
	public void shouldPerformAndOperationWithinCriteria() {

		// given
		List<IndexQuery> indexQueries = new ArrayList<>();

		// first document
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("some message");
		sampleEntity.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery = new IndexQuery();
		indexQuery.setId(documentId);
		indexQuery.setObject(sampleEntity);
		indexQueries.add(indexQuery);

		operations.bulkIndex(indexQueries, index);
		operations.refresh(SampleEntity.class);
		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria().and(new Criteria("message").contains("some")));

		// when

		SearchHits<SampleEntity> searchHits = operations.search(criteriaQuery, SampleEntity.class, index);

		// then
		assertThat(searchHits).isNotNull();
		assertThat(searchHits.getTotalHits()).isGreaterThanOrEqualTo(1);
	}

	@Test
	public void shouldPerformOrOperationWithinCriteria() {

		// given
		List<IndexQuery> indexQueries = new ArrayList<>();

		// first document
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("some message");
		sampleEntity.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery = new IndexQuery();
		indexQuery.setId(documentId);
		indexQuery.setObject(sampleEntity);
		indexQueries.add(indexQuery);

		operations.bulkIndex(indexQueries, index);
		operations.refresh(SampleEntity.class);
		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria().or(new Criteria("message").contains("some")));

		// when
		SearchHits<SampleEntity> searchHits = operations.search(criteriaQuery, SampleEntity.class, index);

		// then
		assertThat(searchHits).isNotNull();
		assertThat(searchHits.getTotalHits()).isGreaterThanOrEqualTo(1);
	}

	@Test
	public void shouldPerformIsOperation() {

		// given
		List<IndexQuery> indexQueries = new ArrayList<>();
		// first document
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("some message");
		sampleEntity.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery = new IndexQuery();
		indexQuery.setId(documentId);
		indexQuery.setObject(sampleEntity);
		indexQueries.add(indexQuery);

		operations.bulkIndex(indexQueries, index);
		operations.refresh(SampleEntity.class);
		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("message").is("some message"));

		// when
		SearchHits<SampleEntity> searchHits = operations.search(criteriaQuery, SampleEntity.class, index);

		// then
		assertThat(criteriaQuery.getCriteria().getField().getName()).isEqualTo("message");
		assertThat(searchHits.getTotalHits()).isGreaterThanOrEqualTo(1);
	}

	@Test
	public void shouldPerformMultipleIsOperations() {

		// given
		List<IndexQuery> indexQueries = new ArrayList<>();

		// first document
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity1 = new SampleEntity();
		sampleEntity1.setId(documentId);
		sampleEntity1.setMessage("some message");
		sampleEntity1.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery1 = new IndexQuery();
		indexQuery1.setId(documentId);
		indexQuery1.setObject(sampleEntity1);
		indexQueries.add(indexQuery1);

		// second document
		String documentId2 = randomNumeric(5);
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setMessage("test message");
		sampleEntity2.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery2 = new IndexQuery();
		indexQuery2.setId(documentId2);
		indexQuery2.setObject(sampleEntity2);
		indexQueries.add(indexQuery2);

		operations.bulkIndex(indexQueries, index);
		operations.refresh(SampleEntity.class);
		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("message").is("some message"));

		// when
		SearchHits<SampleEntity> searchHits = operations.search(criteriaQuery, SampleEntity.class, index);

		// then
		assertThat(criteriaQuery.getCriteria().getField().getName()).isEqualTo("message");
		assertThat(searchHits.getTotalHits()).isEqualTo(1);
	}

	@Test
	public void shouldPerformEndsWithOperation() {

		// given
		List<IndexQuery> indexQueries = new ArrayList<>();

		// first document
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity1 = new SampleEntity();
		sampleEntity1.setId(documentId);
		sampleEntity1.setMessage("some message");
		sampleEntity1.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery1 = new IndexQuery();
		indexQuery1.setId(documentId);
		indexQuery1.setObject(sampleEntity1);
		indexQueries.add(indexQuery1);

		// second document
		String documentId2 = randomNumeric(5);
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setMessage("test message end");
		sampleEntity2.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery2 = new IndexQuery();
		indexQuery2.setId(documentId2);
		indexQuery2.setObject(sampleEntity2);
		indexQueries.add(indexQuery2);

		operations.bulkIndex(indexQueries, index);
		operations.refresh(SampleEntity.class);
		Criteria criteria = new Criteria("message").endsWith("end");
		CriteriaQuery criteriaQuery = new CriteriaQuery(criteria);

		// when
		SearchHit<SampleEntity> sampleEntity = operations.searchOne(criteriaQuery, SampleEntity.class, index);

		// then
		assertThat(criteriaQuery.getCriteria().getField().getName()).isEqualTo("message");
		assertThat(sampleEntity).isNotNull();
	}

	@Test
	public void shouldPerformStartsWithOperation() {

		// given
		List<IndexQuery> indexQueries = new ArrayList<>();
		// first document
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity1 = new SampleEntity();
		sampleEntity1.setId(documentId);
		sampleEntity1.setMessage("start some message");
		sampleEntity1.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery1 = new IndexQuery();
		indexQuery1.setId(documentId);
		indexQuery1.setObject(sampleEntity1);
		indexQueries.add(indexQuery1);

		// second document
		String documentId2 = randomNumeric(5);
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setMessage("test message");
		sampleEntity2.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery2 = new IndexQuery();
		indexQuery2.setId(documentId2);
		indexQuery2.setObject(sampleEntity2);
		indexQueries.add(indexQuery2);

		operations.bulkIndex(indexQueries, index);
		operations.refresh(SampleEntity.class);
		Criteria criteria = new Criteria("message").startsWith("start");
		CriteriaQuery criteriaQuery = new CriteriaQuery(criteria);

		// when
		SearchHit<SampleEntity> sampleEntity = operations.searchOne(criteriaQuery, SampleEntity.class, index);

		// then
		assertThat(criteriaQuery.getCriteria().getField().getName()).isEqualTo("message");
		assertThat(sampleEntity).isNotNull();
	}

	@Test
	public void shouldPerformContainsOperation() {

		// given
		List<IndexQuery> indexQueries = new ArrayList<>();
		// first document
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity1 = new SampleEntity();
		sampleEntity1.setId(documentId);
		sampleEntity1.setMessage("contains some message");
		sampleEntity1.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery1 = new IndexQuery();
		indexQuery1.setId(documentId);
		indexQuery1.setObject(sampleEntity1);
		indexQueries.add(indexQuery1);

		// second document
		String documentId2 = randomNumeric(5);
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setMessage("test message");
		sampleEntity2.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery2 = new IndexQuery();
		indexQuery2.setId(documentId2);
		indexQuery2.setObject(sampleEntity2);
		indexQueries.add(indexQuery2);

		operations.bulkIndex(indexQueries, index);
		operations.refresh(SampleEntity.class);
		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("message").contains("contains"));

		// when
		SearchHit<SampleEntity> sampleEntity = operations.searchOne(criteriaQuery, SampleEntity.class, index);

		// then
		assertThat(criteriaQuery.getCriteria().getField().getName()).isEqualTo("message");
		assertThat(sampleEntity).isNotNull();
	}

	@Test
	public void shouldExecuteExpression() {

		// given
		List<IndexQuery> indexQueries = new ArrayList<>();
		// first document
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity1 = new SampleEntity();
		sampleEntity1.setId(documentId);
		sampleEntity1.setMessage("elasticsearch search");
		sampleEntity1.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery1 = new IndexQuery();
		indexQuery1.setId(documentId);
		indexQuery1.setObject(sampleEntity1);
		indexQueries.add(indexQuery1);

		// second document
		String documentId2 = randomNumeric(5);
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setMessage("test message");
		sampleEntity2.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery2 = new IndexQuery();
		indexQuery2.setId(documentId2);
		indexQuery2.setObject(sampleEntity2);
		indexQueries.add(indexQuery2);

		operations.bulkIndex(indexQueries, index);
		operations.refresh(SampleEntity.class);
		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("message").expression("+elasticsearch || test"));

		// when
		SearchHit<SampleEntity> sampleEntity = operations.searchOne(criteriaQuery, SampleEntity.class, index);

		// then
		assertThat(criteriaQuery.getCriteria().getField().getName()).isEqualTo("message");
		assertThat(sampleEntity).isNotNull();
	}

	@Test
	public void shouldExecuteCriteriaChain() {

		// given
		List<IndexQuery> indexQueries = new ArrayList<>();
		// first document
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity1 = new SampleEntity();
		sampleEntity1.setId(documentId);
		sampleEntity1.setMessage("some message search");
		sampleEntity1.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery1 = new IndexQuery();
		indexQuery1.setId(documentId);
		indexQuery1.setObject(sampleEntity1);
		indexQueries.add(indexQuery1);

		// second document
		String documentId2 = randomNumeric(5);
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setMessage("test test message");
		sampleEntity2.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery2 = new IndexQuery();
		indexQuery2.setId(documentId2);
		indexQuery2.setObject(sampleEntity2);
		indexQueries.add(indexQuery2);

		operations.bulkIndex(indexQueries, index);
		operations.refresh(SampleEntity.class);
		CriteriaQuery criteriaQuery = new CriteriaQuery(
				new Criteria("message").startsWith("some").endsWith("search").contains("message").is("some message search"));

		// when
		SearchHit<SampleEntity> sampleEntity = operations.searchOne(criteriaQuery, SampleEntity.class, index);

		// then
		assertThat(criteriaQuery.getCriteria().getField().getName()).isEqualTo("message");
		assertThat(sampleEntity).isNotNull();
	}

	@Test
	public void shouldPerformIsNotOperation() {

		// given
		List<IndexQuery> indexQueries = new ArrayList<>();
		// first document
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity1 = new SampleEntity();
		sampleEntity1.setId(documentId);
		sampleEntity1.setMessage("bar");
		sampleEntity1.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery1 = new IndexQuery();
		indexQuery1.setId(documentId);
		indexQuery1.setObject(sampleEntity1);
		indexQueries.add(indexQuery1);

		// second document
		String documentId2 = randomNumeric(5);
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setMessage("foo");
		sampleEntity2.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery2 = new IndexQuery();
		indexQuery2.setId(documentId2);
		indexQuery2.setObject(sampleEntity2);
		indexQueries.add(indexQuery2);

		operations.bulkIndex(indexQueries, index);
		operations.refresh(SampleEntity.class);
		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("message").is("foo").not());

		// when
		SearchHits<SampleEntity> searchHits = operations.search(criteriaQuery, SampleEntity.class, index);

		// then
		assertThat(criteriaQuery.getCriteria().isNegating()).isTrue();
		assertThat(searchHits).isNotNull();
		assertThat(searchHits.iterator().next().getContent().getMessage()).doesNotContain("foo");
	}

	@Test
	public void shouldPerformBetweenOperation() {

		// given
		List<IndexQuery> indexQueries = new ArrayList<>();
		// first document
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity1 = new SampleEntity();
		sampleEntity1.setId(documentId);
		sampleEntity1.setRate(100);
		sampleEntity1.setMessage("bar");
		sampleEntity1.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery1 = new IndexQuery();
		indexQuery1.setId(documentId);
		indexQuery1.setObject(sampleEntity1);
		indexQueries.add(indexQuery1);

		// second document
		String documentId2 = randomNumeric(5);
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setRate(200);
		sampleEntity2.setMessage("foo");
		sampleEntity2.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery2 = new IndexQuery();
		indexQuery2.setId(documentId2);
		indexQuery2.setObject(sampleEntity2);
		indexQueries.add(indexQuery2);

		operations.bulkIndex(indexQueries, index);
		operations.refresh(SampleEntity.class);
		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("rate").between(100, 150));

		// when
		SearchHit<SampleEntity> sampleEntity = operations.searchOne(criteriaQuery, SampleEntity.class, index);

		// then
		assertThat(sampleEntity).isNotNull();
	}

	@Test
	public void shouldPerformBetweenOperationWithoutUpperBound() {

		// given
		List<IndexQuery> indexQueries = new ArrayList<>();
		// first document
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity1 = new SampleEntity();
		sampleEntity1.setId(documentId);
		sampleEntity1.setRate(300);
		sampleEntity1.setMessage("bar");
		sampleEntity1.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery1 = new IndexQuery();
		indexQuery1.setId(documentId);
		indexQuery1.setObject(sampleEntity1);
		indexQueries.add(indexQuery1);

		// second document
		String documentId2 = randomNumeric(5);
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setRate(400);
		sampleEntity2.setMessage("foo");
		sampleEntity2.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery2 = new IndexQuery();
		indexQuery2.setId(documentId2);
		indexQuery2.setObject(sampleEntity2);
		indexQueries.add(indexQuery2);

		operations.bulkIndex(indexQueries, index);
		operations.refresh(SampleEntity.class);
		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("rate").between(350, null));

		// when
		SearchHits<SampleEntity> searchHits = operations.search(criteriaQuery, SampleEntity.class, index);

		// then
		assertThat(searchHits).isNotNull();
		assertThat(searchHits.getTotalHits()).isGreaterThanOrEqualTo(1);
	}

	@Test
	public void shouldPerformBetweenOperationWithoutLowerBound() {

		// given
		List<IndexQuery> indexQueries = new ArrayList<>();
		// first document
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity1 = new SampleEntity();
		sampleEntity1.setId(documentId);
		sampleEntity1.setRate(500);
		sampleEntity1.setMessage("bar");
		sampleEntity1.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery1 = new IndexQuery();
		indexQuery1.setId(documentId);
		indexQuery1.setObject(sampleEntity1);
		indexQueries.add(indexQuery1);

		// second document
		String documentId2 = randomNumeric(5);
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setRate(600);
		sampleEntity2.setMessage("foo");
		sampleEntity2.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery2 = new IndexQuery();
		indexQuery2.setId(documentId2);
		indexQuery2.setObject(sampleEntity2);
		indexQueries.add(indexQuery2);

		operations.bulkIndex(indexQueries, index);
		operations.refresh(SampleEntity.class);
		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("rate").between(null, 550));

		// when
		SearchHits<SampleEntity> searchHits = operations.search(criteriaQuery, SampleEntity.class, index);

		// then
		assertThat(searchHits).isNotNull();
		assertThat(searchHits.getTotalHits()).isGreaterThanOrEqualTo(1);
	}

	@Test
	public void shouldPerformLessThanEqualOperation() {

		// given
		List<IndexQuery> indexQueries = new ArrayList<>();
		// first document
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity1 = new SampleEntity();
		sampleEntity1.setId(documentId);
		sampleEntity1.setRate(700);
		sampleEntity1.setMessage("bar");
		sampleEntity1.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery1 = new IndexQuery();
		indexQuery1.setId(documentId);
		indexQuery1.setObject(sampleEntity1);
		indexQueries.add(indexQuery1);

		// second document
		String documentId2 = randomNumeric(5);
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setRate(800);
		sampleEntity2.setMessage("foo");
		sampleEntity2.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery2 = new IndexQuery();
		indexQuery2.setId(documentId2);
		indexQuery2.setObject(sampleEntity2);
		indexQueries.add(indexQuery2);

		operations.bulkIndex(indexQueries, index);
		operations.refresh(SampleEntity.class);
		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("rate").lessThanEqual(750));

		// when
		SearchHits<SampleEntity> searchHits = operations.search(criteriaQuery, SampleEntity.class, index);

		// then
		assertThat(searchHits).isNotNull();
		assertThat(searchHits.getTotalHits()).isGreaterThanOrEqualTo(1);
	}

	@Test
	public void shouldPerformGreaterThanEquals() {

		// given
		List<IndexQuery> indexQueries = new ArrayList<>();
		// first document
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity1 = new SampleEntity();
		sampleEntity1.setId(documentId);
		sampleEntity1.setRate(900);
		sampleEntity1.setMessage("bar");
		sampleEntity1.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery1 = new IndexQuery();
		indexQuery1.setId(documentId);
		indexQuery1.setObject(sampleEntity1);
		indexQueries.add(indexQuery1);

		// second document
		String documentId2 = randomNumeric(5);
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setRate(1000);
		sampleEntity2.setMessage("foo");
		sampleEntity2.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery2 = new IndexQuery();
		indexQuery2.setId(documentId2);
		indexQuery2.setObject(sampleEntity2);
		indexQueries.add(indexQuery2);

		operations.bulkIndex(indexQueries, index);
		operations.refresh(SampleEntity.class);
		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("rate").greaterThanEqual(950));

		// when
		SearchHits<SampleEntity> searchHits = operations.search(criteriaQuery, SampleEntity.class, index);

		// then
		assertThat(searchHits).isNotNull();
		assertThat(searchHits.getTotalHits()).isGreaterThanOrEqualTo(1);
	}

	@Test
	public void shouldPerformBoostOperation() {

		// given
		List<IndexQuery> indexQueries = new ArrayList<>();
		// first document
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity1 = new SampleEntity();
		sampleEntity1.setId(documentId);
		sampleEntity1.setRate(700);
		sampleEntity1.setMessage("bar foo");
		sampleEntity1.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery1 = new IndexQuery();
		indexQuery1.setId(documentId);
		indexQuery1.setObject(sampleEntity1);
		indexQueries.add(indexQuery1);

		// second document
		String documentId2 = randomNumeric(5);
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setRate(800);
		sampleEntity2.setMessage("foo");
		sampleEntity2.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery2 = new IndexQuery();
		indexQuery2.setId(documentId2);
		indexQuery2.setObject(sampleEntity2);
		indexQueries.add(indexQuery2);

		operations.bulkIndex(indexQueries, index);
		operations.refresh(SampleEntity.class);
		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("message").contains("foo").boost(1));

		// when
		SearchHits<SampleEntity> searchHits = operations.search(criteriaQuery, SampleEntity.class, index);

		// then
		assertThat(searchHits.getTotalHits()).isGreaterThanOrEqualTo(1);
	}

	@Test
	public void shouldReturnDocumentAboveMinimalScoreGivenCriteria() {

		// given
		List<IndexQuery> indexQueries = new ArrayList<>();

		indexQueries.add(buildIndex(SampleEntity.builder().id("1").message("ab").build()));
		indexQueries.add(buildIndex(SampleEntity.builder().id("2").message("bc").build()));
		indexQueries.add(buildIndex(SampleEntity.builder().id("3").message("ac").build()));

		operations.bulkIndex(indexQueries, index);
		operations.refresh(SampleEntity.class);

		// when
		CriteriaQuery criteriaQuery = new CriteriaQuery(
				new Criteria("message").contains("a").or(new Criteria("message").contains("b")));
		criteriaQuery.setMinScore(2.0F);
		SearchHits<SampleEntity> searchHits = operations.search(criteriaQuery, SampleEntity.class, index);

		// then
		assertThat(searchHits.getTotalHits()).isEqualTo(1);
		assertThat(searchHits.getSearchHit(0).getContent().getMessage()).isEqualTo("ab");
	}

	@Test // DATAES-213
	public void shouldEscapeValue() {

		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("Hello World!");
		sampleEntity.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery = new IndexQuery();
		indexQuery.setId(documentId);
		indexQuery.setObject(sampleEntity);
		operations.index(indexQuery, index);
		operations.refresh(SampleEntity.class);

		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("message").is("Hello World!"));

		// when
		SearchHit<SampleEntity> sampleEntity1 = operations.searchOne(criteriaQuery, SampleEntity.class, index);

		// then
		assertThat(sampleEntity1).isNotNull();
	}

	@Builder
	@Setter
	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	@Document(indexName = "test-index-sample-core-query", type = "test-type", shards = 1, replicas = 0,
			refreshInterval = "-1")
	static class SampleEntity {

		@Id private String id;
		@Field(type = Text, store = true, fielddata = true) private String type;
		@Field(type = Text, store = true, fielddata = true) private String message;
		private int rate;
		@Version private Long version;
		@Score private float score;
	}
}
