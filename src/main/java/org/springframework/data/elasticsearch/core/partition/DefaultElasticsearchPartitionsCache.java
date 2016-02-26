package org.springframework.data.elasticsearch.core.partition;

import org.elasticsearch.client.Client;
import org.elasticsearch.indices.IndexAlreadyExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

/**
 * Created by flefebure on 25/02/2016.
 */
public class DefaultElasticsearchPartitionsCache implements ElasticsearchPartitionsCache {

    Logger logger = LoggerFactory.getLogger(DefaultElasticsearchPartitionsCache.class);
    ElasticsearchOperations elasticsearchOperations;

    public DefaultElasticsearchPartitionsCache() {
    }

    public void setElasticsearchOperations(ElasticsearchOperations elasticsearchOperations) {
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
        try {
            elasticsearchOperations.createIndex(clazz, indexName);
        }
        catch (IndexAlreadyExistsException exception) {
            // ignore such exception
            logger.info("tried to existing partition");
        }

    }

    @Override
    @CacheEvict(value = "esPartitions", allEntries = true)
    public boolean putMapping(String indexName, String type) {
        return false;
    }
}
