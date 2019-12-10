/*
 * Copyright 2014-2019 the original author or authors.
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

import org.elasticsearch.index.query.QueryBuilders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.junit.jupiter.ElasticsearchRestTemplateConfiguration;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.elasticsearch.utils.IndexInitializer;
import org.springframework.test.context.ContextConfiguration;

/**
 * SpELEntityTest
 *
 * @author Artur Konczak
 * @author Peter-Josef Meisch
 */
@SpringIntegrationTest
@ContextConfiguration(classes = { SpELEntityTests.Config.class })
public class SpELEntityTests {

	@Configuration
	@Import(ElasticsearchRestTemplateConfiguration.class)
	@EnableElasticsearchRepositories(considerNestedRepositories = true)
	static class Config {}

	@Autowired private SpELRepository repository;

	@Autowired private ElasticsearchOperations operations;

	@BeforeEach
	public void before() {
		IndexInitializer.init(operations, SpELEntity.class);
	}

	@Test
	public void shouldDo() {

		// given
		repository.save(new SpELEntity());
		repository.save(new SpELEntity());

		// when

		// then
		NativeSearchQuery nativeSearchQuery = new NativeSearchQuery(QueryBuilders.matchAllQuery());
		long count = operations.count(nativeSearchQuery, IndexCoordinates.of("test-index-abz-entity"));
		assertThat(count).isEqualTo(2);
	}

	@Test
	public void shouldSupportSpelInType() {

		// given
		SpELEntity spELEntity = new SpELEntity();
		repository.save(spELEntity);

		// when

		// then
		NativeSearchQuery nativeSearchQuery = new NativeSearchQuery(QueryBuilders.matchAllQuery());
		long count = operations.count(nativeSearchQuery, IndexCoordinates.of("test-index-abz-entity"));
		assertThat(count).isEqualTo(1);
	}

	/**
	 * SpELEntity
	 *
	 * @author Artur Konczak
	 */
	@Document(indexName = "#{'test-index-abz'+'-'+'entity'}", type = "#{'my'+'Type'}", shards = 1, replicas = 0,
			refreshInterval = "-1")
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
