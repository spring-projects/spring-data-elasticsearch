/*
* Copyright 2013 the original author or authors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.springframework.data.elasticsearch.core.query;


import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.sort.SortBuilder;

/**
 * SearchQuery
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 */
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
