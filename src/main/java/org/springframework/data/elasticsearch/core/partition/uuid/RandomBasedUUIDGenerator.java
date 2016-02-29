package org.springframework.data.elasticsearch.core.partition.uuid;

import org.elasticsearch.ElasticsearchIllegalStateException;
import org.elasticsearch.common.Base64;

import java.io.IOException;
import java.util.Random;

/**
 * Created by franck.lefebure on 28/02/2016.
 */
public class RandomBasedUUIDGenerator implements UUIDGenerator {
    public RandomBasedUUIDGenerator() {
    }

    public String getBase64UUID() {
        return this.getBase64UUID(SecureRandomHolder.INSTANCE);
    }

    public String getBase64UUID(Random random) {
        byte[] randomBytes = new byte[16];
        random.nextBytes(randomBytes);
        randomBytes[6] = (byte)(randomBytes[6] & 15);
        randomBytes[6] = (byte)(randomBytes[6] | 64);
        randomBytes[8] = (byte)(randomBytes[8] & 63);
        randomBytes[8] = (byte)(randomBytes[8] | 128);

        try {
            byte[] e = Base64.encodeBytesToBytes(randomBytes, 0, randomBytes.length, 16);

            assert e[e.length - 1] == 61;

            assert e[e.length - 2] == 61;

            return new String(e, 0, e.length - 2, Base64.PREFERRED_ENCODING);
        } catch (IOException var4) {
            throw new ElasticsearchIllegalStateException("should not be thrown");
        }
    }
}
