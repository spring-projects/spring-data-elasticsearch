package org.springframework.data.elasticsearch.core.query;

import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.sort.SortBuilder;

public interface SearchQuery extends Query {
    QueryBuilder getQuery();
    FilterBuilder getFilter();
    SortBuilder getElasticsearchSort();
}
