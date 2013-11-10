package org.springframework.data.elasticsearch.core;

import java.io.IOException;

/**
 * @author Artur Konczak
 */
public class CustomEntityMapper implements EntityMapper {

    public CustomEntityMapper(){
        //custom configuration/implementation (e.g. FasterXML/jackson)
    }

    @Override
    public String mapToString(Object object) throws IOException {
        //mapping Object to String
        return null;
    }

    @Override
    public <T> T mapToObject(String source, Class<T> clazz) throws IOException {
        //mapping String to Object
        return null;
    }
}
