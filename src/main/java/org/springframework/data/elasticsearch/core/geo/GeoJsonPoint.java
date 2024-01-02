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

import java.util.Arrays;
import java.util.List;

import org.springframework.data.geo.Point;

/**
 * {@link GeoJson} representation of {@link Point}.<br/>
 * Copied from Spring Data Mongodb, not derived from {@link Point} as this conflicts with the already existing converter
 * for Point in {@link org.springframework.data.elasticsearch.core.convert.GeoConverters}.
 *
 * @author Christoph Strobl
 * @author Peter-Josef Meisch
 * @since 4.1
 * @see <a href="https://geojson.org/geojson-spec.html#point">https://geojson.org/geojson-spec.html#point</a>
 */
public class GeoJsonPoint implements GeoJson<List<Double>> {

	private final double x;
	private final double y;

	public static final String TYPE = "Point";

	private GeoJsonPoint(double x, double y) {
		this.x = x;
		this.y = y;
	}

	/**
	 * Creates {@link GeoJsonPoint} for given coordinates.
	 *
	 * @param x
	 * @param y
	 */
	public static GeoJsonPoint of(double x, double y) {
		return new GeoJsonPoint(x, y);
	}

	/**
	 * Creates {@link GeoJsonPoint} for given {@link Point}.
	 *
	 * @param point must not be {@literal null}.
	 */
	public static GeoJsonPoint of(Point point) {
		return new GeoJsonPoint(point.getX(), point.getY());
	}

	/**
	 * Creates {@link GeoJsonPoint} for given {@link GeoPoint}.
	 *
	 * @param geoPoint must not be {@literal null}.
	 */
	public static GeoJsonPoint of(GeoPoint geoPoint) {
		return new GeoJsonPoint(geoPoint.getLon(), geoPoint.getLat());
	}

	@Override
	public String getType() {
		return TYPE;
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	@Override
	public List<Double> getCoordinates() {
		return Arrays.asList(getX(), getY());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		GeoJsonPoint that = (GeoJsonPoint) o;

		if (Double.compare(that.x, x) != 0)
			return false;
		return Double.compare(that.y, y) == 0;
	}

	@Override
	public int hashCode() {
		int result;
		long temp;
		temp = Double.doubleToLongBits(x);
		result = (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(y);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public String toString() {
		return "GeoJsonPoint{" + "x=" + x + ", y=" + y + '}';
	}
}
