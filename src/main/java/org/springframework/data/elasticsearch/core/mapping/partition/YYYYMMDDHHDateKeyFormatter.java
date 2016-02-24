package org.springframework.data.elasticsearch.core.mapping.partition;

import java.text.SimpleDateFormat;

/**
 * Created by franck.lefebure on 24/02/2016.
 */
public class YYYYMMDDHHDateKeyFormatter extends DateKeyFormatter implements PartitionKeyFormatter {
    static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("YYYYMMDDHH");

    @Override
    protected SimpleDateFormat getSimpleDateFormat() {
        return simpleDateFormat;
    }
}
