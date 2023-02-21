/*
 * Copyright 2022-2023 the original author or authors.
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
package org.springframework.data.elasticsearch.client.erhlc;

import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.ElasticsearchPartQueryIntegrationTests;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.junit.jupiter.ElasticsearchRestTemplateConfiguration;
import org.springframework.test.context.ContextConfiguration;

/**
 * The base class for these tests lives in the org.springframework.data.elasticsearch.core.query package, but we need
 * access to the {@link RequestFactory} class here
 *
 * @author Peter-Josef Meisch
 * @since 4.4
 */
@ContextConfiguration(classes = { ElasticsearchPartQueryERHLCIntegrationTests.Config.class })
public class ElasticsearchPartQueryERHLCIntegrationTests extends ElasticsearchPartQueryIntegrationTests {

	@Configuration
	@Import({ ElasticsearchRestTemplateConfiguration.class })
	static class Config {}

	protected String buildQueryString(Query query, Class<?> clazz) {
		SearchSourceBuilder source = new RequestFactory(operations.getElasticsearchConverter())
				.searchRequest(query, null, clazz, IndexCoordinates.of("dummy")).source();
		// remove defaultboost values
		return source.toString().replaceAll("(\\^\\d+\\.\\d+)", "");
	}
}
