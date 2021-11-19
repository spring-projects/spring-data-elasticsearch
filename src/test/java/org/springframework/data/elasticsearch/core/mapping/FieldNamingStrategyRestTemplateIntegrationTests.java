/*
 * Copyright 2021 the original author or authors.
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
package org.springframework.data.elasticsearch.core.mapping;

import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.junit.jupiter.ElasticsearchRestTemplateConfiguration;
import org.springframework.data.mapping.model.FieldNamingStrategy;
import org.springframework.data.mapping.model.SnakeCaseFieldNamingStrategy;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Peter-Josef Meisch
 */
@ContextConfiguration(classes = { FieldNamingStrategyRestTemplateIntegrationTests.Config.class })
public class FieldNamingStrategyRestTemplateIntegrationTests extends FieldNamingStrategyIntegrationTests {

	@Configuration
	static class Config extends ElasticsearchRestTemplateConfiguration {

		@Override
		@Bean
		public ElasticsearchOperations elasticsearchOperations(ElasticsearchConverter elasticsearchConverter,
				RestHighLevelClient elasticsearchClient) {
			return super.elasticsearchOperations(elasticsearchConverter, elasticsearchClient);
		}

		@Override
		protected FieldNamingStrategy fieldNamingStrategy() {
			return new SnakeCaseFieldNamingStrategy();
		}
	}
}
