package org.springframework.data.elasticsearch.core.mapping.partition;

/**
 * Created by franck.lefebure on 24/02/2016.
 */
public class StringKeyFormatter implements PartitionKeyFormatter {
    @Override
    public String getKey(Object field) {
        return field.toString();
    }
}
