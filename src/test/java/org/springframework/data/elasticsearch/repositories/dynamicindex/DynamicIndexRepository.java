package org.springframework.data.elasticsearch.repositories.dynamicindex;

import org.springframework.data.elasticsearch.entities.DynamicIndexEntity;
import org.springframework.data.elasticsearch.repository.IndexedCrudRepository;

/**
 * @author Ivan Greene
 */
public interface DynamicIndexRepository extends IndexedCrudRepository<DynamicIndexEntity, String> {
}
