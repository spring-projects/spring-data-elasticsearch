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
import java.util.Iterator;
import java.util.List;

import org.springframework.data.geo.Point;
import org.springframework.util.Assert;

/**
 * {@link GeoJson} representation of a polygon. <br/>
 * Copied from Spring Data Mongodb
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @author Peter-Josef Meisch
 * @since 4.1
 * @see <a href="https://geojson.org/geojson-spec.html#polygon">https://geojson.org/geojson-spec.html#polygon</a>
 */
public class GeoJsonPolygon implements GeoJson<Iterable<GeoJsonLineString>> {

	public static final String TYPE = "Polygon";

	private final List<GeoJsonLineString> coordinates = new ArrayList<>();

	private GeoJsonPolygon(GeoJsonLineString geoJsonLineString) {
		Assert.notNull(geoJsonLineString, "geoJsonLineString must not be null");
		Assert.isTrue(geoJsonLineString.getCoordinates().size() >= 4, "geoJsonLineString must have at least 4 points");

		this.coordinates.add(geoJsonLineString);
	}

	private GeoJsonPolygon(List<Point> points) {
		this(GeoJsonLineString.of(points));
	}

	/**
	 * Creates new {@link GeoJsonPolygon} from the given {@link GeoJsonLineString}.
	 *
	 * @param geoJsonLineString must not be {@literal null}.
	 */
	public static GeoJsonPolygon of(GeoJsonLineString geoJsonLineString) {
		return new GeoJsonPolygon(geoJsonLineString);
	}

	/**
	 * Creates new {@link GeoJsonPolygon} from the given {@link Point}s.
	 *
	 * @param points must not be {@literal null}.
	 */
	public static GeoJsonPolygon of(List<Point> points) {
		return new GeoJsonPolygon(points);
	}

	/**
	 * Creates new {@link GeoJsonPolygon} from the given {@link GeoPoint}s.
	 *
	 * @param geoPoints must not be {@literal null}.
	 */
	public static GeoJsonPolygon ofGeoPoints(List<GeoPoint> geoPoints) {
		return new GeoJsonPolygon(GeoJsonLineString.ofGeoPoints(geoPoints));
	}

	/**
	 * Creates new {@link GeoJsonPolygon} from the given {@link Point}s.
	 *
	 * @param first must not be {@literal null}.
	 * @param second must not be {@literal null}.
	 * @param third must not be {@literal null}.
	 * @param fourth must not be {@literal null}
	 * @param others can be empty.
	 */
	public static GeoJsonPolygon of(Point first, Point second, Point third, Point fourth, Point... others) {
		return new GeoJsonPolygon(asList(first, second, third, fourth, others));
	}

	/**
	 * Creates new {@link GeoJsonPolygon} from the given {@link GeoPoint}s.
	 *
	 * @param first must not be {@literal null}.
	 * @param second must not be {@literal null}.
	 * @param third must not be {@literal null}.
	 * @param fourth must not be {@literal null}
	 * @param others can be empty.
	 */
	public static GeoJsonPolygon of(GeoPoint first, GeoPoint second, GeoPoint third, GeoPoint fourth,
			GeoPoint... others) {
		return new GeoJsonPolygon(GeoJsonLineString.ofGeoPoints(asList(first, second, third, fourth, others)));
	}

	/**
	 * Creates a new {@link GeoJsonPolygon} with an inner ring defined be the given {@link Point}s.
	 *
	 * @param first must not be {@literal null}.
	 * @param second must not be {@literal null}.
	 * @param third must not be {@literal null}.
	 * @param fourth must not be {@literal null}.
	 * @param others can be empty.
	 * @return new {@link GeoJsonPolygon}.
	 */
	public GeoJsonPolygon withInnerRing(Point first, Point second, Point third, Point fourth, Point... others) {
		return withInnerRing(asList(first, second, third, fourth, others));
	}

	/**
	 * Creates a new {@link GeoJsonPolygon} with an inner ring defined be the given {@link GeoPoint}s.
	 *
	 * @param first must not be {@literal null}.
	 * @param second must not be {@literal null}.
	 * @param third must not be {@literal null}.
	 * @param fourth must not be {@literal null}.
	 * @param others can be empty.
	 * @return new {@link GeoJsonPolygon}.
	 */
	public GeoJsonPolygon withInnerRing(GeoPoint first, GeoPoint second, GeoPoint third, GeoPoint fourth,
			GeoPoint... others) {
		return withInnerRingOfGeoPoints(asList(first, second, third, fourth, others));
	}

	/**
	 * Creates a new {@link GeoJsonPolygon} with an inner ring defined be the given {@link List} of {@link Point}s.
	 *
	 * @param points must not be {@literal null}.
	 * @return new {@link GeoJsonPolygon}.
	 */
	public GeoJsonPolygon withInnerRing(List<Point> points) {
		return withInnerRing(GeoJsonLineString.of(points));
	}

	/**
	 * Creates a new {@link GeoJsonPolygon} with an inner ring defined be the given {@link List} of {@link GeoPoint}s.
	 *
	 * @param geoPoints must not be {@literal null}.
	 * @return new {@link GeoJsonPolygon}.
	 */
	public GeoJsonPolygon withInnerRingOfGeoPoints(List<GeoPoint> geoPoints) {
		return withInnerRing(GeoJsonLineString.ofGeoPoints(geoPoints));
	}

	/**
	 * Creates a new {@link GeoJsonPolygon} with an inner ring defined be the given {@link GeoJsonLineString}.
	 *
	 * @param lineString must not be {@literal null}.
	 * @return new {@link GeoJsonPolygon}.
	 * @since 1.10
	 */
	public GeoJsonPolygon withInnerRing(GeoJsonLineString lineString) {

		Assert.notNull(lineString, "LineString must not be null!");

		Iterator<GeoJsonLineString> it = this.coordinates.iterator();
		GeoJsonPolygon polygon = new GeoJsonPolygon(it.next().getCoordinates());

		while (it.hasNext()) {
			polygon.coordinates.add(it.next());
		}

		polygon.coordinates.add(lineString);
		return polygon;
	}

	@Override
	public String getType() {
		return TYPE;
	}

	@Override
	public List<GeoJsonLineString> getCoordinates() {
		return Collections.unmodifiableList(this.coordinates);
	}

	@SafeVarargs
	private static <T> List<T> asList(T first, T second, T third, T fourth, T... others) {

		ArrayList<T> result = new ArrayList<>(3 + others.length);

		result.add(first);
		result.add(second);
		result.add(third);
		result.add(fourth);
		result.addAll(Arrays.asList(others));

		return result;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		GeoJsonPolygon that = (GeoJsonPolygon) o;

		return coordinates.equals(that.coordinates);
	}

	@Override
	public int hashCode() {
		return coordinates.hashCode();
	}

	@Override
	public String toString() {
		return "GeoJsonPolygon{" + "coordinates=" + coordinates + '}';
	}
}
