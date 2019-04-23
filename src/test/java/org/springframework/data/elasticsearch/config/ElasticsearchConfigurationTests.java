/*
 * Copyright 2019 the original author or authors.
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
package org.springframework.data.elasticsearch.config;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.elasticsearch.client.RestHighLevelClient;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repositories.existing.index.CreateIndexFalseRepository;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Tests for {@link AbstractElasticsearchConfiguration}.
 *
 * @author Peter-Josef Meisch
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class ElasticsearchConfigurationTests {

	/*
	 * using a repository with an entity that is set to createIndex = false as we have no elastic running for this test
	 * and just check that all the necessary beans are created.
	 */
	@Autowired private CreateIndexFalseRepository repository;

	@Configuration
	@EnableElasticsearchRepositories(
			basePackages = { "org.springframework.data.elasticsearch.repositories.existing.index",
					"org.springframework.data.elasticsearch.config" })
	static class Config extends AbstractElasticsearchConfiguration {

		@Override
		public RestHighLevelClient elasticsearchClient() {
			return mock(RestHighLevelClient.class);
		}

	}

	@Test // DATAES-563
	public void bootstrapsRepository() {
		assertThat(repository, is(notNullValue()));
	}
}
