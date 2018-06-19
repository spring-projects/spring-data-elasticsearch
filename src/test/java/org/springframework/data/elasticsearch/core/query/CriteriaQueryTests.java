/*
 * Copyright 2013-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.elasticsearch.core.query;

import static org.apache.commons.lang.RandomStringUtils.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.springframework.data.elasticsearch.utils.IndexBuilder.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.entities.SampleEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Rizwan Idrees
 * @author Mohsin Husen
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:elasticsearch-template-test.xml")
public class CriteriaQueryTests {

	@Autowired
	private ElasticsearchTemplate elasticsearchTemplate;

	@Before
	public void before() {
		elasticsearchTemplate.deleteIndex(SampleEntity.class);
		elasticsearchTemplate.createIndex(SampleEntity.class);
		elasticsearchTemplate.putMapping(SampleEntity.class);
		elasticsearchTemplate.refresh(SampleEntity.class);
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
		elasticsearchTemplate.index(indexQuery);
		elasticsearchTemplate.refresh(SampleEntity.class);
		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("message").contains("test").and("message")
				.contains("some"));
		// when
		SampleEntity sampleEntity1 = elasticsearchTemplate.queryForObject(criteriaQuery, SampleEntity.class);
		// then
		assertThat(sampleEntity1, is(notNullValue()));
	}

	@Ignore("DATAES-30")
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
		elasticsearchTemplate.bulkIndex(indexQueries);
		elasticsearchTemplate.refresh(SampleEntity.class);
		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("message").contains("some").or("message")
				.contains("test"));
		// when
		Page<SampleEntity> page = elasticsearchTemplate.queryForPage(criteriaQuery, SampleEntity.class);
		// then
		assertThat(page, is(notNullValue()));
		assertThat(page.getTotalElements(), is(greaterThanOrEqualTo(1L)));
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

		elasticsearchTemplate.bulkIndex(indexQueries);
		elasticsearchTemplate.refresh(SampleEntity.class);
		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria().and(new Criteria("message").contains("some")));
		// when
		Page<SampleEntity> page = elasticsearchTemplate.queryForPage(criteriaQuery, SampleEntity.class);
		// then
		assertThat(page, is(notNullValue()));
		assertThat(page.getTotalElements(), is(greaterThanOrEqualTo(1L)));
	}

	@Ignore("DATAES-30")
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

		elasticsearchTemplate.bulkIndex(indexQueries);
		elasticsearchTemplate.refresh(SampleEntity.class);
		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria().or(new Criteria("message").contains("some")));
		// when
		Page<SampleEntity> page = elasticsearchTemplate.queryForPage(criteriaQuery, SampleEntity.class);
		// then
		assertThat(page, is(notNullValue()));
		assertThat(page.getTotalElements(), is(greaterThanOrEqualTo(1L)));
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

		elasticsearchTemplate.bulkIndex(indexQueries);
		elasticsearchTemplate.refresh(SampleEntity.class);
		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("message").is("some message"));
		// when
		Page<SampleEntity> page = elasticsearchTemplate.queryForPage(criteriaQuery, SampleEntity.class);
		// then
		assertThat("message", is(criteriaQuery.getCriteria().getField().getName()));
		assertThat(page.getTotalElements(), is(greaterThanOrEqualTo(1L)));
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

		elasticsearchTemplate.bulkIndex(indexQueries);
		elasticsearchTemplate.refresh(SampleEntity.class);
		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("message").is("some message"));
		// when
		Page<SampleEntity> page = elasticsearchTemplate.queryForPage(criteriaQuery, SampleEntity.class);
		// then
		assertThat("message", is(criteriaQuery.getCriteria().getField().getName()));
		assertThat(page.getTotalElements(), is(equalTo(1L)));
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

		elasticsearchTemplate.bulkIndex(indexQueries);
		elasticsearchTemplate.refresh(SampleEntity.class);
		Criteria criteria = new Criteria("message").endsWith("end");
		CriteriaQuery criteriaQuery = new CriteriaQuery(criteria);
		// when
		SampleEntity sampleEntity = elasticsearchTemplate.queryForObject(criteriaQuery, SampleEntity.class);
		// then
		assertThat("message", is(criteriaQuery.getCriteria().getField().getName()));
		assertThat(sampleEntity, is(notNullValue()));
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

		elasticsearchTemplate.bulkIndex(indexQueries);
		elasticsearchTemplate.refresh(SampleEntity.class);
		Criteria criteria = new Criteria("message").startsWith("start");
		CriteriaQuery criteriaQuery = new CriteriaQuery(criteria);
		// when
		SampleEntity sampleEntity = elasticsearchTemplate.queryForObject(criteriaQuery, SampleEntity.class);
		// then
		assertThat("message", is(criteriaQuery.getCriteria().getField().getName()));
		assertThat(sampleEntity, is(notNullValue()));
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

		elasticsearchTemplate.bulkIndex(indexQueries);
		elasticsearchTemplate.refresh(SampleEntity.class);
		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("message").contains("contains"));
		// when
		SampleEntity sampleEntity = elasticsearchTemplate.queryForObject(criteriaQuery, SampleEntity.class);
		// then
		assertThat("message", is(criteriaQuery.getCriteria().getField().getName()));
		assertThat(sampleEntity, is(notNullValue()));
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

		elasticsearchTemplate.bulkIndex(indexQueries);
		elasticsearchTemplate.refresh(SampleEntity.class);
		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("message").expression("+elasticsearch || test"));
		// when
		SampleEntity sampleEntity = elasticsearchTemplate.queryForObject(criteriaQuery, SampleEntity.class);
		// then
		assertThat("message", is(criteriaQuery.getCriteria().getField().getName()));
		assertThat(sampleEntity, is(notNullValue()));
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

		elasticsearchTemplate.bulkIndex(indexQueries);
		elasticsearchTemplate.refresh(SampleEntity.class);
		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("message").startsWith("some").endsWith("search")
				.contains("message").is("some message search"));
		// when
		SampleEntity sampleEntity = elasticsearchTemplate.queryForObject(criteriaQuery, SampleEntity.class);
		// then
		assertThat("message", is(criteriaQuery.getCriteria().getField().getName()));
		assertThat(sampleEntity, is(notNullValue()));
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

		elasticsearchTemplate.bulkIndex(indexQueries);
		elasticsearchTemplate.refresh(SampleEntity.class);
		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("message").is("foo").not());
		// when
		Page<SampleEntity> page = elasticsearchTemplate.queryForPage(criteriaQuery, SampleEntity.class);
		// then
		assertTrue(criteriaQuery.getCriteria().isNegating());
		assertThat(page, is(notNullValue()));
		assertFalse(page.iterator().next().getMessage().contains("foo"));
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

		elasticsearchTemplate.bulkIndex(indexQueries);
		elasticsearchTemplate.refresh(SampleEntity.class);
		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("rate").between(100, 150));
		// when
		SampleEntity sampleEntity = elasticsearchTemplate.queryForObject(criteriaQuery, SampleEntity.class);
		// then
		assertThat(sampleEntity, is(notNullValue()));
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

		elasticsearchTemplate.bulkIndex(indexQueries);
		elasticsearchTemplate.refresh(SampleEntity.class);
		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("rate").between(350, null));
		// when
		Page<SampleEntity> page = elasticsearchTemplate.queryForPage(criteriaQuery, SampleEntity.class);
		// then
		assertThat(page, is(notNullValue()));
		assertThat(page.getTotalElements(), is(greaterThanOrEqualTo(1L)));
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

		elasticsearchTemplate.bulkIndex(indexQueries);
		elasticsearchTemplate.refresh(SampleEntity.class);
		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("rate").between(null, 550));
		// when
		Page<SampleEntity> page = elasticsearchTemplate.queryForPage(criteriaQuery, SampleEntity.class);
		// then
		assertThat(page, is(notNullValue()));
		assertThat(page.getTotalElements(), is(greaterThanOrEqualTo(1L)));
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

		elasticsearchTemplate.bulkIndex(indexQueries);
		elasticsearchTemplate.refresh(SampleEntity.class);
		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("rate").lessThanEqual(750));
		// when
		Page<SampleEntity> page = elasticsearchTemplate.queryForPage(criteriaQuery, SampleEntity.class);
		// then
		assertThat(page, is(notNullValue()));
		assertThat(page.getTotalElements(), is(greaterThanOrEqualTo(1L)));
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

		elasticsearchTemplate.bulkIndex(indexQueries);
		elasticsearchTemplate.refresh(SampleEntity.class);
		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("rate").greaterThanEqual(950));
		// when
		Page<SampleEntity> page = elasticsearchTemplate.queryForPage(criteriaQuery, SampleEntity.class);
		// then
		assertThat(page, is(notNullValue()));
		assertThat(page.getTotalElements(), is(greaterThanOrEqualTo(1L)));
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

		elasticsearchTemplate.bulkIndex(indexQueries);
		elasticsearchTemplate.refresh(SampleEntity.class);
		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("message").contains("foo").boost(1));
		// when
		Page<SampleEntity> page = elasticsearchTemplate.queryForPage(criteriaQuery, SampleEntity.class);
		// then
		assertThat(page.getTotalElements(), is(greaterThanOrEqualTo(1L)));
	}

	@Test
	public void shouldReturnDocumentAboveMinimalScoreGivenCriteria() {
		// given
		List<IndexQuery> indexQueries = new ArrayList<>();

		indexQueries.add(buildIndex(SampleEntity.builder().id("1").message("ab").build()));
		indexQueries.add(buildIndex(SampleEntity.builder().id("2").message("bc").build()));
		indexQueries.add(buildIndex(SampleEntity.builder().id("3").message("ac").build()));

		elasticsearchTemplate.bulkIndex(indexQueries);
		elasticsearchTemplate.refresh(SampleEntity.class);

		// when
		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("message").contains("a").or(new Criteria("message").contains("b")));
		criteriaQuery.setMinScore(2.0F);
		Page<SampleEntity> page = elasticsearchTemplate.queryForPage(criteriaQuery, SampleEntity.class);
		// then
		assertThat(page.getTotalElements(), is(1L));
		assertThat(page.getContent().get(0).getMessage(), is("ab"));
	}
}
