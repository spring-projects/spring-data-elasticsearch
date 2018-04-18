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
package org.springframework.data.elasticsearch.repository.support;

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
import org.springframework.data.elasticsearch.entities.SampleEntityUUIDKeyed;
import org.springframework.data.elasticsearch.repositories.sample.SampleUUIDKeyedElasticsearchRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import static org.elasticsearch.index.query.QueryBuilders.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * @author Gad Akuka
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Mark Paluch
 * @author Christoph Strobl
 * @author Michael Wirth
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:/simple-repository-test.xml")
public class UUIDElasticsearchRepositoryTests {

	@Autowired private SampleUUIDKeyedElasticsearchRepository repository;

	@Autowired private ElasticsearchTemplate elasticsearchTemplate;

	@Before
	public void before() {
		elasticsearchTemplate.deleteIndex(SampleEntityUUIDKeyed.class);
		elasticsearchTemplate.createIndex(SampleEntityUUIDKeyed.class);
		elasticsearchTemplate.putMapping(SampleEntityUUIDKeyed.class);
		elasticsearchTemplate.refresh(SampleEntityUUIDKeyed.class);
	}

	@Test
	public void shouldDoBulkIndexDocument() {
		// given
		UUID documentId1 = UUID.randomUUID();
		SampleEntityUUIDKeyed sampleEntityUUIDKeyed1 = new SampleEntityUUIDKeyed();
		sampleEntityUUIDKeyed1.setId(documentId1);
		sampleEntityUUIDKeyed1.setMessage("some message");
		sampleEntityUUIDKeyed1.setVersion(System.currentTimeMillis());

		UUID documentId2 = UUID.randomUUID();
		SampleEntityUUIDKeyed sampleEntityUUIDKeyed2 = new SampleEntityUUIDKeyed();
		sampleEntityUUIDKeyed2.setId(documentId2);
		sampleEntityUUIDKeyed2.setMessage("some message");
		sampleEntityUUIDKeyed2.setVersion(System.currentTimeMillis());

		// when
		repository.saveAll(Arrays.asList(sampleEntityUUIDKeyed1, sampleEntityUUIDKeyed2));
		// then
		Optional<SampleEntityUUIDKeyed> entity1FromElasticSearch = repository.findById(documentId1);
		assertThat(entity1FromElasticSearch.isPresent(), is(true));

		Optional<SampleEntityUUIDKeyed> entity2FromElasticSearch = repository.findById(documentId2);
		assertThat(entity2FromElasticSearch.isPresent(), is(true));
	}

	@Test
	public void shouldSaveDocument() {
		// given
		UUID documentId = UUID.randomUUID();
		SampleEntityUUIDKeyed sampleEntityUUIDKeyed = new SampleEntityUUIDKeyed();
		sampleEntityUUIDKeyed.setId(documentId);
		sampleEntityUUIDKeyed.setMessage("some message");
		sampleEntityUUIDKeyed.setVersion(System.currentTimeMillis());
		// when
		repository.save(sampleEntityUUIDKeyed);
		// then
		Optional<SampleEntityUUIDKeyed> entityFromElasticSearch = repository.findById(documentId);
		assertThat(entityFromElasticSearch.isPresent(), is(true));
	}

	@Test
	public void shouldFindDocumentById() {
		// given
		UUID documentId = UUID.randomUUID();
		SampleEntityUUIDKeyed sampleEntityUUIDKeyed = new SampleEntityUUIDKeyed();
		sampleEntityUUIDKeyed.setId(documentId);
		sampleEntityUUIDKeyed.setMessage("some message");
		sampleEntityUUIDKeyed.setVersion(System.currentTimeMillis());
		repository.save(sampleEntityUUIDKeyed);
		// when
		Optional<SampleEntityUUIDKeyed> entityFromElasticSearch = repository.findById(documentId);
		// then
		assertThat(entityFromElasticSearch.isPresent(), is(true));
		assertThat(sampleEntityUUIDKeyed, is((equalTo(sampleEntityUUIDKeyed))));
	}

	@Test
	public void shouldReturnCountOfDocuments() {
		// given
		UUID documentId = UUID.randomUUID();
		SampleEntityUUIDKeyed sampleEntityUUIDKeyed = new SampleEntityUUIDKeyed();
		sampleEntityUUIDKeyed.setId(documentId);
		sampleEntityUUIDKeyed.setMessage("some message");
		sampleEntityUUIDKeyed.setVersion(System.currentTimeMillis());
		repository.save(sampleEntityUUIDKeyed);
		// when
		Long count = repository.count();
		// then
		assertThat(count, is(greaterThanOrEqualTo(1L)));
	}

	@Test
	public void shouldFindAllDocuments() {
		// when
		Iterable<SampleEntityUUIDKeyed> results = repository.findAll();
		// then
		assertThat(results, is(notNullValue()));
	}

	@Test
	public void shouldDeleteDocument() {
		// given
		UUID documentId = UUID.randomUUID();
		SampleEntityUUIDKeyed sampleEntityUUIDKeyed = new SampleEntityUUIDKeyed();
		sampleEntityUUIDKeyed.setId(documentId);
		sampleEntityUUIDKeyed.setMessage("some message");
		sampleEntityUUIDKeyed.setVersion(System.currentTimeMillis());
		repository.save(sampleEntityUUIDKeyed);
		// when
		repository.deleteById(documentId);
		// then
		Optional<SampleEntityUUIDKeyed> entityFromElasticSearch = repository.findById(documentId);
		assertThat(entityFromElasticSearch.isPresent(), is(false));
	}

	@Test
	public void shouldSearchDocumentsGivenSearchQuery() {
		// given
		UUID documentId = UUID.randomUUID();
		SampleEntityUUIDKeyed sampleEntityUUIDKeyed = new SampleEntityUUIDKeyed();
		sampleEntityUUIDKeyed.setId(documentId);
		sampleEntityUUIDKeyed.setMessage("some test message");
		sampleEntityUUIDKeyed.setVersion(System.currentTimeMillis());
		repository.save(sampleEntityUUIDKeyed);

		SearchQuery query = new NativeSearchQueryBuilder().withQuery(termQuery("message", "test")).build();
		// when
		Page<SampleEntityUUIDKeyed> page = repository.search(query);
		// then
		assertThat(page, is(notNullValue()));
		assertThat(page.getNumberOfElements(), is(greaterThanOrEqualTo(1)));
	}

	@Test
	public void shouldSearchDocumentsGivenElasticsearchQuery() {
		// given
		UUID documentId = UUID.randomUUID();
		SampleEntityUUIDKeyed sampleEntityUUIDKeyed = new SampleEntityUUIDKeyed();
		sampleEntityUUIDKeyed.setId(documentId);
		sampleEntityUUIDKeyed.setMessage("hello world.");
		sampleEntityUUIDKeyed.setVersion(System.currentTimeMillis());
		repository.save(sampleEntityUUIDKeyed);
		// when
		Page<SampleEntityUUIDKeyed> page = repository.search(termQuery("message", "world"), new PageRequest(0, 50));
		// then
		assertThat(page, is(notNullValue()));
		assertThat(page.getNumberOfElements(), is(greaterThanOrEqualTo(1)));
	}

	/*
	DATAES-82
	*/
	@Test
	public void shouldFindAllByIdQuery() {
		// given
		UUID documentId = UUID.randomUUID();
		SampleEntityUUIDKeyed sampleEntityUUIDKeyed = new SampleEntityUUIDKeyed();
		sampleEntityUUIDKeyed.setId(documentId);
		sampleEntityUUIDKeyed.setMessage("hello world.");
		sampleEntityUUIDKeyed.setVersion(System.currentTimeMillis());
		repository.save(sampleEntityUUIDKeyed);

		UUID documentId2 = UUID.randomUUID();
		SampleEntityUUIDKeyed sampleEntityUUIDKeyed2 = new SampleEntityUUIDKeyed();
		sampleEntityUUIDKeyed2.setId(documentId2);
		sampleEntityUUIDKeyed2.setMessage("hello world.");
		sampleEntityUUIDKeyed2.setVersion(System.currentTimeMillis());
		repository.save(sampleEntityUUIDKeyed2);

		// when
		LinkedList<SampleEntityUUIDKeyed> sampleEntities = (LinkedList<SampleEntityUUIDKeyed>) repository
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
		UUID documentId = UUID.randomUUID();
		SampleEntityUUIDKeyed sampleEntityUUIDKeyed1 = new SampleEntityUUIDKeyed();
		sampleEntityUUIDKeyed1.setId(documentId);
		sampleEntityUUIDKeyed1.setMessage("hello world.");
		sampleEntityUUIDKeyed1.setVersion(System.currentTimeMillis());

		UUID documentId2 = UUID.randomUUID();
		SampleEntityUUIDKeyed sampleEntityUUIDKeyed2 = new SampleEntityUUIDKeyed();
		sampleEntityUUIDKeyed2.setId(documentId2);
		sampleEntityUUIDKeyed2.setMessage("hello world.");
		sampleEntityUUIDKeyed2.setVersion(System.currentTimeMillis());

		Iterable<SampleEntityUUIDKeyed> sampleEntities = Arrays.asList(sampleEntityUUIDKeyed1, sampleEntityUUIDKeyed2);
		// when
		repository.saveAll(sampleEntities);
		// then
		Page<SampleEntityUUIDKeyed> entities = repository.search(termQuery("id", documentId.toString()), new PageRequest(0, 50));
		assertNotNull(entities);
	}

	@Test
	public void shouldReturnTrueGivenDocumentWithIdExists() {
		// given
		UUID documentId = UUID.randomUUID();
		SampleEntityUUIDKeyed sampleEntityUUIDKeyed = new SampleEntityUUIDKeyed();
		sampleEntityUUIDKeyed.setId(documentId);
		sampleEntityUUIDKeyed.setMessage("hello world.");
		sampleEntityUUIDKeyed.setVersion(System.currentTimeMillis());
		repository.save(sampleEntityUUIDKeyed);

		// when
		boolean exist = repository.existsById(documentId);

		// then
		assertEquals(exist, true);
	}

	@Test // DATAES-363
	public void shouldReturnFalseGivenDocumentWithIdDoesNotExist() {
		
		// given
		UUID documentId = UUID.randomUUID();

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
		Page<SampleEntityUUIDKeyed> sampleEntities = repository.search(searchQuery);
		assertThat(sampleEntities.getTotalElements(), equalTo(0L));
	}

	@Test
	public void shouldDeleteById() {
		// given
		UUID documentId = UUID.randomUUID();
		SampleEntityUUIDKeyed sampleEntityUUIDKeyed = new SampleEntityUUIDKeyed();
		sampleEntityUUIDKeyed.setId(documentId);
		sampleEntityUUIDKeyed.setMessage("hello world.");
		sampleEntityUUIDKeyed.setVersion(System.currentTimeMillis());
		repository.save(sampleEntityUUIDKeyed);
		// when
		long result = repository.deleteSampleEntityUUIDKeyedById(documentId);
		// then
		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(termQuery("id", documentId.toString())).build();
		Page<SampleEntityUUIDKeyed> sampleEntities = repository.search(searchQuery);
		assertThat(sampleEntities.getTotalElements(), equalTo(0L));
		assertThat(result, equalTo(1L));
	}

	@Test
	public void shouldDeleteByMessageAndReturnList() {
		// given
		UUID documentId = UUID.randomUUID();
		SampleEntityUUIDKeyed sampleEntityUUIDKeyed1 = new SampleEntityUUIDKeyed();
		sampleEntityUUIDKeyed1.setId(documentId);
		sampleEntityUUIDKeyed1.setMessage("hello world 1");
		sampleEntityUUIDKeyed1.setAvailable(true);
		sampleEntityUUIDKeyed1.setVersion(System.currentTimeMillis());

		documentId = UUID.randomUUID();
		SampleEntityUUIDKeyed sampleEntityUUIDKeyed2 = new SampleEntityUUIDKeyed();
		sampleEntityUUIDKeyed2.setId(documentId);
		sampleEntityUUIDKeyed2.setMessage("hello world 2");
		sampleEntityUUIDKeyed2.setAvailable(true);
		sampleEntityUUIDKeyed2.setVersion(System.currentTimeMillis());

		documentId = UUID.randomUUID();
		SampleEntityUUIDKeyed sampleEntityUUIDKeyed3 = new SampleEntityUUIDKeyed();
		sampleEntityUUIDKeyed3.setId(documentId);
		sampleEntityUUIDKeyed3.setMessage("hello world 3");
		sampleEntityUUIDKeyed3.setAvailable(false);
		sampleEntityUUIDKeyed3.setVersion(System.currentTimeMillis());
		repository.saveAll(Arrays.asList(sampleEntityUUIDKeyed1, sampleEntityUUIDKeyed2, sampleEntityUUIDKeyed3));
		// when
		List<SampleEntityUUIDKeyed> result = repository.deleteByAvailable(true);
		repository.refresh();
		// then
		assertThat(result.size(), equalTo(2));
		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery()).build();
		Page<SampleEntityUUIDKeyed> sampleEntities = repository.search(searchQuery);
		assertThat(sampleEntities.getTotalElements(), equalTo(1L));
	}

	@Test
	public void shouldDeleteByListForMessage() {
		// given
		UUID documentId = UUID.randomUUID();
		SampleEntityUUIDKeyed sampleEntityUUIDKeyed1 = new SampleEntityUUIDKeyed();
		sampleEntityUUIDKeyed1.setId(documentId);
		sampleEntityUUIDKeyed1.setMessage("hello world 1");
		sampleEntityUUIDKeyed1.setVersion(System.currentTimeMillis());

		documentId = UUID.randomUUID();
		SampleEntityUUIDKeyed sampleEntityUUIDKeyed2 = new SampleEntityUUIDKeyed();
		sampleEntityUUIDKeyed2.setId(documentId);
		sampleEntityUUIDKeyed2.setMessage("hello world 2");
		sampleEntityUUIDKeyed2.setVersion(System.currentTimeMillis());

		documentId = UUID.randomUUID();
		SampleEntityUUIDKeyed sampleEntityUUIDKeyed3 = new SampleEntityUUIDKeyed();
		sampleEntityUUIDKeyed3.setId(documentId);
		sampleEntityUUIDKeyed3.setMessage("hello world 3");
		sampleEntityUUIDKeyed3.setVersion(System.currentTimeMillis());
		repository.saveAll(Arrays.asList(sampleEntityUUIDKeyed1, sampleEntityUUIDKeyed2, sampleEntityUUIDKeyed3));
		// when
		List<SampleEntityUUIDKeyed> result = repository.deleteByMessage("hello world 3");
		repository.refresh();
		// then
		assertThat(result.size(), equalTo(1));
		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery()).build();
		Page<SampleEntityUUIDKeyed> sampleEntities = repository.search(searchQuery);
		assertThat(sampleEntities.getTotalElements(), equalTo(2L));
	}

	@Test
	public void shouldDeleteByType() {
		// given
		UUID documentId = UUID.randomUUID();
		SampleEntityUUIDKeyed sampleEntityUUIDKeyed1 = new SampleEntityUUIDKeyed();
		sampleEntityUUIDKeyed1.setId(documentId);
		sampleEntityUUIDKeyed1.setType("book");
		sampleEntityUUIDKeyed1.setVersion(System.currentTimeMillis());

		documentId = UUID.randomUUID();
		SampleEntityUUIDKeyed sampleEntityUUIDKeyed2 = new SampleEntityUUIDKeyed();
		sampleEntityUUIDKeyed2.setId(documentId);
		sampleEntityUUIDKeyed2.setType("article");
		sampleEntityUUIDKeyed2.setVersion(System.currentTimeMillis());

		documentId = UUID.randomUUID();
		SampleEntityUUIDKeyed sampleEntityUUIDKeyed3 = new SampleEntityUUIDKeyed();
		sampleEntityUUIDKeyed3.setId(documentId);
		sampleEntityUUIDKeyed3.setType("image");
		sampleEntityUUIDKeyed3.setVersion(System.currentTimeMillis());
		repository.saveAll(Arrays.asList(sampleEntityUUIDKeyed1, sampleEntityUUIDKeyed2, sampleEntityUUIDKeyed3));
		// when
		repository.deleteByType("article");
		repository.refresh();
		// then
		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery()).build();
		Page<SampleEntityUUIDKeyed> sampleEntities = repository.search(searchQuery);
		assertThat(sampleEntities.getTotalElements(), equalTo(2L));
	}

	@Test
	public void shouldDeleteEntity() {
		// given
		UUID documentId = UUID.randomUUID();
		SampleEntityUUIDKeyed sampleEntityUUIDKeyed = new SampleEntityUUIDKeyed();
		sampleEntityUUIDKeyed.setId(documentId);
		sampleEntityUUIDKeyed.setMessage("hello world.");
		sampleEntityUUIDKeyed.setVersion(System.currentTimeMillis());
		repository.save(sampleEntityUUIDKeyed);
		// when
		repository.delete(sampleEntityUUIDKeyed);
		repository.refresh();
		// then
		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(termQuery("id", documentId.toString())).build();
		Page<SampleEntityUUIDKeyed> sampleEntities = repository.search(searchQuery);
		assertThat(sampleEntities.getTotalElements(), equalTo(0L));
	}

	@Test
	public void shouldReturnIterableEntities() {
		// given
		UUID documentId1 = UUID.randomUUID();
		SampleEntityUUIDKeyed sampleEntityUUIDKeyed1 = new SampleEntityUUIDKeyed();
		sampleEntityUUIDKeyed1.setId(documentId1);
		sampleEntityUUIDKeyed1.setMessage("hello world.");
		sampleEntityUUIDKeyed1.setVersion(System.currentTimeMillis());
		repository.save(sampleEntityUUIDKeyed1);

		UUID documentId2 = UUID.randomUUID();
		SampleEntityUUIDKeyed sampleEntityUUIDKeyed2 = new SampleEntityUUIDKeyed();
		sampleEntityUUIDKeyed2.setId(documentId2);
		sampleEntityUUIDKeyed2.setMessage("hello world.");
		sampleEntityUUIDKeyed2.setVersion(System.currentTimeMillis());
		repository.save(sampleEntityUUIDKeyed2);

		// when
		Iterable<SampleEntityUUIDKeyed> sampleEntities = repository.search(termQuery("id", documentId1.toString()));
		// then
		assertNotNull("sample entities cant be null..", sampleEntities);
	}

	@Test
	public void shouldDeleteIterableEntities() {
		// given
		UUID documentId1 = UUID.randomUUID();
		SampleEntityUUIDKeyed sampleEntityUUIDKeyed1 = new SampleEntityUUIDKeyed();
		sampleEntityUUIDKeyed1.setId(documentId1);
		sampleEntityUUIDKeyed1.setMessage("hello world.");
		sampleEntityUUIDKeyed1.setVersion(System.currentTimeMillis());

		UUID documentId2 = UUID.randomUUID();
		SampleEntityUUIDKeyed sampleEntityUUIDKeyed2 = new SampleEntityUUIDKeyed();
		sampleEntityUUIDKeyed2.setId(documentId2);
		sampleEntityUUIDKeyed2.setMessage("hello world.");
		sampleEntityUUIDKeyed2.setVersion(System.currentTimeMillis());
		repository.save(sampleEntityUUIDKeyed2);

		Iterable<SampleEntityUUIDKeyed> sampleEntities = Arrays.asList(sampleEntityUUIDKeyed2, sampleEntityUUIDKeyed2);
		// when
		repository.deleteAll(sampleEntities);
		// then
		assertThat(repository.findById(documentId1).isPresent(), is(false));
		assertThat(repository.findById(documentId2).isPresent(), is(false));
	}

	@Test
	public void shouldSortByGivenField() {
		// given
		UUID documentId = UUID.randomUUID();
		SampleEntityUUIDKeyed sampleEntityUUIDKeyed = new SampleEntityUUIDKeyed();
		sampleEntityUUIDKeyed.setId(documentId);
		sampleEntityUUIDKeyed.setMessage("world");
		repository.save(sampleEntityUUIDKeyed);

		UUID documentId2 = UUID.randomUUID();
		SampleEntityUUIDKeyed sampleEntityUUIDKeyed2 = new SampleEntityUUIDKeyed();
		sampleEntityUUIDKeyed2.setId(documentId2);
		sampleEntityUUIDKeyed2.setMessage("hello");
		repository.save(sampleEntityUUIDKeyed2);
		// when
		Iterable<SampleEntityUUIDKeyed> sampleEntities = repository
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

		List<SampleEntityUUIDKeyed> sampleEntities = createSampleEntitiesWithMessage(sampleMessage, 30);
		repository.saveAll(sampleEntities);

		// when
		Page<SampleEntityUUIDKeyed> results = repository.searchSimilar(sampleEntities.get(0), new String[] { "message" },
				new PageRequest(0, 5));

		// then
		assertThat(results.getTotalElements(), is(greaterThanOrEqualTo(1L)));
	}

	private static List<SampleEntityUUIDKeyed> createSampleEntitiesWithMessage(String message, int numberOfEntities) {
		List<SampleEntityUUIDKeyed> sampleEntities = new ArrayList<>();
		for (int i = 0; i < numberOfEntities; i++) {
			UUID documentId = UUID.randomUUID();
			SampleEntityUUIDKeyed sampleEntityUUIDKeyed = new SampleEntityUUIDKeyed();
			sampleEntityUUIDKeyed.setId(documentId);
			sampleEntityUUIDKeyed.setMessage(message);
			sampleEntityUUIDKeyed.setRate(2);
			sampleEntityUUIDKeyed.setVersion(System.currentTimeMillis());
			sampleEntities.add(sampleEntityUUIDKeyed);
		}
		return sampleEntities;
	}
}
