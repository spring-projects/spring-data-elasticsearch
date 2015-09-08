package org.springframework.data.elasticsearch.core;

import org.elasticsearch.action.search.SearchResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PageSearchResultMapper<T,R> {

    Page<R> mapResults(SearchResponse response, Class<T> clazz, Pageable pageable);
}
