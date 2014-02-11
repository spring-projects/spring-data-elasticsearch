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

import java.util.ArrayList;
import java.util.List;

import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.springframework.data.elasticsearch.core.facet.FacetRequest;

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
	private SortBuilder sort;
	private List<FacetRequest> facets;
	private HighlightBuilder.Field[] highlightFields;


	public NativeSearchQuery(QueryBuilder query) {
		this.query = query;
	}

	public NativeSearchQuery(QueryBuilder query, FilterBuilder filter) {
		this.query = query;
		this.filter = filter;
	}

	public NativeSearchQuery(QueryBuilder query, FilterBuilder filter, SortBuilder sort) {
		this.query = query;
		this.filter = filter;
		this.sort = sort;
	}

	public NativeSearchQuery(QueryBuilder query, FilterBuilder filter, SortBuilder sort, HighlightBuilder.Field[] highlightFields) {
		this.query = query;
		this.filter = filter;
		this.sort = sort;
		this.highlightFields = highlightFields;
	}

	public QueryBuilder getQuery() {
		return query;
	}

	public FilterBuilder getFilter() {
		return filter;
	}

	public SortBuilder getElasticsearchSort() {
		return sort;
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
}
