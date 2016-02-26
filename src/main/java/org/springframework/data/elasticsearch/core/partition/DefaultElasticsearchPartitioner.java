package org.springframework.data.elasticsearch.core.partition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.PropertyAccessorUtils;
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

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Created by flefebure on 24/02/2016.
 */
public class DefaultElasticsearchPartitioner implements ElasticsearchPartitioner {

    Logger logger = LoggerFactory.getLogger(DefaultElasticsearchPartitioner.class);

    ElasticsearchConverter elasticsearchConverter;

    ElasticsearchPartitionsCache elasticsearchPartitionsCache;

    Map<String,Set<String>> existingIndexes = new HashMap<String, Set<String>>();

    public DefaultElasticsearchPartitioner(){
        elasticsearchConverter = new MappingElasticsearchConverter(new SimpleElasticsearchMappingContext());
    }


    public void setElasticsearchPartitionsCache(ElasticsearchPartitionsCache elasticsearchPartitionsCache) {
        this.elasticsearchPartitionsCache = elasticsearchPartitionsCache;
        elasticsearchConverter = new MappingElasticsearchConverter(new SimpleElasticsearchMappingContext());
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

        List<String> slices = PartitionBoundary.getSlices(partitionBoundaries, persistentEntity.getPartitions(), persistentEntity.getPartitionStrategies(), persistentEntity.getPartitionParameters());
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
            else if (elasticsearchPartitionsCache.indexExists(indexName)) {
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
        String indexName;
        // if id contains an _, it's an update..
        if (indexQuery.getId() != null && indexQuery.getId().indexOf("_") >= 0) {
            String partitionKey = extractKeyFromId(indexQuery.getId(), persistentEntity);
            indexName = persistentEntity.getIndexName()+"_"+partitionKey;
        }
        else {
            String partitionKey = extractPartitionKeyFromObject(indexQuery.getObject(), persistentEntity);
            if (indexQuery.getId() == null)
                indexQuery.setId(partitionKey+"_"+UUID.randomUUID());
            else if (!indexQuery.getId().startsWith(partitionKey+"_"))
                indexQuery.setId(partitionKey+"_"+indexQuery.getId());
            indexName = persistentEntity.getIndexName()+"_"+partitionKey;
            createPartitionIfNotExists(clazz, persistentEntity.getIndexType(), indexName);
        }
        indexQuery.setIndexName(indexName);
    }

    @Override
    public <T> String processPartitioning(GetQuery getQuery, Class<T> clazz) {
        ElasticsearchPersistentEntity persistentEntity = elasticsearchConverter.getMappingContext().getPersistentEntity(clazz);
       return  persistentEntity.getIndexName()+"_"+extractKeyFromId(getQuery.getId(), persistentEntity);

    }

    private <T> String extractKeyFromId(String id, ElasticsearchPersistentEntity persistentEntity) {
        int keyCount = persistentEntity.getPartitions().length;
        String[] splittedId = id.split("_");
        splittedId = Arrays.copyOfRange(splittedId, 0, keyCount);
        String key = String.join("_", splittedId);
        return key;
    }


    public <T> String extractPartitionKeyFromObject(T object, ElasticsearchPersistentEntity persistentEntity) {

        String[] keys = new String[persistentEntity.getPartitions().length];
        for (int i = 0; i < persistentEntity.getPartitions().length ; i++) {
            Object fieldValue = null;
            try {
                Method getter = new PropertyDescriptor(persistentEntity.getPartitions()[i], object.getClass()).getReadMethod();
                fieldValue = getter.invoke(object);
                if (fieldValue == null) {
                    throw new ElasticsearchException("impossible to evaluate partition key for field " + persistentEntity.getPartitions()[i]);
                }
                PartitionKey keyFormatter = (PartitionKey) (persistentEntity.getPartitionStrategies()[i].getKeyFormatter().newInstance());
                keys[i] = keyFormatter.getKey(fieldValue, persistentEntity.getPartitionParameters()[i]).toUpperCase();
            } catch (Exception e) {
                throw new ElasticsearchException("impossible to evaluate partition key for field " + persistentEntity.getPartitions()[i], e);
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
            if (!elasticsearchPartitionsCache.indexExists(indexName)) {
                logger.info("creating partitioned index "+indexName);
                elasticsearchPartitionsCache.createIndexPartition(clazz, indexName);
                elasticsearchPartitionsCache.putMapping(indexName, type);
            }
            partitions.add(indexName);
        }
    }
}
