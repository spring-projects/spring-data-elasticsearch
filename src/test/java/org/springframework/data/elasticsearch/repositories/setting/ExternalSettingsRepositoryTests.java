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

import org.apache.commons.lang.RandomStringUtils;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.data.elasticsearch.entities.DynamicSettingAndMappingEntity;
import org.springframework.data.elasticsearch.entities.ExternalSettingEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * ExternalSettingsRepositoryTests
 *
 * @author Franck Lefebure
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:dynamic-settings-test.xml")
public class ExternalSettingsRepositoryTests {

	Logger logger = LoggerFactory.getLogger(ExternalSettingsRepositoryTests.class);

	@Autowired
	private ExternalSettingsEntityRepository repository;

	@Autowired
	private ElasticsearchTemplate elasticsearchTemplate;

	@Autowired
	private String externalSettingPath;

	@Before
	public void before() {



	}

	@Test
	public void shouldCreateGivenDynamicSettingsForGivenIndex() throws IOException {

		File settings = new File(externalSettingPath);
		if (settings.exists()) {
			settings.delete();
		}

		ClassPathResource testSettings = new ClassPathResource("/settings/test-settings.json");
		FileSystemResource fileSettings = new FileSystemResource(settings);
		FileCopyUtils.copy(testSettings.getInputStream(), fileSettings.getOutputStream());
		assertTrue(settings.exists());

		elasticsearchTemplate.deleteIndex(ExternalSettingEntity.class);
		elasticsearchTemplate.createIndex(ExternalSettingEntity.class);
		elasticsearchTemplate.putMapping(ExternalSettingEntity.class);
		elasticsearchTemplate.refresh(ExternalSettingEntity.class, true);

		Map map = elasticsearchTemplate.getSetting(ExternalSettingEntity.class);
		assertThat(map.containsKey("index.number_of_replicas"), is(true));
		assertThat(map.containsKey("index.number_of_shards"), is(true));
		assertThat(map.containsKey("index.analysis.analyzer.emailAnalyzer.tokenizer"), is(true));
		assertThat((String) map.get("index.number_of_replicas"), is("0"));
		assertThat((String) map.get("index.number_of_shards"), is("1"));
		assertThat((String) map.get("index.analysis.analyzer.emailAnalyzer.tokenizer"), is("uax_url_email"));



	}

}
