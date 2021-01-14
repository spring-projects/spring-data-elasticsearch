package org.springframework.data.elasticsearch.core.geo;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.geo.Point;

/**
 * @author Peter-Josef Meisch
 */
class GeoPointUnitTests {

	@Test
	@DisplayName("should create From a Point")
	void shouldCreateFromAPoint() {
		Point point = new Point(8, 48);

		GeoPoint geoPoint = GeoPoint.fromPoint(point);

		assertThat(geoPoint).isEqualTo(new GeoPoint(48, 8));
	}

	@Test
	@DisplayName("should convert to a Point")
	void shouldConvertToAPoint() {

		GeoPoint geoPoint = new GeoPoint(48, 8);

		Point point = GeoPoint.toPoint(geoPoint);

		assertThat(point).isEqualTo(new Point(8, 48));
	}

	@Test
	@DisplayName("should not be equal to a Point")
	void shouldNotBeEqualToAPoint() {

		// noinspection AssertBetweenInconvertibleTypes
		assertThat(new GeoPoint(48, 8)).isNotEqualTo(new Point(8, 48));
	}

	@Test
	@DisplayName("should compare lat and lon values")
	void shouldCompareLatAndLonValues() {

		GeoPoint geoPoint = new GeoPoint(48, 8);

		assertThat(geoPoint).isEqualTo(new GeoPoint(48, 8));
	}
}
