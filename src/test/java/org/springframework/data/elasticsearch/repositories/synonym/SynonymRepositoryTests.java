/*
 * Copyright 2014-2020 the original author or authors.
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
package org.springframework.data.elasticsearch.repositories.synonym;

import static org.assertj.core.api.Assertions.*;

import lombok.Data;

import org.elasticsearch.index.query.QueryBuilders;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Mapping;
import org.springframework.data.elasticsearch.annotations.Setting;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.junit.jupiter.ElasticsearchRestTemplateConfiguration;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.elasticsearch.utils.IndexInitializer;
import org.springframework.test.context.ContextConfiguration;

/**
 * SynonymRepositoryTests
 *
 * @author Artur Konczak
 * @author Peter-Josef Meisch
 */
@SpringIntegrationTest
@ContextConfiguration(classes = { SynonymRepositoryTests.Config.class })
public class SynonymRepositoryTests {

	@Configuration
	@Import({ ElasticsearchRestTemplateConfiguration.class })
	@EnableElasticsearchRepositories(considerNestedRepositories = true)
	static class Config {}

	@Autowired private SynonymRepository repository;

	@Autowired private ElasticsearchOperations operations;
	private IndexOperations indexOperations;

	@BeforeEach
	public void before() {
		indexOperations = operations.indexOps(SynonymEntity.class);
		IndexInitializer.init(indexOperations);
	}

	@AfterEach
	void after() {
		indexOperations.delete();
	}

	@Test
	public void shouldDo() {

		// given
		SynonymEntity entry1 = new SynonymEntity();
		entry1.setText("Elizabeth is the english queen");
		SynonymEntity entry2 = new SynonymEntity();
		entry2.setText("Other text");

		repository.save(entry1);
		repository.save(entry2);

		// whe
		// then
		assertThat(repository.count()).isEqualTo(2L);

		SearchHits<SynonymEntity> synonymEntities = operations.search(
				new NativeSearchQueryBuilder().withQuery(QueryBuilders.termQuery("text", "british")).build(),
				SynonymEntity.class, IndexCoordinates.of("test-index-synonym"));
		assertThat(synonymEntities).hasSize(1);
	}

	/**
	 * @author Mohsin Husen
	 */
	@Data
	@Document(indexName = "test-index-synonym")
	@Setting(settingPath = "/synonyms/settings.json")
	@Mapping(mappingPath = "/synonyms/mappings.json")
	static class SynonymEntity {

		@Id private String id;
		private String text;
	}

	/**
	 * SynonymRepository
	 *
	 * @author Artur Konczak
	 */
	interface SynonymRepository extends ElasticsearchRepository<SynonymEntity, String> {}
}
