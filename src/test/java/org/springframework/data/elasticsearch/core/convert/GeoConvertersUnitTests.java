/*
 * Copyright 2020-2022 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;
import static org.skyscreamer.jsonassert.JSONAssert.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.geo.GeoJson;
import org.springframework.data.elasticsearch.core.geo.GeoJsonGeometryCollection;
import org.springframework.data.elasticsearch.core.geo.GeoJsonLineString;
import org.springframework.data.elasticsearch.core.geo.GeoJsonMultiLineString;
import org.springframework.data.elasticsearch.core.geo.GeoJsonMultiPoint;
import org.springframework.data.elasticsearch.core.geo.GeoJsonMultiPolygon;
import org.springframework.data.elasticsearch.core.geo.GeoJsonPoint;
import org.springframework.data.elasticsearch.core.geo.GeoJsonPolygon;
import org.springframework.data.geo.Point;

/**
 * @author Peter-Josef Meisch
 */
class GeoConvertersUnitTests {

	@Nested
	@DisplayName("GeoJson Converters")
	class GeoJsonTests {

		private final GeoConverters.GeoJsonToMapConverter geoJsonToMapConverter = GeoConverters.GeoJsonToMapConverter.INSTANCE;
		private final GeoConverters.MapToGeoJsonConverter mapToGeoJsonConverter = GeoConverters.MapToGeoJsonConverter.INSTANCE;

		@Nested
		@DisplayName("GeoJsonPoint")
		class GeoJsonPointUnitTests {

			// NOTE: the test converting from a map contains the type names in lowercase, that might be returned from
			// Elasticsearch

			@Test // DATAES-930
			@DisplayName("should be converted to a Map")
			void shouldBeConvertedToAMap() throws JSONException {

				String expected = "{\n" + //
						"  \"type\": \"Point\",\n" + //
						"  \"coordinates\": [12.0, 34.0]\n" + //
						"}"; //
				GeoJsonPoint geoJsonPoint = GeoJsonPoint.of(12, 34);

				Map<String, Object> map = geoJsonToMapConverter.convert(geoJsonPoint);
				String json = Document.from(map).toJson();

				assertEquals(expected, json, false);
			}

			@Test // DATAES-930
			@DisplayName("should be converted from a Map")
			void shouldBeConvertedFromAMap() {

				// make sure we can read int values as well
				String json = "{\n" + //
						"  \"type\": \"point\",\n" + //
						"  \"coordinates\": [12, 34.0]\n" + //
						"}"; //

				Document document = Document.parse(json);
				GeoJsonPoint expected = GeoJsonPoint.of(12, 34);

				GeoJson<? extends Iterable<?>> geoJsonPoint = mapToGeoJsonConverter.convert(document);

				assertThat(geoJsonPoint).isInstanceOf(GeoJsonPoint.class).isEqualTo(expected);
			}
		}

		@Nested
		@DisplayName("GeoJsonMultiPoint")
		class GeoJsonMultipointUnitTests {

			@Test // DATAES-930
			@DisplayName("should be converted to a Map")
			void shouldBeConvertedToAMap() throws JSONException {

				String expected = "{\n" + //
						"  \"type\": \"MultiPoint\",\n" + //
						"  \"coordinates\": [\n" + //
						"    [12.0, 34.0],\n" + //
						"    [56.0, 78.0]\n" + //
						"  ]\n" + //
						"}\n"; //

				GeoJsonMultiPoint geoJsonMultiPoint = GeoJsonMultiPoint.of(new Point(12, 34), new Point(56, 78));

				Map<String, Object> map = geoJsonToMapConverter.convert(geoJsonMultiPoint);
				String json = Document.from(map).toJson();

				assertEquals(expected, json, false);
			}

			@Test // DATAES-930
			@DisplayName("should be converted from a Map")
			void shouldBeConvertedFromAMap() {

				// make sure we can read int values as well
				String json = "{\n" + //
						"  \"type\": \"multipoint\",\n" //
						+ "  \"coordinates\": [\n" + //
						"    [12.0, 34],\n" + //
						"    [56, 78.0]\n" + //
						"  ]\n" + //
						"}\n"; //

				Document document = Document.parse(json);

				GeoJsonMultiPoint expected = GeoJsonMultiPoint.of(new Point(12, 34), new Point(56, 78));
				GeoJson<? extends Iterable<?>> geoJsonMultiPoint = mapToGeoJsonConverter.convert(document);

				assertThat(geoJsonMultiPoint).isInstanceOf(GeoJsonMultiPoint.class).isEqualTo(expected);
			}
		}

		@Nested
		@DisplayName("GeoJsonLineString")
		class GeoJsonLineStringUnitTests {

			@Test // DATAES-930
			@DisplayName("should be converted to a Map")
			void shouldBeConvertedToAMap() throws JSONException {

				String expected = "{\n" + //
						"  \"type\": \"LineString\",\n" + //
						"  \"coordinates\": [\n" + //
						"    [12.0, 34.0],\n" + //
						"    [56.0, 78.0]\n" + //
						"  ]\n" + //
						"}\n"; //

				GeoJsonLineString geoJsonLineString = GeoJsonLineString.of(new Point(12, 34), new Point(56, 78));

				Map<String, Object> map = geoJsonToMapConverter.convert(geoJsonLineString);
				String json = Document.from(map).toJson();

				assertEquals(expected, json, false);
			}

			@Test // DATAES-930
			@DisplayName("should be converted from a Map")
			void shouldBeConvertedFromAMap() {

				// make sure we can read int values as well
				String json = "{\n" + //
						"  \"type\": \"linestring\",\n" + //
						"  \"coordinates\": [\n" + //
						"    [12.0, 34],\n" + //
						"    [56, 78.0]\n" //
						+ "  ]\n" //
						+ "}\n"; //
				Document document = Document.parse(json);

				GeoJsonLineString expected = GeoJsonLineString.of(new Point(12, 34), new Point(56, 78));
				GeoJson<? extends Iterable<?>> geoJsonLineString = mapToGeoJsonConverter.convert(document);

				assertThat(geoJsonLineString).isInstanceOf(GeoJsonLineString.class).isEqualTo(expected);
			}
		}

		@Nested
		@DisplayName("GeoJsonMultiLineString")
		class GeoJsonMultiLineStringUnitTests {

			@Test // DATAES-930
			@DisplayName("should be converted to a Map")
			void shouldBeConvertedToAMap() throws JSONException {
				String expected = "{\n" + //
						"  \"type\": \"MultiLineString\",\n" + //
						"  \"coordinates\": [\n" + //
						"    [[12.0, 34.0], [56.0, 78.0]],\n" + //
						"    [[90.0, 12.0], [34.0, 56.0]]\n" + //
						"  ]\n" + //
						"}\n"; //

				List<GeoJsonLineString> lines = Arrays.asList( //
						GeoJsonLineString.of(new Point(12, 34), new Point(56, 78)), //
						GeoJsonLineString.of(new Point(90, 12), new Point(34, 56)) //
				);
				GeoJsonMultiLineString geoJsonLineString = GeoJsonMultiLineString.of(lines);

				Map<String, Object> map = geoJsonToMapConverter.convert(geoJsonLineString);
				String json = Document.from(map).toJson();

				assertEquals(expected, json, false);
			}

			@Test // DATAES-930
			@DisplayName("should be converted from a Map")
			void shouldBeConvertedFromAMap() {
				// make sure we can read int values as well
				String json = "{\n" + //
						"  \"type\": \"multilinestring\",\n" + //
						"  \"coordinates\": [\n" + //
						"    [[12, 34.0], [56.0, 78]],\n" + //
						"    [[90.0, 12], [34, 56.0]]\n" + //
						"  ]\n" + //
						"}\n"; //
				Document document = Document.parse(json);
				List<GeoJsonLineString> lines = Arrays.asList( //
						GeoJsonLineString.of(new Point(12, 34), new Point(56, 78)), //
						GeoJsonLineString.of(new Point(90, 12), new Point(34, 56)) //
				);
				GeoJsonMultiLineString expected = GeoJsonMultiLineString.of(lines);

				GeoJson<? extends Iterable<?>> geoJsonLineString = mapToGeoJsonConverter.convert(document);

				assertThat(geoJsonLineString).isInstanceOf(GeoJsonMultiLineString.class).isEqualTo(expected);
			}
		}

		@Nested
		@DisplayName("GeoJsonPolygon")
		class GeoJsonPolygonUnitTests {

			@Test // DATAES-930
			@DisplayName("should be converted to a Map")
			void shouldBeConvertedToAMap() throws JSONException {
				String expected = "{\n" + //
						"  \"type\": \"Polygon\",\n" + //
						"  \"coordinates\": [\n" + //
						"    [[12.0, 34.0], [56.0, 78.0], [90.0, 12.0], [12.0, 34.0]],\n" + //
						"    [[56.0, 78.0], [90.0, 12.0], [34.0, 56.0], [56.0, 78.0]]\n" + //
						"  ]\n" + //
						"}\n"; //

				GeoJsonLineString polygonLineString = GeoJsonLineString.of(new Point(12, 34), new Point(56, 78),
						new Point(90, 12), new Point(12, 34));
				GeoJsonLineString innerRingLineString = GeoJsonLineString.of(new Point(56, 78), new Point(90, 12),
						new Point(34, 56), new Point(56, 78));
				GeoJsonPolygon geoJsonPolygon = GeoJsonPolygon.of(polygonLineString).withInnerRing(innerRingLineString);

				Map<String, Object> map = geoJsonToMapConverter.convert(geoJsonPolygon);
				String json = Document.from(map).toJson();

				assertEquals(expected, json, false);
			}

			@Test // DATAES-930
			@DisplayName("should be converted from a Map")
			void shouldBeConvertedFromAMap() {

				String json = "{\n" + //
						"  \"type\": \"polygon\",\n" + //
						"  \"coordinates\": [\n" + //
						"    [[12, 34.0], [56.0, 78], [90, 12.0], [12, 34.0]],\n" + //
						"    [[56.0, 78], [90, 12.0], [34.0, 56], [56.0, 78]]\n" + //
						"  ]\n" + //
						"}\n"; //
				Document document = Document.parse(json);
				GeoJsonLineString polygonLineString = GeoJsonLineString.of(new Point(12, 34), new Point(56, 78),
						new Point(90, 12), new Point(12, 34));
				GeoJsonLineString innerRingLineString = GeoJsonLineString.of(new Point(56, 78), new Point(90, 12),
						new Point(34, 56), new Point(56, 78));
				GeoJsonPolygon expected = GeoJsonPolygon.of(polygonLineString).withInnerRing(innerRingLineString);

				GeoJson<? extends Iterable<?>> geoJsonLineString = mapToGeoJsonConverter.convert(document);

				assertThat(geoJsonLineString).isInstanceOf(GeoJsonPolygon.class).isEqualTo(expected);
			}
		}

		@Nested
		@DisplayName("GeoJsonMultiPolygon")
		class GeoJsonMultiPolygonUnitTests {

			@Test // DATAES-390
			@DisplayName("should be converted to a Map")
			void shouldBeConvertedToAMap() throws JSONException {
				String expected = "{\n" + //
						"  \"type\": \"MultiPolygon\",\n" + //
						"  \"coordinates\": [\n" + //
						"    [[[12.0, 34.0], [56.0, 78.0], [90.0, 12.0], [12.0, 34.0]]],\n" + //
						"    [[[56.0, 78.0], [90.0, 12.0], [34.0, 56.0], [56.0, 78.0]]]\n" + //
						"  ]\n" + //
						"}\n"; //

				GeoJsonLineString polygonLineString1 = GeoJsonLineString.of(new Point(12, 34), new Point(56, 78),
						new Point(90, 12), new Point(12, 34));
				GeoJsonLineString polygonLineString2 = GeoJsonLineString.of(new Point(56, 78), new Point(90, 12),
						new Point(34, 56), new Point(56, 78));
				GeoJsonMultiPolygon geoJsonMultiPolygon = GeoJsonMultiPolygon
						.of(Arrays.asList(GeoJsonPolygon.of(polygonLineString1), GeoJsonPolygon.of(polygonLineString2)));

				Map<String, Object> map = geoJsonToMapConverter.convert(geoJsonMultiPolygon);
				String json = Document.from(map).toJson();

				assertEquals(expected, json, false);
			}

			@Test // DATAES-930
			@DisplayName("should be converted from a Map")
			void shouldBeConvertedFromAMap() {

				String json = "{\n" + //
						"  \"type\": \"multipolygon\",\n" + //
						"  \"coordinates\": [\n" + //
						"    [[[12, 34.0], [56.0, 78], [90, 12.0], [12, 34.0]]],\n" + //
						"    [[[56, 78.0], [90, 12.0], [34.0, 56], [56, 78.0]]]\n" + //
						"  ]\n" + //
						"}\n"; //
				Document document = Document.parse(json);
				GeoJsonLineString polygonLineString1 = GeoJsonLineString.of(new Point(12, 34), new Point(56, 78),
						new Point(90, 12), new Point(12, 34));
				GeoJsonLineString polygonLineString2 = GeoJsonLineString.of(new Point(56, 78), new Point(90, 12),
						new Point(34, 56), new Point(56, 78));
				GeoJsonMultiPolygon expected = GeoJsonMultiPolygon
						.of(Arrays.asList(GeoJsonPolygon.of(polygonLineString1), GeoJsonPolygon.of(polygonLineString2)));

				GeoJson<? extends Iterable<?>> geoJsonMultiPolygon = mapToGeoJsonConverter.convert(document);

				assertThat(geoJsonMultiPolygon).isInstanceOf(GeoJsonMultiPolygon.class).isEqualTo(expected);
			}
		}

		@Nested
		@DisplayName("GeoJsonGeometryCollection")
		class GeoJsonGeometryCollectionUnitTests {

			@Test // DATAES-930
			@DisplayName("should be converted to a Map")
			void shouldBeConvertedToAMap() throws JSONException {

				String expected = "{\n" + //
						"  \"type\": \"GeometryCollection\",\n" + //
						"  \"geometries\": [\n" + //
						"    {\n" + //
						"      \"type\": \"Point\",\n" + //
						"      \"coordinates\": [12.0, 34.0]\n" + //
						"    },\n" + //
						"    {\n" + //
						"      \"type\": \"Polygon\",\n" + //
						"      \"coordinates\": [\n" + //
						"        [[12.0, 34.0], [56.0, 78.0], [90.0, 12.0], [12.0, 34.0]]\n" + //
						"      ]\n" + //
						"    }\n" + //
						"  ]\n" + //
						"}\n"; //
				GeoJsonPoint geoJsonPoint = GeoJsonPoint.of(12, 34);
				GeoJsonPolygon geoJsonPolygon = GeoJsonPolygon
						.of(GeoJsonLineString.of(new Point(12, 34), new Point(56, 78), new Point(90, 12), new Point(12, 34)));

				GeoJsonGeometryCollection geoJsonGeometryCollection = GeoJsonGeometryCollection
						.of(Arrays.asList(geoJsonPoint, geoJsonPolygon));

				Map<String, Object> map = geoJsonToMapConverter.convert(geoJsonGeometryCollection);
				String json = Document.from(map).toJson();

				assertEquals(expected, json, false);
			}

			@Test // DATAES-930
			@DisplayName("should be converted from a Map")
			void shouldBeConvertedFromAMap() {

				String json = "{\n" + //
						"  \"type\": \"geometrycollection\",\n" + //
						"  \"geometries\": [\n" + //
						"    {\n" + //
						"      \"type\": \"point\",\n" + //
						"      \"coordinates\": [12.0, 34.0]\n" + //
						"    },\n" + //
						"    {\n" + //
						"      \"type\": \"polygon\",\n" + //
						"      \"coordinates\": [\n" + //
						"        [[12.0, 34.0], [56.0, 78.0], [90.0, 12.0], [12.0, 34.0]]\n" + //
						"      ]\n" + //
						"    }\n" + //
						"  ]\n" + //
						"}\n"; //
				Document document = Document.parse(json);
				GeoJsonPoint geoJsonPoint = GeoJsonPoint.of(12, 34);
				GeoJsonPolygon geoJsonPolygon = GeoJsonPolygon
						.of(GeoJsonLineString.of(new Point(12, 34), new Point(56, 78), new Point(90, 12), new Point(12, 34)));

				GeoJsonGeometryCollection expected = GeoJsonGeometryCollection.of(Arrays.asList(geoJsonPoint, geoJsonPolygon));

				GeoJson<? extends Iterable<?>> geoJsonGeometryCollection = mapToGeoJsonConverter.convert(document);

				assertThat(geoJsonGeometryCollection).isInstanceOf(GeoJsonGeometryCollection.class).isEqualTo(expected);

			}
		}
	}
}
