package org.springframework.data.elasticsearch.core.facet.request;

import org.springframework.data.elasticsearch.core.facet.FacetRequest;

/**
 * Basic range facet
 *
 * @author Artur Konczak
 */
public class RangeFacetRequestBuilder {

    RangeFacetRequest result;

    public RangeFacetRequestBuilder(String name) {
        result = new RangeFacetRequest(name);
    }

    public RangeFacetRequestBuilder field(String field) {
        result.setField(field);
        return this;
    }

    public RangeFacetRequestBuilder fields(String keyField, String valueField) {
        result.setFields(keyField, valueField);
        return this;
    }


    public RangeFacetRequestBuilder range(double from, double to) {
        result.range(from, to);
        return this;
    }

    public RangeFacetRequestBuilder from(double from) {
        result.range(from, null);
        return this;
    }

    public RangeFacetRequestBuilder to(double to) {
        result.range(null, to);
        return this;
    }

    public RangeFacetRequestBuilder applyQueryFilter() {
        result.setApplyQueryFilter(true);
        return this;
    }

    public FacetRequest build() {
        return result;
    }
}
