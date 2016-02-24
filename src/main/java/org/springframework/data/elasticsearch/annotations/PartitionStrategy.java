package org.springframework.data.elasticsearch.annotations;

import org.springframework.data.elasticsearch.core.mapping.partition.*;

/**
 * Created by franck.lefebure on 24/02/2016.
 */
public enum PartitionStrategy {
    STRING(StringKeyFormatter.class),
    LONG(StringKeyFormatter.class),
    INTEGER(StringKeyFormatter.class),
    DATE_YYYYMMDDHH(YYYYMMDDHHDateKeyFormatter.class),
    DATE_YYYYMMDD(YYYYMMDDDateKeyFormatter.class),
    DATE_YYYYMMD(YYYYMMDDateKeyFormatter.class),
    DATE_YYYYMM(YYYYMMDateKeyFormatter.class),
    DATE_YYYYM(YYYYMMDateKeyFormatter.class),
    DATE_YYY(YYYYDateKeyFormatter.class);

    Class keyFformatter;

    public Class getKeyFformatter() {
        return keyFformatter;
    }

    PartitionStrategy(Class keyFformatter) {
        this.keyFformatter = keyFformatter;
    }
}
