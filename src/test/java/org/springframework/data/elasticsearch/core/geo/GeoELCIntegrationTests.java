/*
 * Copyright 2019-2024 the original author or authors.
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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.Queries;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.junit.jupiter.ElasticsearchTemplateConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.data.elasticsearch.utils.geohash.Geohash;
import org.springframework.data.elasticsearch.utils.geohash.Rectangle;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Peter-Josef Meisch
 * @since 4.4
 */
@ContextConfiguration(classes = { GeoELCIntegrationTests.Config.class })
public class GeoELCIntegrationTests extends GeoIntegrationTests {

	@Configuration
	@Import({ ElasticsearchTemplateConfiguration.class })
	@EnableElasticsearchRepositories(considerNestedRepositories = true)
	static class Config {
		@Bean
		IndexNameProvider indexNameProvider() {
			return new IndexNameProvider("geo-integration");
		}
	}

	@Override
	protected Query nativeQueryForBoundingBox(String fieldName, double top, double left, double bottom, double right) {
		return NativeQuery.builder() //
				.withQuery(q -> q //
						.geoBoundingBox(bb -> bb //
								.field(fieldName) //
								.boundingBox(gb -> gb //
										.tlbr(tlbr -> tlbr //
												.topLeft(tl -> tl //
														.latlon(Queries.latLon(top, left)))
												.bottomRight(br -> br //
														.latlon(Queries.latLon(bottom, right)))))))
				.build();
	}

	@Override
	protected Query nativeQueryForBoundingBox(String fieldName, String geoHash) {
		Rectangle rect = Geohash.toBoundingBox(geoHash);
		return nativeQueryForBoundingBox(fieldName, rect.getMaxY(), rect.getMinX(), rect.getMinY(), rect.getMaxX());
	}
}
