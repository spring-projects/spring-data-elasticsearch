package org.springframework.data.elasticsearch.core.facet;

/**
 * Single term
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Artur Konczak
 * @author Jonathan Yan
 */
public class Term {

    private String term;
    private int count;

    public Term(String term, int count) {
        this.term = term;
        this.count = count;
    }

    public String getTerm() {
        return term;
    }

    public int getCount() {
        return count;
    }
}
