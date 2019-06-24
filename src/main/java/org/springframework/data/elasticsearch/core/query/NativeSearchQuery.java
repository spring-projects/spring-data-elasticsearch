/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.elasticsearch.core.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.collapse.CollapseBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.springframework.data.elasticsearch.core.facet.FacetRequest;

/**
 * NativeSearchQuery
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Artur Konczak
 * @author Jean-Baptiste Nizet
 * @author Martin Choraine
 */
public class NativeSearchQuery extends AbstractQuery implements SearchQuery {

	private QueryBuilder query;
	private QueryBuilder filter;
	private List<SortBuilder> sorts;
	private final List<ScriptField> scriptFields = new ArrayList<>();
	private CollapseBuilder collapseBuilder;
	private List<FacetRequest> facets;
	private List<AbstractAggregationBuilder> aggregations;
	private HighlightBuilder highlightBuilder;
	private HighlightBuilder.Field[] highlightFields;
	private List<IndexBoost> indicesBoost;

	public NativeSearchQuery(QueryBuilder query) {

		this.query = query;
	}

	public NativeSearchQuery(QueryBuilder query, QueryBuilder filter) {

		this.query = query;
		this.filter = filter;
	}

	public NativeSearchQuery(QueryBuilder query, QueryBuilder filter, List<SortBuilder> sorts) {

		this.query = query;
		this.filter = filter;
		this.sorts = sorts;
	}

	public NativeSearchQuery(QueryBuilder query, QueryBuilder filter, List<SortBuilder> sorts,
			HighlightBuilder.Field[] highlightFields) {

		this.query = query;
		this.filter = filter;
		this.sorts = sorts;
		this.highlightFields = highlightFields;
	}

	public NativeSearchQuery(QueryBuilder query, QueryBuilder filter, List<SortBuilder> sorts,
			HighlightBuilder highlighBuilder, HighlightBuilder.Field[] highlightFields) {

		this.query = query;
		this.filter = filter;
		this.sorts = sorts;
		this.highlightBuilder = highlighBuilder;
		this.highlightFields = highlightFields;
	}

	public QueryBuilder getQuery() {
		return query;
	}

	public QueryBuilder getFilter() {
		return filter;
	}

	public List<SortBuilder> getElasticsearchSorts() {
		return sorts;
	}

	@Override
	public HighlightBuilder getHighlightBuilder() {
		return highlightBuilder;
	}

	@Override
	public HighlightBuilder.Field[] getHighlightFields() {
		return highlightFields;
	}

	@Override
	public List<ScriptField> getScriptFields() {
		return scriptFields;
	}

	public void setScriptFields(List<ScriptField> scriptFields) {
		this.scriptFields.addAll(scriptFields);
	}

	public void addScriptField(ScriptField... scriptField) {
		scriptFields.addAll(Arrays.asList(scriptField));
	}

	@Override
	public CollapseBuilder getCollapseBuilder() {
		return collapseBuilder;
	}

	public void setCollapseBuilder(CollapseBuilder collapseBuilder) {
		this.collapseBuilder = collapseBuilder;
	}

	public void addFacet(FacetRequest facetRequest) {

		if (facets == null) {
			facets = new ArrayList<>();
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
			aggregations = new ArrayList<>();
		}

		aggregations.add(aggregationBuilder);
	}

	public void setAggregations(List<AbstractAggregationBuilder> aggregations) {
		this.aggregations = aggregations;
	}

	@Override
	public List<IndexBoost> getIndicesBoost() {
		return indicesBoost;
	}

	public void setIndicesBoost(List<IndexBoost> indicesBoost) {
		this.indicesBoost = indicesBoost;
	}

}
