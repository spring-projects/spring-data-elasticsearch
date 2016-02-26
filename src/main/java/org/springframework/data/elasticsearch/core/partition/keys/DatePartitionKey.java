package org.springframework.data.elasticsearch.core.partition.keys;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by franck.lefebure on 24/02/2016.
 */
public abstract class DatePartitionKey implements PartitionKey {

    @Override
    public String getKey(Object field, String parameter) {
        Date date = (Date)field;
        SimpleDateFormat sdf = new SimpleDateFormat(parameter);
        return sdf.format(field);
    }

}
