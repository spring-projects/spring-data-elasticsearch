package org.springframework.data.elasticsearch.core.partition.uuid;

import java.security.SecureRandom;

/**
 * Created by franck.lefebure on 28/02/2016.
 */
public class SecureRandomHolder {
    public static final SecureRandom INSTANCE = new SecureRandom();

    SecureRandomHolder() {
    }
}
