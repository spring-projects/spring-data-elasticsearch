/*
 * Copyright 2019-2024 the original author or authors.
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.elasticsearch.core.geo.GeoJson;
import org.springframework.data.elasticsearch.core.geo.GeoJsonGeometryCollection;
import org.springframework.data.elasticsearch.core.geo.GeoJsonLineString;
import org.springframework.data.elasticsearch.core.geo.GeoJsonMultiLineString;
import org.springframework.data.elasticsearch.core.geo.GeoJsonMultiPoint;
import org.springframework.data.elasticsearch.core.geo.GeoJsonMultiPolygon;
import org.springframework.data.elasticsearch.core.geo.GeoJsonPoint;
import org.springframework.data.elasticsearch.core.geo.GeoJsonPolygon;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.data.geo.Point;
import org.springframework.util.Assert;
import org.springframework.util.NumberUtils;

/**
 * Set of {@link Converter converters} specific to Elasticsearch Geo types.
 *
 * @author Christoph Strobl
 * @author Peter-Josef Meisch
 * @since 3.2
 */
public class GeoConverters {

	static Collection<Converter<?, ?>> getConvertersToRegister() {

		return Arrays.asList(PointToMapConverter.INSTANCE, MapToPointConverter.INSTANCE, //
				GeoPointToMapConverter.INSTANCE, MapToGeoPointConverter.INSTANCE, //
				GeoJsonToMapConverter.INSTANCE, MapToGeoJsonConverter.INSTANCE, //
				GeoJsonPointToMapConverter.INSTANCE, MapToGeoJsonPointConverter.INSTANCE, //
				GeoJsonMultiPointToMapConverter.INSTANCE, MapToGeoJsonMultiPointConverter.INSTANCE, //
				GeoJsonLineStringToMapConverter.INSTANCE, MapToGeoJsonLineStringConverter.INSTANCE, //
				GeoJsonMultiLineStringToMapConverter.INSTANCE, MapToGeoJsonMultiLineStringConverter.INSTANCE, //
				GeoJsonPolygonToMapConverter.INSTANCE, MapToGeoJsonPolygonConverter.INSTANCE, //
				GeoJsonMultiPolygonToMapConverter.INSTANCE, MapToGeoJsonMultiPolygonConverter.INSTANCE, //
				GeoJsonGeometryCollectionToMapConverter.INSTANCE, MapToGeoJsonGeometryCollectionConverter.INSTANCE);
	}

	// region Point
	/**
	 * {@link Converter} to write a {@link Point} to {@link Map} using {@code lat/long} properties.
	 */
	@WritingConverter
	public enum PointToMapConverter implements Converter<Point, Map<String, Object>> {

		INSTANCE;

		@Override
		public Map<String, Object> convert(Point source) {

			Map<String, Object> target = new LinkedHashMap<>();
			target.put("lat", source.getY());
			target.put("lon", source.getX());
			return target;
		}
	}

	/**
	 * {@link Converter} to read a {@link Point} from {@link Map} using {@code lat/long} properties.
	 */
	@ReadingConverter
	public enum MapToPointConverter implements Converter<Map<String, Object>, Point> {

		INSTANCE;

		@Override
		public Point convert(Map<String, Object> source) {
			Double x = NumberUtils.convertNumberToTargetClass((Number) source.get("lon"), Double.class);
			Double y = NumberUtils.convertNumberToTargetClass((Number) source.get("lat"), Double.class);

			return new Point(x, y);
		}
	}
	// endregion

	// region GeoPoint
	/**
	 * {@link Converter} to write a {@link GeoPoint} to {@link Map} using {@code lat/long} properties.
	 */
	@WritingConverter
	public enum GeoPointToMapConverter implements Converter<GeoPoint, Map<String, Object>> {

		INSTANCE;

		@Override
		public Map<String, Object> convert(GeoPoint source) {
			Map<String, Object> target = new LinkedHashMap<>();
			target.put("lat", source.getLat());
			target.put("lon", source.getLon());
			return target;
		}

	}

	@ReadingConverter
	public enum MapToGeoPointConverter implements Converter<Map<String, Object>, GeoPoint> {

		INSTANCE;

		@Override
		public GeoPoint convert(Map<String, Object> source) {
			Double lat = NumberUtils.convertNumberToTargetClass((Number) source.get("lat"), Double.class);
			Double lon = NumberUtils.convertNumberToTargetClass((Number) source.get("lon"), Double.class);

			return new GeoPoint(lat, lon);
		}
	}
	// endregion

	// region GeoJson
	@WritingConverter
	public enum GeoJsonToMapConverter implements Converter<GeoJson<? extends Iterable<?>>, Map<String, Object>> {

		INSTANCE;

		@Override
		public Map<String, Object> convert(GeoJson<? extends Iterable<?>> source) {
			if (source instanceof GeoJsonPoint geoJsonPoint) {
				return GeoJsonPointToMapConverter.INSTANCE.convert(geoJsonPoint);
			} else if (source instanceof GeoJsonMultiPoint geoJsonMultiPoint) {
				return GeoJsonMultiPointToMapConverter.INSTANCE.convert(geoJsonMultiPoint);
			} else if (source instanceof GeoJsonLineString geoJsonLineString) {
				return GeoJsonLineStringToMapConverter.INSTANCE.convert(geoJsonLineString);
			} else if (source instanceof GeoJsonMultiLineString geoJsonMultiLineString) {
				return GeoJsonMultiLineStringToMapConverter.INSTANCE.convert(geoJsonMultiLineString);
			} else if (source instanceof GeoJsonPolygon geoJsonPolygon) {
				return GeoJsonPolygonToMapConverter.INSTANCE.convert(geoJsonPolygon);
			} else if (source instanceof GeoJsonMultiPolygon geoJsonMultiPolygon) {
				return GeoJsonMultiPolygonToMapConverter.INSTANCE.convert(geoJsonMultiPolygon);
			} else if (source instanceof GeoJsonGeometryCollection geoJsonGeometryCollection) {
				return GeoJsonGeometryCollectionToMapConverter.INSTANCE.convert(geoJsonGeometryCollection);
			} else {
				throw new IllegalArgumentException("unknown GeoJson class " + source.getClass().getSimpleName());
			}
		}
	}

	@ReadingConverter
	public enum MapToGeoJsonConverter implements Converter<Map<String, Object>, GeoJson<? extends Iterable<?>>> {

		INSTANCE;

		@Override
		public GeoJson<? extends Iterable<?>> convert(Map<String, Object> source) {

			String type = GeoConverters.getGeoJsonType(source);

			return switch (type) {
				case "point" -> MapToGeoJsonPointConverter.INSTANCE.convert(source);
				case "multipoint" -> MapToGeoJsonMultiPointConverter.INSTANCE.convert(source);
				case "linestring" -> MapToGeoJsonLineStringConverter.INSTANCE.convert(source);
				case "multilinestring" -> MapToGeoJsonMultiLineStringConverter.INSTANCE.convert(source);
				case "polygon" -> MapToGeoJsonPolygonConverter.INSTANCE.convert(source);
				case "multipolygon" -> MapToGeoJsonMultiPolygonConverter.INSTANCE.convert(source);
				case "geometrycollection" -> MapToGeoJsonGeometryCollectionConverter.INSTANCE.convert(source);
				default -> throw new IllegalArgumentException("unknown GeoJson type " + type);
			};
		}
	}
	// endregion

	// region GeoJsonPoint
	@WritingConverter
	public enum GeoJsonPointToMapConverter implements Converter<GeoJsonPoint, Map<String, Object>> {

		INSTANCE;

		@Override
		public Map<String, Object> convert(GeoJsonPoint geoJsonPoint) {
			Map<String, Object> map = new LinkedHashMap<>();
			map.put("type", geoJsonPoint.getType());
			map.put("coordinates", geoJsonPoint.getCoordinates());
			return map;
		}
	}

	@ReadingConverter
	public enum MapToGeoJsonPointConverter implements Converter<Map<String, Object>, GeoJsonPoint> {

		INSTANCE;

		@Override
		public GeoJsonPoint convert(Map<String, Object> source) {

			String type = GeoConverters.getGeoJsonType(source);
			Assert.isTrue(type.equalsIgnoreCase(GeoJsonPoint.TYPE), "does not contain a type 'Point'");

			Object coordinates = source.get("coordinates");
			Assert.notNull(coordinates, "Document to convert does not contain coordinates");
			Assert.isTrue(coordinates instanceof List, "coordinates must be a List of Numbers");
			// noinspection unchecked
			List<Number> numbers = (List<Number>) coordinates;
			Assert.isTrue(numbers.size() >= 2, "coordinates must have at least 2 elements");

			return GeoJsonPoint.of(numbers.get(0).doubleValue(), numbers.get(1).doubleValue());
		}
	}
	// endregion

	// region GeoJsonMultiPoint
	@WritingConverter
	public enum GeoJsonMultiPointToMapConverter implements Converter<GeoJsonMultiPoint, Map<String, Object>> {

		INSTANCE;

		@Override
		public Map<String, Object> convert(GeoJsonMultiPoint geoJsonMultiPoint) {
			Map<String, Object> map = new LinkedHashMap<>();
			map.put("type", geoJsonMultiPoint.getType());
			map.put("coordinates", pointsToCoordinates(geoJsonMultiPoint.getCoordinates()));
			return map;
		}
	}

	@ReadingConverter
	public enum MapToGeoJsonMultiPointConverter implements Converter<Map<String, Object>, GeoJsonMultiPoint> {

		INSTANCE;

		@Override
		public GeoJsonMultiPoint convert(Map<String, Object> source) {

			String type = GeoConverters.getGeoJsonType(source);
			Assert.isTrue(type.equalsIgnoreCase(GeoJsonMultiPoint.TYPE), "does not contain a type 'MultiPoint'");
			Object coordinates = source.get("coordinates");
			Assert.notNull(coordinates, "Document to convert does not contain coordinates");
			Assert.isTrue(coordinates instanceof List, "coordinates must be a List");

			// noinspection unchecked
			return GeoJsonMultiPoint.of(coordinatesToPoints((List<List<Number>>) coordinates));
		}
	}
	// endregion

	// region GeoJsonLineString
	@WritingConverter
	public enum GeoJsonLineStringToMapConverter implements Converter<GeoJsonLineString, Map<String, Object>> {

		INSTANCE;

		@Override
		public Map<String, Object> convert(GeoJsonLineString geoJsonLineString) {
			Map<String, Object> map = new LinkedHashMap<>();
			map.put("type", geoJsonLineString.getType());
			map.put("coordinates", pointsToCoordinates(geoJsonLineString.getCoordinates()));
			return map;
		}
	}

	@ReadingConverter
	public enum MapToGeoJsonLineStringConverter implements Converter<Map<String, Object>, GeoJsonLineString> {

		INSTANCE;

		@Override
		public GeoJsonLineString convert(Map<String, Object> source) {

			String type = GeoConverters.getGeoJsonType(source);
			Assert.isTrue(type.equalsIgnoreCase(GeoJsonLineString.TYPE), "does not contain a type 'LineString'");
			Object coordinates = source.get("coordinates");
			Assert.notNull(coordinates, "Document to convert does not contain coordinates");
			Assert.isTrue(coordinates instanceof List, "coordinates must be a List");

			// noinspection unchecked
			return GeoJsonLineString.of(coordinatesToPoints((List<List<Number>>) coordinates));
		}
	}
	// endregion

	// region GeoJsonMultiLineString
	@WritingConverter
	public enum GeoJsonMultiLineStringToMapConverter implements Converter<GeoJsonMultiLineString, Map<String, Object>> {

		INSTANCE;

		@Override
		public Map<String, Object> convert(GeoJsonMultiLineString source) {
			return geoJsonLinesStringsToMap(source.getType(), source.getCoordinates());
		}
	}

	@ReadingConverter
	public enum MapToGeoJsonMultiLineStringConverter implements Converter<Map<String, Object>, GeoJsonMultiLineString> {

		INSTANCE;

		@Override
		public GeoJsonMultiLineString convert(Map<String, Object> source) {

			String type = GeoConverters.getGeoJsonType(source);
			Assert.isTrue(type.equalsIgnoreCase(GeoJsonMultiLineString.TYPE), "does not contain a type 'MultiLineString'");
			List<GeoJsonLineString> lines = geoJsonLineStringsFromMap(source);
			return GeoJsonMultiLineString.of(lines);
		}
	}
	// endregion

	// region GeoJsonPolygon
	@WritingConverter
	public enum GeoJsonPolygonToMapConverter implements Converter<GeoJsonPolygon, Map<String, Object>> {

		INSTANCE;

		@Override
		public Map<String, Object> convert(GeoJsonPolygon source) {
			return geoJsonLinesStringsToMap(source.getType(), source.getCoordinates());
		}
	}

	@ReadingConverter
	public enum MapToGeoJsonPolygonConverter implements Converter<Map<String, Object>, GeoJsonPolygon> {

		INSTANCE;

		@Override
		public GeoJsonPolygon convert(Map<String, Object> source) {

			String type = GeoConverters.getGeoJsonType(source);
			Assert.isTrue(type.equalsIgnoreCase(GeoJsonPolygon.TYPE), "does not contain a type 'Polygon'");
			List<GeoJsonLineString> lines = geoJsonLineStringsFromMap(source);
			Assert.isTrue(!lines.isEmpty(), "no linestrings defined in polygon");
			GeoJsonPolygon geoJsonPolygon = GeoJsonPolygon.of(lines.get(0));
			for (int i = 1; i < lines.size(); i++) {
				geoJsonPolygon = geoJsonPolygon.withInnerRing(lines.get(i));
			}
			return geoJsonPolygon;
		}
	}
	// endregion

	// region GeoJsonMultiPolygon
	@WritingConverter
	public enum GeoJsonMultiPolygonToMapConverter implements Converter<GeoJsonMultiPolygon, Map<String, Object>> {

		INSTANCE;

		@Override
		public Map<String, Object> convert(GeoJsonMultiPolygon source) {

			Map<String, Object> map = new LinkedHashMap<>();
			map.put("type", source.getType());

			List<Object> coordinates = source.getCoordinates().stream() //
					.map(GeoJsonPolygonToMapConverter.INSTANCE::convert) //
					.filter(Objects::nonNull) //
					.map(it -> it.get("coordinates")) //
					.collect(Collectors.toList()); //
			map.put("coordinates", coordinates);

			return map;
		}
	}

	@ReadingConverter
	public enum MapToGeoJsonMultiPolygonConverter implements Converter<Map<String, Object>, GeoJsonMultiPolygon> {

		INSTANCE;

		@Override
		public GeoJsonMultiPolygon convert(Map<String, Object> source) {

			String type = GeoConverters.getGeoJsonType(source);
			Assert.isTrue(type.equalsIgnoreCase(GeoJsonMultiPolygon.TYPE), "does not contain a type 'MultiPolygon'");
			Object coordinates = source.get("coordinates");
			Assert.notNull(coordinates, "Document to convert does not contain coordinates");
			Assert.isTrue(coordinates instanceof List, "coordinates must be a List");

			List<GeoJsonPolygon> geoJsonPolygons = ((List<?>) coordinates).stream().map(it -> {
				Map<String, Object> map = new LinkedHashMap<>();
				map.put("type", GeoJsonPolygon.TYPE);
				map.put("coordinates", it);
				return map;
			}).map(MapToGeoJsonPolygonConverter.INSTANCE::convert).collect(Collectors.toList());

			return GeoJsonMultiPolygon.of(geoJsonPolygons);
		}
	}
	// endregion

	// region GeoJsonGeometryCollection
	@WritingConverter
	public enum GeoJsonGeometryCollectionToMapConverter
			implements Converter<GeoJsonGeometryCollection, Map<String, Object>> {

		INSTANCE;

		@Override
		public Map<String, Object> convert(GeoJsonGeometryCollection source) {

			Map<String, Object> map = new LinkedHashMap<>();
			map.put("type", source.getType());
			List<Map<String, Object>> geometries = source.getGeometries().stream()
					.map(GeoJsonToMapConverter.INSTANCE::convert).collect(Collectors.toList());
			map.put("geometries", geometries);

			return map;
		}
	}

	@ReadingConverter
	public enum MapToGeoJsonGeometryCollectionConverter
			implements Converter<Map<String, Object>, GeoJsonGeometryCollection> {

		INSTANCE;

		@Override
		public GeoJsonGeometryCollection convert(Map<String, Object> source) {

			String type = GeoConverters.getGeoJsonType(source);
			Assert.isTrue(type.equalsIgnoreCase(GeoJsonGeometryCollection.TYPE),
					"does not contain a type 'GeometryCollection'");
			Object geometries = source.get("geometries");
			Assert.notNull(geometries, "Document to convert does not contain geometries");
			Assert.isTrue(geometries instanceof List, "geometries must be a List");

			// noinspection unchecked
			List<GeoJson<?>> geoJsonList = ((List<Map<String, Object>>) geometries).stream()
					.map(MapToGeoJsonConverter.INSTANCE::convert).collect(Collectors.toList());
			return GeoJsonGeometryCollection.of(geoJsonList);
		}
	}
	// endregion

	// region helper functions
	private static String getGeoJsonType(Map<String, Object> source) {

		Object type = source.get("type");
		Assert.notNull(type, "Document to convert does not contain a type");
		Assert.isTrue(type instanceof String, "type must be a String");

		return type.toString().toLowerCase();
	}

	private static List<Double> toCoordinates(Point point) {
		return Arrays.asList(point.getX(), point.getY());
	}

	private static List<List<Double>> pointsToCoordinates(List<Point> points) {
		return points.stream().map(GeoConverters::toCoordinates).collect(Collectors.toList());
	}

	private static List<Point> coordinatesToPoints(List<List<Number>> pointList) {

		Assert.isTrue(pointList.size() >= 2, "pointList must have at least 2 elements");

		return pointList.stream().map(numbers -> {
			Assert.isTrue(numbers.size() >= 2, "coordinates must have at least 2 elements");

			return new Point(numbers.get(0).doubleValue(), numbers.get(1).doubleValue());
		}).collect(Collectors.toList());
	}

	private static Map<String, Object> geoJsonLinesStringsToMap(String type, List<GeoJsonLineString> lineStrings) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("type", type);
		List<List<List<Double>>> coordinates = lineStrings.stream()
				.map(it -> GeoConverters.pointsToCoordinates(it.getCoordinates())).collect(Collectors.toList());
		map.put("coordinates", coordinates);
		return map;
	}

	private static List<GeoJsonLineString> geoJsonLineStringsFromMap(Map<String, Object> source) {
		Object coordinates = source.get("coordinates");
		Assert.notNull(coordinates, "Document to convert does not contain coordinates");
		Assert.isTrue(coordinates instanceof List, "coordinates must be a List");

		// noinspection unchecked
		List<GeoJsonLineString> lines = ((List<?>) coordinates).stream()
				.map(it -> GeoJsonLineString.of(coordinatesToPoints((List<List<Number>>) it))).collect(Collectors.toList());
		return lines;
	}

	// endregion
}
