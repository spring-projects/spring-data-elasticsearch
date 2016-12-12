package org.springframework.data.elasticsearch.entities;

import java.util.HashMap;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.DynamicTemplates;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * @author Petr Kukral
 */
@Document(indexName = "test-dynamictemplates", type = "test-dynamictemplatestype", indexStoreType = "memory", shards = 1,
        replicas = 0, refreshInterval = "-1")
@DynamicTemplates(mappingPath = "/mappings/test-dynamic_templates_mappings_two.json")
public class SampleDynamicTemplatesEntityTwo {

    @Id
    private String id;

    @Field(type = FieldType.Object)
    private Map<String, String> names = new HashMap<String, String>();
}
