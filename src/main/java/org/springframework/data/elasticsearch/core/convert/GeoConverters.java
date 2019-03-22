/*
 * Copyright 2019 the original author or authors.
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
package org.springframework.data.elasticsearch.core.convert;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.data.geo.Point;
import org.springframework.util.NumberUtils;

/**
 * Set of {@link Converter converters} specific to Elasticsearch Geo types.
 *
 * @author Christoph Strobl
 * @since 3.2
 */
class GeoConverters {

	static Collection<Object> getConvertersToRegister() {

		return Arrays.asList(PointToMapConverter.INSTANCE, MapToPointConverter.INSTANCE, GeoPointToMapConverter.INSTANCE,
				MapToGeoPointConverter.INSTANCE);
	}

	/**
	 * {@link Converter} to write a {@link Point} to {@link Map} using {@code lat/long} properties.
	 */
	@WritingConverter
	enum PointToMapConverter implements Converter<Point, Map<String, Object>> {

		INSTANCE;

		@Override
		public Map convert(Point source) {

			Map<String, Object> target = new LinkedHashMap<>();
			target.put("lat", source.getX());
			target.put("lon", source.getY());
			return target;
		}
	}

	/**
	 * {@link Converter} to write a {@link GeoPoint} to {@link Map} using {@code lat/long} properties.
	 */
	@WritingConverter
	enum GeoPointToMapConverter implements Converter<GeoPoint, Map<String, Object>> {

		INSTANCE;

		@Override
		public Map convert(GeoPoint source) {
			Map<String, Object> target = new LinkedHashMap<>();
			target.put("lat", source.getLat());
			target.put("lon", source.getLon());
			return target;
		}
	}

	/**
	 * {@link Converter} to read a {@link Point} from {@link Map} using {@code lat/long} properties.
	 */
	@ReadingConverter
	enum MapToPointConverter implements Converter<Map<String, Object>, Point> {

		INSTANCE;

		@Override
		public Point convert(Map source) {
			Double x = NumberUtils.convertNumberToTargetClass((Number) source.get("lat"), Double.class);
			Double y = NumberUtils.convertNumberToTargetClass((Number) source.get("lon"), Double.class);

			return new Point(x, y);
		}
	}

	/**
	 * {@link Converter} to read a {@link GeoPoint} from {@link Map} using {@code lat/long} properties.
	 */
	@ReadingConverter
	enum MapToGeoPointConverter implements Converter<Map<String, Object>, GeoPoint> {

		INSTANCE;

		@Override
		public GeoPoint convert(Map source) {
			Double x = NumberUtils.convertNumberToTargetClass((Number) source.get("lat"), Double.class);
			Double y = NumberUtils.convertNumberToTargetClass((Number) source.get("lon"), Double.class);

			return new GeoPoint(x, y);
		}
	}
}
