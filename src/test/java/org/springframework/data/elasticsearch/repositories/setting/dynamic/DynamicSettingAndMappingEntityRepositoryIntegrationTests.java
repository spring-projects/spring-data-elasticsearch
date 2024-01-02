/*
 * Copyright 2014-2024 the original author or authors.
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
package org.springframework.data.elasticsearch.repositories.setting.dynamic;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.elasticsearch.core.document.Document.*;
import static org.springframework.data.elasticsearch.utils.IdGenerator.*;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Mapping;
import org.springframework.data.elasticsearch.annotations.Setting;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;

/**
 * DynamicSettingAndMappingEntityRepositoryIntegrationTests
 *
 * @author Mohsin Husen
 * @author Ilkang Na
 * @author Peter-Josef Meisch
 */
@SpringIntegrationTest
public abstract class DynamicSettingAndMappingEntityRepositoryIntegrationTests {

	@Autowired private ElasticsearchOperations operations;
	private IndexOperations indexOperations;
	@Autowired IndexNameProvider indexNameProvider;
	@Autowired private DynamicSettingAndMappingEntityRepository repository;

	@BeforeEach
	public void before() {
		indexNameProvider.increment();
		indexOperations = operations.indexOps(DynamicSettingAndMappingEntity.class);
		indexOperations.createWithMapping();
	}

	@Test
	@Order(Integer.MAX_VALUE)
	void cleanup() {
		operations.indexOps(IndexCoordinates.of(indexNameProvider.getPrefix() + "*")).delete();
	}

	@Test // DATAES-64
	public void shouldCreateGivenDynamicSettingsForGivenIndex() {

		assertThat(indexOperations.exists()).isTrue();
		Map<String, Object> map = indexOperations.getSettings().flatten();
		assertThat(map.containsKey("index.number_of_replicas")).isTrue();
		assertThat(map.containsKey("index.number_of_shards")).isTrue();
		assertThat(map.containsKey("index.analysis.analyzer.emailAnalyzer.tokenizer")).isTrue();
		assertThat(map.get("index.number_of_replicas")).isEqualTo("0");
		assertThat(map.get("index.number_of_shards")).isEqualTo("1");
		assertThat(map.get("index.analysis.analyzer.emailAnalyzer.tokenizer")).isEqualTo("uax_url_email");
	}

	@Test // DATAES-64
	public void shouldSearchOnGivenTokenizerUsingGivenDynamicSettingsForGivenIndex() {

		DynamicSettingAndMappingEntity dynamicSettingAndMappingEntity1 = new DynamicSettingAndMappingEntity();
		dynamicSettingAndMappingEntity1.setId(nextIdAsString());
		dynamicSettingAndMappingEntity1.setName("test-setting1");
		dynamicSettingAndMappingEntity1.setEmail("test_setting1@Test.com");

		repository.save(dynamicSettingAndMappingEntity1);

		DynamicSettingAndMappingEntity dynamicSettingAndMappingEntity2 = new DynamicSettingAndMappingEntity();
		dynamicSettingAndMappingEntity2.setId(nextIdAsString());
		dynamicSettingAndMappingEntity2.setName("test-setting2");
		dynamicSettingAndMappingEntity2.setEmail("test_setting2@Test.com");

		repository.save(dynamicSettingAndMappingEntity2);

		// use a term query to prevent the input from being analysed
		Query searchQuery = new StringQuery(
				"{\"term\": {\"email\": \"" + dynamicSettingAndMappingEntity1.getEmail() + "\"}}\n");

		IndexCoordinates index = IndexCoordinates.of(indexNameProvider.indexName());
		SearchHits<DynamicSettingAndMappingEntity> entityList = operations.search(searchQuery,
				DynamicSettingAndMappingEntity.class, index);

		assertThat(entityList).hasSize(1);
		assertThat(entityList).isNotNull().hasSize(1);
		assertThat(entityList.getSearchHit(0).getContent().getEmail())
				.isEqualTo(dynamicSettingAndMappingEntity1.getEmail());
	}

	@Test
	public void shouldGetMappingForGivenIndexAndType() {

		Map<String, Object> mapping = indexOperations.getMapping();

		Map<String, Object> properties = (Map<String, Object>) mapping.get("properties");
		assertThat(mapping).isNotNull();
		assertThat(properties).isNotNull();
		Map<String, Object> emailProperties = (Map<String, Object>) properties.get("email");
		assertThat(emailProperties.get("type")).isEqualTo("text");
		assertThat(emailProperties.get("analyzer")).isEqualTo("emailAnalyzer");
	}

	@Test
	public void shouldCreateMappingWithSpecifiedMappings() {

		// given
		indexOperations.delete();
		indexOperations.create();
		indexOperations.refresh();

		String mappings = """
				{
				        "properties" : {
				            "email" : {"type" : "text", "analyzer" : "emailAnalyzer" }
				        }
				}""";
		indexOperations.putMapping(parse(mappings));
		indexOperations.refresh();

		// then
		Map<String, Object> mapping = indexOperations.getMapping();
		Map<String, Object> properties = (Map<String, Object>) mapping.get("properties");
		assertThat(mapping).isNotNull();
		assertThat(properties).isNotNull();
		Map<String, Object> emailProperties = (Map<String, Object>) properties.get("email");
		assertThat(emailProperties.get("type")).isEqualTo("text");
		assertThat(emailProperties.get("analyzer")).isEqualTo("emailAnalyzer");
	}

	@Test // DATAES-86
	public void shouldCreateMappingWithUsingMappingAnnotation() {

		Map<String, Object> mapping = indexOperations.getMapping();
		Map<String, Object> properties = (Map<String, Object>) mapping.get("properties");
		assertThat(mapping).isNotNull();
		assertThat(properties).isNotNull();
		Map<String, Object> emailProperties = (Map<String, Object>) properties.get("email");
		assertThat(emailProperties.get("type")).isEqualTo("text");
		assertThat(emailProperties.get("analyzer")).isEqualTo("emailAnalyzer");
	}

	@Document(indexName = "#{@indexNameProvider.indexName()}")
	@Setting(settingPath = "/settings/test-settings.json")
	@Mapping(mappingPath = "/mappings/test-mappings.json")
	static class DynamicSettingAndMappingEntity {

		@Id private String id;
		private String name;
		private String email;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getEmail() {
			return email;
		}

		public void setEmail(String email) {
			this.email = email;
		}
	}

	public interface DynamicSettingAndMappingEntityRepository
			extends ElasticsearchRepository<DynamicSettingAndMappingEntity, String> {}

}
