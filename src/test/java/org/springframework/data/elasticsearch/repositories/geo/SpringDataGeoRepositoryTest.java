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


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:/repository-spring-data-geo-support.xml")
public class SpringDataGeoRepositoryTest {

	@Autowired
	private ElasticsearchTemplate template;

	@Autowired
	private SpringDataGeoRepository repository;

	@Before
	public void init() {
		template.deleteIndex(GeoEntity.class);
		template.createIndex(GeoEntity.class);
		template.putMapping(GeoEntity.class);
		template.refresh(GeoEntity.class, true);
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

		assertThat(entity.getPointA(), is(result.getPointA()));
		assertThat(entity.getPointB(), is(result.getPointB()));
		assertThat(entity.getPointC(), is(result.getPointC()));
		assertThat(entity.getPointD(), is(result.getPointD()));
	}

	private String toGeoString(Point point) {
		return String.format("%.1f,%.1f", point.getX(), point.getY());
	}

	private double[] toGeoArray(Point point) {
		return new double[]{
				point.getX(), point.getY()
		};
	}
}