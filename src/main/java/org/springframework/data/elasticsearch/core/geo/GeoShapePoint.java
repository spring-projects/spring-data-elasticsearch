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

import org.springframework.data.geo.Point;
import org.springframework.util.Assert;

/**
 * geo-location used for #{@link org.springframework.data.elasticsearch.core.query.Criteria}.
 *
 * @author Lukas Vorisek
 */
public class GeoShapePoint extends GeoShape<double[]> {

	private Coordinate coordinates;


	private GeoShapePoint() {
		//required by mapper to instantiate object
		super(GeoShapeType.POINT);
	}

	public GeoShapePoint(Point point) {
		this();

		Assert.notNull(point, "Point cannot be null");

		this.coordinates = new Coordinate(point);
	}

	public GeoShapePoint(double x, double y) {
		this();

		this.coordinates = new Coordinate(x, y);
	}


	@Override
	public double [] getCoordinates() {
		return this.coordinates.getCoordinate();
	}

	public double getX() {
		return this.coordinates.getX();
	}

	public double getY() {
		return this.coordinates.getY();
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

		if (!(obj instanceof GeoShapePoint)) {
			return false;
		}

		GeoShapePoint that = (GeoShapePoint) obj;

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
		return coordinates.hashCode();
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format("GeoShapePoint: [%s, %s]", this.coordinates.getX(), this.coordinates.getY());
	}

}
