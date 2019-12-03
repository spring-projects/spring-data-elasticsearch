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

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.junit.jupiter.ElasticsearchRestTemplateConfiguration;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.test.context.ContextConfiguration;

/**
 * DynamicIndexEntityTests
 *
 * @author Sylvain Laurent
 * @author Peter-Josef Meisch
 */

@SpringIntegrationTest
@ContextConfiguration(classes = { DynamicIndexEntityTests.Config.class })
public class DynamicIndexEntityTests {

	@Configuration
	@Import({ ElasticsearchRestTemplateConfiguration.class })
	@EnableElasticsearchRepositories(considerNestedRepositories = true)
	static class Config {
		@Bean
		public IndexNameProvider indexNameProvider() {
			return new IndexNameProvider();
		}
	}

	@Autowired private DynamicIndexRepository repository;

	@Autowired private ElasticsearchOperations operations;
	private IndexOperations indexOperations;

	@Autowired private IndexNameProvider indexNameProvider;

	@BeforeEach
	public void init() {

		indexOperations = operations.getIndexOperations();

		deleteIndexes();
		indexOperations.createIndex("index1");
		indexOperations.createIndex("index2");
	}

	@AfterEach
	public void teardown() {
		deleteIndexes();
	}

	private void deleteIndexes() {

		indexOperations.deleteIndex("index1");
		indexOperations.deleteIndex("index2");
	}

	@Test // DATAES-456
	public void indexNameIsDynamicallyProvided() {

		int initialCallsCount = indexNameProvider.callsCount;

		indexNameProvider.setIndexName("index1");
		repository.save(new DynamicIndexEntity());
		assertThat(indexNameProvider.callsCount > initialCallsCount).isTrue();
		assertThat(repository.count()).isEqualTo(1L);

		indexNameProvider.setIndexName("index2");
		assertThat(repository.count()).isEqualTo(0L);
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
