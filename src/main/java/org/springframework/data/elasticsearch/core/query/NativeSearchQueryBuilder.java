/*
 * Copyright 2013-2014 the original author or authors.
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
import java.util.Collection;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.facet.FacetRequest;
import org.springframework.data.elasticsearch.core.partition.keys.Partition;

/**
 * NativeSearchQuery
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Artur Konczak
 */

public class NativeSearchQueryBuilder {

	private QueryBuilder queryBuilder;
	private FilterBuilder filterBuilder;
	private List<SortBuilder> sortBuilders = new ArrayList<SortBuilder>();
	private List<FacetRequest> facetRequests = new ArrayList<FacetRequest>();
	private List<AbstractAggregationBuilder> aggregationBuilders = new ArrayList<AbstractAggregationBuilder>();
	private HighlightBuilder.Field[] highlightFields;
	private Pageable pageable;
	private String[] indices;
	private String[] types;
	private String[] fields;
	private SourceFilter sourceFilter;
	private float minScore;
	private Collection<String> ids;
	private String route;
	private SearchType searchType;
	private List<Partition> partitions = new ArrayList<Partition>();



	public NativeSearchQueryBuilder withQuery(QueryBuilder queryBuilder) {
		this.queryBuilder = queryBuilder;
		return this;
	}

	public NativeSearchQueryBuilder withFilter(FilterBuilder filterBuilder) {
		this.filterBuilder = filterBuilder;
		return this;
	}

	public NativeSearchQueryBuilder withSort(SortBuilder sortBuilder) {
		this.sortBuilders.add(sortBuilder);
		return this;
	}

	public NativeSearchQueryBuilder addAggregation(AbstractAggregationBuilder aggregationBuilder) {
		this.aggregationBuilders.add(aggregationBuilder);
		return this;
	}

	public NativeSearchQueryBuilder withFacet(FacetRequest facetRequest) {
		facetRequests.add(facetRequest);
		return this;
	}

	public NativeSearchQueryBuilder withHighlightFields(HighlightBuilder.Field... highlightFields) {
		this.highlightFields = highlightFields;
		return this;
	}

	public NativeSearchQueryBuilder withPageable(Pageable pageable) {
		this.pageable = pageable;
		return this;
	}

	public NativeSearchQueryBuilder withIndices(String... indices) {
		this.indices = indices;
		return this;
	}

	public NativeSearchQueryBuilder withTypes(String... types) {
		this.types = types;
		return this;
	}

	public NativeSearchQueryBuilder withFields(String... fields) {
		this.fields = fields;
		return this;
	}

	public NativeSearchQueryBuilder withSourceFilter(SourceFilter sourceFilter) {
		this.sourceFilter = sourceFilter;
		return this;
	}

	public NativeSearchQueryBuilder withMinScore(float minScore) {
		this.minScore = minScore;
		return this;
	}

	public NativeSearchQueryBuilder withIds(Collection<String> ids) {
		this.ids = ids;
		return this;
	}

	public NativeSearchQueryBuilder withRoute(String route) {
		this.route = route;
		return this;
	}

	public NativeSearchQueryBuilder withSearchType(SearchType searchType) {
		this.searchType = searchType;
		return this;
	}

	public NativeSearchQueryBuilder withPartition(Partition partition) {
		this.partitions.add(partition);
		return this;
	}

	public NativeSearchQuery build() {
		NativeSearchQuery nativeSearchQuery = new NativeSearchQuery(queryBuilder, filterBuilder, sortBuilders, highlightFields);
		if (pageable != null) {
			nativeSearchQuery.setPageable(pageable);
		}

		if (indices != null) {
			nativeSearchQuery.addIndices(indices);
		}

		if (types != null) {
			nativeSearchQuery.addTypes(types);
		}

		if (fields != null) {
			nativeSearchQuery.addFields(fields);
		}

		if (sourceFilter != null) {
			nativeSearchQuery.addSourceFilter(sourceFilter);
		}

		if (CollectionUtils.isNotEmpty(facetRequests)) {
			nativeSearchQuery.setFacets(facetRequests);
		}

		if (CollectionUtils.isNotEmpty(aggregationBuilders)) {
			nativeSearchQuery.setAggregations(aggregationBuilders);
		}

		if (minScore > 0) {
			nativeSearchQuery.setMinScore(minScore);
		}

		if (ids != null) {
			nativeSearchQuery.setIds(ids);
		}

		if (route != null) {
			nativeSearchQuery.setRoute(route);
		}

		if (searchType != null) {
			nativeSearchQuery.setSearchType(searchType);
		}
		if (!partitions.isEmpty()) {
			nativeSearchQuery.setPartitions(partitions);
		}
		return nativeSearchQuery;
	}
}
