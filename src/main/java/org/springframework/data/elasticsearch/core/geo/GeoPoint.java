/*
 * Copyright 2013-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.elasticsearch.core.geo;

import java.util.Objects;

import org.springframework.data.geo.Point;

/**
 * geo-location used for #{@link org.springframework.data.elasticsearch.core.query.Criteria}.
 *
 * @author Franck Marchand
 * @author Mohsin Husen
 * @author Peter-Josef Meisch
 */
public class GeoPoint {

	private double lat;
	private double lon;

	private GeoPoint() {
		// required by mapper to instantiate object
	}

	public GeoPoint(double latitude, double longitude) {
		this.lat = latitude;
		this.lon = longitude;
	}

	public double getLat() {
		return lat;
	}

	public double getLon() {
		return lon;
	}

	/**
	 * build a GeoPoint from a {@link org.springframework.data.geo.Point}
	 *
	 * @param point {@link org.springframework.data.geo.Point}
	 * @return a {@link org.springframework.data.elasticsearch.core.geo.GeoPoint}
	 */
	public static GeoPoint fromPoint(Point point) {
		return new GeoPoint(point.getY(), point.getX());
	}

	public static Point toPoint(GeoPoint point) {
		return new Point(point.getLon(), point.getLat());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		GeoPoint geoPoint = (GeoPoint) o;
		return Double.compare(geoPoint.lat, lat) == 0 && Double.compare(geoPoint.lon, lon) == 0;
	}

	@Override
	public int hashCode() {
		return Objects.hash(lat, lon);
	}

	@Override
	public String toString() {
		return "GeoPoint{" + "lat=" + lat + ", lon=" + lon + '}';
	}
}
