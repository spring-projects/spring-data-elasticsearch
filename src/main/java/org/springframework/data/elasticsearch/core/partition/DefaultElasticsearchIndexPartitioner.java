package org.springframework.data.elasticsearch.core.partition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.elasticsearch.ElasticsearchException;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.partition.boundaries.PartitionBoundary;
import org.springframework.data.elasticsearch.core.partition.keys.PartitionKey;
import org.springframework.data.elasticsearch.core.query.*;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Created by flefebure on 24/02/2016.
 */
public class DefaultElasticsearchIndexPartitioner implements ElasticsearchIndexPartitioner {

    Logger logger = LoggerFactory.getLogger(DefaultElasticsearchIndexPartitioner.class);

    ElasticsearchConverter elasticsearchConverter;

    ElasticsearchIndexPartitionLister elasticsearchIndexPartitionLister;

    SimpleElasticsearchPersistentEntity persistentEntity;
     
    Map<String,Set<String>> existingIndexes = new HashMap<String, Set<String>>();

    public DefaultElasticsearchIndexPartitioner(){}

    public DefaultElasticsearchIndexPartitioner(SimpleElasticsearchPersistentEntity persistentEntity) {
        this.persistentEntity = persistentEntity;
    }

    public DefaultElasticsearchIndexPartitioner(ElasticsearchIndexPartitionLister elasticsearchIndexPartitionLister) {
        this.elasticsearchConverter = new MappingElasticsearchConverter(
                new SimpleElasticsearchMappingContext());
        this.elasticsearchIndexPartitionLister = elasticsearchIndexPartitionLister;
    }
    public DefaultElasticsearchIndexPartitioner(ElasticsearchIndexPartitionLister elasticsearchIndexPartitionLister, SimpleElasticsearchPersistentEntity persistentEntity) {
        this.persistentEntity = persistentEntity;
        this.elasticsearchIndexPartitionLister = elasticsearchIndexPartitionLister;
    }

    @Override
    public <T> void processPartitioning(Query query, Class<T> clazz) {
        ElasticsearchPersistentEntity persistentEntity = elasticsearchConverter.getMappingContext().getPersistentEntity(clazz);
        String type = persistentEntity.getIndexType();
        List<PartitionBoundary> partitionBoundaries = null;
        if (query instanceof NativeSearchQuery) {
            partitionBoundaries = ((NativeSearchQuery) query).getPartitionBoundaries();
        }
        else if(query instanceof CriteriaQuery) {
            partitionBoundaries = ((NativeSearchQuery)query).getPartitionBoundaries();
        }
        if (partitionBoundaries == null)
            throw new ElasticsearchException("the Query doesn't contain partition boundaries for partitioned type "+type);

        List<String> slices = PartitionBoundary.getSlices(partitionBoundaries, persistentEntity.getPartitions(), persistentEntity.getPartitionStrategies());
        List<String> indices = new ArrayList<String>();
        for (String slice : slices) {
            String indexName = persistentEntity.getIndexName()+"_"+slice;
            Set<String> partitions = existingIndexes.get(type);
            if (partitions == null) {
                partitions = new HashSet<String>();
                existingIndexes.put(type, partitions);
            }
            if (partitions.contains(indexName)) {
                indices.add(indexName);
            }
            else if (elasticsearchIndexPartitionLister.indexExists(indexName)) {
                partitions.add(indexName);
                indices.add(indexName);
            }
        }
        query.getIndices().clear();
        query.getIndices().addAll(indices);
    }

    @Override
    public <T> void processPartitioning(IndexQuery indexQuery, Class<T> clazz) {
        if (indexQuery.getObject() == null) {
            throw new ElasticsearchException("Partitioned index is not supported for source queries");
        }
        ElasticsearchPersistentEntity persistentEntity = elasticsearchConverter.getMappingContext().getPersistentEntity(clazz);
        String partitionKey = extractPartitionKeyFromObject(indexQuery.getObject(), persistentEntity);

        if (indexQuery.getId() != null)
            indexQuery.setId(partitionKey+"_"+indexQuery.getId());
        else
            indexQuery.setId(partitionKey+"_"+UUID.randomUUID());
        indexQuery.setIndexName(persistentEntity.getIndexName()+"_"+partitionKey);
        createPartitionIfNotExists(clazz, persistentEntity.getIndexType(), indexQuery.getIndexName());
    }

    public <T> String extractPartitionKeyFromObject(T object) {
        return extractPartitionKeyFromObject(object, persistentEntity);
    }

    private <T> String extractPartitionKeyFromObject(T object, ElasticsearchPersistentEntity persistentEntity) {

        String[] keys = new String[persistentEntity.getPartitions().length];
        for (int i = 0; i < persistentEntity.getPartitions().length ; i++) {
            Field field = ReflectionUtils.findField(object.getClass(), persistentEntity.getPartitions()[i]);
            if (field == null) {
                throw new ElasticsearchException("impossible to evaluate partition key for field " + persistentEntity.getPartitions()[i]);
            }
            Object fieldValue = ReflectionUtils.getField(field, object);
            if (field == null) {
                throw new ElasticsearchException("impossible to evaluate partition key for field " + persistentEntity.getPartitions()[i]);
            }
            try {
                PartitionKey keyFormatter = (PartitionKey) (persistentEntity.getPartitionStrategies()[i].getKeyFormatter().newInstance());
                keys[i] = keyFormatter.getKey(fieldValue).toUpperCase();
            } catch (Exception e) {
                throw new ElasticsearchException("impossible to evaluate partition key for field " + persistentEntity.getPartitions()[i]);
            }
        }
        return String.join("_", keys);
    }

    @Override
    public <T> boolean isIndexPartitioned(T object) {
        if (object == null) return false;
        return isIndexPartitioned(object.getClass());
    }

    @Override
    public <T> boolean isIndexPartitioned(Class<T> clazz) {
        ElasticsearchPersistentEntity persistentEntity = elasticsearchConverter.getMappingContext().getPersistentEntity(clazz);
        return persistentEntity.getPartitions().length > 0;
    }

    @Override
    public <T> void processPartitioning(UpdateQuery updateQuery, Class<T> clazz) {
        ElasticsearchPersistentEntity persistentEntity = elasticsearchConverter.getMappingContext().getPersistentEntity(clazz);
        String[] splittedId = updateQuery.getId().split("_");
        if (splittedId.length == 1) {
            throw new ElasticsearchException("impossible to extract partition from key from document ID");
        }
        String partitionKey = splittedId[splittedId.length-1];
        updateQuery.setIndexName(updateQuery.getIndexName()+"_"+partitionKey);

    }

    private <T> void createPartitionIfNotExists(Class<T> clazz, String type, String indexName) {
        Set<String> partitions = existingIndexes.get(type);
        if (partitions == null) {
            partitions = new HashSet<String>();
            existingIndexes.put(type, partitions);
        }
        if (!partitions.contains(indexName)) {
            if (!elasticsearchIndexPartitionLister.indexExists(indexName)) {
                logger.info("creating partitioned index "+indexName);
                elasticsearchIndexPartitionLister.createIndexPartition(clazz, indexName);
            }
            if(!elasticsearchIndexPartitionLister.mappingExists(indexName, type)) {
                elasticsearchIndexPartitionLister.putMapping(indexName, type);
            }
            partitions.add(indexName);
        }
    }
}
