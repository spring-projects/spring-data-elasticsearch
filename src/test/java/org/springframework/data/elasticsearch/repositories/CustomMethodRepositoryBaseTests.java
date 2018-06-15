/*
 * Copyright 2013-2018 the original author or authors.
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
package org.springframework.data.elasticsearch.repositories;

import static org.apache.commons.lang.RandomStringUtils.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.data.elasticsearch.entities.SampleEntity;
import org.springframework.data.elasticsearch.repositories.custom.SampleCustomMethodRepository;
import org.springframework.data.geo.Box;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Franck Marchand
 * @author Kevin Leturc
 * @author Christoph Strobl
 * @author Don Wellington
 */

public abstract class CustomMethodRepositoryBaseTests {

	@Autowired private SampleCustomMethodRepository repository;

	@Test
	public void shouldExecuteCustomMethod() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setType("test");
		sampleEntity.setMessage("some message");
		repository.save(sampleEntity);
		// when
		Page<SampleEntity> page = repository.findByType("test", new PageRequest(0, 10));
		// then
		assertThat(page, is(notNullValue()));
		assertThat(page.getTotalElements(), is(greaterThanOrEqualTo(1L)));
	}

	@Test
	public void shouldExecuteCustomMethodForNot() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setType("some");
		sampleEntity.setMessage("some message");
		repository.save(sampleEntity);
		// when
		Page<SampleEntity> page = repository.findByTypeNot("test", new PageRequest(0, 10));
		// then
		assertThat(page, is(notNullValue()));
		assertThat(page.getTotalElements(), is(equalTo(1L)));
	}

	@Test
	public void shouldExecuteCustomMethodWithQuery() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setType("test");
		String searchTerm = "customQuery";
		sampleEntity.setMessage(searchTerm);
		repository.save(sampleEntity);
		// when
		Page<SampleEntity> page = repository.findByMessage(searchTerm.toLowerCase(), new PageRequest(0, 10));
		// then
		assertThat(page, is(notNullValue()));
		assertThat(page.getTotalElements(), is(greaterThanOrEqualTo(1L)));
	}

	@Test
	public void shouldExecuteCustomMethodWithLessThan() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setType("test");
		sampleEntity.setRate(9);
		sampleEntity.setMessage("some message");
		repository.save(sampleEntity);

		String documentId2 = randomNumeric(5);
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setType("test");
		sampleEntity2.setRate(20);
		sampleEntity2.setMessage("some message");
		repository.save(sampleEntity2);

		// when
		Page<SampleEntity> page = repository.findByRateLessThan(10, new PageRequest(0, 10));
		// then
		assertThat(page, is(notNullValue()));
		assertThat(page.getTotalElements(), is(equalTo(1L)));
	}

	@Test
	public void shouldExecuteCustomMethodWithBefore() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setType("test");
		sampleEntity.setRate(10);
		sampleEntity.setMessage("some message");
		repository.save(sampleEntity);

		// when
		Page<SampleEntity> page = repository.findByRateBefore(10, new PageRequest(0, 10));
		// then
		assertThat(page, is(notNullValue()));
		assertThat(page.getTotalElements(), is(equalTo(1L)));
	}

	@Test
	public void shouldExecuteCustomMethodWithAfter() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setType("test");
		sampleEntity.setRate(10);
		sampleEntity.setMessage("some message");
		repository.save(sampleEntity);

		// when
		Page<SampleEntity> page = repository.findByRateAfter(10, new PageRequest(0, 10));
		// then
		assertThat(page, is(notNullValue()));
		assertThat(page.getTotalElements(), is(equalTo(1L)));
	}

	@Test
	public void shouldExecuteCustomMethodWithLike() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setType("test");
		sampleEntity.setRate(10);
		sampleEntity.setMessage("foo");
		repository.save(sampleEntity);

		// when
		Page<SampleEntity> page = repository.findByMessageLike("fo", new PageRequest(0, 10));
		// then
		assertThat(page, is(notNullValue()));
		assertThat(page.getTotalElements(), is(equalTo(1L)));
	}

	@Test
	public void shouldExecuteCustomMethodForStartingWith() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setType("test");
		sampleEntity.setRate(10);
		sampleEntity.setMessage("foo");
		repository.save(sampleEntity);

		// when
		Page<SampleEntity> page = repository.findByMessageStartingWith("fo", new PageRequest(0, 10));
		// then
		assertThat(page, is(notNullValue()));
		assertThat(page.getTotalElements(), is(equalTo(1L)));
	}

	@Test
	public void shouldExecuteCustomMethodForEndingWith() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setType("test");
		sampleEntity.setRate(10);
		sampleEntity.setMessage("foo");
		repository.save(sampleEntity);

		// when
		Page<SampleEntity> page = repository.findByMessageEndingWith("o", new PageRequest(0, 10));
		// then
		assertThat(page, is(notNullValue()));
		assertThat(page.getTotalElements(), is(equalTo(1L)));
	}

	@Test
	public void shouldExecuteCustomMethodForContains() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setType("test");
		sampleEntity.setRate(10);
		sampleEntity.setMessage("foo");
		repository.save(sampleEntity);

		// when
		Page<SampleEntity> page = repository.findByMessageContaining("fo", new PageRequest(0, 10));
		// then
		assertThat(page, is(notNullValue()));
		assertThat(page.getTotalElements(), is(equalTo(1L)));
	}

	@Test
	public void shouldExecuteCustomMethodForIn() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setType("test");
		sampleEntity.setMessage("foo");
		repository.save(sampleEntity);

		// given
		String documentId2 = randomNumeric(5);
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setType("test");
		sampleEntity2.setMessage("bar");
		repository.save(sampleEntity2);

		List<String> ids = Arrays.asList(documentId, documentId2);

		// when
		Page<SampleEntity> page = repository.findByIdIn(ids, new PageRequest(0, 10));
		// then
		assertThat(page, is(notNullValue()));
		assertThat(page.getTotalElements(), is(equalTo(2L)));
	}

	@Test
	public void shouldExecuteCustomMethodForNotIn() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setType("test");
		sampleEntity.setMessage("foo");
		repository.save(sampleEntity);

		// given
		String documentId2 = randomNumeric(5);
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setType("test");
		sampleEntity2.setMessage("bar");
		repository.save(sampleEntity2);

		List<String> ids = Arrays.asList(documentId);

		// when
		Page<SampleEntity> page = repository.findByIdNotIn(ids, new PageRequest(0, 10));
		// then
		assertThat(page, is(notNullValue()));
		assertThat(page.getTotalElements(), is(equalTo(1L)));
		assertThat(page.getContent().get(0).getId(), is(documentId2));
	}

	@Test
	public void shouldExecuteCustomMethodForTrue() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setType("test");
		sampleEntity.setMessage("foo");
		sampleEntity.setAvailable(true);
		repository.save(sampleEntity);

		// given
		String documentId2 = randomNumeric(5);
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setType("test");
		sampleEntity2.setMessage("bar");
		sampleEntity2.setAvailable(false);
		repository.save(sampleEntity2);
		// when
		Page<SampleEntity> page = repository.findByAvailableTrue(new PageRequest(0, 10));
		// then
		assertThat(page, is(notNullValue()));
		assertThat(page.getTotalElements(), is(equalTo(1L)));
	}

	@Test
	public void shouldExecuteCustomMethodForFalse() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setType("test");
		sampleEntity.setMessage("foo");
		sampleEntity.setAvailable(true);
		repository.save(sampleEntity);

		// given
		String documentId2 = randomNumeric(5);
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setType("test");
		sampleEntity2.setMessage("bar");
		sampleEntity2.setAvailable(false);
		repository.save(sampleEntity2);
		// when
		Page<SampleEntity> page = repository.findByAvailableFalse(new PageRequest(0, 10));
		// then
		assertThat(page, is(notNullValue()));
		assertThat(page.getTotalElements(), is(equalTo(1L)));
	}

	@Test
	public void shouldExecuteCustomMethodForOrderBy() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setType("abc");
		sampleEntity.setMessage("test");
		sampleEntity.setAvailable(true);
		repository.save(sampleEntity);

		// document 2
		String documentId2 = randomNumeric(5);
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setType("xyz");
		sampleEntity2.setMessage("bar");
		sampleEntity2.setAvailable(false);
		repository.save(sampleEntity2);

		// document 3
		String documentId3 = randomNumeric(5);
		SampleEntity sampleEntity3 = new SampleEntity();
		sampleEntity3.setId(documentId3);
		sampleEntity3.setType("def");
		sampleEntity3.setMessage("foo");
		sampleEntity3.setAvailable(false);
		repository.save(sampleEntity3);

		// when
		Page<SampleEntity> page = repository.findByMessageOrderByTypeAsc("foo", new PageRequest(0, 10));
		// then
		assertThat(page, is(notNullValue()));
		assertThat(page.getTotalElements(), is(equalTo(1L)));
	}

	@Test
	public void shouldExecuteCustomMethodWithBooleanParameter() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setType("test");
		sampleEntity.setMessage("foo");
		sampleEntity.setAvailable(true);
		repository.save(sampleEntity);

		// given
		String documentId2 = randomNumeric(5);
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setType("test");
		sampleEntity2.setMessage("bar");
		sampleEntity2.setAvailable(false);
		repository.save(sampleEntity2);
		// when
		Page<SampleEntity> page = repository.findByAvailable(false, new PageRequest(0, 10));
		// then
		assertThat(page, is(notNullValue()));
		assertThat(page.getTotalElements(), is(equalTo(1L)));
	}

	@Test
	public void shouldReturnPageableResultsWithQueryAnnotationExpectedPageSize() {
		// given
		for (int i = 0; i < 30; i++) {
			String documentId = String.valueOf(i);
			SampleEntity sampleEntity = new SampleEntity();
			sampleEntity.setId(documentId);
			sampleEntity.setMessage("message");
			sampleEntity.setVersion(System.currentTimeMillis());
			repository.save(sampleEntity);
		}
		// when
		Page<SampleEntity> pageResult = repository.findByMessage("message", new PageRequest(0, 23));
		// then
		assertThat(pageResult.getTotalElements(), is(equalTo(30L)));
		assertThat(pageResult.getContent().size(), is(equalTo(23)));
	}

	@Test
	public void shouldReturnPageableResultsWithGivenSortingOrder() {
		// given
		String documentId = random(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("abc");
		sampleEntity.setVersion(System.currentTimeMillis());
		repository.save(sampleEntity);

		String documentId2 = randomNumeric(5);
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setMessage("abd");
		sampleEntity.setVersion(System.currentTimeMillis());
		repository.save(sampleEntity2);

		String documentId3 = randomNumeric(5);
		SampleEntity sampleEntity3 = new SampleEntity();
		sampleEntity3.setId(documentId3);
		sampleEntity3.setMessage("abe");
		sampleEntity.setVersion(System.currentTimeMillis());
		repository.save(sampleEntity3);
		// when
		Page<SampleEntity> pageResult = repository.findByMessageContaining("a",
				new PageRequest(0, 23, new Sort(new Sort.Order(Sort.Direction.DESC, "message"))));
		// then
		assertThat(pageResult.getContent().isEmpty(), is(false));
		assertThat(pageResult.getContent().get(0).getMessage(), is(sampleEntity3.getMessage()));
	}

	@Test
	public void shouldReturnListForMessage() {
		// given
		String documentId = random(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("abc");
		sampleEntity.setVersion(System.currentTimeMillis());
		repository.save(sampleEntity);

		String documentId2 = randomNumeric(5);
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setMessage("abd");
		sampleEntity.setVersion(System.currentTimeMillis());
		repository.save(sampleEntity2);

		String documentId3 = randomNumeric(5);
		SampleEntity sampleEntity3 = new SampleEntity();
		sampleEntity3.setId(documentId3);
		sampleEntity3.setMessage("abe");
		sampleEntity.setVersion(System.currentTimeMillis());
		repository.save(sampleEntity3);
		// when
		List<SampleEntity> sampleEntities = repository.findByMessage("abc");
		// then
		assertThat(sampleEntities.isEmpty(), is(false));
		assertThat(sampleEntities.size(), is(1));
	}

	@Test
	public void shouldExecuteCustomMethodWithGeoPoint() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setType("test");
		sampleEntity.setRate(10);
		sampleEntity.setMessage("foo");
		sampleEntity.setLocation(new GeoPoint(45.7806d, 3.0875d));

		repository.save(sampleEntity);

		// when
		Page<SampleEntity> page = repository.findByLocation(new GeoPoint(45.7806d, 3.0875d), new PageRequest(0, 10));
		// then
		assertThat(page, is(notNullValue()));
		assertThat(page.getTotalElements(), is(equalTo(1L)));
	}

	@Test
	public void shouldExecuteCustomMethodWithGeoPointAndString() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setType("test");
		sampleEntity.setRate(10);
		sampleEntity.setMessage("foo");
		sampleEntity.setLocation(new GeoPoint(45.7806d, 3.0875d));

		repository.save(sampleEntity);

		documentId = randomNumeric(5);
		sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setType("test");
		sampleEntity.setRate(10);
		sampleEntity.setMessage("foo");
		sampleEntity.setLocation(new GeoPoint(48.7806d, 3.0875d));

		repository.save(sampleEntity);

		// when
		Page<SampleEntity> page = repository.findByLocationAndMessage(new GeoPoint(45.7806d, 3.0875d), "foo",
				new PageRequest(0, 10));
		// then
		assertThat(page, is(notNullValue()));
		assertThat(page.getTotalElements(), is(equalTo(1L)));
	}

	@Test
	public void shouldExecuteCustomMethodWithWithinGeoPoint() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setType("test");
		sampleEntity.setRate(10);
		sampleEntity.setMessage("foo");
		sampleEntity.setLocation(new GeoPoint(45.7806d, 3.0875d));

		repository.save(sampleEntity);

		// when
		Page<SampleEntity> page = repository.findByLocationWithin(new GeoPoint(45.7806d, 3.0875d), "2km",
				new PageRequest(0, 10));
		// then
		assertThat(page, is(notNullValue()));
		assertThat(page.getTotalElements(), is(equalTo(1L)));
	}

	@Test
	public void shouldExecuteCustomMethodWithWithinPoint() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setType("test");
		sampleEntity.setRate(10);
		sampleEntity.setMessage("foo");
		sampleEntity.setLocation(new GeoPoint(45.7806d, 3.0875d));

		repository.save(sampleEntity);

		// when
		Page<SampleEntity> page = repository.findByLocationWithin(new Point(45.7806d, 3.0875d),
				new Distance(2, Metrics.KILOMETERS), new PageRequest(0, 10));
		// then
		assertThat(page, is(notNullValue()));
		assertThat(page.getTotalElements(), is(equalTo(1L)));
	}

	@Test
	public void shouldExecuteCustomMethodWithNearBox() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setType("test");
		sampleEntity.setRate(10);
		sampleEntity.setMessage("foo");
		sampleEntity.setLocation(new GeoPoint(45.7806d, 3.0875d));

		repository.save(sampleEntity);

		documentId = randomNumeric(5);
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId);
		sampleEntity2.setType("test2");
		sampleEntity2.setRate(10);
		sampleEntity2.setMessage("foo");
		sampleEntity2.setLocation(new GeoPoint(30.7806d, 0.0875d));

		repository.save(sampleEntity2);

		// when
		Page<SampleEntity> pageAll = repository.findAll(new PageRequest(0, 10));
		// then
		assertThat(pageAll, is(notNullValue()));
		assertThat(pageAll.getTotalElements(), is(equalTo(2L)));

		// when
		Page<SampleEntity> page = repository.findByLocationNear(new Box(new Point(46d, 3d), new Point(45d, 4d)),
				new PageRequest(0, 10));
		// then
		assertThat(page, is(notNullValue()));
		assertThat(page.getTotalElements(), is(equalTo(1L)));
	}

	@Test
	public void shouldExecuteCustomMethodWithNearPointAndDistance() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setType("test");
		sampleEntity.setRate(10);
		sampleEntity.setMessage("foo");
		sampleEntity.setLocation(new GeoPoint(45.7806d, 3.0875d));

		repository.save(sampleEntity);

		// when
		Page<SampleEntity> page = repository.findByLocationNear(new Point(45.7806d, 3.0875d),
				new Distance(2, Metrics.KILOMETERS), new PageRequest(0, 10));
		// then
		assertThat(page, is(notNullValue()));
		assertThat(page.getTotalElements(), is(equalTo(1L)));
	}

	/*
	DATAES-165
	 */
	@Test
	public void shouldAllowReturningJava8StreamInCustomQuery() {
		// given
		List<SampleEntity> entities = createSampleEntities("abc", 30);
		repository.saveAll(entities);

		// when
		Stream<SampleEntity> stream = repository.findByType("abc");
		// then
		assertThat(stream, is(notNullValue()));
		assertThat(stream.count(), is(equalTo(30L)));
	}

	/*
	DATAES-106
	 */
	@Test
	public void shouldCountCustomMethod() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setType("test");
		sampleEntity.setMessage("some message");

		repository.save(sampleEntity);

		documentId = randomNumeric(5);
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId);
		sampleEntity2.setType("test2");
		sampleEntity2.setMessage("some message");

		repository.save(sampleEntity2);

		// when
		long count = repository.countByType("test");
		// then
		assertThat(count, is(equalTo(1L)));
	}

	/*
	DATAES-106
	 */
	@Test
	public void shouldCountCustomMethodForNot() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setType("some");
		sampleEntity.setMessage("some message");

		repository.save(sampleEntity);

		documentId = randomNumeric(5);
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId);
		sampleEntity2.setType("test");
		sampleEntity2.setMessage("some message");

		repository.save(sampleEntity2);

		// when
		long count = repository.countByTypeNot("test");
		// then
		assertThat(count, is(equalTo(1L)));
	}

	/*
	DATAES-106
	 */
	@Test
	public void shouldCountCustomMethodWithBooleanParameter() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setType("test");
		sampleEntity.setMessage("foo");
		sampleEntity.setAvailable(true);
		repository.save(sampleEntity);

		// given
		String documentId2 = randomNumeric(5);
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setType("test");
		sampleEntity2.setMessage("bar");
		sampleEntity2.setAvailable(false);
		repository.save(sampleEntity2);
		// when
		long count = repository.countByAvailable(false);
		// then
		assertThat(count, is(equalTo(1L)));
	}

	/*
	DATAES-106
	 */
	@Test
	public void shouldCountCustomMethodWithLessThan() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setType("test");
		sampleEntity.setRate(9);
		sampleEntity.setMessage("some message");
		repository.save(sampleEntity);

		String documentId2 = randomNumeric(5);
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setType("test");
		sampleEntity2.setRate(20);
		sampleEntity2.setMessage("some message");
		repository.save(sampleEntity2);

		// when
		long count = repository.countByRateLessThan(10);
		// then
		assertThat(count, is(equalTo(1L)));
	}

	/*
	DATAES-106
	 */
	@Test
	public void shouldCountCustomMethodWithBefore() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setType("test");
		sampleEntity.setRate(10);
		sampleEntity.setMessage("some message");

		repository.save(sampleEntity);

		documentId = randomNumeric(5);
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId);
		sampleEntity2.setType("test");
		sampleEntity2.setRate(20);
		sampleEntity2.setMessage("some message");

		repository.save(sampleEntity2);

		// when
		long count = repository.countByRateBefore(10);
		// then
		assertThat(count, is(equalTo(1L)));
	}

	/*
	DATAES-106
	 */
	@Test
	public void shouldCountCustomMethodWithAfter() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setType("test");
		sampleEntity.setRate(10);
		sampleEntity.setMessage("some message");

		repository.save(sampleEntity);

		documentId = randomNumeric(5);
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId);
		sampleEntity2.setType("test");
		sampleEntity2.setRate(0);
		sampleEntity2.setMessage("some message");

		repository.save(sampleEntity2);

		// when
		long count = repository.countByRateAfter(10);
		// then
		assertThat(count, is(equalTo(1L)));
	}

	/*
	DATAES-106
	 */
	@Test
	public void shouldCountCustomMethodWithLike() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setType("test");
		sampleEntity.setRate(10);
		sampleEntity.setMessage("foo");

		repository.save(sampleEntity);

		documentId = randomNumeric(5);
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId);
		sampleEntity2.setType("test");
		sampleEntity2.setRate(10);
		sampleEntity2.setMessage("some message");

		repository.save(sampleEntity2);

		// when
		long count = repository.countByMessageLike("fo");
		// then
		assertThat(count, is(equalTo(1L)));
	}

	/*
	DATAES-106
	 */
	@Test
	public void shouldCountCustomMethodForStartingWith() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setType("test");
		sampleEntity.setRate(10);
		sampleEntity.setMessage("foo");

		repository.save(sampleEntity);

		documentId = randomNumeric(5);
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId);
		sampleEntity2.setType("test");
		sampleEntity2.setRate(10);
		sampleEntity2.setMessage("some message");

		repository.save(sampleEntity2);

		// when
		long count = repository.countByMessageStartingWith("fo");
		// then
		assertThat(count, is(equalTo(1L)));
	}

	/*
	DATAES-106
	 */
	@Test
	public void shouldCountCustomMethodForEndingWith() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setType("test");
		sampleEntity.setRate(10);
		sampleEntity.setMessage("foo");

		repository.save(sampleEntity);

		documentId = randomNumeric(5);
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId);
		sampleEntity2.setType("test");
		sampleEntity2.setRate(10);
		sampleEntity2.setMessage("some message");

		repository.save(sampleEntity2);

		// when
		long count = repository.countByMessageEndingWith("o");
		// then
		assertThat(count, is(equalTo(1L)));
	}

	/*
	DATAES-106
	 */
	@Test
	public void shouldCountCustomMethodForContains() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setType("test");
		sampleEntity.setRate(10);
		sampleEntity.setMessage("foo");

		repository.save(sampleEntity);

		documentId = randomNumeric(5);
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId);
		sampleEntity2.setType("test");
		sampleEntity2.setRate(10);
		sampleEntity2.setMessage("some message");

		repository.save(sampleEntity2);

		// when
		long count = repository.countByMessageContaining("fo");
		// then
		assertThat(count, is(equalTo(1L)));
	}

	/*
	DATAES-106
	 */
	@Test
	public void shouldCountCustomMethodForIn() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setType("test");
		sampleEntity.setMessage("foo");
		repository.save(sampleEntity);

		// given
		String documentId2 = randomNumeric(5);
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setType("test");
		sampleEntity2.setMessage("bar");
		repository.save(sampleEntity2);

		List<String> ids = Arrays.asList(documentId, documentId2);

		// when
		long count = repository.countByIdIn(ids);
		// then
		assertThat(count, is(equalTo(2L)));
	}

	/*
	DATAES-106
	 */
	@Test
	public void shouldCountCustomMethodForNotIn() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setType("test");
		sampleEntity.setMessage("foo");
		repository.save(sampleEntity);

		// given
		String documentId2 = randomNumeric(5);
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setType("test");
		sampleEntity2.setMessage("bar");
		repository.save(sampleEntity2);

		List<String> ids = Arrays.asList(documentId);

		// when
		long count = repository.countByIdNotIn(ids);
		// then
		assertThat(count, is(equalTo(1L)));
	}

	/*
	DATAES-106
	 */
	@Test
	public void shouldCountCustomMethodForTrue() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setType("test");
		sampleEntity.setMessage("foo");
		sampleEntity.setAvailable(true);
		repository.save(sampleEntity);

		// given
		String documentId2 = randomNumeric(5);
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setType("test");
		sampleEntity2.setMessage("bar");
		sampleEntity2.setAvailable(false);
		repository.save(sampleEntity2);
		// when
		long count = repository.countByAvailableTrue();
		// then
		assertThat(count, is(equalTo(1L)));
	}

	/*
	DATAES-106
	 */
	@Test
	public void shouldCountCustomMethodForFalse() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setType("test");
		sampleEntity.setMessage("foo");
		sampleEntity.setAvailable(true);
		repository.save(sampleEntity);

		// given
		String documentId2 = randomNumeric(5);
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setType("test");
		sampleEntity2.setMessage("bar");
		sampleEntity2.setAvailable(false);
		repository.save(sampleEntity2);
		// when
		long count = repository.countByAvailableFalse();
		// then
		assertThat(count, is(equalTo(1L)));
	}

	/*
	DATAES-106
	 */
	@Test
	public void shouldCountCustomMethodWithWithinGeoPoint() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setType("test");
		sampleEntity.setRate(10);
		sampleEntity.setMessage("foo");
		sampleEntity.setLocation(new GeoPoint(45.7806d, 3.0875d));

		repository.save(sampleEntity);

		documentId = randomNumeric(5);
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId);
		sampleEntity2.setType("test");
		sampleEntity2.setRate(10);
		sampleEntity2.setMessage("foo");
		sampleEntity2.setLocation(new GeoPoint(30.7806d, 0.0875d));

		repository.save(sampleEntity2);

		// when
		long count = repository.countByLocationWithin(new GeoPoint(45.7806d, 3.0875d), "2km");
		// then
		assertThat(count, is(equalTo(1L)));
	}

	/*
	DATAES-106
	 */
	@Test
	public void shouldCountCustomMethodWithWithinPoint() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setType("test");
		sampleEntity.setRate(10);
		sampleEntity.setMessage("foo");
		sampleEntity.setLocation(new GeoPoint(45.7806d, 3.0875d));

		repository.save(sampleEntity);

		documentId = randomNumeric(5);
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId);
		sampleEntity2.setType("test");
		sampleEntity2.setRate(10);
		sampleEntity2.setMessage("foo");
		sampleEntity2.setLocation(new GeoPoint(30.7806d, 0.0875d));

		repository.save(sampleEntity2);

		// when
		long count = repository.countByLocationWithin(new Point(45.7806d, 3.0875d), new Distance(2, Metrics.KILOMETERS));
		// then
		assertThat(count, is(equalTo(1L)));
	}

	/*
	DATAES-106
	 */
	@Test
	public void shouldCountCustomMethodWithNearBox() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setType("test");
		sampleEntity.setRate(10);
		sampleEntity.setMessage("foo");
		sampleEntity.setLocation(new GeoPoint(45.7806d, 3.0875d));

		repository.save(sampleEntity);

		documentId = randomNumeric(5);
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId);
		sampleEntity2.setType("test2");
		sampleEntity2.setRate(10);
		sampleEntity2.setMessage("foo");
		sampleEntity2.setLocation(new GeoPoint(30.7806d, 0.0875d));

		repository.save(sampleEntity2);

		// when
		long count = repository.countByLocationNear(new Box(new Point(46d, 3d), new Point(45d, 4d)));
		// then
		assertThat(count, is(equalTo(1L)));
	}

	/*
	DATAES-106
	 */
	@Test
	public void shouldCountCustomMethodWithNearPointAndDistance() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setType("test");
		sampleEntity.setRate(10);
		sampleEntity.setMessage("foo");
		sampleEntity.setLocation(new GeoPoint(45.7806d, 3.0875d));

		repository.save(sampleEntity);

		documentId = randomNumeric(5);
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId);
		sampleEntity2.setType("test");
		sampleEntity2.setRate(10);
		sampleEntity2.setMessage("foo");
		sampleEntity2.setLocation(new GeoPoint(30.7806d, 0.0875d));

		repository.save(sampleEntity2);

		// when
		long count = repository.countByLocationNear(new Point(45.7806d, 3.0875d), new Distance(2, Metrics.KILOMETERS));
		// then
		assertThat(count, is(equalTo(1L)));
	}

	private List<SampleEntity> createSampleEntities(String type, int numberOfEntities) {
		List<SampleEntity> entities = new ArrayList<>();
		for (int i = 0; i < numberOfEntities; i++) {
			SampleEntity entity = new SampleEntity();
			entity.setId(randomNumeric(numberOfEntities));
			entity.setAvailable(true);
			entity.setMessage("Message");
			entity.setType(type);
			entities.add(entity);
		}
		return entities;
	}
}
