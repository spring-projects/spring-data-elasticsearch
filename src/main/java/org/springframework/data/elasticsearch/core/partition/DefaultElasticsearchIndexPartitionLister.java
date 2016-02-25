package org.springframework.data.elasticsearch.core.partition;

import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.client.Client;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

import java.util.Map;
import java.util.Set;

/**
 * Created by flefebure on 25/02/2016.
 */
public class DefaultElasticsearchIndexPartitionLister implements ElasticsearchIndexPartitionLister {

    ElasticsearchOperations elasticsearchOperations;

    Client client;

    public DefaultElasticsearchIndexPartitionLister(ElasticsearchOperations elasticsearchOperations) {
        this.elasticsearchOperations = elasticsearchOperations;
    }

    @Override
    @Cacheable(value = "esPartitions")
    public boolean mappingExists(String indexName, String type) {
        return elasticsearchOperations.getMapping(indexName, type) != null;
    }

    @Override
    @Cacheable(value = "esPartitions")
    public boolean indexExists(String indexName) {
        return elasticsearchOperations.indexExists(indexName);
    }

    @Override
    @CacheEvict(value = "esPartitions", allEntries = true)
    public void createIndexPartition(Class clazz, String indexName) {
        elasticsearchOperations.createIndex(clazz, indexName);

    }

    @Override
    @CacheEvict(value = "esPartitions", allEntries = true)
    public boolean putMapping(String indexName, String type) {
        return false;
    }
}
