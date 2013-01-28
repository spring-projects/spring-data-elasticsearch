package org.springframework.data.elasticsearch.repositories;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.SampleEntity;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface SampleCustomMethodRepository extends ElasticsearchRepository<SampleEntity,String> {

    Page<SampleEntity> findByType(String type, Pageable pageable);

    Page<SampleEntity> findByTypeNot(String type, Pageable pageable);

    @Query("{\"bool\" : {\"must\" : {\"field\" : {\"message\" : \"?0\"}}}}")
    Page<SampleEntity> findByMessage(String message, Pageable pageable);

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
