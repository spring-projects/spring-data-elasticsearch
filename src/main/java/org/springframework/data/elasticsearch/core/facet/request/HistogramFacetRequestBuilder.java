package org.springframework.data.elasticsearch.core.facet.request;

import org.springframework.data.elasticsearch.core.facet.FacetRequest;

import java.util.concurrent.TimeUnit;

/**
 * @author Artur Konczak
 */
public class HistogramFacetRequestBuilder {

    HistogramFacetRequest result;

    public HistogramFacetRequestBuilder(String name) {
        result = new HistogramFacetRequest(name);
    }

    public HistogramFacetRequestBuilder field(String field) {
        result.setField(field);
        return this;
    }

    public HistogramFacetRequestBuilder interval(long interval) {
        result.setInterval(interval);
        return this;
    }

    public HistogramFacetRequestBuilder timeUnit(TimeUnit timeUnit) {
        result.setTimeUnit(timeUnit);
        return this;
    }

    public FacetRequest build() {
        return result;
    }

    public HistogramFacetRequestBuilder applyQueryFilter() {
        result.setApplyQueryFilter(true);
        return this;
    }
}
