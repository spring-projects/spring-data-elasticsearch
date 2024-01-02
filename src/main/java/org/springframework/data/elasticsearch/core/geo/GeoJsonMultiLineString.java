/*
 * Copyright 2015-2024 the original author or authors.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.geo.Point;
import org.springframework.util.Assert;

/**
 * {@link GeoJsonMultiLineString} is defined as list of {@link GeoJsonLineString}s. <br/>
 * Copied from Spring Data Mongodb
 * 
 * @author Christoph Strobl
 * @author Peter-Josef Meisch
 * @since 4.1
 * @see <a href=
 *      "https://geojson.org/geojson-spec.html#multilinestring">https://geojson.org/geojson-spec.html#multilinestring</a>
 */
public class GeoJsonMultiLineString implements GeoJson<Iterable<GeoJsonLineString>> {

	public static final String TYPE = "MultiLineString";

	private final List<GeoJsonLineString> coordinates = new ArrayList<>();

	private GeoJsonMultiLineString(List<GeoJsonLineString> lines) {
		this.coordinates.addAll(lines);
	}

	/**
	 * Creates new {@link GeoJsonMultiLineString} for the given {@link GeoJsonLineString}s.
	 *
	 * @param lines must not be {@literal null}.
	 */
	public static GeoJsonMultiLineString of(List<GeoJsonLineString> lines) {

		Assert.notNull(lines, "Lines for MultiLineString must not be null!");

		return new GeoJsonMultiLineString(lines);
	}

	/**
	 * Creates new {@link GeoJsonMultiLineString} for the given {@link Point}s.
	 *
	 * @param lines must not be {@literal null}.
	 */
	public static GeoJsonMultiLineString of(List<Point>... lines) {

		Assert.notEmpty(lines, "Points for MultiLineString must not be null!");
		List<GeoJsonLineString> geoJsonLineStrings = Arrays.stream(lines).map(GeoJsonLineString::of)
				.collect(Collectors.toList());

		return new GeoJsonMultiLineString(geoJsonLineStrings);
	}

	@Override
	public String getType() {
		return TYPE;
	}

	@Override
	public List<GeoJsonLineString> getCoordinates() {
		return Collections.unmodifiableList(this.coordinates);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		GeoJsonMultiLineString that = (GeoJsonMultiLineString) o;

		return coordinates.equals(that.coordinates);
	}

	@Override
	public int hashCode() {
		return coordinates.hashCode();
	}

	@Override
	public String toString() {
		return "GeoJsonMultiLineString{" + "coordinates=" + coordinates + '}';
	}
}
