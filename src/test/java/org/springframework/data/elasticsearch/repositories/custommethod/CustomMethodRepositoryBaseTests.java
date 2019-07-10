/*
 * Copyright 2013-2019 the original author or authors.
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
package org.springframework.data.elasticsearch.repositories.custommethod;

import static org.apache.commons.lang.RandomStringUtils.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.elasticsearch.annotations.FieldType.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.lang.Long;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.core.geo.GeoBox;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.geo.Box;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;

/**
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Franck Marchand
 * @author Kevin Leturc
 * @author Christoph Strobl
 * @author Don Wellington
 * @author Peter-Josef Meisch
 * @author Rasmus Faber-Espensen
 */
public abstract class CustomMethodRepositoryBaseTests {

	@Autowired private SampleCustomMethodRepository repository;

	@Autowired private SampleStreamingCustomMethodRepository streamingRepository;

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
		Page<SampleEntity> page = repository.findByType("test", PageRequest.of(0, 10));

		// then
		assertThat(page).isNotNull();
		assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(1);
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
		Page<SampleEntity> page = repository.findByTypeNot("test", PageRequest.of(0, 10));
		// then
		assertThat(page).isNotNull();
		assertThat(page.getTotalElements()).isEqualTo(1L);
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
		Page<SampleEntity> page = repository.findByMessage(searchTerm.toLowerCase(), PageRequest.of(0, 10));

		// then
		assertThat(page).isNotNull();
		assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(1L);
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
		Page<SampleEntity> page = repository.findByRateLessThan(10, PageRequest.of(0, 10));

		// then
		assertThat(page).isNotNull();
		assertThat(page.getTotalElements()).isEqualTo(1L);
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
		Page<SampleEntity> page = repository.findByRateBefore(10, PageRequest.of(0, 10));

		// then
		assertThat(page).isNotNull();
		assertThat(page.getTotalElements()).isEqualTo(1L);
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
		Page<SampleEntity> page = repository.findByRateAfter(10, PageRequest.of(0, 10));

		// then
		assertThat(page).isNotNull();
		assertThat(page.getTotalElements()).isEqualTo(1L);
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
		Page<SampleEntity> page = repository.findByMessageLike("fo", PageRequest.of(0, 10));

		// then
		assertThat(page).isNotNull();
		assertThat(page.getTotalElements()).isEqualTo(1L);
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
		Page<SampleEntity> page = repository.findByMessageStartingWith("fo", PageRequest.of(0, 10));

		// then
		assertThat(page).isNotNull();
		assertThat(page.getTotalElements()).isEqualTo(1L);
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
		Page<SampleEntity> page = repository.findByMessageEndingWith("o", PageRequest.of(0, 10));

		// then
		assertThat(page).isNotNull();
		assertThat(page.getTotalElements()).isEqualTo(1L);
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
		Page<SampleEntity> page = repository.findByMessageContaining("fo", PageRequest.of(0, 10));

		// then
		assertThat(page).isNotNull();
		assertThat(page.getTotalElements()).isEqualTo(1L);
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
		Page<SampleEntity> page = repository.findByIdIn(ids, PageRequest.of(0, 10));

		// then
		assertThat(page).isNotNull();
		assertThat(page.getTotalElements()).isEqualTo(2L);
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

		List<String> ids = Collections.singletonList(documentId);

		// when
		Page<SampleEntity> page = repository.findByIdNotIn(ids, PageRequest.of(0, 10));
		// then
		assertThat(page).isNotNull();
		assertThat(page.getTotalElements()).isEqualTo(1L);
		assertThat(page.getContent().get(0).getId()).isEqualTo(documentId2);
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
		Page<SampleEntity> page = repository.findByAvailableTrue(PageRequest.of(0, 10));

		// then
		assertThat(page).isNotNull();
		assertThat(page.getTotalElements()).isEqualTo(1L);
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
		Page<SampleEntity> page = repository.findByAvailableFalse(PageRequest.of(0, 10));

		// then
		assertThat(page).isNotNull();
		assertThat(page.getTotalElements()).isEqualTo(1L);
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
		Page<SampleEntity> page = repository.findByMessageOrderByTypeAsc("foo", PageRequest.of(0, 10));

		// then
		assertThat(page).isNotNull();
		assertThat(page.getTotalElements()).isEqualTo(1L);
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
		Page<SampleEntity> page = repository.findByAvailable(false, PageRequest.of(0, 10));

		// then
		assertThat(page).isNotNull();
		assertThat(page.getTotalElements()).isEqualTo(1L);
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
		Page<SampleEntity> pageResult = repository.findByMessage("message", PageRequest.of(0, 23));

		// then
		assertThat(pageResult.getTotalElements()).isEqualTo(30L);
		assertThat(pageResult.getContent().size()).isEqualTo(23);
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
				PageRequest.of(0, 23, Sort.by(Order.desc("message"))));

		// then
		assertThat(pageResult.getContent()).isNotEmpty();
		assertThat(pageResult.getContent().get(0).getMessage()).isEqualTo(sampleEntity3.getMessage());
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
		assertThat(sampleEntities).hasSize(1);
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
		Page<SampleEntity> page = repository.findByLocation(new GeoPoint(45.7806d, 3.0875d), PageRequest.of(0, 10));

		// then
		assertThat(page).isNotNull();
		assertThat(page.getTotalElements()).isEqualTo(1L);
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
				PageRequest.of(0, 10));

		// then
		assertThat(page).isNotNull();
		assertThat(page.getTotalElements()).isEqualTo(1L);
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
				PageRequest.of(0, 10));

		// then
		assertThat(page).isNotNull();
		assertThat(page.getTotalElements()).isEqualTo(1L);
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
				new Distance(2, Metrics.KILOMETERS), PageRequest.of(0, 10));

		// then
		assertThat(page).isNotNull();
		assertThat(page.getTotalElements()).isEqualTo(1L);
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
		Page<SampleEntity> pageAll = repository.findAll(PageRequest.of(0, 10));

		// then
		assertThat(pageAll).isNotNull();
		assertThat(pageAll.getTotalElements()).isEqualTo(2L);

		// when
		Page<SampleEntity> page = repository.findByLocationNear(new Box(new Point(46d, 3d), new Point(45d, 4d)),
				PageRequest.of(0, 10));

		// then
		assertThat(page).isNotNull();
		assertThat(page.getTotalElements()).isEqualTo(1L);
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
				new Distance(2, Metrics.KILOMETERS), PageRequest.of(0, 10));

		// then
		assertThat(page).isNotNull();
		assertThat(page.getTotalElements()).isEqualTo(1L);
	}

	@Test // DATAES-165
	public void shouldAllowReturningJava8StreamInCustomQuery() {

		// given
		List<SampleEntity> entities = createSampleEntities("abc", 30);
		repository.saveAll(entities);

		// when
		Stream<SampleEntity> stream = streamingRepository.findByType("abc");

		// then
		assertThat(stream).isNotNull();
		assertThat(stream.count()).isEqualTo(30L);
	}

	@Test // DATAES-106
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
		assertThat(count).isEqualTo(1L);
	}

	@Test // DATAES-106
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
		assertThat(count).isEqualTo(1L);
	}

	@Test // DATAES-106
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
		assertThat(count).isEqualTo(1L);
	}

	@Test // DATAES-106
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
		assertThat(count).isEqualTo(1L);
	}

	@Test // DATAES-106
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
		assertThat(count).isEqualTo(1L);
	}

	@Test // DATAES-106
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
		assertThat(count).isEqualTo(1L);
	}

	@Test // DATAES-106
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
		assertThat(count).isEqualTo(1L);
	}

	@Test // DATAES-106
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
		assertThat(count).isEqualTo(1L);
	}

	@Test // DATAES-106
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
		assertThat(count).isEqualTo(1L);
	}

	@Test // DATAES-106
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
		assertThat(count).isEqualTo(1L);
	}

	@Test // DATAES-106
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
		assertThat(count).isEqualTo(2L);
	}

	@Test // DATAES-106
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

		List<String> ids = Collections.singletonList(documentId);

		// when
		long count = repository.countByIdNotIn(ids);

		// then
		assertThat(count).isEqualTo(1L);
	}

	@Test // DATAES-106
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
		assertThat(count).isEqualTo(1L);
	}

	@Test // DATAES-106
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
		assertThat(count).isEqualTo(1L);
	}

	@Test // DATAES-106
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
		assertThat(count).isEqualTo(1L);
	}

	@Test // DATAES-106
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
		assertThat(count).isEqualTo(1L);
	}

	@Test // DATAES-106
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
		assertThat(count).isEqualTo(1L);
	}

	@Test // DATAES-106
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
		assertThat(count).isEqualTo(1L);
	}

	@Test // DATAES-605
	public void streamMethodsShouldWorkWithLargeResultSets() {

		// given
		List<SampleEntity> entities = createSampleEntities("abc", 10001);
		repository.saveAll(entities);

		// when
		Stream<SampleEntity> stream = streamingRepository.findByType("abc");

		// then
		assertThat(stream).isNotNull();
		assertThat(stream.count()).isEqualTo(10001L);
	}

	@Test // DATAES-605
	public void streamMethodsCanHandlePageable() {

		// given
		List<SampleEntity> entities = createSampleEntities("abc", 10);
		repository.saveAll(entities);

		// when
		Stream<SampleEntity> stream = streamingRepository.findByType("abc", PageRequest.of(0, 2));

		// then
		assertThat(stream).isNotNull();
		assertThat(stream.count()).isEqualTo(10L);
	}

	private List<SampleEntity> createSampleEntities(String type, int numberOfEntities) {

		List<SampleEntity> entities = new ArrayList<>();
		for (int i = 0; i < numberOfEntities; i++) {

			SampleEntity entity = new SampleEntity();
			entity.setId(UUID.randomUUID().toString());
			entity.setAvailable(true);
			entity.setMessage("Message");
			entity.setType(type);
			entities.add(entity);
		}

		return entities;
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	@Document(indexName = "test-index-sample-repositories-custo-method", type = "test-type", shards = 1, replicas = 0,
			refreshInterval = "-1")
	static class SampleEntity {

		@Id private String id;
		@Field(type = Text, store = true, fielddata = true) private String type;
		@Field(type = Text, store = true, fielddata = true) private String message;
		private int rate;
		private boolean available;
		private GeoPoint location;
		@Version private Long version;
	}

	/**
	 * @author Rizwan Idrees
	 * @author Mohsin Husen
	 * @author Kevin Leturc
	 */
	public interface SampleCustomMethodRepository extends ElasticsearchRepository<SampleEntity, String> {

		Page<SampleEntity> findByType(String type, Pageable pageable);

		Page<SampleEntity> findByTypeNot(String type, Pageable pageable);

		@Query("{\"bool\" : {\"must\" : {\"term\" : {\"message\" : \"?0\"}}}}")
		Page<SampleEntity> findByMessage(String message, Pageable pageable);

		@Query("{\"bool\" : {\"must\" : {\"term\" : {\"message\" : \"?0\"}}}}")
		List<SampleEntity> findByMessage(String message);

		Page<SampleEntity> findByAvailable(boolean available, Pageable pageable);

		Page<SampleEntity> findByRateLessThan(int rate, Pageable pageable);

		Page<SampleEntity> findByRateBefore(int rate, Pageable pageable);

		Page<SampleEntity> findByRateAfter(int rate, Pageable pageable);

		Page<SampleEntity> findByMessageLike(String message, Pageable pageable);

		Page<SampleEntity> findByMessageStartingWith(String message, Pageable pageable);

		Page<SampleEntity> findByMessageEndingWith(String message, Pageable pageable);

		Page<SampleEntity> findByMessageContaining(String message, Pageable pageable);

		Page<SampleEntity> findByIdIn(List<String> ids, Pageable pageable);

		Page<SampleEntity> findByIdNotIn(List<String> ids, Pageable pageable);

		Page<SampleEntity> findByAvailableTrue(Pageable pageable);

		Page<SampleEntity> findByAvailableFalse(Pageable pageable);

		Page<SampleEntity> findByMessageOrderByTypeAsc(String message, Pageable pageable);

		Page<SampleEntity> findByLocation(GeoPoint point, Pageable pageable);

		Page<SampleEntity> findByLocationAndMessage(GeoPoint point, String msg, Pageable pageable);

		Page<SampleEntity> findByLocationWithin(GeoPoint point, String distance, Pageable pageable);

		Page<SampleEntity> findByLocationWithin(Point point, Distance distance, Pageable pageable);

		Page<SampleEntity> findByLocationNear(GeoBox box, Pageable pageable);

		Page<SampleEntity> findByLocationNear(Box box, Pageable pageable);

		Page<SampleEntity> findByLocationNear(Point point, Distance distance, Pageable pageable);

		Page<SampleEntity> findByLocationNear(GeoPoint point, String distance, Pageable pageable);

		long countByType(String type);

		long countByTypeNot(String type);

		long countByAvailable(boolean available);

		long countByRateLessThan(int rate);

		long countByRateBefore(int rate);

		long countByRateAfter(int rate);

		long countByMessageLike(String message);

		long countByMessageStartingWith(String message);

		long countByMessageEndingWith(String message);

		long countByMessageContaining(String message);

		long countByIdIn(List<String> ids);

		long countByIdNotIn(List<String> ids);

		long countByAvailableTrue();

		long countByAvailableFalse();

		long countByLocationWithin(GeoPoint point, String distance);

		long countByLocationWithin(Point point, Distance distance);

		long countByLocationNear(GeoBox box);

		long countByLocationNear(Box box);

		long countByLocationNear(Point point, Distance distance);

		long countByLocationNear(GeoPoint point, String distance);
	}

	/**
	 * @author Rasmus Faber-Espensen
	 */
	public interface SampleStreamingCustomMethodRepository extends ElasticsearchRepository<SampleEntity, String> {
		Stream<SampleEntity> findByType(String type);

		Stream<SampleEntity> findByType(String type, Pageable pageable);
	}
}
