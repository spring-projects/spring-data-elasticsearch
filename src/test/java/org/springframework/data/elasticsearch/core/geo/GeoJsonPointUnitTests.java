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

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.geo.Point;

/**
 * @author Peter-Josef Meisch
 */
class GeoJsonPointUnitTests {

	@Test // DATAES-930
	@DisplayName("should return x and y as coordinates")
	void shouldReturnXAndYAsCoordinates() {

		GeoJsonPoint geoJsonPoint = GeoJsonPoint.of(12, 34);

		List<Double> coordinates = geoJsonPoint.getCoordinates();

		assertThat(coordinates).containsExactly(12d, 34d);
	}

	@Test // DATAES-930
	@DisplayName("should accept a Point as parameter")
	void shouldAcceptAPointAsParameter() {

		GeoJsonPoint geoJsonPoint1 = GeoJsonPoint.of(12, 34);
		GeoJsonPoint geoJsonPoint2 = GeoJsonPoint.of(new Point(12, 34));

		assertThat(geoJsonPoint1).isEqualTo(geoJsonPoint2);
	}

	@Test // DATAES-930
	@DisplayName("should accept a GeoPoint as parameter")
	void shouldAcceptAGeoPointAsParameter() {

		GeoJsonPoint geoJsonPoint1 = GeoJsonPoint.of(12, 34);
		GeoJsonPoint geoJsonPoint2 = GeoJsonPoint.of(new GeoPoint(34, 12));

		assertThat(geoJsonPoint1).isEqualTo(geoJsonPoint2);
	}
}
