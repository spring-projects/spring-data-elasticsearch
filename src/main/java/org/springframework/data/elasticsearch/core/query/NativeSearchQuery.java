/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.springframework.data.elasticsearch.core.facet.FacetRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * NativeSearchQuery
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Artur Konczak
 */
public class NativeSearchQuery extends AbstractQuery implements SearchQuery {

	private QueryBuilder query;
	private FilterBuilder filter;
	private List<SortBuilder> sorts;
	private List<FacetRequest> facets;
	private List<AbstractAggregationBuilder> aggregations;
	private HighlightBuilder.Field[] highlightFields;
    private IndexBoost[] indicesBoost;


	public NativeSearchQuery(QueryBuilder query) {
		this.query = query;
	}

	public NativeSearchQuery(QueryBuilder query, FilterBuilder filter) {
		this.query = query;
		this.filter = filter;
	}

	public NativeSearchQuery(QueryBuilder query, FilterBuilder filter, List<SortBuilder> sorts) {
		this.query = query;
		this.filter = filter;
		this.sorts = sorts;
	}

	public NativeSearchQuery(QueryBuilder query, FilterBuilder filter, List<SortBuilder> sorts, HighlightBuilder.Field[] highlightFields) {
		this.query = query;
		this.filter = filter;
		this.sorts = sorts;
		this.highlightFields = highlightFields;
	}

	public QueryBuilder getQuery() {
		return query;
	}

	public FilterBuilder getFilter() {
		return filter;
	}

	public List<SortBuilder> getElasticsearchSorts() {
		return sorts;
	}

	@Override
	public HighlightBuilder.Field[] getHighlightFields() {
		return highlightFields;
	}

	public void addFacet(FacetRequest facetRequest) {
		if (facets == null) {
			facets = new ArrayList<FacetRequest>();
		}
		facets.add(facetRequest);
	}

	public void setFacets(List<FacetRequest> facets) {
		this.facets = facets;
	}

	@Override
	public List<FacetRequest> getFacets() {
		return facets;
	}

	@Override
	public List<AbstractAggregationBuilder> getAggregations() {
		return aggregations;
	}


	public void addAggregation(AbstractAggregationBuilder aggregationBuilder) {
		if (aggregations == null) {
			aggregations = new ArrayList<AbstractAggregationBuilder>();
		}
		aggregations.add(aggregationBuilder);
	}

	public void setAggregations(List<AbstractAggregationBuilder> aggregations) {
		this.aggregations = aggregations;
	}

	@Override
    public IndexBoost[] getIndicesBoost() {
        return indicesBoost;
    }

    public void setIndicesBoost(IndexBoost... indicesBoost) {
        this.indicesBoost = indicesBoost;
    }
	
}
