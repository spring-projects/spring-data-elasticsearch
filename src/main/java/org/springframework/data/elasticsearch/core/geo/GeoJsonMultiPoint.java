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
 * {@link GeoJsonMultiPoint} is defined as list of {@link Point}s.<br/>
 * Copied from Spring Data Mongodb
 *
 * @author Christoph Strobl
 * @author Peter-Josef Meisch
 * @since 4.1
 * @see <a href="https://geojson.org/geojson-spec.html#multipoint">https://geojson.org/geojson-spec.html#multipoint</a>
 */
public class GeoJsonMultiPoint implements GeoJson<Iterable<Point>> {

	public static final String TYPE = "MultiPoint";

	private final List<Point> points;

	private GeoJsonMultiPoint(List<Point> points) {
		this.points = new ArrayList<>(points);
	}

	/**
	 * Creates a new {@link GeoJsonMultiPoint} for the given {@link Point}s.
	 *
	 * @param points points must not be {@literal null} and have at least 2 entries.
	 */
	public static GeoJsonMultiPoint of(List<Point> points) {

		Assert.notNull(points, "Points must not be null.");
		Assert.isTrue(points.size() >= 2, "Minimum of 2 Points required.");

		return new GeoJsonMultiPoint(points);
	}

	/**
	 * Creates a new {@link GeoJsonMultiPoint} for the given {@link Point}s.
	 *
	 * @param first must not be {@literal null}.
	 * @param second must not be {@literal null}.
	 * @param others must not be {@literal null}.
	 */
	public static GeoJsonMultiPoint of(Point first, Point second, Point... others) {

		Assert.notNull(first, "First point must not be null!");
		Assert.notNull(second, "Second point must not be null!");
		Assert.notNull(others, "Additional points must not be null!");

		List<Point> points = new ArrayList<>();
		points.add(first);
		points.add(second);
		points.addAll(Arrays.asList(others));

		return new GeoJsonMultiPoint(points);
	}

	/**
	 * Creates a new {@link GeoJsonMultiPoint} for the given {@link GeoPoint}s.
	 *
	 * @param geoPoints geoPoints must not be {@literal null} and have at least 2 entries.
	 */
	public static GeoJsonMultiPoint ofGeoPoints(List<GeoPoint> geoPoints) {

		Assert.notNull(geoPoints, "Points must not be null.");
		Assert.isTrue(geoPoints.size() >= 2, "Minimum of 2 Points required.");

		return new GeoJsonMultiPoint(geoPoints.stream().map(GeoPoint::toPoint).collect(Collectors.toList()));
	}

	/**
	 * Creates a new {@link GeoJsonMultiPoint} for the given {@link GeoPoint}s.
	 *
	 * @param first must not be {@literal null}.
	 * @param second must not be {@literal null}.
	 * @param others must not be {@literal null}.
	 */
	public static GeoJsonMultiPoint of(GeoPoint first, GeoPoint second, GeoPoint... others) {

		Assert.notNull(first, "First point must not be null!");
		Assert.notNull(second, "Second point must not be null!");
		Assert.notNull(others, "Additional points must not be null!");

		List<Point> points = new ArrayList<>();
		points.add(GeoPoint.toPoint(first));
		points.add(GeoPoint.toPoint(second));
		points.addAll(Arrays.stream(others).map(GeoPoint::toPoint).collect(Collectors.toList()));

		return new GeoJsonMultiPoint(points);
	}

	@Override
	public String getType() {
		return TYPE;
	}

	@Override
	public List<Point> getCoordinates() {
		return Collections.unmodifiableList(this.points);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		GeoJsonMultiPoint that = (GeoJsonMultiPoint) o;

		return points.equals(that.points);
	}

	@Override
	public int hashCode() {
		return points.hashCode();
	}

	@Override
	public String toString() {
		return "GeoJsonMultiPoint{" + "points=" + points + '}';
	}
}
