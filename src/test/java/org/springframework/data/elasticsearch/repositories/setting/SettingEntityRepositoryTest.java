/*
 * Copyright 2014 the original author or authors.
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
import static org.junit.Assert.assertThat;

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
import org.springframework.data.elasticsearch.entities.SettingEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * SettingEntityRepositoryTest
 *
 * @author Mohsin Husen
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:dynamic-settings-test.xml")
public class SettingEntityRepositoryTest {

	@Autowired
	private SettingEntityRepository repository;

	@Autowired
	private ElasticsearchTemplate elasticsearchTemplate;

	@Before
	public void before() {
		elasticsearchTemplate.deleteIndex(SettingEntity.class);
		elasticsearchTemplate.createIndex(SettingEntity.class);
		elasticsearchTemplate.putMapping(SettingEntity.class);
		elasticsearchTemplate.refresh(SettingEntity.class, true);
	}

	/*
	DATAES-64
	*/
	@Test
	public void shouldCreateGivenDynamicSettingsForGivenIndex() {
		//given
		//delete , create and apply mapping in before method

		// then
		assertThat(elasticsearchTemplate.indexExists(SettingEntity.class), is(true));
		Map map = elasticsearchTemplate.getSetting(SettingEntity.class);
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
		SettingEntity settingEntity1 = new SettingEntity();
		settingEntity1.setId(RandomStringUtils.randomNumeric(5));
		settingEntity1.setName("test-setting1");
		settingEntity1.setEmail("test_setting1@test.com");

		repository.save(settingEntity1);

		SettingEntity settingEntity2 = new SettingEntity();
		settingEntity2.setId(RandomStringUtils.randomNumeric(5));
		settingEntity2.setName("test-setting2");
		settingEntity2.setEmail("test_setting2@test.com");

		repository.save(settingEntity2);

		//when
		SearchQuery searchQuery = new NativeSearchQueryBuilder()
				.withQuery(QueryBuilders.termQuery("email", settingEntity1.getEmail())).build();

		long count = elasticsearchTemplate.count(searchQuery, SettingEntity.class);
		List<SettingEntity> entityList = elasticsearchTemplate.queryForList(searchQuery, SettingEntity.class);

		//then
		assertThat(count, is(1L));
		assertThat(entityList, is(notNullValue()));
		assertThat(entityList.size(), is(1));
		assertThat(entityList.get(0).getEmail(), is(settingEntity1.getEmail()));
	}
}
