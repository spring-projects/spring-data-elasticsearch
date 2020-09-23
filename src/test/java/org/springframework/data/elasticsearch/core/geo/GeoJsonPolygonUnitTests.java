package org.springframework.data.elasticsearch.core.geo;

import static org.assertj.core.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.geo.Point;

/**
 * @author Peter-Josef Meisch
 */
class GeoJsonPolygonUnitTests {

	@Test // DATAES-930
	@DisplayName("should accept a GeoJsonLineString")
	void shouldAcceptAGeoJsonLineString() {

		GeoJsonLineString geoJsonLineString = GeoJsonLineString.of(new Point(12, 34), new Point(56, 78), new Point(90, 12),
				new Point(34, 56));
		GeoJsonLineString innerRing = GeoJsonLineString.of(new Point(56, 78), new Point(90, 12), new Point(34, 56),
				new Point(12, 34));

		GeoJsonPolygon geoJsonPolygon = GeoJsonPolygon.of(geoJsonLineString).withInnerRing(innerRing);

		assertThat(geoJsonPolygon.getCoordinates()).containsExactly(geoJsonLineString, innerRing);
	}

	@Test // DATES-930
	@DisplayName("should accept a list of Points")
	void shouldAcceptAListOfPoints() {

		List<Point> pointList = Arrays.asList(new Point(12, 34), new Point(56, 78), new Point(90, 12), new Point(34, 56));
		List<Point> innerRingPointList = Arrays.asList(new Point(56, 78), new Point(90, 12), new Point(34, 56),
				new Point(12, 34));

		GeoJsonPolygon geoJsonPolygon = GeoJsonPolygon.of(pointList).withInnerRing(innerRingPointList);

		assertThat(geoJsonPolygon.getCoordinates()).containsExactly(GeoJsonLineString.of(pointList),
				GeoJsonLineString.of(innerRingPointList));
	}

	@Test // DATES-930
	@DisplayName("should accept a list of GeoPoints")
	void shouldAcceptAListOfGeoPoints() {

		List<Point> pointList = Arrays.asList(new Point(12, 34), new Point(56, 78), new Point(90, 12), new Point(34, 56));
		List<Point> innerRingPointList = Arrays.asList(new Point(56, 78), new Point(90, 12), new Point(34, 56),
				new Point(12, 34));

		GeoJsonPolygon geoJsonPolygon = GeoJsonPolygon
				.ofGeoPoints(pointList.stream().map(GeoPoint::fromPoint).collect(Collectors.toList()))
				.withInnerRingOfGeoPoints(innerRingPointList.stream().map(GeoPoint::fromPoint).collect(Collectors.toList()));

		assertThat(geoJsonPolygon.getCoordinates()).containsExactly(GeoJsonLineString.of(pointList),
				GeoJsonLineString.of(innerRingPointList));
	}

	@Test // DATES-930
	@DisplayName("should accept an array of points")
	void shouldAcceptAnArrayOfPoints() {

		List<Point> pointList = Arrays.asList(new Point(12, 34), new Point(56, 78), new Point(90, 12), new Point(34, 56));
		List<Point> innerRingPointList = Arrays.asList(new Point(56, 78), new Point(90, 12), new Point(34, 56),
				new Point(12, 34));

		GeoJsonPolygon geoJsonPolygon = GeoJsonPolygon
				.of(pointList.get(0), pointList.get(1), pointList.get(2), pointList.get(3)).withInnerRing(
						innerRingPointList.get(0), innerRingPointList.get(1), innerRingPointList.get(2), innerRingPointList.get(3));

		assertThat(geoJsonPolygon.getCoordinates()).containsExactly(GeoJsonLineString.of(pointList),
				GeoJsonLineString.of(innerRingPointList));
	}

	@Test // DATES-930
	@DisplayName("should accept an array of GeoPoints")
	void shouldAcceptAnArrayOfGeoPoints() {

		List<Point> pointList = Arrays.asList(new Point(12, 34), new Point(56, 78), new Point(90, 12), new Point(34, 56));
		List<Point> innerRingPointList = Arrays.asList(new Point(56, 78), new Point(90, 12), new Point(34, 56),
				new Point(12, 34));

		GeoJsonPolygon geoJsonPolygon = GeoJsonPolygon
				.of(GeoPoint.fromPoint(pointList.get(0)), GeoPoint.fromPoint(pointList.get(1)),
						GeoPoint.fromPoint(pointList.get(2)), GeoPoint.fromPoint(pointList.get(3)))
				.withInnerRing(GeoPoint.fromPoint(innerRingPointList.get(0)), GeoPoint.fromPoint(innerRingPointList.get(1)),
						GeoPoint.fromPoint(innerRingPointList.get(2)), GeoPoint.fromPoint(innerRingPointList.get(3)));

		assertThat(geoJsonPolygon.getCoordinates()).containsExactly(GeoJsonLineString.of(pointList),
				GeoJsonLineString.of(innerRingPointList));
	}
}
