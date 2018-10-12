/*
 * Copyright 2014-2016 the original author or authors.
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
package org.springframework.data.elasticsearch.repositories.setting;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.RandomStringUtils;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.data.elasticsearch.entities.DynamicSettingAndMappingEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * DynamicSettingAndMappingEntityRepositoryTests
 *
 * @author Mohsin Husen
 * @author Ilkang Na
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:dynamic-settings-test.xml")
public class DynamicSettingAndMappingEntityRepositoryTests {

	@Autowired
	private DynamicSettingAndMappingEntityRepository repository;

	@Autowired
	private ElasticsearchTemplate elasticsearchTemplate;

	@Before
	public void before() {
		elasticsearchTemplate.deleteIndex(DynamicSettingAndMappingEntity.class);
		elasticsearchTemplate.createIndex(DynamicSettingAndMappingEntity.class);
		elasticsearchTemplate.putMapping(DynamicSettingAndMappingEntity.class);
		elasticsearchTemplate.refresh(DynamicSettingAndMappingEntity.class);
	}

	/*
	DATAES-64
	*/
	@Test
	public void shouldCreateGivenDynamicSettingsForGivenIndex() {
		//given
		//delete , create and apply mapping in before method

		// then
		assertThat(elasticsearchTemplate.indexExists(DynamicSettingAndMappingEntity.class), is(true));
		Map map = elasticsearchTemplate.getSetting(DynamicSettingAndMappingEntity.class);
		assertThat(map.containsKey("index.number_of_replicas"), is(true));
		assertThat(map.containsKey("index.number_of_shards"), is(true));
		assertThat(map.containsKey("index.analysis.analyzer.emailAnalyzer.tokenizer"), is(true));
		assertThat((String) map.get("index.number_of_replicas"), is("0"));
		assertThat((String) map.get("index.number_of_shards"), is("1"));
		assertThat((String) map.get("index.analysis.analyzer.emailAnalyzer.tokenizer"), is("uax_url_email"));
	}

	/*
	DATAES-64
	*/
	@Test
	public void shouldSearchOnGivenTokenizerUsingGivenDynamicSettingsForGivenIndex() {
		//given
		DynamicSettingAndMappingEntity dynamicSettingAndMappingEntity1 = new DynamicSettingAndMappingEntity();
		dynamicSettingAndMappingEntity1.setId(RandomStringUtils.randomNumeric(5));
		dynamicSettingAndMappingEntity1.setName("test-setting1");
		dynamicSettingAndMappingEntity1.setEmail("test_setting1@test.com");

		repository.save(dynamicSettingAndMappingEntity1);

		DynamicSettingAndMappingEntity dynamicSettingAndMappingEntity2 = new DynamicSettingAndMappingEntity();
		dynamicSettingAndMappingEntity2.setId(RandomStringUtils.randomNumeric(5));
		dynamicSettingAndMappingEntity2.setName("test-setting2");
		dynamicSettingAndMappingEntity2.setEmail("test_setting2@test.com");

		repository.save(dynamicSettingAndMappingEntity2);

		//when
		SearchQuery searchQuery = new NativeSearchQueryBuilder()
				.withQuery(QueryBuilders.termQuery("email", dynamicSettingAndMappingEntity1.getEmail())).build();

		long count = elasticsearchTemplate.count(searchQuery, DynamicSettingAndMappingEntity.class);
		List<DynamicSettingAndMappingEntity> entityList = elasticsearchTemplate.queryForList(searchQuery, DynamicSettingAndMappingEntity.class);

		//then
		assertThat(count, is(1L));
		assertThat(entityList, is(notNullValue()));
		assertThat(entityList.size(), is(1));
		assertThat(entityList.get(0).getEmail(), is(dynamicSettingAndMappingEntity1.getEmail()));
	}

	@Test
	public void shouldGetMappingForGivenIndexAndType() {
		//given
		//delete , create and apply mapping in before method
		//when
		Map mapping = elasticsearchTemplate.getMapping(DynamicSettingAndMappingEntity.class);
		//then
		Map properties = (Map) mapping.get("properties");
		assertThat(mapping, is(notNullValue()));
		assertThat(properties, is(notNullValue()));
		assertThat(((String) ((Map) properties.get("email")).get("type")), is("text"));
		assertThat((String) ((Map) properties.get("email")).get("analyzer"), is("emailAnalyzer"));
	}

	@Test
	public void shouldCreateMappingWithSpecifiedMappings() {
		//given
		elasticsearchTemplate.deleteIndex(DynamicSettingAndMappingEntity.class);
		elasticsearchTemplate.createIndex(DynamicSettingAndMappingEntity.class);
		elasticsearchTemplate.refresh(DynamicSettingAndMappingEntity.class);
		//when
		String mappings = "{\n" +
				"    \"test-setting-type\" : {\n" +
				"        \"properties\" : {\n" +
				"            \"email\" : {\"type\" : \"text\", \"analyzer\" : \"emailAnalyzer\" }\n" +
				"        }\n" +
				"    }\n" +
				"}";
		elasticsearchTemplate.putMapping(DynamicSettingAndMappingEntity.class, mappings);
		elasticsearchTemplate.refresh(DynamicSettingAndMappingEntity.class);
		//then
		Map mapping = elasticsearchTemplate.getMapping(DynamicSettingAndMappingEntity.class);
		Map properties = (Map) mapping.get("properties");
		assertThat(mapping, is(notNullValue()));
		assertThat(properties, is(notNullValue()));
		assertThat(((String) ((Map) properties.get("email")).get("type")), is("text"));
		assertThat((String) ((Map) properties.get("email")).get("analyzer"), is("emailAnalyzer"));
	}

	/*
	DATAES-86
	*/
	@Test
	public void shouldCreateMappingWithUsingMappingAnnotation() {
		//given

		//then
		Map mapping = elasticsearchTemplate.getMapping(DynamicSettingAndMappingEntity.class);
		Map properties = (Map) mapping.get("properties");
		assertThat(mapping, is(notNullValue()));
		assertThat(properties, is(notNullValue()));
		assertThat(((String) ((Map) properties.get("email")).get("type")), is("text"));
		assertThat((String) ((Map) properties.get("email")).get("analyzer"), is("emailAnalyzer"));
	}
}
