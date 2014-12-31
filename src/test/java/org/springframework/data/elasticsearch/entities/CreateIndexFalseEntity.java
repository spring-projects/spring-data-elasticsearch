package org.springframework.data.elasticsearch.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

/**
 * CreateIndexFalseEntity
 *
 * @author Mason Chan
 */

@Document(indexName = "test-index", type = "test-type", createIndex = "false")
public class CreateIndexFalseEntity {
    @Id
    private String id;

}
