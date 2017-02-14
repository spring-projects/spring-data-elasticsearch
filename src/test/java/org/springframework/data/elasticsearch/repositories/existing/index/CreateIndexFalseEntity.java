package org.springframework.data.elasticsearch.repositories.existing.index;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

/**
 * CreateIndexFalseEntity
 *
 * @author Mason Chan
 */

@Document(indexName = "test-index-not-create", type = "test-type", createIndex = false)
public class CreateIndexFalseEntity {
    @Id
    private String id;

}
