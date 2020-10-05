/*
 * Copyright 2020 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;

import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.junit.jupiter.ElasticsearchRestTemplateConfiguration;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.data.geo.Point;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Peter-Josef Meisch
 */
@SpringIntegrationTest
@ContextConfiguration(classes = { ElasticsearchRestTemplateConfiguration.class })
@DisplayName("GeoJson integration test with REST client")
class GeoJsonIntegrationTest {

	@Autowired private ElasticsearchOperations operations;

	private IndexOperations indexOps;

	@BeforeEach
	void setUp() {
		indexOps = operations.indexOps(GeoJsonEntity.class);
		indexOps.delete();
		indexOps.create();
		indexOps.putMapping();
	}

	@AfterEach
	void tearDown() {
		indexOps.delete();
	}

	@Test // DATAES-930
	@DisplayName("should write and read an entity with GeoJson properties")
	void shouldWriteAndReadAnEntityWithGeoJsonProperties() {

		GeoJsonMultiLineString multiLineString = GeoJsonMultiLineString.of(Arrays.asList( //
				GeoJsonLineString.of(new Point(12, 34), new Point(56, 78)), //
				GeoJsonLineString.of(new Point(90, 12), new Point(34, 56)) //
		));
		GeoJsonPolygon geoJsonPolygon = GeoJsonPolygon
				.of(new Point(12, 34), new Point(56, 78), new Point(90, 12), new Point(12, 34))
				.withInnerRing(new Point(21, 43), new Point(65, 87), new Point(9, 21), new Point(21, 43));
		GeoJsonMultiPolygon geoJsonMultiPolygon = GeoJsonMultiPolygon
				.of(Arrays.asList(GeoJsonPolygon.of(new Point(12, 34), new Point(56, 78), new Point(90, 12), new Point(12, 34)),
						GeoJsonPolygon.of(new Point(21, 43), new Point(65, 87), new Point(9, 21), new Point(21, 43))));
		GeoJsonGeometryCollection geoJsonGeometryCollection = GeoJsonGeometryCollection
				.of(Arrays.asList(GeoJsonPoint.of(12, 34), GeoJsonPolygon
						.of(GeoJsonLineString.of(new Point(12, 34), new Point(56, 78), new Point(90, 12), new Point(12, 34)))));

		GeoJsonEntity entity = GeoJsonEntity.builder() //
				.id("42") //
				.point1(GeoJsonPoint.of(12, 34)) //
				.point2(GeoJsonPoint.of(56, 78)) //
				.multiPoint1(GeoJsonMultiPoint.of(new Point(12, 34), new Point(56, 78), new Point(90, 12))) //
				.multiPoint2(GeoJsonMultiPoint.of(new Point(90, 12), new Point(56, 78), new Point(12, 34))) //
				.lineString1(GeoJsonLineString.of(new Point(12, 34), new Point(56, 78), new Point(90, 12))) //
				.lineString2(GeoJsonLineString.of(new Point(90, 12), new Point(56, 78), new Point(12, 34))) //
				.multiLineString1(multiLineString) //
				.multiLineString2(multiLineString) //
				.polygon1(geoJsonPolygon) //
				.polygon2(geoJsonPolygon) //
				.multiPolygon1(geoJsonMultiPolygon) //
				.multiPolygon2(geoJsonMultiPolygon) //
				.geometryCollection1(geoJsonGeometryCollection) //
				.geometryCollection2(geoJsonGeometryCollection) //
				.build(); //

		operations.save(entity);
		indexOps.refresh();

		GeoJsonEntity result = operations.get("42", GeoJsonEntity.class);

		assertThat(result).isEqualTo(entity);
	}
}
