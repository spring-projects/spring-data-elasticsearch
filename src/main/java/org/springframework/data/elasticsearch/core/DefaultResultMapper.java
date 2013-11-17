package org.springframework.data.elasticsearch.core;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.facet.Facet;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.facet.DefaultFacetMapper;
import org.springframework.data.elasticsearch.core.facet.FacetResult;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Artur Konczak
 */
public class DefaultResultMapper extends AbstractResultMapper {

    public DefaultResultMapper(){
        super(new DefaultEntityMapper());
    }

    public DefaultResultMapper(EntityMapper entityMapper) {
        super(entityMapper);
    }

    @Override
    public <T> FacetedPage<T> mapResults(SearchResponse response, Class<T> clazz, Pageable pageable) {
        long totalHits = response.getHits().totalHits();
        List<T> results = new ArrayList<T>();
        for (SearchHit hit : response.getHits()) {
            if (hit != null) {
                results.add(mapEntity(hit.sourceAsString(), clazz));
            }
        }
        List<FacetResult> facets = new ArrayList<FacetResult>();
        if (response.getFacets() != null) {
            for (Facet facet : response.getFacets()) {
                FacetResult facetResult = DefaultFacetMapper.parse(facet);
                if (facetResult != null) {
                    facets.add(facetResult);
                }
            }
        }

        return new FacetedPageImpl<T>(results, pageable, totalHits, facets);
    }

    @Override
    public <T> T mapResult(GetResponse response, Class<T> clazz) {
        return mapEntity(response.getSourceAsString(),clazz);
    }
}
