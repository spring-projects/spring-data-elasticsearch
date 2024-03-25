/*
 * Copyright 2021-2024 the original author or authors.
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
package org.springframework.data.elasticsearch.config.configuration;

import static org.assertj.core.api.Assertions.*;

import co.elastic.clients.elasticsearch.ElasticsearchClient;

import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.lang.Nullable;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Tests for {@link ElasticsearchConfiguration}.
 *
 * @author Peter-Josef Meisch
 * @since 4.4
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration
public class ElasticsearchConfigurationELCTests {

	@Configuration
	@EnableElasticsearchRepositories(basePackages = { "org.springframework.data.elasticsearch.config.configuration" },
			considerNestedRepositories = true)
	static class Config extends ElasticsearchConfiguration {
		@Override
		public ClientConfiguration clientConfiguration() {
			return ClientConfiguration.builder() //
					.connectedTo("localhost:9200") //
					.build();
		}
	}

	/*
	 * using a repository with an entity that is set to createIndex = false as we have no elastic running for this test
	 * and just check that all the necessary beans are created.
	 */
	@Autowired private RestClient restClient;
	@Autowired private ElasticsearchClient elasticsearchClient;
	@Autowired private ElasticsearchOperations elasticsearchOperations;

	@Autowired private CreateIndexFalseRepository repository;

	@Test
	public void providesRequiredBeans() {
		assertThat(restClient).isNotNull();
		assertThat(elasticsearchClient).isNotNull();
		assertThat(elasticsearchOperations).isNotNull();
		assertThat(repository).isNotNull();
	}

	@Document(indexName = "test-index-config-configuration", createIndex = false)
	static class CreateIndexFalseEntity {

		@Nullable
		@Id private String id;
	}

	interface CreateIndexFalseRepository extends ElasticsearchRepository<CreateIndexFalseEntity, String> {}
}
