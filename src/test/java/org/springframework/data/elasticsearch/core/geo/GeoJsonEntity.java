/*
 * Copyright 2020 the original author or authors.
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

import lombok.Builder;
import lombok.Data;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.geo.Point;

/**
 * this class contains each GeoJson type as explicit type and as GeoJson interface. Used by several test classes
 */
@Data
@Builder
@Document(indexName = "geojson-index")
public class GeoJsonEntity {
	@Id private String id;
	GeoJsonPoint point1;
	GeoJson<? extends Iterable<?>> point2;
	GeoJsonMultiPoint multiPoint1;
	GeoJson<Iterable<Point>> multiPoint2;
	GeoJsonLineString lineString1;
	GeoJson<Iterable<Point>> lineString2;
	GeoJsonMultiLineString multiLineString1;
	GeoJson<Iterable<GeoJsonLineString>> multiLineString2;
	GeoJsonPolygon polygon1;
	GeoJson<Iterable<GeoJsonLineString>> polygon2;
	GeoJsonMultiPolygon multiPolygon1;
	GeoJson<Iterable<GeoJsonPolygon>> multiPolygon2;
	GeoJsonGeometryCollection geometryCollection1;
	GeoJson<Iterable<GeoJson<?>>> geometryCollection2;
}
