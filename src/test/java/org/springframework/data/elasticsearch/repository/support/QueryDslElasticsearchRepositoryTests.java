/*
 * Copyright 2013-2014 the original author or authors.
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
package org.springframework.data.elasticsearch.repository.support;

import java.util.Arrays;
import java.util.List;

import com.querydsl.core.types.dsl.BooleanExpression;

import org.elasticsearch.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.data.elasticsearch.entities.QSampleEntity;
import org.springframework.data.elasticsearch.entities.SampleEntity;
import org.springframework.data.elasticsearch.repositories.querydsl.QueryDslElasticsearchRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.apache.commons.lang.RandomStringUtils.randomNumeric;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * @author Kevin Leturc
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:/querydsl-repository-test.xml")
public class QueryDslElasticsearchRepositoryTests {

	@Autowired
	private QueryDslElasticsearchRepository repository;

	@Autowired
	private ElasticsearchTemplate elasticsearchTemplate;


	@Before
	public void before() {
		elasticsearchTemplate.deleteIndex(SampleEntity.class);
		elasticsearchTemplate.createIndex(SampleEntity.class);
		elasticsearchTemplate.refresh(SampleEntity.class, true);
	}

	@Test
	public void shouldDoBulkIndexDocument() {
		// given
		String documentId1 = randomNumeric(5);
		SampleEntity sampleEntity1 = new SampleEntity();
		sampleEntity1.setId(documentId1);
		sampleEntity1.setMessage("some message");
		sampleEntity1.setVersion(System.currentTimeMillis());

		String documentId2 = randomNumeric(5);
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setMessage("some message");
		sampleEntity2.setVersion(System.currentTimeMillis());

		// when
		repository.save(Arrays.asList(sampleEntity1, sampleEntity2));
		// then
		SampleEntity entity1FromElasticSearch = repository.findOne(documentId1);
		assertThat(entity1FromElasticSearch, is(notNullValue()));

		SampleEntity entity2FromElasticSearch = repository.findOne(documentId2);
		assertThat(entity2FromElasticSearch, is(notNullValue()));
	}

	@Test
	public void shouldSaveDocument() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("some message");
		sampleEntity.setVersion(System.currentTimeMillis());
		// when
		repository.save(sampleEntity);
		// then
		SampleEntity entityFromElasticSearch = repository.findOne(documentId);
		assertThat(entityFromElasticSearch, is(notNullValue()));
	}

	@Test
	public void shouldSaveDocumentWithoutId() {
		// given
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setMessage("some message");
		sampleEntity.setVersion(System.currentTimeMillis());
		// when
		repository.save(sampleEntity);
		// then
		assertThat(sampleEntity.getId(), is(notNullValue()));
	}

	@Test
	public void shouldFindDocumentById() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("some message");
		sampleEntity.setVersion(System.currentTimeMillis());
		repository.save(sampleEntity);
		// when
		SampleEntity entityFromElasticSearch = repository.findOne(documentId);
		// then
		assertThat(entityFromElasticSearch, is(notNullValue()));
		assertThat(sampleEntity, is((equalTo(sampleEntity))));
	}

	@Test
	public void shouldReturnCountOfDocuments() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("some message");
		sampleEntity.setVersion(System.currentTimeMillis());
		repository.save(sampleEntity);
		// when
		Long count = repository.count();
		// then
		assertThat(count, is(greaterThanOrEqualTo(1L)));
	}

	@Test
	public void shouldFindAllDocuments() {
		// when
		Iterable<SampleEntity> results = repository.findAll();
		// then
		assertThat(results, is(notNullValue()));
	}

	@Test
	public void shouldDeleteDocument() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("some message");
		sampleEntity.setVersion(System.currentTimeMillis());
		repository.save(sampleEntity);
		// when
		repository.delete(documentId);
		// then
		SampleEntity entityFromElasticSearch = repository.findOne(documentId);
		assertThat(entityFromElasticSearch, is(nullValue()));
	}

	@Test
	public void shouldSearchDocumentsGivenContainsPredicate() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("some test message");
		sampleEntity.setVersion(System.currentTimeMillis());
		repository.save(sampleEntity);

		BooleanExpression predicate = QSampleEntity.sampleEntity.message.contains("test");
		// when
		List<SampleEntity> entities = Lists.newArrayList(repository.findAll(predicate));
		// then
		assertThat(entities, is(notNullValue()));
		assertThat(entities.size(), is(equalTo(1)));
		assertThat(entities.get(0), is(equalTo(sampleEntity)));
	}

	@Test
	public void shouldSaveIterableEntities() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity1 = new SampleEntity();
		sampleEntity1.setId(documentId);
		sampleEntity1.setMessage("hello world.");
		sampleEntity1.setVersion(System.currentTimeMillis());

		String documentId2 = randomNumeric(5);
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setMessage("hello world.");
		sampleEntity2.setVersion(System.currentTimeMillis());

		Iterable<SampleEntity> sampleEntities = Arrays.asList(sampleEntity1, sampleEntity2);
		// when
		repository.save(sampleEntities);
		// then
		Page<SampleEntity> entities = repository.search(termQuery("id", documentId), new PageRequest(0, 50));
		assertNotNull(entities);
	}

	@Test
	public void shouldReturnTrueGivenPredicateExists() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("hello world.");
		sampleEntity.setVersion(System.currentTimeMillis());
		repository.save(sampleEntity);

		// when
		BooleanExpression predicate = QSampleEntity.sampleEntity.message.eq("hello world");
		boolean exist = repository.exists(predicate);

		// then
		assertEquals(exist, true);
	}

	@Test
	public void shouldDeleteAll() {
		// when
		repository.deleteAll();
		// then
		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery()).build();
		Page<SampleEntity> sampleEntities = repository.search(searchQuery);
		assertThat(sampleEntities.getTotalElements(), equalTo(0L));
	}

	@Test
	public void shouldDeleteEntity() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("hello world.");
		sampleEntity.setVersion(System.currentTimeMillis());
		repository.save(sampleEntity);
		// when
		repository.delete(sampleEntity);
		// then
		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(termQuery("id", documentId)).build();
		Page<SampleEntity> sampleEntities = repository.search(searchQuery);
		assertThat(sampleEntities.getTotalElements(), equalTo(0L));
	}

	@Test
	public void shouldSortByGivenField() {
		// todo
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("world");
		repository.save(sampleEntity);

		String documentId2 = randomNumeric(5);
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setMessage("hello");
		repository.save(sampleEntity2);
		// when
		List<SampleEntity> sampleEntities = Lists.newArrayList(repository.findAll(QSampleEntity.sampleEntity.message.asc()));
		// then
		assertThat(sampleEntities, is(notNullValue()));
		assertThat(sampleEntities.get(0).getMessage(), is(equalTo("hello")));
		assertThat(sampleEntities.get(1).getMessage(), is(equalTo("world")));
	}

}
