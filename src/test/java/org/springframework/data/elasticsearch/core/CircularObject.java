package org.springframework.data.elasticsearch.core;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * @author Stuart Stevenson
 */
@Document(indexName = "circular-objects", type = "circular-object" , indexStoreType = "memory", shards = 1, replicas = 0, refreshInterval = "-1")
public class CircularObject {

    @Id
    private String id;
    @Field(type = FieldType.Object, ignoreFields = {"circularObject"})
    private CircularObject circularObject;
}
