/*
 * Copyright 2016 the original author or authors.
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
package org.springframework.data.elasticsearch;

import org.elasticsearch.index.query.QueryBuilders;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.data.elasticsearch.entities.SampleAllFieldsSearchEntity;
import org.springframework.data.elasticsearch.repositories.allfieldssearch.SampleAllFieldsSearchEntityRepository;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * @author Aleksandr Olisov
 */
@RunWith(SpringJUnit4ClassRunner.class)
public class AllFieldsSearchTests implements ApplicationContextAware {

	ApplicationContext context;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.context = applicationContext;
	}

	@Configuration
	@EnableElasticsearchRepositories(basePackages = "org.springframework.data.elasticsearch.repositories.allfieldssearch")
	static class Config {

		@Bean
		public ElasticsearchOperations elasticsearchTemplate() {
			return new ElasticsearchTemplate(Utils.getNodeClient());
		}
	}

	@Autowired
	private ElasticsearchTemplate elasticsearchTemplate;

	@Autowired
	private SampleAllFieldsSearchEntityRepository repo;

	@Before
	public void before() {

		elasticsearchTemplate.deleteIndex(SampleAllFieldsSearchEntity.class);
		elasticsearchTemplate.createIndex(SampleAllFieldsSearchEntity.class);
		elasticsearchTemplate.putMapping(SampleAllFieldsSearchEntity.class);
		elasticsearchTemplate.refresh(SampleAllFieldsSearchEntity.class);
	}

	/**
	 * @see DATAES-226
	 */
	@Test
	public void shouldNotDoGlobalSearchInExcludedFields() {

		// given
		final String presentedValue = "abc";
		final String includedFieldEntityId1 = "1";
		final String includedFieldEntityId2 = "2";
		final String includedFieldEntityId3 = "3";
		final String excludedFieldEntityId = "100";

		{
			SampleAllFieldsSearchEntity entity = new SampleAllFieldsSearchEntity();
			entity.setId(includedFieldEntityId1);
			entity.setFullSearchField1(presentedValue);
			entity.setFullSearchField2("1-2");
			entity.setFullSearchField3("1-3");
			entity.setAttributeOnlySearchField("1-4");
			repo.save(entity);
		}
		{
			SampleAllFieldsSearchEntity entity = new SampleAllFieldsSearchEntity();
			entity.setId(includedFieldEntityId2);
			entity.setFullSearchField1("2-1");
			entity.setFullSearchField2(presentedValue);
			entity.setFullSearchField3("2-3");
			entity.setAttributeOnlySearchField("2-4");
			repo.save(entity);
		}
		{
			SampleAllFieldsSearchEntity entity = new SampleAllFieldsSearchEntity();
			entity.setId(includedFieldEntityId3);
			entity.setFullSearchField1("3-1");
			entity.setFullSearchField2("3-2");
			entity.setFullSearchField3(presentedValue);
			entity.setAttributeOnlySearchField("3-4");
			repo.save(entity);
		}
		{
			SampleAllFieldsSearchEntity entity = new SampleAllFieldsSearchEntity();
			entity.setId(excludedFieldEntityId);
			entity.setFullSearchField1("4-1");
			entity.setFullSearchField2("4-2");
			entity.setFullSearchField3("1-3");
			entity.setAttributeOnlySearchField(presentedValue);
			repo.save(entity);
		}

		SearchQuery query = new NativeSearchQueryBuilder()
				.withQuery(QueryBuilders.matchQuery("_all", presentedValue))
				.build();

		// when
		List<SampleAllFieldsSearchEntity> searchResult
				= (elasticsearchTemplate.queryForList(query, SampleAllFieldsSearchEntity.class));
		List<String> resultIds = new ArrayList<String>();
		for (SampleAllFieldsSearchEntity entity : searchResult) {
			resultIds.add(entity.getId());
		}

		// then
		assertTrue(resultIds.contains(includedFieldEntityId1));
		assertTrue(resultIds.contains(includedFieldEntityId2));
		assertTrue(resultIds.contains(includedFieldEntityId3));
		assertFalse(resultIds.contains(excludedFieldEntityId));
		assertThat(searchResult.size(), is(3));
	}

	/**
	 * @see DATAES-226
	 */
	@Test
	public void shouldDoAttributeSearchInExcludedFields() {

		// given
		final String presentedValue = "foo";
		final String notPresentedValue = "bar";
		final String entityId = "100";

		{
			SampleAllFieldsSearchEntity entity = new SampleAllFieldsSearchEntity();
			entity.setId(entityId);
			entity.setFullSearchField1("aaa");
			entity.setFullSearchField2("bbb");
			entity.setFullSearchField3("ccc");
			entity.setAttributeOnlySearchField(presentedValue);
			repo.save(entity);
		}

		// when
		SampleAllFieldsSearchEntity foundEntity = repo.findByAttributeOnlySearchField(presentedValue);
		SampleAllFieldsSearchEntity notFoundEntity = repo.findByAttributeOnlySearchField(notPresentedValue);

		// then
		assertThat(foundEntity.getId(), is(entityId));
		assertThat(notFoundEntity, is(nullValue()));
	}
}
