/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.data.elasticsearch.repositories.custom;

import java.util.List;
import java.util.stream.Stream;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.core.geo.GeoBox;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.data.elasticsearch.entities.SampleEntity;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.geo.Box;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Point;

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

	Stream<SampleEntity> findByType(String type);

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
