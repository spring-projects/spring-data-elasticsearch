package org.springframework.data.elasticsearch.core;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * DocumentMapper using jackson
 *
 * @author Artur Konczak
 * @author Petar Tahchiev
 */
public class DefaultEntityMapper implements EntityMapper {

    private ObjectMapper objectMapper;

    public DefaultEntityMapper() {
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public String mapToString(Object object) throws IOException {
        return objectMapper.writeValueAsString(object);
    }

    @Override
    public <T> T mapToObject(String source, Class<T> clazz) throws IOException {
        return objectMapper.readValue(source, clazz);
    }
}
