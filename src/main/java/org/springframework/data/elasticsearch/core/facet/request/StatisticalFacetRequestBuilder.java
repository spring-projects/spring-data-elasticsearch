package org.springframework.data.elasticsearch.core.facet.request;

import org.springframework.data.elasticsearch.core.facet.FacetRequest;

/**
 * @author Petar Tahchiev
 */
public class StatisticalFacetRequestBuilder {

    StatisticalFacetRequest result;

    public StatisticalFacetRequestBuilder(String name) {
        result = new StatisticalFacetRequest(name);
    }

    public StatisticalFacetRequestBuilder field(String field) {
        result.setField(field);
        return this;
    }

    public StatisticalFacetRequestBuilder fields(String... fields) {
        result.setFields(fields);
        return this;
    }

    public StatisticalFacetRequestBuilder applyQueryFilter() {
        result.setApplyQueryFilter(true);
        return this;
    }

    public FacetRequest build() {
        return result;
    }
}
