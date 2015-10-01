/*
 * Copyright 2015 the original author or authors.
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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.Utils;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.entities.SampleEntity;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.repository.Repository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertNotNull;

/**
 * @author Kevin Leturc
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class EnableNestedElasticsearchRepositoriesTests {

	@Configuration
	@EnableElasticsearchRepositories(basePackages = {"org.springframework.data.elasticsearch.repositories.sample",
			"org.springframework.data.elasticsearch.config"}, considerNestedRepositories = true)
	static class Config {

		@Bean
		public ElasticsearchOperations elasticsearchTemplate() {
			return new ElasticsearchTemplate(Utils.getNodeClient());
		}
	}

	@Autowired(required = false)
	private SampleRepository nestedRepository;

	interface SampleRepository extends Repository<SampleEntity, Long> {};

	@Test
	public void hasNestedRepository() {
		assertNotNull(nestedRepository);
	}
}
