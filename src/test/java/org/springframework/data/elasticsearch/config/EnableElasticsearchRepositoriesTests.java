/*
 * Copyright 2013-15 the original author or authors.
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
package org.springframework.data.elasticsearch.config;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.Utils;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.entities.SampleEntity;
import org.springframework.data.elasticsearch.repositories.sample.SampleElasticsearchRepository;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.repository.Repository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Kevin Leturc
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class EnableElasticsearchRepositoriesTests implements ApplicationContextAware {

	ApplicationContext context;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.context = applicationContext;
	}

	@Configuration
	@EnableElasticsearchRepositories(basePackages = {"org.springframework.data.elasticsearch.repositories.sample",
			"org.springframework.data.elasticsearch.config"})
	static class Config {

		@Bean
		public ElasticsearchOperations elasticsearchTemplate() {
			return new ElasticsearchTemplate(Utils.getNodeClient());
		}
	}

	@Autowired
	private SampleElasticsearchRepository repository;

	@Autowired(required = false)
	private SampleRepository nestedRepository;

	interface SampleRepository extends Repository<SampleEntity, Long> {};

	@Test
	public void bootstrapsRepository() {
		assertThat(repository, is(notNullValue()));
	}

	@Test
	public void shouldScanSelectedPackage() {
		//given

		//when
		String[] beanNamesForType = context.getBeanNamesForType(ElasticsearchRepository.class);

		//then
		assertThat(beanNamesForType.length, is(1));
		assertThat(beanNamesForType[0], is("sampleElasticsearchRepository"));
	}

	@Test
	public void hasNotNestedRepository() {
		assertNull(nestedRepository);
	}
}
