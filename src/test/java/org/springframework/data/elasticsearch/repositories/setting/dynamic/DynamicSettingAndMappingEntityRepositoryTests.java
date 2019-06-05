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
package org.springframework.data.elasticsearch.repositories.setting.dynamic;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.RandomStringUtils;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Mapping;
import org.springframework.data.elasticsearch.annotations.Setting;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.data.elasticsearch.repository.ElasticsearchCrudRepository;
import org.springframework.data.elasticsearch.utils.IndexInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * DynamicSettingAndMappingEntityRepositoryTests
 *
 * @author Mohsin Husen
 * @author Ilkang Na
 * @author Peter-Josef Meisch
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:dynamic-settings-test.xml")
public class DynamicSettingAndMappingEntityRepositoryTests {

	@Autowired private DynamicSettingAndMappingEntityRepository repository;

	@Autowired private ElasticsearchTemplate elasticsearchTemplate;

	@Before
	public void before() {
		IndexInitializer.init(elasticsearchTemplate, DynamicSettingAndMappingEntity.class);
	}

	@Test // DATAES-64
	public void shouldCreateGivenDynamicSettingsForGivenIndex() {

		// given
		// delete , create and apply mapping in before method

		// then
		assertThat(elasticsearchTemplate.indexExists(DynamicSettingAndMappingEntity.class)).isTrue();
		Map<String, Object> map = elasticsearchTemplate.getSetting(DynamicSettingAndMappingEntity.class);
		assertThat(map.containsKey("index.number_of_replicas")).isTrue();
		assertThat(map.containsKey("index.number_of_shards")).isTrue();
		assertThat(map.containsKey("index.analysis.analyzer.emailAnalyzer.tokenizer")).isTrue();
		assertThat(map.get("index.number_of_replicas")).isEqualTo("0");
		assertThat(map.get("index.number_of_shards")).isEqualTo("1");
		assertThat(map.get("index.analysis.analyzer.emailAnalyzer.tokenizer")).isEqualTo("uax_url_email");
	}

	@Test // DATAES-64
	public void shouldSearchOnGivenTokenizerUsingGivenDynamicSettingsForGivenIndex() {

		// given
		DynamicSettingAndMappingEntity dynamicSettingAndMappingEntity1 = new DynamicSettingAndMappingEntity();
		dynamicSettingAndMappingEntity1.setId(RandomStringUtils.randomNumeric(5));
		dynamicSettingAndMappingEntity1.setName("test-setting1");
		dynamicSettingAndMappingEntity1.setEmail("test_setting1@Test.com");

		repository.save(dynamicSettingAndMappingEntity1);

		DynamicSettingAndMappingEntity dynamicSettingAndMappingEntity2 = new DynamicSettingAndMappingEntity();
		dynamicSettingAndMappingEntity2.setId(RandomStringUtils.randomNumeric(5));
		dynamicSettingAndMappingEntity2.setName("test-setting2");
		dynamicSettingAndMappingEntity2.setEmail("test_setting2@Test.com");

		repository.save(dynamicSettingAndMappingEntity2);

		// when
		SearchQuery searchQuery = new NativeSearchQueryBuilder()
				.withQuery(QueryBuilders.termQuery("email", dynamicSettingAndMappingEntity1.getEmail())).build();

		long count = elasticsearchTemplate.count(searchQuery, DynamicSettingAndMappingEntity.class);
		List<DynamicSettingAndMappingEntity> entityList = elasticsearchTemplate.queryForList(searchQuery,
				DynamicSettingAndMappingEntity.class);

		// then
		assertThat(count).isEqualTo(1L);
		assertThat(entityList).isNotNull().hasSize(1);
		assertThat(entityList.get(0).getEmail()).isEqualTo(dynamicSettingAndMappingEntity1.getEmail());
	}

	@Test
	public void shouldGetMappingForGivenIndexAndType() {

		// given
		// delete , create and apply mapping in before method

		// when
		Map<String, Object> mapping = elasticsearchTemplate.getMapping(DynamicSettingAndMappingEntity.class);

		// then
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
		elasticsearchTemplate.deleteIndex(DynamicSettingAndMappingEntity.class);
		elasticsearchTemplate.createIndex(DynamicSettingAndMappingEntity.class);
		elasticsearchTemplate.refresh(DynamicSettingAndMappingEntity.class);

		// when
		String mappings = "{\n" + //
				"    \"test-setting-type\" : {\n" + //
				"        \"properties\" : {\n" + //
				"            \"email\" : {\"type\" : \"text\", \"analyzer\" : \"emailAnalyzer\" }\n" + //
				"        }\n" + //
				"    }\n" + //
				"}";
		elasticsearchTemplate.putMapping(DynamicSettingAndMappingEntity.class, mappings);
		elasticsearchTemplate.refresh(DynamicSettingAndMappingEntity.class);

		// then
		Map<String, Object> mapping = elasticsearchTemplate.getMapping(DynamicSettingAndMappingEntity.class);
		Map<String, Object> properties = (Map<String, Object>) mapping.get("properties");
		assertThat(mapping).isNotNull();
		assertThat(properties).isNotNull();
		Map<String, Object> emailProperties = (Map<String, Object>) properties.get("email");
		assertThat(emailProperties.get("type")).isEqualTo("text");
		assertThat(emailProperties.get("analyzer")).isEqualTo("emailAnalyzer");
	}

	@Test // DATAES-86
	public void shouldCreateMappingWithUsingMappingAnnotation() {

		// given

		// then
		Map<String, Object> mapping = elasticsearchTemplate.getMapping(DynamicSettingAndMappingEntity.class);
		Map<String, Object> properties = (Map<String, Object>) mapping.get("properties");
		assertThat(mapping).isNotNull();
		assertThat(properties).isNotNull();
		Map<String, Object> emailProperties = (Map<String, Object>) properties.get("email");
		assertThat(emailProperties.get("type")).isEqualTo("text");
		assertThat(emailProperties.get("analyzer")).isEqualTo("emailAnalyzer");
	}

	/**
	 * @author Mohsin Husen
	 */
	@Document(indexName = "test-index-dynamic-setting-and-mapping", type = "test-setting-type")
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

	/**
	 * @author Mohsin Husen
	 */
	public interface DynamicSettingAndMappingEntityRepository
			extends ElasticsearchCrudRepository<DynamicSettingAndMappingEntity, String> {}

}
