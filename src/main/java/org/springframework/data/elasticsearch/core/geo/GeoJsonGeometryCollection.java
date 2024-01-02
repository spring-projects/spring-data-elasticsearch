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
import java.util.Collections;
import java.util.List;

import org.springframework.util.Assert;

/**
 * Defines a {@link GeoJsonGeometryCollection} that consists of a {@link List} of {@link GeoJson} objects.<br/>
 * Copied from Spring Data Mongodb
 *
 * @author Christoph Strobl
 * @author Peter-Josef Meisch
 * @since 4.1
 * @see <a href=
 *      "https://geojson.org/geojson-spec.html#geometry-collection">https://geojson.org/geojson-spec.html#geometry-collection</a>
 */
public class GeoJsonGeometryCollection implements GeoJson<Iterable<GeoJson<?>>> {

	public static final String TYPE = "GeometryCollection";

	private final List<GeoJson<?>> geometries = new ArrayList<>();

	private GeoJsonGeometryCollection(List<GeoJson<?>> geometries) {
		this.geometries.addAll(geometries);
	}

	/**
	 * Creates a new {@link GeoJsonGeometryCollection} for the given {@link GeoJson} instances.
	 *
	 * @param geometries must not be {@literal null}.
	 */
	public static GeoJsonGeometryCollection of(List<GeoJson<?>> geometries) {

		Assert.notNull(geometries, "Geometries must not be null!");

		return new GeoJsonGeometryCollection(geometries);
	}

	@Override
	public String getType() {
		return TYPE;
	}

	@Override
	public Iterable<GeoJson<?>> getCoordinates() {
		return getGeometries();
	}

	public List<GeoJson<?>> getGeometries() {
		return Collections.unmodifiableList(this.geometries);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		GeoJsonGeometryCollection that = (GeoJsonGeometryCollection) o;

		return geometries.equals(that.geometries);
	}

	@Override
	public int hashCode() {
		return geometries.hashCode();
	}

	@Override
	public String toString() {
		return "GeoJsonGeometryCollection{" + "geometries=" + geometries + '}';
	}
}
