package org.springframework.data.elasticsearch.core.partition;

import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.query.GetQuery;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;

/**
 * Created by franck.lefebure on 24/02/2016.
 */
public interface ElasticsearchPartitioner {

    public <T> void processPartitioning(IndexQuery indexQuery, Class<T> clazz);

    public <T> void processPartitioning(UpdateQuery updateQuery, Class<T> clazz);

    public <T> void processPartitioning(Query query, Class<T> clazz);

    public <T> String processPartitioning(GetQuery indexQuery, Class<T> clazz);

    public <T> String extractPartitionKeyFromObject(T object, ElasticsearchPersistentEntity persistentEntity);

    public <T> boolean isIndexPartitioned(T object);

    public <T> boolean isIndexPartitioned(Class<T> clazz);
}
