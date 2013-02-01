package org.springframework.data.elasticsearch.repositories;

import org.springframework.data.elasticsearch.Book;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface SampleElasticSearchBookRepository  extends ElasticsearchRepository<Book,String> {
}
