package org.springframework.data.elasticsearch.core;

import org.springframework.data.elasticsearch.ElasticsearchException;

import java.io.IOException;

import static org.apache.commons.lang.StringUtils.isBlank;

/**
 * @author Artur Konczak
 */
public abstract class AbstractResultMapper implements ResultsMapper {

    private EntityMapper entityMapper;

    public AbstractResultMapper(EntityMapper entityMapper) {
        this.entityMapper = entityMapper;
    }

    public <T> T mapEntity(String source, Class<T> clazz) {
        if (isBlank(source)) {
            return null;
        }
        try {
            return entityMapper.mapToObject(source, clazz);
        } catch (IOException e) {
            throw new ElasticsearchException("failed to map source [ " + source + "] to class " + clazz.getSimpleName(), e);
        }
    }

    @Override
    public EntityMapper getEntityMapper() {
        return this.entityMapper;
    }
}
