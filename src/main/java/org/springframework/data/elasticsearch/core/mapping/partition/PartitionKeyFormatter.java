package org.springframework.data.elasticsearch.core.mapping.partition;

/**
 * Created by franck.lefebure on 24/02/2016.
 */
public interface PartitionKeyFormatter {

    public String getKey(Object field);
}
