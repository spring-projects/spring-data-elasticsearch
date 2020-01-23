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
package org.springframework.data.elasticsearch.core.query;

import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;

/**
 * {@link org.springframework.data.domain.Sort.Order} derived class to be able to define a _geo_distance order for a
 * search.
 * 
 * @author Peter-Josef Meisch
 * @since 4.0
 */
public class GeoDistanceOrder extends Sort.Order {

	private static final DistanceType DEFAULT_DISTANCE_TYPE = DistanceType.arc;
	private static final Mode DEFAULT_MODE = Mode.min;
	private static final String DEFAULT_UNIT = "m";
	private static final Boolean DEFAULT_IGNORE_UNMAPPED = false;

	private final GeoPoint geoPoint;
	private final DistanceType distanceType;
	private final Mode mode;
	private final String unit;
	private final Boolean ignoreUnmapped;

	public GeoDistanceOrder(String property, GeoPoint geoPoint) {
		this(property, geoPoint, Sort.Direction.ASC, DEFAULT_DISTANCE_TYPE, DEFAULT_MODE, DEFAULT_UNIT,
				DEFAULT_IGNORE_UNMAPPED);
	}

	private GeoDistanceOrder(String property, GeoPoint geoPoint, Sort.Direction direction, DistanceType distanceType,
			Mode mode, String unit, Boolean ignoreUnmapped) {
		super(direction, property);
		this.geoPoint = geoPoint;
		this.distanceType = distanceType;
		this.mode = mode;
		this.unit = unit;
		this.ignoreUnmapped = ignoreUnmapped;
	}

	public GeoPoint getGeoPoint() {
		return geoPoint;
	}

	public DistanceType getDistanceType() {
		return distanceType;
	}

	public Mode getMode() {
		return mode;
	}

	public String getUnit() {
		return unit;
	}

	public Boolean getIgnoreUnmapped() {
		return ignoreUnmapped;
	}

	@Override
	public GeoDistanceOrder withProperty(String property) {
		return new GeoDistanceOrder(property, getGeoPoint(), getDirection(), getDistanceType(), getMode(), getUnit(),
				getIgnoreUnmapped());
	}

	@Override
	public GeoDistanceOrder with(Sort.Direction direction) {
		return new GeoDistanceOrder(getProperty(), getGeoPoint(), direction, getDistanceType(), getMode(), getUnit(),
				getIgnoreUnmapped());
	}

	@Override
	public GeoDistanceOrder with(Sort.NullHandling nullHandling) {
		throw new UnsupportedOperationException("null handling is not supported for _geo_distance sorts");
	}

	public GeoDistanceOrder with(DistanceType distanceType) {
		return new GeoDistanceOrder(getProperty(), getGeoPoint(), getDirection(), distanceType, getMode(), getUnit(),
				getIgnoreUnmapped());
	}

	public GeoDistanceOrder with(Mode mode) {
		return new GeoDistanceOrder(getProperty(), getGeoPoint(), getDirection(), getDistanceType(), mode, getUnit(),
				getIgnoreUnmapped());
	}

	public GeoDistanceOrder withUnit(String unit) {
		return new GeoDistanceOrder(getProperty(), getGeoPoint(), getDirection(), getDistanceType(), getMode(), unit,
				getIgnoreUnmapped());
	}

	public GeoDistanceOrder withIgnoreUnmapped(Boolean ignoreUnmapped) {
		return new GeoDistanceOrder(getProperty(), getGeoPoint(), getDirection(), getDistanceType(), getMode(), getUnit(),
				ignoreUnmapped);
	}

	@Override
	public String toString() {
		return "GeoDistanceOrder{" + "geoPoint=" + geoPoint + ", distanceType=" + distanceType + ", mode=" + mode
				+ ", unit='" + unit + '\'' + ", ignoreUnmapped=" + ignoreUnmapped + "} " + super.toString();
	}

	public enum DistanceType {
		arc, plane
	}

	public enum Mode {
		min, max, median, avg
	}
}
