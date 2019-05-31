/*
 * Copyright 2016-2019 the original author or authors.
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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Locale;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.GeoPointField;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.elasticsearch.utils.IndexInitializer;
import org.springframework.data.geo.Box;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Point;
import org.springframework.data.geo.Polygon;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Paluch
 * @author Christoph Strobl
 * @author Peter-Josef Meisch
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:/repository-spring-data-geo-support.xml")
public class SpringDataGeoRepositoryTests {

	@Autowired ElasticsearchTemplate template;

	@Autowired SpringDataGeoRepository repository;

	@Before
	public void init() {
		IndexInitializer.init(template, GeoEntity.class);
	}

	@Test
	public void shouldSaveAndLoadGeoPoints() {

		// given
		Point point = new Point(15, 25);
		GeoEntity entity = GeoEntity.builder().pointA(point).pointB(new GeoPoint(point.getX(), point.getY()))
				.pointC(toGeoString(point)).pointD(toGeoArray(point)).build();

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

	/**
	 * @author Artur Konczak
	 */
	@Setter
	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	@Document(indexName = "test-index-geo-repository", type = "geo-test-index", shards = 1, replicas = 0,
			refreshInterval = "-1")
	static class GeoEntity {

		@Id private String id;

		// geo shape - Spring Data
		private Box box;
		private Circle circle;
		private Polygon polygon;

		// geo point - Custom implementation + Spring Data
		@GeoPointField private Point pointA;

		private GeoPoint pointB;

		@GeoPointField private String pointC;

		@GeoPointField private double[] pointD;
	}

	/**
	 * Created by akonczak on 22/11/2015.
	 */
	interface SpringDataGeoRepository extends ElasticsearchRepository<GeoEntity, String> {}
}
