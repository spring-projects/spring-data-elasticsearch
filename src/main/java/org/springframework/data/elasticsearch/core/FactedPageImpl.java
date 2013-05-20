package org.springframework.data.elasticsearch.core;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.facet.FacetResult;

import java.util.List;
import java.util.Map;

/**
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Artur Konczak
 * @author Jonathan Yan
 */
public class FactedPageImpl<T> extends PageImpl<T> implements FacetedPage<T> {

    private List<FacetResult> facets;
    private Map<String, FacetResult> mapOfFacets;

    public FactedPageImpl(List<T> content) {
        super(content);
    }

    public FactedPageImpl(List<T> content, Pageable pageable, long total) {
        super(content, pageable, total);
    }

    public FactedPageImpl(List<T> content, Pageable pageable, long total, List<FacetResult> facets) {
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
