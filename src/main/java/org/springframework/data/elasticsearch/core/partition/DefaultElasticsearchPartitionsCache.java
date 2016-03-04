package org.springframework.data.elasticsearch.core.partition;

import org.elasticsearch.action.admin.cluster.state.ClusterStateResponse;
import org.elasticsearch.action.admin.indices.status.IndicesStatusResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.collect.ImmutableMap;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.hppc.cursors.ObjectCursor;
import org.elasticsearch.common.hppc.cursors.ObjectObjectCursor;
import org.elasticsearch.indices.IndexAlreadyExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.elasticsearch.annotations.Setting;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang.StringUtils.isNotBlank;

/**
 * Created by franck.lefebure on 25/02/2016.
 */
public class DefaultElasticsearchPartitionsCache implements ElasticsearchPartitionsCache {

    Logger logger = LoggerFactory.getLogger(DefaultElasticsearchPartitionsCache.class);
    ElasticsearchOperations elasticsearchOperations;

    Client client;

    public DefaultElasticsearchPartitionsCache(Client client) {
        this.client = client;
    }

    public void setElasticsearchOperations(ElasticsearchOperations elasticsearchOperations) {
        this.elasticsearchOperations = elasticsearchOperations;
    }

    @Override
    @CacheEvict(value = "esPartitions", allEntries = true)
    public <T> void createPartition(String partition, Class<T> clazz) {
        try {
            elasticsearchOperations.createIndex(clazz, null, partition);
        }
        catch (IndexAlreadyExistsException exception) {
            // ignore such exception
            logger.info("tried to create existing partition");
        }
    }

    @Override
    public <T> void putMapping(String partition, Class<T> clazz) {
        elasticsearchOperations.putMapping(clazz, null, partition);
    }

    @Override
    @Cacheable(value = "esPartitions")
    public List<String> listTypesForPartition(String partition) {
        List<String> types = new ArrayList<String>();
        ClusterStateResponse clusterStateResponse = client.admin().cluster().prepareState().execute().actionGet();
        ImmutableOpenMap<String,MappingMetaData> indexMappings = clusterStateResponse.getState().getMetaData().index(partition).getMappings();
        for (ObjectCursor<String> stringObjectCursor : indexMappings.keys()) {
            types.add(stringObjectCursor.value);
        }
        return types;
    }


    @Override
    @Cacheable(value = "esPartitions")
    public List<String> listIndicesForPrefix(String prefix) {
        List<String> indices = new ArrayList<String>();
        IndicesStatusResponse response = client.admin().indices().prepareStatus().execute().actionGet();
        for (String index : response.getIndices().keySet()) {
            if (index.startsWith(prefix)) {
                logger.debug("adding indice " + index + " to partition cache");
                indices.add(index);
            }
        }
        return indices;
    }
}
