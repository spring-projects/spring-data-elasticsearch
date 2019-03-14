/*
 * Copyright 2013-2019 the original author or authors.
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

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.entities.DynamicIndexEntity;
import org.springframework.data.elasticsearch.repositories.dynamicindex.DynamicIndexRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Ivan Greene
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:/dynamic-index-repository-test.xml")
public class DynamicIndexRepositoryTests {

	@Autowired private DynamicIndexRepository repository;

	@Autowired private ElasticsearchTemplate elasticsearchTemplate;

	private String indexNameA = "test-dyn-index-one";
	private String indexNameB = "test-dyn-index-two";

	@Before
	public void before() {
		elasticsearchTemplate.deleteIndex(indexNameA);
		elasticsearchTemplate.createIndex(indexNameA);
		elasticsearchTemplate.putMapping(indexNameA, DynamicIndexEntity.class);
		elasticsearchTemplate.refresh(indexNameA);
		elasticsearchTemplate.deleteIndex(indexNameB);
		elasticsearchTemplate.createIndex(indexNameB);
		elasticsearchTemplate.putMapping(indexNameB, DynamicIndexEntity.class);
		elasticsearchTemplate.refresh(indexNameB);
	}

	private DynamicIndexEntity storeTestDocument(String indexName, String id, String name) {
		DynamicIndexEntity entity = DynamicIndexEntity.builder()
				.id(id)
				.index(indexName)
				.name(name)
				.build();
		return repository.save(entity);
	}

	@Test
	public void shouldDoBasicCrud() {
		// given
		String documentIdA = RandomStringUtils.random(10);
		String nameA = RandomStringUtils.random(10);

		String documentIdB = RandomStringUtils.random(10);
		String nameB = RandomStringUtils.random(10);

		DynamicIndexEntity entityA = storeTestDocument(indexNameA, documentIdA, nameA);
		DynamicIndexEntity entityB = storeTestDocument(indexNameB, documentIdB, nameB);

		// then
		assertEquals(1, repository.count(indexNameA));
		assertTrue(repository.existsById(indexNameA, documentIdA));

		Optional<DynamicIndexEntity> entityAFromElasticSearch = repository.findById(indexNameA, documentIdA);
		assertTrue(entityAFromElasticSearch.isPresent());
		assertEquals(entityA, entityAFromElasticSearch.get());

		assertEquals(1, repository.count(indexNameB));
		assertTrue(repository.existsById(indexNameB, documentIdB));

		Optional<DynamicIndexEntity> entityBFromElasticSearch = repository.findById(indexNameB, documentIdB);
		assertTrue(entityBFromElasticSearch.isPresent());
		assertEquals(entityB, entityBFromElasticSearch.get());

		repository.delete(entityA);
		assertEquals(0, repository.count(indexNameA));
		repository.deleteById(indexNameB, documentIdB);
		assertEquals(0, repository.count(indexNameB));
	}

	@Test
	public void shouldFindAll() {
		List<DynamicIndexEntity> entities = Stream.generate(() -> storeTestDocument(indexNameA, RandomStringUtils.random(10), RandomStringUtils.random(10)))
				.limit(10)
				.collect(Collectors.toList());

		List<DynamicIndexEntity> all = new ArrayList<>();
		repository.findAll(indexNameA).forEach(all::add);

		assertEquals(10, all.size());
		assertTrue(all.containsAll(entities));
	}

	@Test(expected = UnsupportedOperationException.class)
	public void shouldNotFindAll() {
		repository.findAll();
	}

	@Test(expected = UnsupportedOperationException.class)
	public void shouldNotDeleteAll() {
		repository.deleteAll();
	}

	@Test(expected = UnsupportedOperationException.class)
	public void shouldNotCount() {
		repository.count();
	}

	@Test(expected = UnsupportedOperationException.class)
	public void shouldNotRefresh() {
		repository.refresh();
	}

	@Test(expected = UnsupportedOperationException.class)
	public void shouldNotExistsById() {
		repository.existsById("foo");
	}

}
