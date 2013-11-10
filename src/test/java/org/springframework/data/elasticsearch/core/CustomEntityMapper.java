package org.springframework.data.elasticsearch.core;

import java.io.IOException;

/**
 * @author Artur Konczak
 */
public class CustomEntityMapper implements EntityMapper {

    @Override
    public String mapToString(Object object) throws IOException {
        return null;
    }

    @Override
    public <T> T mapToObject(String source, Class<T> clazz) throws IOException {
        return null;
    }
}
