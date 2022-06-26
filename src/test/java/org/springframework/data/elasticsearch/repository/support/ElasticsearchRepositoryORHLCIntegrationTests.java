/*
 * Copyright 2022 the original author or authors.
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
package org.springframework.data.elasticsearch.repository.support;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.EnabledIfOpensearch;
import org.springframework.data.elasticsearch.junit.jupiter.OpensearchRestTemplateConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Peter-Josef Meisch
 * @author Andriy Redko
 * @since 5.0
 */
@EnabledIfOpensearch
@ContextConfiguration(classes = {ElasticsearchRepositoryORHLCIntegrationTests.Config.class })
public class ElasticsearchRepositoryORHLCIntegrationTests extends ElasticsearchRepositoryIntegrationTests {

	@Configuration
	@Import({OpensearchRestTemplateConfiguration.class })
	@EnableElasticsearchRepositories(basePackages = {"org.springframework.data.elasticsearch.repository.support" },
			considerNestedRepositories = true)
	static class Config {
		@Bean
		IndexNameProvider indexNameProvider() {
			return new IndexNameProvider("repository-os");
		}

	}
}
