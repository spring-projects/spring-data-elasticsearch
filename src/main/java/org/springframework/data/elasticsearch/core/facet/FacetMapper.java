package org.springframework.data.elasticsearch.core.facet;

import org.elasticsearch.search.facet.Facet;
import org.elasticsearch.search.facet.range.RangeFacet;
import org.elasticsearch.search.facet.terms.TermsFacet;
import org.springframework.data.elasticsearch.core.facet.result.Range;
import org.springframework.data.elasticsearch.core.facet.result.RangeResult;
import org.springframework.data.elasticsearch.core.facet.result.Term;
import org.springframework.data.elasticsearch.core.facet.result.TermResult;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Artur Konczak
 */
public class FacetMapper {

    public static FacetResult parse(Facet facet) {
        if (facet instanceof TermsFacet) {
            return parseTerm((TermsFacet) facet);
        }

        if (facet instanceof RangeFacet) {
            return parseRange((RangeFacet) facet);
        }

        return null;
    }

    private static FacetResult parseTerm(TermsFacet facet) {
        List<Term> entries = new ArrayList<Term>();
        for (TermsFacet.Entry entry : facet.getEntries()) {
            entries.add(new Term(entry.getTerm().toString(), entry.getCount()));
        }
        return new TermResult(facet.getName(), entries);
    }

    private static FacetResult parseRange(RangeFacet facet) {
        List<Range> entries = new ArrayList<Range>();
        for (RangeFacet.Entry entry : facet.getEntries()) {
            entries.add(new Range(entry.getFrom() == Double.NEGATIVE_INFINITY ? null : entry.getFrom(), entry.getTo() == Double.POSITIVE_INFINITY ? null : entry.getTo(), entry.getCount(), entry.getTotal()));
        }
        return new RangeResult(facet.getName(), entries);
    }

}
