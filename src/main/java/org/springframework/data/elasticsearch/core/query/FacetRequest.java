package org.springframework.data.elasticsearch.core.query;

import org.elasticsearch.search.facet.FacetBuilder;

/**
 * @author Artur Koczak
 */
public interface FacetRequest {

    public static final String FIELD_UNTOUCHED = "untouched";
    public static final String FIELD_SORT = "sort";

    FacetBuilder getFacet();

    boolean applyQueryFilter();

}
