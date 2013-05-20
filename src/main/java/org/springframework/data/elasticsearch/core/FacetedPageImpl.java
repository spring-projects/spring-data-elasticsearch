package org.springframework.data.elasticsearch.core;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.facet.FacetResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Container for query result and facet results
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Artur Konczak
 * @author Jonathan Yan
 */
public class FacetedPageImpl<T> extends PageImpl<T> implements FacetedPage<T> {

    private List<FacetResult> facets;
    private Map<String, FacetResult> mapOfFacets = new HashMap<String, FacetResult>();

    public FacetedPageImpl(List<T> content) {
        super(content);
    }

    public FacetedPageImpl(List<T> content, Pageable pageable, long total) {
        super(content, pageable, total);
    }

    public FacetedPageImpl(List<T> content, Pageable pageable, long total, List<FacetResult> facets) {
        super(content, pageable, total);
        this.facets = facets;
        for (FacetResult facet : facets) {
            mapOfFacets.put(facet.getName(), facet);
        }
    }

    @Override
    public boolean hasFacets() {
        return CollectionUtils.isNotEmpty(facets);
    }

    @Override
    public List<FacetResult> getFacets() {
        return facets;
    }

    @Override
    public FacetResult getFacet(String name) {
        return mapOfFacets.get(name);
    }
}
