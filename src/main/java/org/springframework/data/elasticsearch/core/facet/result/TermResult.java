package org.springframework.data.elasticsearch.core.facet.result;

import org.springframework.data.elasticsearch.core.facet.AbstactFacetResult;
import org.springframework.data.elasticsearch.core.facet.FacetType;

import java.util.List;

/**
 * Basic term facet result
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Artur Konczak
 * @author Jonathan Yan
 */
public class TermResult extends AbstactFacetResult {

    private List<Term> terms;

    public TermResult(String name, List<Term> terms) {
        super(name, FacetType.term);
        this.terms = terms;
    }

    public List<Term> getTerms() {
        return terms;
    }

}
