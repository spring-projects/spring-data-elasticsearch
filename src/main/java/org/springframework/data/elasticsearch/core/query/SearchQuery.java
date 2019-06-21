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
public interface SearchQuery extends Query {

	QueryBuilder getQuery();

	QueryBuilder getFilter();

	List<SortBuilder> getElasticsearchSorts();

	@Deprecated
	List<FacetRequest> getFacets();

	List<AbstractAggregationBuilder> getAggregations();

	HighlightBuilder getHighlightBuilder();

	HighlightBuilder.Field[] getHighlightFields();

	List<IndexBoost> getIndicesBoost();

	List<ScriptField> getScriptFields();

	CollapseBuilder getCollapseBuilder();

}
