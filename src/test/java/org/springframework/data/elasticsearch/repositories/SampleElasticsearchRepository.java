package org.springframework.data.elasticsearch.repositories;

import org.springframework.data.elasticsearch.SampleEntity;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface SampleElasticsearchRepository extends ElasticsearchRepository<SampleEntity,String> {

}
