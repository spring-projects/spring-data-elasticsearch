/*
 * Copyright 2013-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.data.elasticsearch.repository.support;

import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isIn;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.data.elasticsearch.entities.SampleDocumentID;
import org.springframework.data.elasticsearch.entities.SampleEntityDocumentIDKeyed;
import org.springframework.data.elasticsearch.repositories.sample.SampleDocIDKeyedElasticsearchRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Murali Chevuri
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:/simple-repository-test.xml")
public class DocumentIDElasticsearchRepositoryTests {

	@Autowired private SampleDocIDKeyedElasticsearchRepository repository;

	@Autowired private ElasticsearchTemplate elasticsearchTemplate;

	@Before
	public void before() {
		elasticsearchTemplate.deleteIndex(SampleEntityDocumentIDKeyed.class);
		elasticsearchTemplate.createIndex(SampleEntityDocumentIDKeyed.class);
		elasticsearchTemplate.putMapping(SampleEntityDocumentIDKeyed.class);
		elasticsearchTemplate.refresh(SampleEntityDocumentIDKeyed.class);
	}

	@Test
	public void shouldDoBulkIndexDocument() {
		// given
		SampleDocumentID documentId1 = getRandomSampleElasticID();
		SampleEntityDocumentIDKeyed sampleEntityElasticIDKeyed1 = new SampleEntityDocumentIDKeyed();
		sampleEntityElasticIDKeyed1.setId(documentId1);
		sampleEntityElasticIDKeyed1.setMessage("some message");
		sampleEntityElasticIDKeyed1.setVersion(System.currentTimeMillis());

		SampleDocumentID documentId2 = getRandomSampleElasticID();
		SampleEntityDocumentIDKeyed sampleEntityElasticIDKeyed2 = new SampleEntityDocumentIDKeyed();
		sampleEntityElasticIDKeyed2.setId(documentId2);
		sampleEntityElasticIDKeyed2.setMessage("some message");
		sampleEntityElasticIDKeyed2.setVersion(System.currentTimeMillis());

		// when
		repository.saveAll(Arrays.asList(sampleEntityElasticIDKeyed1, sampleEntityElasticIDKeyed2));
		// then
		Optional<SampleEntityDocumentIDKeyed> entity1FromElasticSearch = repository.findById(documentId1);
		assertThat(entity1FromElasticSearch.isPresent(), is(true));

		Optional<SampleEntityDocumentIDKeyed> entity2FromElasticSearch = repository.findById(documentId2);
		assertThat(entity2FromElasticSearch.isPresent(), is(true));
	}

	@Test
	public void shouldSaveDocument() {
		// given
		SampleDocumentID documentId = getRandomSampleElasticID();
		SampleEntityDocumentIDKeyed sampleEntityDocumentIDKeyed = new SampleEntityDocumentIDKeyed();
		sampleEntityDocumentIDKeyed.setId(documentId);
		sampleEntityDocumentIDKeyed.setMessage("some message");
		sampleEntityDocumentIDKeyed.setVersion(System.currentTimeMillis());
		// when
		repository.save(sampleEntityDocumentIDKeyed);
		// then
		Optional<SampleEntityDocumentIDKeyed> entityFromElasticSearch = repository.findById(documentId);
		assertThat(entityFromElasticSearch.isPresent(), is(true));
	}

	@Test
	public void shouldFindDocumentById() {
		// given
		SampleDocumentID documentId = getRandomSampleElasticID();
		SampleEntityDocumentIDKeyed sampleEntityDocumentIDKeyed = new SampleEntityDocumentIDKeyed();
		sampleEntityDocumentIDKeyed.setId(documentId);
		sampleEntityDocumentIDKeyed.setMessage("some message");
		sampleEntityDocumentIDKeyed.setVersion(System.currentTimeMillis());
		repository.save(sampleEntityDocumentIDKeyed);
		// when
		Optional<SampleEntityDocumentIDKeyed> entityFromElasticSearch = repository.findById(documentId);
		// then
		assertThat(entityFromElasticSearch.isPresent(), is(true));
		assertThat(sampleEntityDocumentIDKeyed, is((equalTo(sampleEntityDocumentIDKeyed))));
	}

	@Test
	public void shouldReturnCountOfDocuments() {
		// given
		SampleDocumentID documentId = getRandomSampleElasticID();
		SampleEntityDocumentIDKeyed sampleEntityDocumentIDKeyed = new SampleEntityDocumentIDKeyed();
		sampleEntityDocumentIDKeyed.setId(documentId);
		sampleEntityDocumentIDKeyed.setMessage("some message");
		sampleEntityDocumentIDKeyed.setVersion(System.currentTimeMillis());
		repository.save(sampleEntityDocumentIDKeyed);
		// when
		Long count = repository.count();
		// then
		assertThat(count, is(greaterThanOrEqualTo(1L)));
	}

	@Test
	public void shouldFindAllDocuments() {
		// when
		Iterable<SampleEntityDocumentIDKeyed> results = repository.findAll();
		// then
		assertThat(results, is(notNullValue()));
	}

	@Test
	public void shouldDeleteDocument() {
		// given
		SampleDocumentID documentId = getRandomSampleElasticID();
		SampleEntityDocumentIDKeyed sampleEntityDocumentIDKeyed = new SampleEntityDocumentIDKeyed();
		sampleEntityDocumentIDKeyed.setId(documentId);
		sampleEntityDocumentIDKeyed.setMessage("some message");
		sampleEntityDocumentIDKeyed.setVersion(System.currentTimeMillis());
		repository.save(sampleEntityDocumentIDKeyed);
		// when
		repository.deleteById(documentId);
		// then
		Optional<SampleEntityDocumentIDKeyed> entityFromElasticSearch = repository.findById(documentId);
		assertThat(entityFromElasticSearch.isPresent(), is(false));
	}

	@Test
	public void shouldSearchDocumentsGivenSearchQuery() {
		// given
		SampleDocumentID documentId = getRandomSampleElasticID();
		SampleEntityDocumentIDKeyed sampleEntityDocumentIDKeyed = new SampleEntityDocumentIDKeyed();
		sampleEntityDocumentIDKeyed.setId(documentId);
		sampleEntityDocumentIDKeyed.setMessage("some test message");
		sampleEntityDocumentIDKeyed.setVersion(System.currentTimeMillis());
		repository.save(sampleEntityDocumentIDKeyed);

		SearchQuery query = new NativeSearchQueryBuilder().withQuery(termQuery("message", "test")).build();
		// when
		Page<SampleEntityDocumentIDKeyed> page = repository.search(query);
		// then
		assertThat(page, is(notNullValue()));
		assertThat(page.getNumberOfElements(), is(greaterThanOrEqualTo(1)));
	}

	@Test
	public void shouldSearchDocumentsGivenElasticsearchQuery() {
		// given
		SampleDocumentID documentId = getRandomSampleElasticID();
		SampleEntityDocumentIDKeyed sampleEntityDocumentIDKeyed = new SampleEntityDocumentIDKeyed();
		sampleEntityDocumentIDKeyed.setId(documentId);
		sampleEntityDocumentIDKeyed.setMessage("hello world.");
		sampleEntityDocumentIDKeyed.setVersion(System.currentTimeMillis());
		repository.save(sampleEntityDocumentIDKeyed);
		// when
		Page<SampleEntityDocumentIDKeyed> page = repository.search(termQuery("message", "world"), new PageRequest(0, 50));
		// then
		assertThat(page, is(notNullValue()));
		assertThat(page.getNumberOfElements(), is(greaterThanOrEqualTo(1)));
	}

	/*
	 * DATAES-82
	 */
	@Test
	public void shouldFindAllByIdQuery() {
		// given
		SampleDocumentID documentId = getRandomSampleElasticID();
		SampleEntityDocumentIDKeyed sampleEntityDocumentIDKeyed = new SampleEntityDocumentIDKeyed();
		sampleEntityDocumentIDKeyed.setId(documentId);
		sampleEntityDocumentIDKeyed.setMessage("hello world.");
		sampleEntityDocumentIDKeyed.setVersion(System.currentTimeMillis());
		repository.save(sampleEntityDocumentIDKeyed);

		SampleDocumentID documentId2 = getRandomSampleElasticID();
		SampleEntityDocumentIDKeyed sampleEntityElasticIDKeyed2 = new SampleEntityDocumentIDKeyed();
		sampleEntityElasticIDKeyed2.setId(documentId2);
		sampleEntityElasticIDKeyed2.setMessage("hello world.");
		sampleEntityElasticIDKeyed2.setVersion(System.currentTimeMillis());
		repository.save(sampleEntityElasticIDKeyed2);

		// when
		LinkedList<SampleEntityDocumentIDKeyed> sampleEntities = (LinkedList<SampleEntityDocumentIDKeyed>) repository
				.findAllById(Arrays.asList(documentId, documentId2));

		// then
		assertNotNull("sample entities cant be null..", sampleEntities);
		assertThat(sampleEntities.size(), is(2));
		assertThat(sampleEntities.get(0).getId(), isIn(Arrays.asList(documentId, documentId2)));
		assertThat(sampleEntities.get(1).getId(), isIn(Arrays.asList(documentId, documentId2)));
	}

	@Test
	public void shouldSaveIterableEntities() {
		// given
		SampleDocumentID documentId = getRandomSampleElasticID();
		SampleEntityDocumentIDKeyed sampleEntityElasticIDKeyed1 = new SampleEntityDocumentIDKeyed();
		sampleEntityElasticIDKeyed1.setId(documentId);
		sampleEntityElasticIDKeyed1.setMessage("hello world.");
		sampleEntityElasticIDKeyed1.setVersion(System.currentTimeMillis());

		SampleDocumentID documentId2 = getRandomSampleElasticID();
		SampleEntityDocumentIDKeyed sampleEntityElasticIDKeyed2 = new SampleEntityDocumentIDKeyed();
		sampleEntityElasticIDKeyed2.setId(documentId2);
		sampleEntityElasticIDKeyed2.setMessage("hello world.");
		sampleEntityElasticIDKeyed2.setVersion(System.currentTimeMillis());

		Iterable<SampleEntityDocumentIDKeyed> sampleEntities = Arrays.asList(sampleEntityElasticIDKeyed1,
				sampleEntityElasticIDKeyed2);
		// when
		repository.saveAll(sampleEntities);
		// then
		Page<SampleEntityDocumentIDKeyed> entities = repository.search(termQuery("id", documentId.toString()),
				new PageRequest(0, 50));
		assertNotNull(entities);
	}

	@Test
	public void shouldReturnTrueGivenDocumentWithIdExists() {
		// given
		SampleDocumentID documentId = getRandomSampleElasticID();
		SampleEntityDocumentIDKeyed sampleEntityDocumentIDKeyed = new SampleEntityDocumentIDKeyed();
		sampleEntityDocumentIDKeyed.setId(documentId);
		sampleEntityDocumentIDKeyed.setMessage("hello world.");
		sampleEntityDocumentIDKeyed.setVersion(System.currentTimeMillis());
		repository.save(sampleEntityDocumentIDKeyed);

		// when
		boolean exist = repository.existsById(documentId);

		// then
		assertEquals(exist, true);
	}

	@Test // DATAES-363
	public void shouldReturnFalseGivenDocumentWithIdDoesNotExist() {

		// given
		SampleDocumentID documentId = getRandomSampleElasticID();

		// when
		boolean exist = repository.existsById(documentId);

		// then
		assertThat(exist, is(false));
	}

	@Test
	public void shouldDeleteAll() {
		// when
		repository.deleteAll();
		// then
		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery()).build();
		Page<SampleEntityDocumentIDKeyed> sampleEntities = repository.search(searchQuery);
		assertThat(sampleEntities.getTotalElements(), equalTo(0L));
	}

	@Test
	public void shouldDeleteById() {
		// given
		SampleDocumentID documentId = getRandomSampleElasticID();
		SampleEntityDocumentIDKeyed sampleEntityDocumentIDKeyed = new SampleEntityDocumentIDKeyed();
		sampleEntityDocumentIDKeyed.setId(documentId);
		sampleEntityDocumentIDKeyed.setMessage("hello world.");
		sampleEntityDocumentIDKeyed.setVersion(System.currentTimeMillis());
		repository.save(sampleEntityDocumentIDKeyed);
		// when
		repository.deleteById(documentId);
		// then
		assertThat(repository.count(), equalTo(0l));
	}

	@Test
	public void shouldDeleteByMessageAndReturnList() {
		// given
		SampleDocumentID documentId = getRandomSampleElasticID();
		SampleEntityDocumentIDKeyed sampleEntityElasticIDKeyed1 = new SampleEntityDocumentIDKeyed();
		sampleEntityElasticIDKeyed1.setId(documentId);
		sampleEntityElasticIDKeyed1.setMessage("hello world 1");
		sampleEntityElasticIDKeyed1.setAvailable(true);
		sampleEntityElasticIDKeyed1.setVersion(System.currentTimeMillis());

		documentId = getRandomSampleElasticID();
		SampleEntityDocumentIDKeyed sampleEntityElasticIDKeyed2 = new SampleEntityDocumentIDKeyed();
		sampleEntityElasticIDKeyed2.setId(documentId);
		sampleEntityElasticIDKeyed2.setMessage("hello world 2");
		sampleEntityElasticIDKeyed2.setAvailable(true);
		sampleEntityElasticIDKeyed2.setVersion(System.currentTimeMillis());

		documentId = getRandomSampleElasticID();
		SampleEntityDocumentIDKeyed sampleEntityElasticIDKeyed3 = new SampleEntityDocumentIDKeyed();
		sampleEntityElasticIDKeyed3.setId(documentId);
		sampleEntityElasticIDKeyed3.setMessage("hello world 3");
		sampleEntityElasticIDKeyed3.setAvailable(false);
		sampleEntityElasticIDKeyed3.setVersion(System.currentTimeMillis());
		repository
				.saveAll(Arrays.asList(sampleEntityElasticIDKeyed1, sampleEntityElasticIDKeyed2, sampleEntityElasticIDKeyed3));
		// when
		List<SampleEntityDocumentIDKeyed> result = repository.deleteByAvailable(true);
		repository.refresh();
		// then
		assertThat(result.size(), equalTo(2));
		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery()).build();
		Page<SampleEntityDocumentIDKeyed> sampleEntities = repository.search(searchQuery);
		assertThat(sampleEntities.getTotalElements(), equalTo(1L));
	}

	@Test
	public void shouldDeleteByListForMessage() {
		// given
		SampleDocumentID documentId = getRandomSampleElasticID();
		SampleEntityDocumentIDKeyed sampleEntityElasticIDKeyed1 = new SampleEntityDocumentIDKeyed();
		sampleEntityElasticIDKeyed1.setId(documentId);
		sampleEntityElasticIDKeyed1.setMessage("hello world 1");
		sampleEntityElasticIDKeyed1.setVersion(System.currentTimeMillis());

		documentId = getRandomSampleElasticID();
		SampleEntityDocumentIDKeyed sampleEntityElasticIDKeyed2 = new SampleEntityDocumentIDKeyed();
		sampleEntityElasticIDKeyed2.setId(documentId);
		sampleEntityElasticIDKeyed2.setMessage("hello world 2");
		sampleEntityElasticIDKeyed2.setVersion(System.currentTimeMillis());

		documentId = getRandomSampleElasticID();
		SampleEntityDocumentIDKeyed sampleEntityElasticIDKeyed3 = new SampleEntityDocumentIDKeyed();
		sampleEntityElasticIDKeyed3.setId(documentId);
		sampleEntityElasticIDKeyed3.setMessage("hello world 3");
		sampleEntityElasticIDKeyed3.setVersion(System.currentTimeMillis());
		repository
				.saveAll(Arrays.asList(sampleEntityElasticIDKeyed1, sampleEntityElasticIDKeyed2, sampleEntityElasticIDKeyed3));
		// when
		List<SampleEntityDocumentIDKeyed> result = repository.deleteByMessage("hello world 3");
		repository.refresh();
		// then
		assertThat(result.size(), equalTo(1));
		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery()).build();
		Page<SampleEntityDocumentIDKeyed> sampleEntities = repository.search(searchQuery);
		assertThat(sampleEntities.getTotalElements(), equalTo(2L));
	}

	@Test
	public void shouldDeleteByType() {
		// given
		SampleDocumentID documentId = getRandomSampleElasticID();
		SampleEntityDocumentIDKeyed sampleEntityElasticIDKeyed1 = new SampleEntityDocumentIDKeyed();
		sampleEntityElasticIDKeyed1.setId(documentId);
		sampleEntityElasticIDKeyed1.setType("book");
		sampleEntityElasticIDKeyed1.setVersion(System.currentTimeMillis());

		documentId = getRandomSampleElasticID();
		SampleEntityDocumentIDKeyed sampleEntityElasticIDKeyed2 = new SampleEntityDocumentIDKeyed();
		sampleEntityElasticIDKeyed2.setId(documentId);
		sampleEntityElasticIDKeyed2.setType("article");
		sampleEntityElasticIDKeyed2.setVersion(System.currentTimeMillis());

		documentId = getRandomSampleElasticID();
		SampleEntityDocumentIDKeyed sampleEntityElasticIDKeyed3 = new SampleEntityDocumentIDKeyed();
		sampleEntityElasticIDKeyed3.setId(documentId);
		sampleEntityElasticIDKeyed3.setType("image");
		sampleEntityElasticIDKeyed3.setVersion(System.currentTimeMillis());
		repository
				.saveAll(Arrays.asList(sampleEntityElasticIDKeyed1, sampleEntityElasticIDKeyed2, sampleEntityElasticIDKeyed3));
		// when
		repository.deleteByType("article");
		repository.refresh();
		// then
		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery()).build();
		Page<SampleEntityDocumentIDKeyed> sampleEntities = repository.search(searchQuery);
		assertThat(sampleEntities.getTotalElements(), equalTo(2L));
	}

	@Test
	public void shouldDeleteEntity() {
		// given
		SampleDocumentID documentId = getRandomSampleElasticID();
		SampleEntityDocumentIDKeyed sampleEntityDocumentIDKeyed = new SampleEntityDocumentIDKeyed();
		sampleEntityDocumentIDKeyed.setId(documentId);
		sampleEntityDocumentIDKeyed.setMessage("hello world.");
		sampleEntityDocumentIDKeyed.setVersion(System.currentTimeMillis());
		repository.save(sampleEntityDocumentIDKeyed);
		// when
		repository.delete(sampleEntityDocumentIDKeyed);
		repository.refresh();
		// then
		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(termQuery("id", documentId.toString())).build();
		Page<SampleEntityDocumentIDKeyed> sampleEntities = repository.search(searchQuery);
		assertThat(sampleEntities.getTotalElements(), equalTo(0L));
	}

	@Test
	public void shouldReturnIterableEntities() {
		// given
		SampleDocumentID documentId1 = getRandomSampleElasticID();
		SampleEntityDocumentIDKeyed sampleEntityElasticIDKeyed1 = new SampleEntityDocumentIDKeyed();
		sampleEntityElasticIDKeyed1.setId(documentId1);
		sampleEntityElasticIDKeyed1.setMessage("hello world.");
		sampleEntityElasticIDKeyed1.setVersion(System.currentTimeMillis());
		repository.save(sampleEntityElasticIDKeyed1);

		SampleDocumentID documentId2 = getRandomSampleElasticID();
		SampleEntityDocumentIDKeyed sampleEntityElasticIDKeyed2 = new SampleEntityDocumentIDKeyed();
		sampleEntityElasticIDKeyed2.setId(documentId2);
		sampleEntityElasticIDKeyed2.setMessage("hello world.");
		sampleEntityElasticIDKeyed2.setVersion(System.currentTimeMillis());
		repository.save(sampleEntityElasticIDKeyed2);

		// when
		Iterable<SampleEntityDocumentIDKeyed> sampleEntities = repository.search(termQuery("id", documentId1.toString()));
		// then
		assertNotNull("sample entities cant be null..", sampleEntities);
	}

	@Test
	public void shouldDeleteIterableEntities() {
		// given
		SampleDocumentID documentId1 = getRandomSampleElasticID();
		SampleEntityDocumentIDKeyed sampleEntityElasticIDKeyed1 = new SampleEntityDocumentIDKeyed();
		sampleEntityElasticIDKeyed1.setId(documentId1);
		sampleEntityElasticIDKeyed1.setMessage("hello world.");
		sampleEntityElasticIDKeyed1.setVersion(System.currentTimeMillis());

		SampleDocumentID documentId2 = getRandomSampleElasticID();
		SampleEntityDocumentIDKeyed sampleEntityElasticIDKeyed2 = new SampleEntityDocumentIDKeyed();
		sampleEntityElasticIDKeyed2.setId(documentId2);
		sampleEntityElasticIDKeyed2.setMessage("hello world.");
		sampleEntityElasticIDKeyed2.setVersion(System.currentTimeMillis());
		repository.save(sampleEntityElasticIDKeyed2);

		Iterable<SampleEntityDocumentIDKeyed> sampleEntities = Arrays.asList(sampleEntityElasticIDKeyed2,
				sampleEntityElasticIDKeyed2);
		// when
		repository.deleteAll(sampleEntities);
		// then
		assertThat(repository.findById(documentId1).isPresent(), is(false));
		assertThat(repository.findById(documentId2).isPresent(), is(false));
	}

	@Test
	public void shouldSortByGivenField() {
		// given
		SampleDocumentID documentId = getRandomSampleElasticID();
		SampleEntityDocumentIDKeyed sampleEntityDocumentIDKeyed = new SampleEntityDocumentIDKeyed();
		sampleEntityDocumentIDKeyed.setId(documentId);
		sampleEntityDocumentIDKeyed.setMessage("world");
		repository.save(sampleEntityDocumentIDKeyed);

		SampleDocumentID documentId2 = getRandomSampleElasticID();
		SampleEntityDocumentIDKeyed sampleEntityElasticIDKeyed2 = new SampleEntityDocumentIDKeyed();
		sampleEntityElasticIDKeyed2.setId(documentId2);
		sampleEntityElasticIDKeyed2.setMessage("hello");
		repository.save(sampleEntityElasticIDKeyed2);
		// when
		Iterable<SampleEntityDocumentIDKeyed> sampleEntities = repository
				.findAll(new Sort(new Sort.Order(Sort.Direction.ASC, "message")));
		// then
		assertThat(sampleEntities, is(notNullValue()));
	}

	@Test
	public void shouldReturnSimilarEntities() {
		// given
		String sampleMessage = "So we build a web site or an application and want to add search to it, "
				+ "and then it hits us: getting search working is hard. We want our search solution to be fast,"
				+ " we want a painless setup and a completely free search schema, we want to be able to index data simply using JSON over HTTP, "
				+ "we want our search server to be always available, we want to be able to start with one machine and scale to hundreds, "
				+ "we want real-time search, we want simple multi-tenancy, and we want a solution that is built for the cloud.";

		List<SampleEntityDocumentIDKeyed> sampleEntities = createSampleEntitiesWithMessage(sampleMessage, 30);
		repository.saveAll(sampleEntities);

		// when
		Page<SampleEntityDocumentIDKeyed> results = repository.searchSimilar(sampleEntities.get(0),
				new String[] { "message" }, new PageRequest(0, 5));

		// then
		assertThat(results.getTotalElements(), is(greaterThanOrEqualTo(1L)));
	}

	private static List<SampleEntityDocumentIDKeyed> createSampleEntitiesWithMessage(String message,
			int numberOfEntities) {
		List<SampleEntityDocumentIDKeyed> sampleEntities = new ArrayList<>();
		for (int i = 0; i < numberOfEntities; i++) {
			SampleDocumentID documentId = getRandomSampleElasticID();
			SampleEntityDocumentIDKeyed sampleEntityDocumentIDKeyed = new SampleEntityDocumentIDKeyed();
			sampleEntityDocumentIDKeyed.setId(documentId);
			sampleEntityDocumentIDKeyed.setMessage(message);
			sampleEntityDocumentIDKeyed.setRate(2);
			sampleEntityDocumentIDKeyed.setVersion(System.currentTimeMillis());
			sampleEntities.add(sampleEntityDocumentIDKeyed);
		}
		return sampleEntities;
	}

	private static SampleDocumentID getRandomSampleElasticID() {
		return SampleDocumentID.builder().id1(UUID.randomUUID().toString()).id2(UUID.randomUUID().toString()).build();
	}
}
