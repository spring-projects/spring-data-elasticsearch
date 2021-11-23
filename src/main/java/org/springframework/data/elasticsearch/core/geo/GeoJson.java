/*
 * Copyright 2015-2022 the original author or authors.
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

import org.springframework.data.elasticsearch.core.convert.ConversionException;
import org.springframework.data.elasticsearch.core.convert.GeoConverters;
import org.springframework.data.elasticsearch.core.document.Document;

/**
 * Interface definition for structures defined in <a href="https://geojson.org">GeoJSON</a>
 * format. copied from Spring
 * Data Mongodb
 *
 * @author Christoph Strobl
 * @since 1.7
 */
public interface GeoJson<T extends Iterable<?>> {

	/**
	 * String value representing the type of the {@link GeoJson} object.
	 *
	 * @return will never be {@literal null}.
	 * @see <a href=
	 *      "https://geojson.org/geojson-spec.html#geojson-objects">https://geojson.org/geojson-spec.html#geojson-objects</a>
	 */
	String getType();

	/**
	 * The value of the coordinates member is always an {@link Iterable}. The structure for the elements within is
	 * determined by {@link #getType()} of geometry.
	 *
	 * @return will never be {@literal null}.
	 * @see <a href=
	 *      "https://geojson.org/geojson-spec.html#geometry-objects">https://geojson.org/geojson-spec.html#geometry-objects</a>
	 */
	T getCoordinates();

	/**
	 * @param json the JSON string to parse
	 * @return the parsed {@link GeoJson} object
	 * @throws ConversionException on parse erros
	 */
	static GeoJson<?> of(String json) {
		return GeoConverters.MapToGeoJsonConverter.INSTANCE.convert(Document.parse(json));
	}

	/**
	 * @return a JSON representation of this object
	 */
	default String toJson() {
		return Document.from(GeoConverters.GeoJsonToMapConverter.INSTANCE.convert(this)).toJson();
	}
}
