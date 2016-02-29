package org.springframework.data.elasticsearch.core.partition.uuid;

/**
 * Created by franck.lefebure on 28/02/2016.
 */
public interface UUIDGenerator {
    String getBase64UUID();
}
