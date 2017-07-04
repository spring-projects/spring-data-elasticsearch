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
public class GeoShapeMultiLinestring extends GeoShape<List<GeoShapeLinestring>> {

	private List<GeoShapeLinestring> coordinates;


	private GeoShapeMultiLinestring() {
		//required by mapper to instantiate object
		super(GeoShapeType.MULTILINESTRING);
	}

	public GeoShapeMultiLinestring(GeoShapeLinestring x, GeoShapeLinestring y, GeoShapeLinestring... linestrings) {
		this(asList(x, y, linestrings));
	}

	public GeoShapeMultiLinestring(List<? extends GeoShapeLinestring> linestrings) {
		this();
		Assert.notNull(linestrings, "Points must not be null!");

		this.coordinates = new ArrayList<>(linestrings.size());

		for(GeoShapeLinestring linestring : linestrings) {
			Assert.notNull(linestring, "Single GeoShapeLinestring in GeoShapeMultiLinestring must not be null!");
			this.coordinates.add(linestring);
		}
	}

	private static List<GeoShapeLinestring> asList(GeoShapeLinestring x, GeoShapeLinestring y, GeoShapeLinestring... linestrings) {
		List<GeoShapeLinestring> rlinestrings = new ArrayList<>();
		rlinestrings.add(x);
		rlinestrings.add(y);
		for(GeoShapeLinestring linestring : linestrings) {
			rlinestrings.add(linestring);
		}

		return rlinestrings;
	}


	@Override
	public List<GeoShapeLinestring> getCoordinates() {
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

		if (!(obj instanceof GeoShapeMultiLinestring)) {
			return false;
		}

		GeoShapeMultiLinestring that = (GeoShapeMultiLinestring) obj;

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
		StringBuilder linestringsString = new StringBuilder();
		for(GeoShapeLinestring linestring : coordinates) {
			linestringsString.append(linestring.toString());
		}
		return String.format("GeoShapeMultiLinestring: [%s]", linestringsString);
	}

}
