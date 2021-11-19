/*
 * Copyright 2014-2021 the original author or authors.
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
package org.springframework.data.elasticsearch.repositories.spel;

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
import org.springframework.data.elasticsearch.utils.IndexInitializer;

/**
 * SpELEntityTest
 *
 * @author Artur Konczak
 * @author Peter-Josef Meisch
 */
@SpringIntegrationTest
public abstract class SpELEntityIntegrationTests {

	@Autowired private SpELRepository repository;

	@Autowired private ElasticsearchOperations operations;
	private IndexOperations indexOperations;

	@BeforeEach
	public void before() {
		indexOperations = operations.indexOps(SpELEntity.class);
		IndexInitializer.init(indexOperations);
	}

	@AfterEach
	void after() {
		operations.indexOps(IndexCoordinates.of("test-index-abz-*")).delete();
	}

	@Test
	public void shouldDo() {

		// given
		repository.save(new SpELEntity());
		repository.save(new SpELEntity());

		// when

		// then
		long count = operations.count(operations.matchAllQuery(), IndexCoordinates.of("test-index-abz-entity"));
		assertThat(count).isEqualTo(2);
	}

	@Test
	public void shouldSupportSpelInType() {

		// given
		SpELEntity spELEntity = new SpELEntity();
		repository.save(spELEntity);

		// when

		// then
		long count = operations.count(operations.matchAllQuery(), IndexCoordinates.of("test-index-abz-entity"));
		assertThat(count).isEqualTo(1);
	}

	@Document(indexName = "#{'test-index-abz'+'-'+'entity'}")
	static class SpELEntity {

		@Id private String id;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}
	}

	interface SpELRepository extends ElasticsearchRepository<SpELEntity, String> {}
}
