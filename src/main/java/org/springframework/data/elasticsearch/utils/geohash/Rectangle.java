/*
 * Copyright 2022-2024 the original author or authors.
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
package org.springframework.data.elasticsearch.utils.geohash;

/**
 * Code copied from Elasticsearch 7.10, Apache License V2
 * https://github.com/elastic/elasticsearch/blob/7.10/libs/geo/src/main/java/org/elasticsearch/geometry/Rectangle.java
 * <br/>
 * <br/>
 * Represents a lat/lon rectangle in decimal degrees and optional altitude in meters.
 */
public class Rectangle implements Geometry {
	public static final Rectangle EMPTY = new Rectangle();
	/**
	 * minimum latitude value (in degrees)
	 */
	private final double minY;
	/**
	 * minimum longitude value (in degrees)
	 */
	private final double minX;
	/**
	 * maximum altitude value (in meters)
	 */
	private final double minZ;
	/**
	 * maximum latitude value (in degrees)
	 */
	private final double maxY;
	/**
	 * minimum longitude value (in degrees)
	 */
	private final double maxX;
	/**
	 * minimum altitude value (in meters)
	 */
	private final double maxZ;

	private final boolean empty;

	private Rectangle() {
		minY = 0;
		minX = 0;
		maxY = 0;
		maxX = 0;
		minZ = Double.NaN;
		maxZ = Double.NaN;
		empty = true;
	}

	/**
	 * Constructs a bounding box by first validating the provided latitude and longitude coordinates
	 */
	public Rectangle(double minX, double maxX, double maxY, double minY) {
		this(minX, maxX, maxY, minY, Double.NaN, Double.NaN);
	}

	/**
	 * Constructs a bounding box by first validating the provided latitude and longitude coordinates
	 */
	public Rectangle(double minX, double maxX, double maxY, double minY, double minZ, double maxZ) {
		this.minX = minX;
		this.maxX = maxX;
		this.minY = minY;
		this.maxY = maxY;
		this.minZ = minZ;
		this.maxZ = maxZ;
		empty = false;
		if (maxY < minY) {
			throw new IllegalArgumentException("max y cannot be less than min x");
		}
		if (Double.isNaN(minZ) != Double.isNaN(maxZ)) {
			throw new IllegalArgumentException("only one z value is specified");
		}
	}

	public double getMinY() {
		return minY;
	}

	public double getMinX() {
		return minX;
	}

	public double getMinZ() {
		return minZ;
	}

	public double getMaxY() {
		return maxY;
	}

	public double getMaxX() {
		return maxX;
	}

	public double getMaxZ() {
		return maxZ;
	}

	public double getMinLat() {
		return minY;
	}

	public double getMinLon() {
		return minX;
	}

	public double getMinAlt() {
		return minZ;
	}

	public double getMaxLat() {
		return maxY;
	}

	public double getMaxLon() {
		return maxX;
	}

	public double getMaxAlt() {
		return maxZ;
	}

	@Override
	public ShapeType type() {
		return ShapeType.ENVELOPE;
	}

	@Override
	public String toString() {
		return WellKnownText.INSTANCE.toWKT(this);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		Rectangle rectangle = (Rectangle) o;

		if (Double.compare(rectangle.minY, minY) != 0)
			return false;
		if (Double.compare(rectangle.minX, minX) != 0)
			return false;
		if (Double.compare(rectangle.maxY, maxY) != 0)
			return false;
		if (Double.compare(rectangle.maxX, maxX) != 0)
			return false;
		if (Double.compare(rectangle.minZ, minZ) != 0)
			return false;
		return Double.compare(rectangle.maxZ, maxZ) == 0;

	}

	@Override
	public int hashCode() {
		int result;
		long temp;
		temp = Double.doubleToLongBits(minY);
		result = (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(minX);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(maxY);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(maxX);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(minZ);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(maxZ);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public <T, E extends Exception> T visit(GeometryVisitor<T, E> visitor) throws E {
		return visitor.visit(this);
	}

	@Override
	public boolean isEmpty() {
		return empty;
	}

	@Override
	public boolean hasZ() {
		return Double.isNaN(maxZ) == false;
	}
}
