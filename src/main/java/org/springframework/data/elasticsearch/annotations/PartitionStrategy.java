package org.springframework.data.elasticsearch.annotations;

import org.springframework.data.elasticsearch.core.partition.keys.*;

import java.text.SimpleDateFormat;

/**
 * Created by franck.lefebure on 24/02/2016.
 */
public enum PartitionStrategy {


    string_fixed (StringPartitionKey.class, null),
    long_1E6(LongPartitionKey.class, 1e6),
    long_1E7(LongPartitionKey.class, 1e7),
    long_1E8(LongPartitionKey.class, 1e8),
    long_1E9(LongPartitionKey.class, 1e9),
    long_1E10(LongPartitionKey.class, 1e10),
    long_1E11(LongPartitionKey.class, 1e12),
    long_1E12(LongPartitionKey.class, 1e12),
    date_YYYYMMDDHH(DatePartitionKey.class, "YYYYMMDDHH"),
    date_YYYYMMDD(DatePartitionKey.class, "YYYYMMDD"),
    date_YYYYMM(DatePartitionKey.class, "YYYYMM"),
    date_YYYY(DatePartitionKey.class, "YYYY");


    Class keyFormatter;
    Object slicer;

    public Object getSlicer() {
        return slicer;
    }

    public Class getKeyFormatter() {
        return keyFormatter;
    }

    PartitionStrategy(Class keyFormatter, Object slice) {
        this.keyFormatter = keyFormatter;
        this.slicer = slice;
    }
}
