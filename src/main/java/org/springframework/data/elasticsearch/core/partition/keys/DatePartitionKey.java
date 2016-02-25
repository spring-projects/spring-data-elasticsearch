package org.springframework.data.elasticsearch.core.partition.keys;

import java.text.SimpleDateFormat;

/**
 * Created by franck.lefebure on 24/02/2016.
 */
public abstract class DatePartitionKey implements PartitionKey {

    protected abstract SimpleDateFormat getSimpleDateFormat();

    @Override
    public String getKey(Object field) {
        return getSimpleDateFormat().format(field);
    }

}
