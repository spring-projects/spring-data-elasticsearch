package org.springframework.data.elasticsearch.core.partition;

import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

import java.util.Map;
import java.util.Set;

/**
 * Created by flefebure on 25/02/2016.
 */
public interface ElasticsearchIndexPartitionLister {

    public boolean indexExists(String indexName);
    public boolean mappingExists(String indexName, String type);
    public <T> void createIndexPartition(Class<T> clazz, String indexName);
    public boolean putMapping(String indexName, String type);

}
