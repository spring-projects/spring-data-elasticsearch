package org.springframework.data.elasticsearch.core.query;


import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.QueryBuilder;

public class SearchQuery extends AbstractQuery{

    private QueryBuilder elasticsearchQuery;
    private FilterBuilder elasticsearchFilter;

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

}
