/*
 * Copyright 2020-2024 the original author or authors.
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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.geo.Point;

/**
 * @author Peter-Josef Meisch
 */
class GeoJsonGeometryCollectionUnitTests {

	@Test // DATAES-930
	@DisplayName("should accept a list of GeoJson objects")
	void shouldAcceptAListOfGeoJsonObjects() {

		GeoJsonPoint geoJsonPoint = GeoJsonPoint.of(12, 34);
		GeoJsonPolygon geoJsonPolygon = GeoJsonPolygon
				.of(GeoJsonLineString.of(new Point(12, 34), new Point(56, 78), new Point(90, 12), new Point(12, 34)));

		GeoJsonGeometryCollection geoJsonGeometryCollection = GeoJsonGeometryCollection
				.of(Arrays.asList(geoJsonPoint, geoJsonPolygon));

		assertThat(geoJsonGeometryCollection.getGeometries()).containsExactly(geoJsonPoint, geoJsonPolygon);
	}
}
