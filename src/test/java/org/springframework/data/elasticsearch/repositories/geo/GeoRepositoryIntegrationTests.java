/*
 * Copyright 2016-2024 the original author or authors.
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
package org.springframework.data.elasticsearch.repositories.geo;

import static org.assertj.core.api.Assertions.*;

import java.util.Locale;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.GeoPointField;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.data.geo.Box;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Point;
import org.springframework.data.geo.Polygon;
import org.springframework.lang.Nullable;

/**
 * @author Mark Paluch
 * @author Christoph Strobl
 * @author Peter-Josef Meisch
 */
@SpringIntegrationTest
public abstract class GeoRepositoryIntegrationTests {

	@Autowired ElasticsearchOperations operations;
	private IndexOperations indexOperations;
	@Autowired IndexNameProvider indexNameProvider;

	@Autowired SpringDataGeoRepository repository;

	@BeforeEach
	public void init() {
		indexNameProvider.increment();
		operations.indexOps(GeoEntity.class).createWithMapping();
	}

	@Test
	@Order(Integer.MAX_VALUE)
	public void cleanup() {
		operations.indexOps(IndexCoordinates.of(indexNameProvider.getPrefix() + "*")).delete();
	}

	@Test
	public void shouldSaveAndLoadGeoPoints() {

		// given
		Point point = new Point(15, 25);
		GeoEntity entity = new GeoEntity();
		entity.setPointA(point);
		entity.setPointB(new GeoPoint(point.getX(), point.getY()));
		entity.setPointC(toGeoString(point));
		entity.setPointD(toGeoArray(point));

		// when
		GeoEntity saved = repository.save(entity);
		Optional<GeoEntity> result = repository.findById(entity.getId());

		// then
		assertThat(result).isPresent();
		result.ifPresent(geoEntity -> {

			assertThat(saved.getPointA().getX()).isEqualTo(geoEntity.getPointA().getX());
			assertThat(saved.getPointA().getY()).isEqualTo(geoEntity.getPointA().getY());
			assertThat(saved.getPointB().getLat()).isEqualTo(geoEntity.getPointB().getLat());
			assertThat(saved.getPointB().getLon()).isEqualTo(geoEntity.getPointB().getLon());
			assertThat(saved.getPointC()).isEqualTo(geoEntity.getPointC());
			assertThat(saved.getPointD()).isEqualTo(geoEntity.getPointD());
		});
	}

	private String toGeoString(Point point) {
		return String.format(Locale.ENGLISH, "%.1f,%.1f", point.getX(), point.getY());
	}

	private double[] toGeoArray(Point point) {
		return new double[] { point.getX(), point.getY() };
	}

	@Document(indexName = "#{@indexNameProvider.indexName()}")
	static class GeoEntity {
		@Nullable
		@Id private String id;
		// geo shape - Spring Data
		@Nullable private Box box;
		@Nullable private Circle circle;
		@Nullable private Polygon polygon;
		// geo point - Custom implementation + Spring Data
		@Nullable
		@GeoPointField private Point pointA;
		@Nullable private GeoPoint pointB;
		@Nullable
		@GeoPointField private String pointC;
		@Nullable
		@GeoPointField private double[] pointD;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public Box getBox() {
			return box;
		}

		public void setBox(@Nullable Box box) {
			this.box = box;
		}

		@Nullable
		public Circle getCircle() {
			return circle;
		}

		public void setCircle(@Nullable Circle circle) {
			this.circle = circle;
		}

		@Nullable
		public Polygon getPolygon() {
			return polygon;
		}

		public void setPolygon(@Nullable Polygon polygon) {
			this.polygon = polygon;
		}

		@Nullable
		public Point getPointA() {
			return pointA;
		}

		public void setPointA(@Nullable Point pointA) {
			this.pointA = pointA;
		}

		@Nullable
		public GeoPoint getPointB() {
			return pointB;
		}

		public void setPointB(@Nullable GeoPoint pointB) {
			this.pointB = pointB;
		}

		@Nullable
		public String getPointC() {
			return pointC;
		}

		public void setPointC(@Nullable String pointC) {
			this.pointC = pointC;
		}

		@Nullable
		public double[] getPointD() {
			return pointD;
		}

		public void setPointD(@Nullable double[] pointD) {
			this.pointD = pointD;
		}
	}

	/**
	 * Created by akonczak on 22/11/2015.
	 */
	interface SpringDataGeoRepository extends ElasticsearchRepository<GeoEntity, String> {}
}
