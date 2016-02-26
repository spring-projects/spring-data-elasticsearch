package org.springframework.data.elasticsearch.annotations;

import org.springframework.data.elasticsearch.core.partition.keys.*;

import java.text.SimpleDateFormat;

/**
 * Created by franck.lefebure on 24/02/2016.
 */
public enum PartitionStrategy {


    fixed_string(StringPartitionKey.class),
    long_range(LongPartitionKey.class),
    date_range(DatePartitionKey.class);


    Class keyFormatter;

    public Class getKeyFormatter() {
        return keyFormatter;
    }

    PartitionStrategy(Class keyFormatter) {
        this.keyFormatter = keyFormatter;
    }
}
