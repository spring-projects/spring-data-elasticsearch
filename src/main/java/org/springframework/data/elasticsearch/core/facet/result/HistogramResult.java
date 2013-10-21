package org.springframework.data.elasticsearch.core.facet.result;

import org.springframework.data.elasticsearch.core.facet.AbstactFacetResult;
import org.springframework.data.elasticsearch.core.facet.FacetType;

import java.util.List;

/**
 * @author Artur Konczak
 */
public class HistogramResult extends AbstactFacetResult {

    private List<IntervalUnit> terms;

    public HistogramResult(String name, List<IntervalUnit> terms) {
        super(name, FacetType.term);
        this.terms = terms;
    }

    public List<IntervalUnit> getIntervalUnit() {
        return terms;
    }

}
