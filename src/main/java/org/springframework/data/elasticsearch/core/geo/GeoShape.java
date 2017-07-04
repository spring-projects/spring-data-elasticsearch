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
import java.util.Objects;

import org.springframework.data.geo.Point;

/**
 * geo-location used for #{@link org.springframework.data.elasticsearch.core.query.Criteria}.
 *
 * @author Lukas Vorisek
 */
public abstract class GeoShape<T> {

	public enum GeoShapeType {
		POLYGON("Polygon"), LINESTRING("LineString"), POINT("Point"), MULTIPOINT("MultiPoint"),
		MULTIPOLYGON("MultiPolygon"), MULTILINESTRING("MultiLineString"), GEOMETRY_COLLECTION("GeometryCollection");

		private final String stringValue;

		GeoShapeType(String stringValue) {
			this.stringValue = stringValue;
		}
		public String getValue() {
			return stringValue;
		}
	    public static GeoShapeType getEnum(String value) {
	        for(GeoShapeType v : values()) {
	            if(v.getValue().equalsIgnoreCase(value)) return v;
	        }
	        throw new IllegalArgumentException();
	    }
	    @Override
	    public String toString() {
	        return this.getValue();
	    }
	}


	protected GeoShapeType type;


	protected GeoShape(GeoShapeType type) {
		this.type = type;
	}


	public GeoShapeType getType() {
		return type;
	}

	abstract public T getCoordinates();

	public static class Coordinate {
		private double x;
		private double y;
		public Coordinate(double x, double y) {
			this.x = x;
			this.y = y;
		}

		public Coordinate(Point p) {
			this.x = p.getX();
			this.y = p.getY();
		}

		public double[] getCoordinate() {
			return new double [] {x, y};
		}

		public double getX() {
			return x;
		}

		public double getY() {
			return y;
		}

		@Override
		public String toString() {
			return String.format("[%f, %f]", this.x, this.y);
		}

		@Override
		public boolean equals(Object obj) {
			if(this == obj) {
				return true;
			}
			if(!(obj instanceof Coordinate)) {
				return false;
			}
			return hashCode() == obj.hashCode();
		}

		@Override
		public int hashCode() {
			int hash = Objects.hash(x, y);
			return hash;
		}
	}
}
