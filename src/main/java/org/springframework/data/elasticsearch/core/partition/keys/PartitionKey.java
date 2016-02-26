package org.springframework.data.elasticsearch.core.partition.keys;

/**
 * Created by franck.lefebure on 24/02/2016.
 */
public interface PartitionKey {

    public String getKey(Object field, String parameter);
}
