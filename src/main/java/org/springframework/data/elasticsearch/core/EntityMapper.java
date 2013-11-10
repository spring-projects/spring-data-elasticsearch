package org.springframework.data.elasticsearch.core;

import java.io.IOException;

/**
 * DocumentMapper interface, it will allow to customize how we mapping object to json
 *
 * @author Artur Konczak
 * @author Rizwan Idrees
 * @author Mohsin Husen
 */
public interface EntityMapper {

    public String mapToString(Object object) throws IOException;

    public <T> T mapToObject(String source, Class<T> clazz) throws IOException;
}
