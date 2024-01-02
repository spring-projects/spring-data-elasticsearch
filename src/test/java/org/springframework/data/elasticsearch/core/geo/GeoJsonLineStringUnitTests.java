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
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.geo.Point;

/**
 * @author Peter-Josef Meisch
 */
class GeoJsonLineStringUnitTests {

	@Test // DATAES-930
	@DisplayName("should accept list of Point")
	void shouldAcceptListOfPoint() {

		List<Point> points = Arrays.asList(new Point(12, 34), new Point(56, 78), new Point(90, 12));
		GeoJsonLineString geoJsonLineString = GeoJsonLineString.of(points);

		List<Point> coordinates = geoJsonLineString.getCoordinates();

		assertThat(coordinates).containsExactlyElementsOf(points);
	}

	@Test // DATAES-930
	@DisplayName("should accept array of Point")
	void shouldAcceptVarargArrayOfPoint() {
		Point[] points = new Point[] { new Point(12, 34), new Point(56, 78), new Point(90, 12) };

		GeoJsonLineString geoJsonLineString = GeoJsonLineString.of(new Point(12, 34), new Point(56, 78), new Point(90, 12));

		List<Point> coordinates = geoJsonLineString.getCoordinates();

		assertThat(coordinates).containsExactly(points);
	}

	@Test // DATAES-930
	@DisplayName("should accept list of GeoPoint")
	void shouldAcceptListOfGeoPoint() {

		List<Point> points = Arrays.asList(new Point(12, 34), new Point(56, 78), new Point(90, 12));
		List<GeoPoint> geoPoints = points.stream().map(GeoPoint::fromPoint).collect(Collectors.toList());

		GeoJsonLineString geoJsonLineString = GeoJsonLineString.ofGeoPoints(geoPoints);

		List<Point> coordinates = geoJsonLineString.getCoordinates();

		assertThat(coordinates).containsExactlyElementsOf(points);
	}

	@Test // DATAES-930
	@DisplayName("should accept array of Point")
	void shouldAcceptVarargArrayOfGeoPoint() {
		Point[] points = new Point[] { new Point(12, 34), new Point(56, 78), new Point(90, 12) };
		GeoPoint[] geoPoints = Arrays.stream(points).map(GeoPoint::fromPoint).toArray(value -> new GeoPoint[points.length]);

		GeoJsonLineString geoJsonLineString = GeoJsonLineString.of(geoPoints[0], geoPoints[1], geoPoints[2]);

		List<Point> coordinates = geoJsonLineString.getCoordinates();

		assertThat(coordinates).containsExactly(points);
	}
}
