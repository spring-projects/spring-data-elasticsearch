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
public class GeoShapeLinestring extends GeoShape<List<org.springframework.data.elasticsearch.core.geo.GeoShape.Coordinate>> {

	private List<Coordinate> coordinates;


	private GeoShapeLinestring() {
		//required by mapper to instantiate object
		super(GeoShapeType.LINESTRING);
	}

	public GeoShapeLinestring(Point x, Point y, Point... points) {
		this();
		this.coordinates = new ArrayList<>(2 + points.length);
		this.coordinates.add(new Coordinate(x));
		this.coordinates.add(new Coordinate(y));
		for(int i = 0; i < points.length; i++) {
			Point point = points[i];
			Assert.notNull(point, "Single point in Polygon must not be null!");
			this.coordinates.add(new Coordinate(point));
		}

	}

	public GeoShapeLinestring(List<? extends Point> points) {
		this();
		Assert.notNull(points, "Points must not be null!");

		this.coordinates = new ArrayList<>(points.size());

		for(int i = 0; i < points.size(); i++) {
			Point point = points.get(i);
			Assert.notNull(point, "Single Point in Polygon must not be null!");
			this.coordinates.add(new Coordinate(point));
		}

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

		if (!(obj instanceof GeoShapeLinestring)) {
			return false;
		}

		GeoShapeLinestring that = (GeoShapeLinestring) obj;

		return this.coordinates.equals(that.coordinates);
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
		return String.format("GeoShapeLinestring: [[%s]]", polygon);
	}

}
