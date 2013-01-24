package org.springframework.data.elasticsearch.core.query;


import org.elasticsearch.index.query.QueryBuilder;

public class DeleteQuery{

    private QueryBuilder elasticsearchQuery;

    public QueryBuilder getElasticsearchQuery() {
        return elasticsearchQuery;
    }

    public void setElasticsearchQuery(QueryBuilder elasticsearchQuery) {
        this.elasticsearchQuery = elasticsearchQuery;
    }
}
