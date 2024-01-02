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
class GeoJsonMultiLineStringUnitTests {

	@Test // DATAES-930
	@DisplayName("should accept list of GeoJsonLineString")
	void shouldAcceptListOfGeoJsonLineString() {

		List<GeoJsonLineString> lines = Arrays.asList( //
				GeoJsonLineString.of(new Point(12, 34), new Point(56, 78)), //
				GeoJsonLineString.of(new Point(90, 12), new Point(34, 56)) //
		);

		GeoJsonMultiLineString geoJsonMultiLineString = GeoJsonMultiLineString.of(lines);
		Iterable<GeoJsonLineString> coordinates = geoJsonMultiLineString.getCoordinates();

		assertThat(coordinates).containsExactlyElementsOf(lines);
	}

	@Test // DATAES-930
	@DisplayName("should accept an array of List of Point")
	void shouldAcceptAnArrayOfListOfPoint() {

		List<Point>[] lists = new List[] { //
				Arrays.asList(new Point(12, 34), new Point(56, 78)), //
				Arrays.asList(new Point(90, 12), new Point(34, 56)) //
		};
		List<GeoJsonLineString> lines = Arrays.stream(lists).map(GeoJsonLineString::of).collect(Collectors.toList());

		GeoJsonMultiLineString geoJsonMultiLineString = GeoJsonMultiLineString.of(lists);
		Iterable<GeoJsonLineString> coordinates = geoJsonMultiLineString.getCoordinates();

		assertThat(coordinates).containsExactlyElementsOf(lines);
	}
}
