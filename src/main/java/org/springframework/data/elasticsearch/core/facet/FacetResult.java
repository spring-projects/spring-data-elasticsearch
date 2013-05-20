package org.springframework.data.elasticsearch.core.facet;

/**
 * Generic interface for all facets
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Artur Konczak
 * @author Jonathan Yan
 *
 */
public interface FacetResult {

    String getName();

    FacetType getType();

}
