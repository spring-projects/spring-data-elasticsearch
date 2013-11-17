package org.springframework.data.elasticsearch.core;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.springframework.data.domain.Pageable;

/**
 * @author Artur Konczak
 */
public class CustomResultMapper implements ResultsMapper{


    private EntityMapper entityMapper;

    public CustomResultMapper(EntityMapper entityMapper) {
        this.entityMapper = entityMapper;
    }

    @Override
    public EntityMapper getEntityMapper() {
        return entityMapper;
    }

    @Override
    public <T> T mapResult(GetResponse response, Class<T> clazz) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public <T> FacetedPage<T> mapResults(SearchResponse response, Class<T> clazz, Pageable pageable) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
