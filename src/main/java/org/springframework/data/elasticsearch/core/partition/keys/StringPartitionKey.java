package org.springframework.data.elasticsearch.core.partition.keys;

/**
 * Created by franck.lefebure on 24/02/2016.
 */
public class StringPartitionKey implements PartitionKey {
    @Override
    public String getKey(Object field) {
        return field.toString();
    }
}
