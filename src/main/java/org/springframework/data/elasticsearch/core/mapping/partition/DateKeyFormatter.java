package org.springframework.data.elasticsearch.core.mapping.partition;

import java.text.SimpleDateFormat;

/**
 * Created by franck.lefebure on 24/02/2016.
 */
public abstract class DateKeyFormatter implements PartitionKeyFormatter {

    protected abstract SimpleDateFormat getSimpleDateFormat();

    @Override
    public String getKey(Object field) {
        return getSimpleDateFormat().format(field);
    }

}
