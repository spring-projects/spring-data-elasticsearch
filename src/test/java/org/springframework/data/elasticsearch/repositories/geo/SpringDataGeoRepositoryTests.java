/*
 * Copyright 2016-2019 the original author or authors.
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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.Locale;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.data.elasticsearch.entities.GeoEntity;
import org.springframework.data.geo.Point;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Paluch
 * @author Christoph Strobl
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

	private String toGeoString(Point point) {
		return String.format(Locale.ENGLISH, "%.1f,%.1f", point.getX(), point.getY());
	}

	private double[] toGeoArray(Point point) {
		return new double[] { point.getX(), point.getY() };
	}
}
