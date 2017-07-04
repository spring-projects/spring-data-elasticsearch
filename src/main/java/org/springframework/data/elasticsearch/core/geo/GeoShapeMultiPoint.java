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
import java.util.Collections;
import java.util.List;

import org.springframework.data.geo.Point;
import org.springframework.util.Assert;

/**
 * geo-location used for #{@link org.springframework.data.elasticsearch.core.query.Criteria}.
 *
 * @author Lukas Vorisek
 */
public class GeoShapeMultiPoint extends GeoShape<List<org.springframework.data.elasticsearch.core.geo.GeoShape.Coordinate>> {

	private List<Coordinate> coordinates;


	private GeoShapeMultiPoint() {
		//required by mapper to instantiate object
		super(GeoShapeType.MULTIPOINT);
	}

	public GeoShapeMultiPoint(Point x, Point y, Point... points) {
		this(asList(x, y, points));
	}

	public GeoShapeMultiPoint(List<? extends Point> points) {
		this();
		Assert.notNull(points, "Points must not be null!");

		this.coordinates = new ArrayList<>(points.size());

		for(Point point : points) {
			Assert.notNull(point, "Single Point in Polygon must not be null!");
			this.coordinates.add(new Coordinate(point));
		}
	}

	private static List<Point> asList(Point x, Point y, Point... points) {
		List<Point> rpoints = new ArrayList<>();
		rpoints.add(x);
		rpoints.add(y);
		for(Point p : points) {
			rpoints.add(p);
		}

		return rpoints;
	}


	@Override
	public List<Coordinate> getCoordinates() {
		return Collections.unmodifiableList(this.coordinates);
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

		if (!(obj instanceof GeoShapeMultiPoint)) {
			return false;
		}

		GeoShapeMultiPoint that = (GeoShapeMultiPoint) obj;

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
		for(Coordinate point : coordinates) {
			polygon.append(point.toString());
		}
		return String.format("GeoShapeMultiPoint: [%s]", polygon);
	}

}
