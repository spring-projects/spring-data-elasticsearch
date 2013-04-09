/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.elasticsearch.repositories;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.SampleEntity;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;
/**
 * @author Rizwan Idrees
 * @author Mohsin Husen
 */
public interface SampleCustomMethodRepository extends ElasticsearchRepository<SampleEntity,String> {

    Page<SampleEntity> findByType(String type, Pageable pageable);

    Page<SampleEntity> findByTypeNot(String type, Pageable pageable);

    @Query("{\"bool\" : {\"must\" : {\"field\" : {\"message\" : \"?0\"}}}}")
    Page<SampleEntity> findByMessage(String message, Pageable pageable);

    @Query("{\"bool\" : {\"must\" : {\"field\" : {\"message\" : \"?0\"}}}}")
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

    Page<SampleEntity> findByIdNotIn(List<String> messages, Pageable pageable);

    Page<SampleEntity> findByAvailableTrue(Pageable pageable);

    Page<SampleEntity> findByAvailableFalse(Pageable pageable);

    Page<SampleEntity> findByMessageOrderByTypeAsc(String message,Pageable pageable);

}
