/*
 * Copyright 2026-present the original author or authors.
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
package org.springframework.data.elasticsearch.repositories.customidconversion;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchCustomConversions;
import org.springframework.data.elasticsearch.junit.jupiter.ReactiveElasticsearchTemplateConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableReactiveElasticsearchRepositories;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author chuuminggg
 */
@ContextConfiguration(classes = { ReactiveCustomIdConversionRepositoryELCIntegrationTests.Config.class })
public class ReactiveCustomIdConversionRepositoryELCIntegrationTests
		extends ReactiveCustomIdConversionRepositoryIntegrationTests {

	@Configuration
	@EnableReactiveElasticsearchRepositories(considerNestedRepositories = true)
	static class Config extends ReactiveElasticsearchTemplateConfiguration {
		@Bean
		IndexNameProvider indexNameProvider() {
			return new IndexNameProvider("reactive-custom-id-conversion");
		}

		@Override
		public ElasticsearchCustomConversions elasticsearchCustomConversions() {
			return Product.customConversions();
		}
	}
}
