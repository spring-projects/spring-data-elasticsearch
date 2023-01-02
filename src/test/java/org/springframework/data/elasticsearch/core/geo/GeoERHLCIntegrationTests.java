/*
 * Copyright 2019-2023 the original author or authors.
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
package org.springframework.data.elasticsearch.core.geo;

import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.client.erhlc.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.junit.jupiter.ElasticsearchRestTemplateConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Peter-Josef Meisch
 */
@ContextConfiguration(classes = { GeoERHLCIntegrationTests.Config.class })
public class GeoERHLCIntegrationTests extends GeoIntegrationTests {

	@Configuration
	@Import({ ElasticsearchRestTemplateConfiguration.class })
	@EnableElasticsearchRepositories(considerNestedRepositories = true)
	static class Config {
		@Bean
		IndexNameProvider indexNameProvider() {
			return new IndexNameProvider("geo-integration-es7");
		}
	}

	@Override
	protected Query nativeQueryForBoundingBox(String fieldName, double top, double left, double bottom, double right) {
		return new NativeSearchQueryBuilder()
				.withFilter(QueryBuilders.geoBoundingBoxQuery(fieldName).setCorners(top, left, bottom, right)).build();
	}

	@Override
	protected Query nativeQueryForBoundingBox(String fieldName, String geoHash) {
		return new NativeSearchQueryBuilder().withFilter(QueryBuilders.geoBoundingBoxQuery(fieldName).setCorners(geoHash))
				.build();
	}
}
