/*
 * Copyright 2021-2022 the original author or authors.
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

import static org.opensearch.index.query.QueryBuilders.*;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.EnabledIfOpensearch;
import org.springframework.data.elasticsearch.client.orhlc.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.junit.jupiter.OpensearchRestTemplateConfiguration;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.data.mapping.model.FieldNamingStrategy;
import org.springframework.data.mapping.model.SnakeCaseFieldNamingStrategy;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Peter-Josef Meisch
 * @author Andriy Redko
 */
@EnabledIfOpensearch
@ContextConfiguration(classes = { FieldNamingStrategyORHLCIntegrationTests.Config.class })
public class FieldNamingStrategyORHLCIntegrationTests extends FieldNamingStrategyIntegrationTests {

	@Configuration
	static class Config extends OpensearchRestTemplateConfiguration {

		@Bean
		IndexNameProvider indexNameProvider() {
			return new IndexNameProvider("fieldnaming-strategy-os");
		}

		@Override
		protected FieldNamingStrategy fieldNamingStrategy() {
			return new SnakeCaseFieldNamingStrategy();
		}
	}

	@Override
	protected Query nativeMatchQuery(String fieldName, String value) {
		return new NativeSearchQueryBuilder().withQuery(matchQuery(fieldName, value)).build();
	}
}
