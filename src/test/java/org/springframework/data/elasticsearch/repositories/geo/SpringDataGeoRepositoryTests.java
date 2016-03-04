/*
 * Copyright 2016 the original author or authors.
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

import java.util.Locale;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:/repository-spring-data-geo-support.xml")
public class SpringDataGeoRepositoryTests {

	@Autowired
	private ElasticsearchTemplate template;

	@Autowired
	private SpringDataGeoRepository repository;

	@Before
	public void init() {
		template.deleteIndex(GeoEntity.class);
		template.createIndex(GeoEntity.class);
		template.putMapping(GeoEntity.class);
		template.refresh(GeoEntity.class);
	}

	@Test
	public void shouldSaveAndLoadGeoPoints() {
		//given
		final Point point = new Point(15, 25);
		GeoEntity entity = GeoEntity.builder()
				.pointA(point)
				.pointB(new GeoPoint(point.getX(), point.getY()))
				.pointC(toGeoString(point))
				.pointD(toGeoArray(point))
				.build();
		//when
		entity = repository.save(entity);
		GeoEntity result = repository.findOne(entity.getId());
		//then

		assertThat(entity.getPointA().getX(), is(result.getPointA().getX()));
		assertThat(entity.getPointA().getY(), is(result.getPointA().getY()));
		assertThat(entity.getPointB().getLat(), is(result.getPointB().getLat()));
		assertThat(entity.getPointB().getLon(), is(result.getPointB().getLon()));
		assertThat(entity.getPointC(), is(result.getPointC()));
		assertThat(entity.getPointD(), is(result.getPointD()));
	}

	private String toGeoString(Point point) {
		return String.format(Locale.ENGLISH, "%.1f,%.1f", point.getX(), point.getY());
	}

	private double[] toGeoArray(Point point) {
		return new double[]{
				point.getX(), point.getY()
		};
	}
}