package org.springframework.data.elasticsearch.core;

import org.elasticsearch.action.search.SearchResponse;
import org.springframework.data.domain.Pageable;

/**
 * @author Artur Konczak
 */
public interface SearchResultMapper {

    <T> FacetedPage<T> mapResults(SearchResponse response, Class<T> clazz, Pageable pageable);

}
