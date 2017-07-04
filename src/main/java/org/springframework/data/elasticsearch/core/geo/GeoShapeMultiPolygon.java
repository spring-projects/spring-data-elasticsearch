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

import org.springframework.util.Assert;

/**
 * geo-location used for #{@link org.springframework.data.elasticsearch.core.query.Criteria}.
 *
 * @author Lukas Vorisek
 */
public class GeoShapeMultiPolygon extends GeoShape<List<GeoShapePolygon>> {

	private List<GeoShapePolygon> coordinates = new ArrayList<>();


	private GeoShapeMultiPolygon() {
		//required by mapper to instantiate object
		super(GeoShapeType.MULTIPOLYGON);
	}

	public GeoShapeMultiPolygon(GeoShapePolygon first, GeoShapePolygon second, final GeoShapePolygon... others) {
		this(asList(first, second, others));
	}

	public GeoShapeMultiPolygon(List<? extends GeoShapePolygon> polygons) {
		this();
		Assert.notNull(polygons, "Points must not be null!");

		this.coordinates.addAll(polygons);
	}

	private static List<GeoShapePolygon> asList(GeoShapePolygon first, GeoShapePolygon second, final GeoShapePolygon... others) {

		ArrayList<GeoShapePolygon> result = new ArrayList<>(2 + others.length);

		result.add(first);
		result.add(second);
		result.addAll(Arrays.asList(others));

		return result;
	}


	@Override
	public List<GeoShapePolygon> getCoordinates() {
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

		if (!(obj instanceof GeoShapeMultiPolygon)) {
			return false;
		}

		GeoShapeMultiPolygon that = (GeoShapeMultiPolygon) obj;

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
		StringBuilder polygonString = new StringBuilder();
		for(GeoShapePolygon polygon : coordinates) {
			polygonString.append(polygon.toString());
		}
		return String.format("GeoShapeMultiPolygon: [[%s]]", polygonString);
	}

}
