package org.springframework.data.elasticsearch.core.facet;

import org.elasticsearch.search.facet.Facet;
import org.elasticsearch.search.facet.terms.TermsFacet;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Artur Konczak
 */
public class FacetMapper {

    public static FacetResult parse(Facet facet){
        if(facet instanceof TermsFacet){
            return parseTerm((TermsFacet) facet);
        }
        return null;
    }

    private static FacetResult parseTerm(TermsFacet facet) {
        List<Term> terms = new ArrayList<Term>();
        for(TermsFacet.Entry entry:facet.getEntries()){
            terms.add(new Term(entry.getTerm().toString(),entry.getCount()));
        }
        return new TermResult(facet.getName(),terms);
    }

}
