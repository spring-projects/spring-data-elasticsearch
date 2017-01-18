/*
 * Copyright 2013-2016 the original author or authors.
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

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.springframework.data.elasticsearch.core.facet.FacetRequest;

import java.util.Arrays;

import static org.springframework.util.ObjectUtils.nullSafeEquals;
import static org.springframework.util.ObjectUtils.nullSafeHashCode;

/**
 * NativeSearchQuery
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Artur Konczak
 */
public class NativeSearchQuery extends AbstractQuery implements SearchQuery {

	private QueryBuilder query;
	private QueryBuilder filter;
	private List<SortBuilder> sorts;
    private final List<ScriptField> scriptFields = new ArrayList<ScriptField>();
	private List<FacetRequest> facets;
	private List<AbstractAggregationBuilder> aggregations;
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

	public NativeSearchQuery(QueryBuilder query, QueryBuilder filter, List<SortBuilder> sorts, HighlightBuilder.Field[] highlightFields) {
		this.query = query;
		this.filter = filter;
		this.sorts = sorts;
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
	public HighlightBuilder.Field[] getHighlightFields() {
		return highlightFields;
	}

    @Override
    public List<ScriptField> getScriptFields() { return scriptFields; }

    public void setScriptFields(List<ScriptField> scriptFields) {
        this.scriptFields.addAll(scriptFields);
    }

    public void addScriptField(ScriptField... scriptField) {
        scriptFields.addAll(Arrays.asList(scriptField));
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
	public List<IndexBoost> getIndicesBoost() {
		return indicesBoost;
	}

	public void setIndicesBoost(List<IndexBoost> indicesBoost) {
		this.indicesBoost = indicesBoost;
	}

	public boolean equals(Object obj) {

		if (this == obj) {
			return true;
		}

		if (obj == null || !getClass().equals(obj.getClass())) {
			return false;
		}

		return querySettingsEquals((NativeSearchQuery) obj);
	}

	protected boolean querySettingsEquals(NativeSearchQuery that) {
		boolean queryEqual = nullSafeEquals(this.query, that.query);
		boolean filterEqual = nullSafeEquals(this.filter, that.filter);
		boolean sortsEqual = nullSafeEquals(this.sorts, that.sorts);
		boolean scriptFieldsEqual = nullSafeEquals(this.scriptFields, that.scriptFields);
		boolean facetsEqual = nullSafeEquals(this.facets, that.facets);
		boolean aggregationsEqual = nullSafeEquals(this.aggregations, that.aggregations);
		boolean highlightFieldsEqual = nullSafeEquals(this.highlightFields, that.highlightFields);
		boolean indicesBoostEqual = nullSafeEquals(this.indicesBoost, that.indicesBoost);
		return queryEqual && filterEqual && sortsEqual && scriptFieldsEqual && facetsEqual && aggregationsEqual && highlightFieldsEqual && indicesBoostEqual;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {

		int result = 17;

		result += 31 * nullSafeHashCode(query);
		result += 31 * nullSafeHashCode(filter);
		result += 31 * nullSafeHashCode(sorts);
		result += 31 * nullSafeHashCode(scriptFields);
		result += 31 * nullSafeHashCode(facets);
		result += 31 * nullSafeHashCode(aggregations);
		result += 31 * nullSafeHashCode(highlightFields);
		result += 31 * nullSafeHashCode(indicesBoost);

		return result;
	}


}
