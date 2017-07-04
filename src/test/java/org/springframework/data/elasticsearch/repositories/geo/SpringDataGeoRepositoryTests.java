/*
 * Copyright 2016-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.elasticsearch.repositories.geo;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.Locale;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.data.elasticsearch.core.geo.GeoShapeGeometryCollection;
import org.springframework.data.elasticsearch.core.geo.GeoShapeLinestring;
import org.springframework.data.elasticsearch.core.geo.GeoShapeMultiLinestring;
import org.springframework.data.elasticsearch.core.geo.GeoShapeMultiPoint;
import org.springframework.data.elasticsearch.core.geo.GeoShapeMultiPolygon;
import org.springframework.data.elasticsearch.core.geo.GeoShapePoint;
import org.springframework.data.elasticsearch.core.geo.GeoShapePolygon;
import org.springframework.data.elasticsearch.entities.GeoEntity;
import org.springframework.data.geo.Point;
import org.springframework.data.geo.Polygon;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Paluch
 * @author Christoph Strobl
 * @author Lukas Vorisek
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:/repository-spring-data-geo-support.xml")
public class SpringDataGeoRepositoryTests {

	@Autowired ElasticsearchTemplate template;

	@Autowired SpringDataGeoRepository repository;

	@Before
	public void init() {
		template.deleteIndex(GeoEntity.class);
		template.createIndex(GeoEntity.class);
		template.putMapping(GeoEntity.class);
		template.refresh(GeoEntity.class);
	}

	@Test
	public void shouldSaveAndLoadGeoPoints() {
		// given
		final Point point = new Point(15, 25);
		final Polygon polygon = new Polygon(new Point(0,0), new Point(0,5), new Point(5,5), new Point(5,0), new Point(0,0));
		GeoEntity entity = GeoEntity.builder().pointA(point).pointB(new GeoPoint(point.getX(), point.getY()))
				.pointC(toGeoString(point)).pointD(toGeoArray(point)).build();
		// when
		GeoEntity saved = repository.save(entity);
		Optional<GeoEntity> result = repository.findById(entity.getId());
		// then

		assertThat(result.isPresent(), is(true));
		result.ifPresent(geoEntity -> {

			assertThat(saved.getPointA().getX(), is(geoEntity.getPointA().getX()));
			assertThat(saved.getPointA().getY(), is(geoEntity.getPointA().getY()));
			assertThat(saved.getPointB().getLat(), is(geoEntity.getPointB().getLat()));
			assertThat(saved.getPointB().getLon(), is(geoEntity.getPointB().getLon()));
			assertThat(saved.getPointC(), is(geoEntity.getPointC()));
			assertThat(saved.getPointD(), is(geoEntity.getPointD()));
		});
	}

	/**
	 * DATAES-169 - Geo Shape support
	 */
	@Test
	public void shouldSaveAndLoadPolygonGeoShapes() {
		// given
		final Polygon polygon = new Polygon(new Point(0,0), new Point(0,5), new Point(5,5), new Point(5,0), new Point(0,0));
		GeoEntity entity = GeoEntity.builder().polygonA(polygon).polygonB(GeoShapePolygon.fromShape(polygon))
				.polygonC(GeoShapePolygon.fromShape(polygon)).build();
		// when
		GeoEntity saved = repository.save(entity);
		Optional<GeoEntity> result = repository.findById(entity.getId());
		// then

		assertThat(result.isPresent(), is(true));
		result.ifPresent(geoEntity -> {

			assertThat(saved.getPolygonA(), is(geoEntity.getPolygonA()));
			assertThat(saved.getPolygonB(), is(geoEntity.getPolygonB()));
			assertThat(saved.getPolygonC(), is(geoEntity.getPolygonC()));
		});
	}

	/**
	 * DATAES-169 - Geo Shape support
	 */
	@Test
	public void shouldSaveAndLoadLinestringGeoShapes() {
		// given
		GeoShapeLinestring linestring = new GeoShapeLinestring(new Point(0, 0), new Point(5,5));
		GeoEntity entity = GeoEntity.builder().linestringA(linestring).linestringB(linestring).build();
		// when
		GeoEntity saved = repository.save(entity);
		Optional<GeoEntity> result = repository.findById(entity.getId());
		// then

		assertThat(result.isPresent(), is(true));
		result.ifPresent(geoEntity -> {

			assertThat(saved.getLinestringA(), is(geoEntity.getLinestringA()));
			assertThat(saved.getLinestringB(), is(geoEntity.getLinestringB()));
		});
	}

	/**
	 * DATAES-169 - Geo Shape support
	 */
	@Test
	public void shouldSaveAndLoadPointGeoShapes() {
		// given
		GeoShapePoint point = new GeoShapePoint(new Point(15, 32));
		GeoEntity entity = GeoEntity.builder().geoshapePointA(point).geoshapePointB(point).build();
		// when
		GeoEntity saved = repository.save(entity);
		Optional<GeoEntity> result = repository.findById(entity.getId());
		// then

		assertThat(result.isPresent(), is(true));
		result.ifPresent(geoEntity -> {

			assertThat(saved.getGeoshapePointA(), is(geoEntity.getGeoshapePointA()));
			assertThat(saved.getGeoshapePointB(), is(geoEntity.getGeoshapePointB()));
		});
	}

	/**
	 * DATAES-169 - Geo Shape support
	 */
	@Test
	public void shouldSaveAndLoadMultiPointGeoShapes() {
		// given
		GeoShapeMultiPoint multipoint = new GeoShapeMultiPoint(new Point(0, 0), new Point(1, 5), new Point(13, 27));
		GeoEntity entity = GeoEntity.builder().multipointA(multipoint).multipointB(multipoint).build();
		// when
		GeoEntity saved = repository.save(entity);
		Optional<GeoEntity> result = repository.findById(entity.getId());
		// then

		assertThat(result.isPresent(), is(true));
		result.ifPresent(geoEntity -> {

			assertThat(saved.getMultipointA(), is(geoEntity.getMultipointA()));
			assertThat(saved.getMultipointB(), is(geoEntity.getMultipointB()));
		});
	}

	/**
	 * DATAES-169 - Geo Shape support
	 */
	@Test
	public void shouldSaveAndLoadMultiPolygonGeoShapes() {
		// given
		GeoShapePolygon polygonA = new GeoShapePolygon(new Point(0, 0), new Point(5, 5), new Point(7, 3), new Point(0, 0));
		GeoShapePolygon polygonB = new GeoShapePolygon(new Point(0, 7), new Point(4, 7), new Point(2, 4), new Point(0, 7));
		GeoShapeMultiPolygon multipolygon = new GeoShapeMultiPolygon(polygonA, polygonB);
		GeoEntity entity = GeoEntity.builder().multipolygonA(multipolygon).multipolygonB(multipolygon).build();
		// when
		GeoEntity saved = repository.save(entity);
		Optional<GeoEntity> result = repository.findById(entity.getId());
		// then

		assertThat(result.isPresent(), is(true));
		result.ifPresent(geoEntity -> {

			assertThat(saved.getMultipolygonA(), is(geoEntity.getMultipolygonA()));
			assertThat(saved.getMultipolygonB(), is(geoEntity.getMultipolygonB()));
		});
	}

	/**
	 * DATAES-169 - Geo Shape support
	 */
	@Test
	public void shouldSaveAndLoadPolygonGeoShapesWithHole() {
		// given
		GeoShapePolygon polygon = new GeoShapePolygon(new Point(0, 0), new Point(1, 5), new Point(7, 3), new Point(7, 0), new Point(0, 0));
		polygon = polygon.addHole(new Point(1,1), new Point(2,1), new Point(2, 2), new Point(1, 2), new Point(1, 1));

		GeoEntity entity = GeoEntity.builder().polygonB(polygon).polygonC(polygon).build();
		// when
		GeoEntity saved = repository.save(entity);
		Optional<GeoEntity> result = repository.findById(entity.getId());
		// then

		assertThat(result.isPresent(), is(true));
		result.ifPresent(geoEntity -> {

			assertThat(saved.getPolygonA(), is(geoEntity.getPolygonA()));
			assertThat(saved.getPolygonB(), is(geoEntity.getPolygonB()));
		});
	}

	/**
	 * DATAES-169 - Geo Shape support
	 */
	@Test
	public void shouldSaveAndLoadMultiLinestringGeoShapes() {
		// given

		GeoShapeLinestring a = new GeoShapeLinestring(new Point(0, 0), new Point(1, 5));
		GeoShapeLinestring b = new GeoShapeLinestring(new Point(5, 5), new Point(10, 10), new Point(15, 0));
		GeoShapeMultiLinestring multilinestring = new GeoShapeMultiLinestring(a, b);
		GeoEntity entity = GeoEntity.builder().multilinestringA(multilinestring).multilinestringB(multilinestring).build();
		// when
		GeoEntity saved = repository.save(entity);
		Optional<GeoEntity> result = repository.findById(entity.getId());
		// then

		assertThat(result.isPresent(), is(true));
		result.ifPresent(geoEntity -> {

			assertThat(saved.getMultilinestringA(), is(geoEntity.getMultilinestringA()));
			assertThat(saved.getMultilinestringB(), is(geoEntity.getMultilinestringB()));
		});
	}

	/**
	 * DATAES-169 - Geo Shape support
	 */
	@Test
	public void shouldSaveAndLoadGeometryCollectionGeoShapes() {
		// given

		GeoShapeLinestring a = new GeoShapeLinestring(new Point(0, 0), new Point(1, 5));
		GeoShapeLinestring b = new GeoShapeLinestring(new Point(5, 5), new Point(10, 10), new Point(15, 0));
		GeoShapeMultiLinestring multilinestring = new GeoShapeMultiLinestring(a, b);

		GeoShapeLinestring linestring = new GeoShapeLinestring(new Point(0, 0), new Point(5,5));

		GeoShapeGeometryCollection collection = new GeoShapeGeometryCollection(multilinestring, linestring);
		GeoEntity entity = GeoEntity.builder().collectionA(collection).collectionB(collection).build();
		// when
		GeoEntity saved = repository.save(entity);
		Optional<GeoEntity> result = repository.findById(entity.getId());
		// then

		assertThat(result.isPresent(), is(true));
		result.ifPresent(geoEntity -> {

			assertThat(saved.getCollectionA(), is(geoEntity.getCollectionA()));
			assertThat(saved.getCollectionB(), is(geoEntity.getCollectionB()));
		});
	}

	private String toGeoString(Point point) {
		return String.format(Locale.ENGLISH, "%.1f,%.1f", point.getX(), point.getY());
	}

	private double[] toGeoArray(Point point) {
		return new double[] { point.getX(), point.getY() };
	}
}
