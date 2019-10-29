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
package org.springframework.data.elasticsearch.repositories.synonym;

import static org.assertj.core.api.Assertions.*;

import lombok.Data;

import java.util.List;

import org.elasticsearch.index.query.QueryBuilders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Mapping;
import org.springframework.data.elasticsearch.annotations.Setting;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.junit.jupiter.ElasticsearchTemplateConfiguration;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.data.elasticsearch.repository.ElasticsearchCrudRepository;
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
	@Import({ ElasticsearchTemplateConfiguration.class })
	@EnableElasticsearchRepositories(basePackages = { "org.springframework.data.elasticsearch.repositories.synonym" },
			considerNestedRepositories = true)
	static class Config {}

	@Autowired private SynonymRepository repository;

	@Autowired private ElasticsearchTemplate elasticsearchTemplate;

	@BeforeEach
	public void before() {
		IndexInitializer.init(elasticsearchTemplate, SynonymEntity.class);
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

		// when

		// then
		assertThat(repository.count()).isEqualTo(2L);

		List<SynonymEntity> synonymEntities = elasticsearchTemplate.queryForList(
				new NativeSearchQueryBuilder().withQuery(QueryBuilders.termQuery("text", "british")).build(),
				SynonymEntity.class, IndexCoordinates.of("test-index-synonym").withTypes( "synonym-type"));
		assertThat(synonymEntities).hasSize(1);
	}

	/**
	 * @author Mohsin Husen
	 */
	@Data
	@Document(indexName = "test-index-synonym", type = "synonym-type")
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
	interface SynonymRepository extends ElasticsearchCrudRepository<SynonymEntity, String> {}
}
