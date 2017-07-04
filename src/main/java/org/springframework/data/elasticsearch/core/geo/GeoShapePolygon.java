/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

import org.springframework.data.geo.Point;
import org.springframework.data.geo.Polygon;
import org.springframework.util.Assert;

/**
 * geo-location used for #{@link org.springframework.data.elasticsearch.core.query.Criteria}.
 *
 * @author Lukas Vorisek
 */
public class GeoShapePolygon extends GeoShape<List<GeoShapeLinestring>> {

	private List<GeoShapeLinestring> coordinates = new ArrayList<>();


	private GeoShapePolygon() {
		//required by mapper to instantiate object
		super(GeoShapeType.POLYGON);
	}

	public GeoShapePolygon(Point x, Point y, Point z, Point w, Point... points) {
		this(asList(x, y, z, w, points));
	}

	public GeoShapePolygon(List<? extends Point> points) {
		this();
		Assert.notNull(points, "Points must not be null!");

		this.coordinates.add(new GeoShapeLinestring(points));
	}

	private static List<Point> asList(Point first, Point second, Point third, Point fourth, final Point... others) {

		ArrayList<Point> result = new ArrayList<Point>(3 + others.length);

		result.add(first);
		result.add(second);
		result.add(third);
		result.add(fourth);
		result.addAll(Arrays.asList(others));

		return result;
	}


	@Override
	public List<GeoShapeLinestring> getCoordinates() {
		return Collections.unmodifiableList(this.coordinates);
	}

	public GeoShapePolygon addHole(Point x, Point y, Point z, Point w, Point... points) {
		return addHole(asList(x, y, z, w, points));
	}

	public GeoShapePolygon addHole(List<? extends Point> points) {
		GeoShapePolygon gsp = new GeoShapePolygon();
		gsp.coordinates = new ArrayList<>(this.coordinates);
		gsp.coordinates.add(new GeoShapeLinestring(points));

		return gsp;
	}

	/**
	 * build a GeoPoint from a {@link org.springframework.data.geo.Point}
	 *
	 * @param point {@link org.springframework.data.geo.Point}
	 * @return a {@link org.springframework.data.elasticsearch.core.geo.GeoShapePolygon}
	 */
	public static GeoShapePolygon fromShape(Polygon polygon) {
		List<Point> points = polygon.getPoints();
		return new GeoShapePolygon(points);
	}

	/**
	 * build a GeoPoint from a {@link org.springframework.data.geo.Point}
	 *
	 * @param point {@link org.springframework.data.geo.Point}
	 * @return a {@link org.springframework.data.elasticsearch.core.geo.GeoShapePolygon}
	 */
	public static Polygon toShape(GeoShapePolygon polygon) {
		List<GeoShapeLinestring> coordinates = polygon.getCoordinates();
		List<Point> points = new ArrayList<Point>();
		for(Coordinate point : coordinates.get(0).getCoordinates()) {
			points.add(new Point(point.getX(), point.getY()));
		}

		return new Polygon(points);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {

		if (this == obj) {
			return true;
		}

		if (!(obj instanceof GeoShapePolygon)) {
			return false;
		}

		GeoShapePolygon that = (GeoShapePolygon) obj;

		if(this.coordinates.equals(that.coordinates)) {
			return true;
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return this.coordinates.hashCode();
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder polygon = new StringBuilder();
		for(GeoShapeLinestring linestring : this.coordinates) {
			polygon.append(linestring.toString());
		}
		return String.format("GeoShapePolygon: [[%s]]", polygon);
	}

}
