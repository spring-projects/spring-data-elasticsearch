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

import org.springframework.util.Assert;

/**
 * geo-location used for #{@link org.springframework.data.elasticsearch.core.query.Criteria}.
 *
 * @author Lukas Vorisek
 */
public class GeoShapeGeometryCollection extends GeoShape<List<GeoShape<?>>> {

	private List<GeoShape<?>> coordinates;

	private GeoShapeGeometryCollection() {
		//required by mapper to instantiate object
		super(GeoShapeType.GEOMETRY_COLLECTION);
	}

	public GeoShapeGeometryCollection(GeoShape<?> x, GeoShape<?> y, GeoShape<?>... shapes) {
		this(asList(x, y, shapes));
	}

	public GeoShapeGeometryCollection(List<? extends GeoShape<?>> shapes) {
		this();
		Assert.notNull(shapes, "Points must not be null!");

		this.coordinates = new ArrayList<>(shapes.size());

		for(GeoShape<?> shape : shapes) {
			Assert.notNull(shape, "Single Point in Polygon must not be null!");
			this.coordinates.add(shape);
		}
	}

	private static List<GeoShape<?>> asList(GeoShape<?> x, GeoShape<?> y, GeoShape<?>... shapes) {
		List<GeoShape<?>> rpoints = new ArrayList<>();
		rpoints.add(x);
		rpoints.add(y);
		for(GeoShape<?> p : shapes) {
			rpoints.add(p);
		}

		return rpoints;
	}

	@Override
	public List<GeoShape<?>> getCoordinates() {
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

		if (!(obj instanceof GeoShapeGeometryCollection)) {
			return false;
		}

		GeoShapeGeometryCollection that = (GeoShapeGeometryCollection) obj;

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
		StringBuilder shapesInCollection = new StringBuilder();
		for(GeoShape<?> shape : coordinates) {
			shapesInCollection.append(shape.toString());
		}
		return String.format("GeoShapeGeometryCollection: [%s]", shapesInCollection);
	}

}
