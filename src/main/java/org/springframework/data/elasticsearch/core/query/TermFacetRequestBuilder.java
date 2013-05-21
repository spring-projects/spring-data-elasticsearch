package org.springframework.data.elasticsearch.core.query;

/**
 * Basic term facet
 *
 * @author Artur Konczak
 */
public class TermFacetRequestBuilder {

    private TermFacetRequest result;

    public TermFacetRequestBuilder(String name) {
        result = new TermFacetRequest(name);
    }

    public TermFacetRequestBuilder withFields(String... fields) {
        result.setFields(fields);
        return this;
    }

    public TermFacetRequestBuilder withSize(int size) {
        result.setSize(size);
        return this;
    }

    public TermFacetRequestBuilder ascTerm() {
        result.ascTerm();
        return this;
    }

    public TermFacetRequestBuilder descTerm() {
        result.descTerm();
        return this;
    }

    public TermFacetRequestBuilder ascCount() {
        result.ascCount();
        return this;
    }

    public TermFacetRequestBuilder descCount() {
        result.descCount();
        return this;
    }

    public TermFacetRequestBuilder applyQueryFilter() {
        result.setApplyQueryFilter(true);
        return this;
    }

    public TermFacetRequest build() {
        return result;
    }
}
