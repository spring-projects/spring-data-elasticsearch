package org.springframework.data.elasticsearch.core.partition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.ElasticsearchException;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.data.elasticsearch.core.partition.keys.Partition;
import org.springframework.data.elasticsearch.core.partition.uuid.RandomBasedUUIDGenerator;
import org.springframework.data.elasticsearch.core.partition.uuid.UUIDGenerator;
import org.springframework.data.elasticsearch.core.query.*;
import org.springframework.data.mapping.PersistentProperty;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Created by flefebure on 24/02/2016.
 */
public class DefaultElasticsearchPartitioner implements ElasticsearchPartitioner {

    Logger logger = LoggerFactory.getLogger(DefaultElasticsearchPartitioner.class);

    ElasticsearchOperations elasticsearchOperations;

    ElasticsearchPartitionsCache elasticsearchPartitionsCache;

    UUIDGenerator uuidGenerator = new RandomBasedUUIDGenerator();

    public DefaultElasticsearchPartitioner() {
    }

    public void setElasticsearchPartitionsCache(ElasticsearchPartitionsCache elasticsearchPartitionsCache) {
        this.elasticsearchPartitionsCache = elasticsearchPartitionsCache;
    }

    public void setElasticsearchOperations(ElasticsearchOperations elasticsearchOperations) {
        this.elasticsearchOperations = elasticsearchOperations;
    }

    @Override
    public <T> void processPartitioning(Query query, Class<T> clazz) {
        query.getIndices().clear();
        ElasticsearchPersistentEntity persistentEntity = elasticsearchOperations.getElasticsearchConverter().getMappingContext().getPersistentEntity(clazz);
        String indexName = persistentEntity.getIndexName();
        String type = persistentEntity.getIndexType();
        List<String> existingPartitions = elasticsearchPartitionsCache.listIndicesForPrefix(indexName);
        List<Partition> targetedPartitions = null;
        if (query instanceof NativeSearchQuery) {
            targetedPartitions = ((NativeSearchQuery) query).getPartitions();
        }
        else if(query instanceof CriteriaQuery) {
            targetedPartitions = ((CriteriaQuery)query).getPartitions();
        }
        if (targetedPartitions == null || targetedPartitions.isEmpty()) {
            // Full scan
            query.getIndices().addAll(existingPartitions);
        }
        else {
            List<String> slices = Partition.getPartitions(targetedPartitions, persistentEntity.getPartitionersFields(), persistentEntity.getPartitioners(), persistentEntity.getPartitionersParameters(), persistentEntity.getPartitionSeparator());
            List<String> indices = new ArrayList<String>();
            for (String slice : slices) {
                String partition = indexName + persistentEntity.getPartitionSeparator() + slice;
                if (existingPartitions.contains(partition)) {
                    indices.add(partition);
                }
            }
            query.getIndices().addAll(indices);
        }
        // if no partitions match query, add the empty base index, so no errors are triggered
        if (query.getIndices().isEmpty())
            query.getIndices().add(indexName);
    }

    @Override
    public <T> void processPartitioning(IndexQuery indexQuery, Class<T> clazz) {
        if (indexQuery.getObject() == null) {
            throw new ElasticsearchException("Partitioned index is not supported for source queries");
        }
        ElasticsearchPersistentEntity persistentEntity = elasticsearchOperations.getElasticsearchConverter().getMappingContext().getPersistentEntity(clazz);
        String indexName= persistentEntity.getIndexName();
        String sep = persistentEntity.getPartitionSeparator();
        String partitionName;
        // if id contains the separator, it's an update... mmm not so clean
        if (indexQuery.getId() != null && indexQuery.getId().indexOf(sep) >= 0) {
            String partitionKey = extractKeyFromId(indexQuery.getId(), persistentEntity);
            partitionName = persistentEntity.getIndexName()+persistentEntity.getPartitionSeparator()+partitionKey;
        }
        else {
            String partitionKey = extractPartitionKeyFromObject(indexQuery.getObject(), persistentEntity);
            if (indexQuery.getId() == null)
                indexQuery.setId(partitionKey + sep + uuidGenerator.getBase64UUID());
            else if (!indexQuery.getId().startsWith(partitionKey+sep))
                indexQuery.setId(partitionKey+sep+indexQuery.getId());

            setPersistentEntityId(indexQuery.getObject(), indexQuery.getId(), persistentEntity);
            partitionName = persistentEntity.getIndexName()+sep+partitionKey;
            createPartitionIfNotExists(clazz, persistentEntity.getIndexType(), indexName, partitionName);
        }
        indexQuery.setIndexName(partitionName);
    }

    @Override
    public <T> String processPartitioning(GetQuery getQuery, Class<T> clazz) {
        ElasticsearchPersistentEntity persistentEntity =  elasticsearchOperations.getElasticsearchConverter().getMappingContext().getPersistentEntity(clazz);
       return  persistentEntity.getIndexName()+persistentEntity.getPartitionSeparator()+extractKeyFromId(getQuery.getId(), persistentEntity);

    }

    private <T> String extractKeyFromId(String id, ElasticsearchPersistentEntity persistentEntity) {
        int keyCount = persistentEntity.getPartitionersFields().length;
        String sep = persistentEntity.getPartitionSeparator();
        String[] splittedId = id.split(sep);
        splittedId = Arrays.copyOfRange(splittedId, 0, keyCount);
        String key = String.join(sep, splittedId);
        return key;
    }


    public <T> String extractPartitionKeyFromObject(T object, ElasticsearchPersistentEntity persistentEntity) {

        String[] keys = new String[persistentEntity.getPartitionersFields().length];
        for (int i = 0; i < persistentEntity.getPartitionersFields().length ; i++) {
            Object fieldValue = null;
            try {
                Method getter = new PropertyDescriptor(persistentEntity.getPartitionersFields()[i], object.getClass()).getReadMethod();
                fieldValue = getter.invoke(object);
                if (fieldValue == null) {
                    throw new ElasticsearchException("impossible to evaluate partition key for field " + persistentEntity.getPartitionersFields()[i]);
                }
                Partition keyFormatter = (Partition) (persistentEntity.getPartitioners()[i].getImplementation().newInstance());
                keys[i] = keyFormatter.getKeyValue(fieldValue, persistentEntity.getPartitionersParameters()[i]).toLowerCase();
            } catch (Exception e) {
                throw new ElasticsearchException("impossible to evaluate partition key for field " + persistentEntity.getPartitionersFields()[i], e);
            }
        }
        return String.join(persistentEntity.getPartitionSeparator(), keys);
    }

    @Override
    public <T> boolean isIndexPartitioned(T object) {
        if (object == null) return false;
        return isIndexPartitioned(object.getClass());
    }

    @Override
    public <T> boolean isIndexPartitioned(Class<T> clazz) {
        ElasticsearchPersistentEntity persistentEntity =  elasticsearchOperations.getElasticsearchConverter().getMappingContext().getPersistentEntity(clazz);
        return persistentEntity.getPartitionersFields().length > 0;
    }

    @Override
    public <T> void processPartitioning(UpdateQuery updateQuery, Class<T> clazz) {
        ElasticsearchPersistentEntity persistentEntity =  elasticsearchOperations.getElasticsearchConverter().getMappingContext().getPersistentEntity(clazz);
        String[] splittedId = updateQuery.getId().split(persistentEntity.getPartitionSeparator());
        if (splittedId.length == 1) {
            throw new ElasticsearchException("impossible to extract partition from key from document ID");
        }
        String partitionKey = String.join(persistentEntity.getPartitionSeparator(), Arrays.copyOfRange(splittedId, 0, splittedId.length-1));
        updateQuery.setIndexName(updateQuery.getIndexName()+persistentEntity.getPartitionSeparator()+partitionKey);

    }

    private <T> void createPartitionIfNotExists(Class<T> clazz, String type, String indexName, String partitionName) {
        List<String> partitions = elasticsearchPartitionsCache.listIndicesForPrefix(indexName);
        if (!partitions.contains(partitionName)) {
            elasticsearchPartitionsCache.createPartition(partitionName, clazz);

        }
        List<String> types = elasticsearchPartitionsCache.listTypesForPartition(partitionName);
        if (!types.contains(type)) {
            elasticsearchPartitionsCache.putMapping(partitionName, clazz);
        }
    }
    private void setPersistentEntityId(Object entity, String id, ElasticsearchPersistentEntity persistentEntity) {
        PersistentProperty idProperty = persistentEntity.getIdProperty();
        // Only deal with String because ES generated Ids are strings !
        if (idProperty != null && idProperty.getType().isAssignableFrom(String.class)) {
            Method setter = idProperty.getSetter();
            if (setter != null) {
                try {
                    setter.invoke(entity, id);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }
    }
}
