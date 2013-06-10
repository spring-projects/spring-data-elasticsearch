package org.springframework.data.elasticsearch.core.facet.request;

import org.springframework.data.elasticsearch.core.facet.FacetRequest;

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

    public TermFacetRequestBuilder fields(String... fields) {
        result.setFields(fields);
        return this;
    }

    public TermFacetRequestBuilder size(int size) {
        result.setSize(size);
        return this;
    }

    public TermFacetRequestBuilder excludeTerms(Object... terms) {
        result.setExcludeTerms(terms);
        return this;
    }

    public TermFacetRequestBuilder allTerms() {
        result.setAllTerms(true);
        return this;
    }

    public TermFacetRequestBuilder regex(String regex) {
        result.setRegex(regex);
        return this;
    }

    public TermFacetRequestBuilder regex(String regex, int regexFlag) {
        result.setRegex(regex, regexFlag);
        return this;
    }

    public TermFacetRequestBuilder ascTerm() {
        result.setOrder(TermFacetOrder.ascTerm);
        return this;
    }

    public TermFacetRequestBuilder descTerm() {
        result.setOrder(TermFacetOrder.descTerm);
        return this;
    }

    public TermFacetRequestBuilder ascCount() {
        result.setOrder(TermFacetOrder.ascCount);
        return this;
    }

    public TermFacetRequestBuilder descCount() {
        result.setOrder(TermFacetOrder.descCount);
        return this;
    }

    public TermFacetRequestBuilder applyQueryFilter() {
        result.setApplyQueryFilter(true);
        return this;
    }

    public FacetRequest build() {
        return result;
    }
}
