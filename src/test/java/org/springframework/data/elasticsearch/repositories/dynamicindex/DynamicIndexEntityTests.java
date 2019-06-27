/*
 * Copyright 2019 the original author or authors.
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
package org.springframework.data.elasticsearch.repositories.dynamicindex;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportResource;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * DynamicIndexEntityTests
 *
 * @author Sylvain Laurent
 * @author Peter-Josef Meisch
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = DynamicIndexEntityTests.TestConfig.class)
public class DynamicIndexEntityTests {

	@Autowired private DynamicIndexRepository repository;

	@Autowired private ElasticsearchTemplate template;

	@Autowired private IndexNameProvider indexNameProvider;

	@Before
	public void init() {

		deleteIndexes();
		template.createIndex("index1");
		template.createIndex("index2");
	}

	@After
	public void teardown() {
		deleteIndexes();
	}

	private void deleteIndexes() {

		template.deleteIndex("index1");
		template.deleteIndex("index2");
	}

	@Test // DATAES-456
	public void indexNameIsDynamicallyProvided() {

		int initialCallsCount = indexNameProvider.callsCount;

		indexNameProvider.setIndexName("index1");
		repository.save(new DynamicIndexEntity());
		assertTrue(indexNameProvider.callsCount > initialCallsCount);
		assertEquals(1L, repository.count());

		indexNameProvider.setIndexName("index2");
		assertEquals(0L, repository.count());
	}

	@ImportResource(value = "classpath:/dynamic-index-repository-test.xml")
	static class TestConfig {

		@Bean
		public IndexNameProvider indexNameProvider() {
			return new IndexNameProvider();
		}

	}

	static class IndexNameProvider {

		private String indexName;

		int callsCount;

		public String getIndexName() {

			callsCount++;
			return indexName;
		}

		public void setIndexName(String indexName) {
			this.indexName = indexName;
		}

	}

	@Document(indexName = "#{@indexNameProvider.getIndexName()}", createIndex = false)
	public static class DynamicIndexEntity {

		@Id private String id;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}
	}

	public interface DynamicIndexRepository extends ElasticsearchRepository<DynamicIndexEntity, String> {}

}
