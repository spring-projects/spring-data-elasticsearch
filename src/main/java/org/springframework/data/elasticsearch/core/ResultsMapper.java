package org.springframework.data.elasticsearch.core;

import org.elasticsearch.action.search.SearchResponse;
import org.springframework.data.domain.Page;


public interface ResultsMapper<T> {

    Page<T> mapResults(SearchResponse response);

}
