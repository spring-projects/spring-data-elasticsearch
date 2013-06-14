package org.springframework.data.elasticsearch.core.facet.request;

import org.elasticsearch.search.facet.FacetBuilder;
import org.elasticsearch.search.facet.terms.TermsFacetBuilder;
import org.springframework.data.elasticsearch.core.facet.FacetRequest;

import org.elasticsearch.search.facet.FacetBuilder;
import org.springframework.data.elasticsearch.core.facet.FacetRequest;

/**
 * @author Artur Konczak
 */
public class NativeFacetRequest implements FacetRequest {

    private FacetBuilder facet;
    private boolean applyQueryFilter;

    public NativeFacetRequest(FacetBuilder facet) {
        this(facet, false);
    }

    public NativeFacetRequest(FacetBuilder facet, boolean applyQueryFilter) {
        this.facet = facet;
        this.applyQueryFilter = applyQueryFilter;
    }

    @Override
    public FacetBuilder getFacet() {
        return facet;
    }

    @Override
    public boolean applyQueryFilter() {
        return applyQueryFilter;
    }
}
