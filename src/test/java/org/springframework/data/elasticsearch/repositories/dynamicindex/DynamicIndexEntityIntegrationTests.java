/*
 * Copyright 2019-2024 the original author or authors.
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
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * DynamicIndexEntityIntegration. Not: this does not use the normal
 * {@link org.springframework.data.elasticsearch.utils.IndexNameProvider} b ut testes this functionality with a custom
 * one.
 *
 * @author Sylvain Laurent
 * @author Peter-Josef Meisch
 */

@SpringIntegrationTest
public abstract class DynamicIndexEntityIntegrationTests {

	@Autowired private DynamicIndexRepository repository;

	@Autowired ElasticsearchOperations operations;
	private IndexOperations indexOperations;

	@Autowired private IndexNameProvider indexNameProvider;

	@BeforeEach
	public void init() {
		indexOperations = operations.indexOps(IndexCoordinates.of("index1"));
		deleteIndexes();
		operations.indexOps(IndexCoordinates.of("index1")).create();
		operations.indexOps(IndexCoordinates.of("index2")).create();
	}

	@AfterEach
	public void after() {
		deleteIndexes();
	}

	private void deleteIndexes() {

		indexOperations.delete();
		operations.indexOps(IndexCoordinates.of("index2")).delete();
	}

	@Test // DATAES-456
	public void indexNameIsDynamicallyProvided() {

		int initialCallsCount = indexNameProvider.callsCount;

		indexNameProvider.indexName = "index1";
		repository.save(new DynamicIndexEntity());
		assertThat(indexNameProvider.callsCount > initialCallsCount).isTrue();
		assertThat(repository.count()).isEqualTo(1L);

		indexNameProvider.indexName = "index2";
		assertThat(repository.count()).isEqualTo(0L);
	}

	@Test // DATAES-821
	void indexOpsShouldUseDynamicallyProvidedName() {

		indexNameProvider.indexName = "index-dynamic";
		indexNameProvider.callsCount = 0;

		operations.indexOps(IndexCoordinates.of("index-dynamic")).delete();

		IndexOperations indexOps = operations.indexOps(DynamicIndexEntity.class);
		indexOps.create();
		indexOps.refresh();
		indexOps.refresh();
		indexOps.delete(); // internally calls doExists

		assertThat(indexNameProvider.callsCount).isGreaterThan(0);
	}

	static class IndexNameProvider {

		private String indexName;

		private int callsCount;

		public String getIndexName() {

			callsCount++;
			return indexName;
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
