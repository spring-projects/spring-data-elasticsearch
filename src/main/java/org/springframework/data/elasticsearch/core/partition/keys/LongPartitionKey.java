package org.springframework.data.elasticsearch.core.partition.keys;

/**
 * Created by franck.lefebure on 24/02/2016.
 */
public class LongPartitionKey implements PartitionKey {
    @Override
    public String getKey(Object field, String parameter) {
        String s = field.toString();
        long key = Long.parseLong(s);
        long slice = Long.parseLong(parameter);
        return new Long(key/slice*slice).toString();
    }
}
