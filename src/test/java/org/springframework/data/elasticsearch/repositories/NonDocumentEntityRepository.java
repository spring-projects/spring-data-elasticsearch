package org.springframework.data.elasticsearch.repositories;

import org.springframework.data.elasticsearch.NonDocumentEntity;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;


public interface NonDocumentEntityRepository extends ElasticsearchRepository<NonDocumentEntity,String> {
}
