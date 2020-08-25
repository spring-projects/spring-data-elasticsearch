/*
 * Copyright 2019-2020 the original author or authors.
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
package org.springframework.data.elasticsearch.repository.config;

import static org.springframework.data.elasticsearch.annotations.FieldType.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.junit.jupiter.ReactiveElasticsearchRestTemplateConfiguration;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.data.elasticsearch.repository.ReactiveElasticsearchRepository;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Christoph Strobl
 * @author Peter-Josef Meisch
 */
@SpringIntegrationTest
@ContextConfiguration(classes = { ReactiveElasticsearchRepositoriesRegistrarTests.Config.class })
public class ReactiveElasticsearchRepositoriesRegistrarTests {

	@Configuration
	@Import({ ReactiveElasticsearchRestTemplateConfiguration.class })
	@EnableReactiveElasticsearchRepositories(considerNestedRepositories = true)
	static class Config {}

	@Autowired ReactiveSampleEntityRepository repository;
	@Autowired ApplicationContext context;

	@Test // DATAES-519
	public void testConfiguration() {

		Assertions.assertThat(context).isNotNull();
		Assertions.assertThat(repository).isNotNull();

	}

	interface ReactiveSampleEntityRepository extends ReactiveElasticsearchRepository<SampleEntity, String> {}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	@Document(indexName = "test-index-sample-reactive-repositories-registrar", replicas = 0, refreshInterval = "-1")
	static class SampleEntity {

		@Id private String id;
		@Field(type = Text, store = true, fielddata = true) private String type;
		@Field(type = Text, store = true, fielddata = true) private String message;
	}
}
