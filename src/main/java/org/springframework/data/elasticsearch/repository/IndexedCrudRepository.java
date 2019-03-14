package org.springframework.data.elasticsearch.repository;

import org.springframework.data.repository.NoRepositoryBean;

import java.util.Optional;

/**
 * Describes methods complementing those in {@link org.springframework.data.repository.CrudRepository}
 * with a signature accepting an index name, to allow for dynamic index names.
 *
 * @author Ivan Greene
 */
@NoRepositoryBean
public interface IndexedCrudRepository<T, ID> extends ElasticsearchRepository<T, ID> {

    Optional<T> findById(String indexName, ID id);

    boolean existsById(String indexName, ID id);

    Iterable<T> findAll(String indexName);

    Iterable<T> findAllById(String indexName, Iterable<ID> ids);

    long count(String indexName);

    void deleteById(String indexName, ID id);

    void deleteAll(String indexName);
}
