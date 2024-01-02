/*
 * Copyright 2020-2024 the original author or authors.
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

import java.util.Objects;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.geo.Point;
import org.springframework.lang.Nullable;

/**
 * this class contains each GeoJson type as explicit type and as GeoJson interface. Used by several test classes
 */
@Document(indexName = "#{@indexNameProvider.indexName()}-geojson")
public class GeoJsonEntity {
	@Nullable
	@Id private String id;
	@Nullable private GeoJsonPoint point1;
	@Nullable private GeoJson<? extends Iterable<?>> point2;
	@Nullable private GeoJsonMultiPoint multiPoint1;
	@Nullable private GeoJson<Iterable<Point>> multiPoint2;
	@Nullable private GeoJsonLineString lineString1;
	@Nullable private GeoJson<Iterable<Point>> lineString2;
	@Nullable private GeoJsonMultiLineString multiLineString1;
	@Nullable private GeoJson<Iterable<GeoJsonLineString>> multiLineString2;
	@Nullable private GeoJsonPolygon polygon1;
	@Nullable private GeoJson<Iterable<GeoJsonLineString>> polygon2;
	@Nullable private GeoJsonMultiPolygon multiPolygon1;
	@Nullable private GeoJson<Iterable<GeoJsonPolygon>> multiPolygon2;
	@Nullable private GeoJsonGeometryCollection geometryCollection1;
	@Nullable private GeoJson<Iterable<GeoJson<?>>> geometryCollection2;

	public GeoJsonEntity() {}

	public GeoJsonEntity(@Nullable String id, @Nullable GeoJsonPoint point1,
			@Nullable GeoJson<? extends Iterable<?>> point2, @Nullable GeoJsonMultiPoint multiPoint1,
			@Nullable GeoJson<Iterable<Point>> multiPoint2, @Nullable GeoJsonLineString lineString1,
			@Nullable GeoJson<Iterable<Point>> lineString2, @Nullable GeoJsonMultiLineString multiLineString1,
			@Nullable GeoJson<Iterable<GeoJsonLineString>> multiLineString2, @Nullable GeoJsonPolygon polygon1,
			@Nullable GeoJson<Iterable<GeoJsonLineString>> polygon2, @Nullable GeoJsonMultiPolygon multiPolygon1,
			@Nullable GeoJson<Iterable<GeoJsonPolygon>> multiPolygon2,
			@Nullable GeoJsonGeometryCollection geometryCollection1,
			@Nullable GeoJson<Iterable<GeoJson<?>>> geometryCollection2) {
		this.id = id;
		this.point1 = point1;
		this.point2 = point2;
		this.multiPoint1 = multiPoint1;
		this.multiPoint2 = multiPoint2;
		this.lineString1 = lineString1;
		this.lineString2 = lineString2;
		this.multiLineString1 = multiLineString1;
		this.multiLineString2 = multiLineString2;
		this.polygon1 = polygon1;
		this.polygon2 = polygon2;
		this.multiPolygon1 = multiPolygon1;
		this.multiPolygon2 = multiPolygon2;
		this.geometryCollection1 = geometryCollection1;
		this.geometryCollection2 = geometryCollection2;
	}

	@Nullable
	public String getId() {
		return id;
	}

	public void setId(@Nullable String id) {
		this.id = id;
	}

	@Nullable
	public GeoJsonPoint getPoint1() {
		return point1;
	}

	public void setPoint1(@Nullable GeoJsonPoint point1) {
		this.point1 = point1;
	}

	@Nullable
	public GeoJson<? extends Iterable<?>> getPoint2() {
		return point2;
	}

	public void setPoint2(@Nullable GeoJson<? extends Iterable<?>> point2) {
		this.point2 = point2;
	}

	@Nullable
	public GeoJsonMultiPoint getMultiPoint1() {
		return multiPoint1;
	}

	public void setMultiPoint1(@Nullable GeoJsonMultiPoint multiPoint1) {
		this.multiPoint1 = multiPoint1;
	}

	@Nullable
	public GeoJson<Iterable<Point>> getMultiPoint2() {
		return multiPoint2;
	}

	public void setMultiPoint2(@Nullable GeoJson<Iterable<Point>> multiPoint2) {
		this.multiPoint2 = multiPoint2;
	}

	@Nullable
	public GeoJsonLineString getLineString1() {
		return lineString1;
	}

	public void setLineString1(@Nullable GeoJsonLineString lineString1) {
		this.lineString1 = lineString1;
	}

	@Nullable
	public GeoJson<Iterable<Point>> getLineString2() {
		return lineString2;
	}

	public void setLineString2(@Nullable GeoJson<Iterable<Point>> lineString2) {
		this.lineString2 = lineString2;
	}

	@Nullable
	public GeoJsonMultiLineString getMultiLineString1() {
		return multiLineString1;
	}

	public void setMultiLineString1(@Nullable GeoJsonMultiLineString multiLineString1) {
		this.multiLineString1 = multiLineString1;
	}

	@Nullable
	public GeoJson<Iterable<GeoJsonLineString>> getMultiLineString2() {
		return multiLineString2;
	}

	public void setMultiLineString2(@Nullable GeoJson<Iterable<GeoJsonLineString>> multiLineString2) {
		this.multiLineString2 = multiLineString2;
	}

	@Nullable
	public GeoJsonPolygon getPolygon1() {
		return polygon1;
	}

	public void setPolygon1(@Nullable GeoJsonPolygon polygon1) {
		this.polygon1 = polygon1;
	}

	@Nullable
	public GeoJson<Iterable<GeoJsonLineString>> getPolygon2() {
		return polygon2;
	}

	public void setPolygon2(@Nullable GeoJson<Iterable<GeoJsonLineString>> polygon2) {
		this.polygon2 = polygon2;
	}

	@Nullable
	public GeoJsonMultiPolygon getMultiPolygon1() {
		return multiPolygon1;
	}

	public void setMultiPolygon1(@Nullable GeoJsonMultiPolygon multiPolygon1) {
		this.multiPolygon1 = multiPolygon1;
	}

	@Nullable
	public GeoJson<Iterable<GeoJsonPolygon>> getMultiPolygon2() {
		return multiPolygon2;
	}

	public void setMultiPolygon2(@Nullable GeoJson<Iterable<GeoJsonPolygon>> multiPolygon2) {
		this.multiPolygon2 = multiPolygon2;
	}

	@Nullable
	public GeoJsonGeometryCollection getGeometryCollection1() {
		return geometryCollection1;
	}

	public void setGeometryCollection1(@Nullable GeoJsonGeometryCollection geometryCollection1) {
		this.geometryCollection1 = geometryCollection1;
	}

	@Nullable
	public GeoJson<Iterable<GeoJson<?>>> getGeometryCollection2() {
		return geometryCollection2;
	}

	public void setGeometryCollection2(@Nullable GeoJson<Iterable<GeoJson<?>>> geometryCollection2) {
		this.geometryCollection2 = geometryCollection2;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof GeoJsonEntity that))
			return false;

		if (!Objects.equals(id, that.id))
			return false;
		if (!Objects.equals(point1, that.point1))
			return false;
		if (!Objects.equals(point2, that.point2))
			return false;
		if (!Objects.equals(multiPoint1, that.multiPoint1))
			return false;
		if (!Objects.equals(multiPoint2, that.multiPoint2))
			return false;
		if (!Objects.equals(lineString1, that.lineString1))
			return false;
		if (!Objects.equals(lineString2, that.lineString2))
			return false;
		if (!Objects.equals(multiLineString1, that.multiLineString1))
			return false;
		if (!Objects.equals(multiLineString2, that.multiLineString2))
			return false;
		if (!Objects.equals(polygon1, that.polygon1))
			return false;
		if (!Objects.equals(polygon2, that.polygon2))
			return false;
		if (!Objects.equals(multiPolygon1, that.multiPolygon1))
			return false;
		if (!Objects.equals(multiPolygon2, that.multiPolygon2))
			return false;
		if (!Objects.equals(geometryCollection1, that.geometryCollection1))
			return false;
		return Objects.equals(geometryCollection2, that.geometryCollection2);
	}

	@Override
	public int hashCode() {
		int result = id != null ? id.hashCode() : 0;
		result = 31 * result + (point1 != null ? point1.hashCode() : 0);
		result = 31 * result + (point2 != null ? point2.hashCode() : 0);
		result = 31 * result + (multiPoint1 != null ? multiPoint1.hashCode() : 0);
		result = 31 * result + (multiPoint2 != null ? multiPoint2.hashCode() : 0);
		result = 31 * result + (lineString1 != null ? lineString1.hashCode() : 0);
		result = 31 * result + (lineString2 != null ? lineString2.hashCode() : 0);
		result = 31 * result + (multiLineString1 != null ? multiLineString1.hashCode() : 0);
		result = 31 * result + (multiLineString2 != null ? multiLineString2.hashCode() : 0);
		result = 31 * result + (polygon1 != null ? polygon1.hashCode() : 0);
		result = 31 * result + (polygon2 != null ? polygon2.hashCode() : 0);
		result = 31 * result + (multiPolygon1 != null ? multiPolygon1.hashCode() : 0);
		result = 31 * result + (multiPolygon2 != null ? multiPolygon2.hashCode() : 0);
		result = 31 * result + (geometryCollection1 != null ? geometryCollection1.hashCode() : 0);
		result = 31 * result + (geometryCollection2 != null ? geometryCollection2.hashCode() : 0);
		return result;
	}
}
