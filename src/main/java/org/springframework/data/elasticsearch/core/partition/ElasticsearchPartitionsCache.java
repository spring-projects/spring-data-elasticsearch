package org.springframework.data.elasticsearch.core.partition;

import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by franck.lefebure on 25/02/2016.
 */
public interface ElasticsearchPartitionsCache {

    public List<String> listIndicesForPrefix(String prefix);
    public List<String> listTypesForPartition(String partition);
    public <T> void createPartition(String partition, Class<T> clazz);
    public <T> void putMapping(String partition, Class<T> clazz);
}
