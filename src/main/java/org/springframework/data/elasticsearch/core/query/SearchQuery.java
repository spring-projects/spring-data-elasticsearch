package org.springframework.data.elasticsearch.core.query;


import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.sort.SortBuilder;

public class SearchQuery extends AbstractQuery{

    private QueryBuilder elasticsearchQuery;
    private FilterBuilder elasticsearchFilter;
    private SortBuilder elasticsearchSort;

    public QueryBuilder getElasticsearchQuery() {
        return elasticsearchQuery;
    }

    public void setElasticsearchQuery(QueryBuilder elasticsearchQuery) {
        this.elasticsearchQuery = elasticsearchQuery;
    }

    public FilterBuilder getElasticsearchFilter() {
        return elasticsearchFilter;
    }

    public void setElasticsearchFilter(FilterBuilder elasticsearchFilter) {
        this.elasticsearchFilter = elasticsearchFilter;
    }

    public SortBuilder getElasticsearchSort() {
        return elasticsearchSort;
    }

    public void setElasticsearchSort(SortBuilder elasticsearchSort) {
        this.elasticsearchSort = elasticsearchSort;
    }
}
