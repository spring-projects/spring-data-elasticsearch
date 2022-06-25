/*
 * Copyright 2013-2022 the original author or authors.
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
package org.springframework.data.elasticsearch.client.erhlc;

import static org.springframework.util.CollectionUtils.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.script.mustache.SearchTemplateRequestBuilder;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.PipelineAggregationBuilder;
import org.elasticsearch.search.collapse.CollapseBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.springframework.data.elasticsearch.core.query.BaseQueryBuilder;
import org.springframework.data.elasticsearch.core.query.IndicesOptions;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.RescorerQuery;
import org.springframework.lang.Nullable;

/**
 * NativeSearchQuery
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Artur Konczak
 * @author Mark Paluch
 * @author Alen Turkovic
 * @author Sascha Woo
 * @author Jean-Baptiste Nizet
 * @author Martin Choraine
 * @author Farid Azaza
 * @author Peter-Josef Meisch
 * @author Peer Mueller
 * @author vdisk
 * @author owen.qq
 * @deprecated since 5.0
 */
@Deprecated
public class NativeSearchQueryBuilder extends BaseQueryBuilder<NativeSearchQuery, NativeSearchQueryBuilder> {

	@Nullable private QueryBuilder queryBuilder;
	@Nullable private QueryBuilder filterBuilder;
	private final List<ScriptField> scriptFields = new ArrayList<>();
	private final List<SortBuilder<?>> sortBuilders = new ArrayList<>();
	private final List<AbstractAggregationBuilder<?>> aggregationBuilders = new ArrayList<>();
	private final List<PipelineAggregationBuilder> pipelineAggregationBuilders = new ArrayList<>();
	@Nullable private HighlightBuilder highlightBuilder;
	@Nullable private List<HighlightBuilder.Field> highlightFields = new ArrayList<>();
	@Nullable protected List<String> storedFields;
	@Nullable private CollapseBuilder collapseBuilder;
	@Nullable private SearchTemplateRequestBuilder searchTemplateBuilder;
	@Nullable private SearchType searchType;
	@Nullable private Boolean trackTotalHits;
	@Nullable private Duration timeout;
	private final List<RescorerQuery> rescorerQueries = new ArrayList<>();
	@Nullable private SuggestBuilder suggestBuilder;
	@Nullable private List<Object> searchAfter;

	public NativeSearchQueryBuilder withQuery(QueryBuilder queryBuilder) {
		this.queryBuilder = queryBuilder;
		return this;
	}

	public NativeSearchQueryBuilder withFilter(QueryBuilder filterBuilder) {
		this.filterBuilder = filterBuilder;
		return this;
	}

	/**
	 * @deprecated use {@link #withSorts(SortBuilder...)} instead.
	 */
	@Deprecated
	public NativeSearchQueryBuilder withSort(SortBuilder<?> sortBuilder) {
		this.sortBuilders.add(sortBuilder);
		return this;
	}

	/**
	 * @since 4.3
	 */
	public NativeSearchQueryBuilder withSorts(Collection<SortBuilder<?>> sortBuilders) {
		this.sortBuilders.addAll(sortBuilders);
		return this;
	}

	/**
	 * @since 4.3
	 */
	public NativeSearchQueryBuilder withSorts(SortBuilder<?>... sortBuilders) {
		Collections.addAll(this.sortBuilders, sortBuilders);
		return this;
	}

	public NativeSearchQueryBuilder withScriptField(ScriptField scriptField) {
		this.scriptFields.add(scriptField);
		return this;
	}

	public NativeSearchQueryBuilder withCollapseField(String collapseField) {
		this.collapseBuilder = new CollapseBuilder(collapseField);
		return this;
	}

	/**
	 * @since 4.3
	 */
	public NativeSearchQueryBuilder withCollapseBuilder(@Nullable CollapseBuilder collapseBuilder) {
		this.collapseBuilder = collapseBuilder;
		return this;
	}

	/**
	 * @deprecated use {@link #withAggregations(AbstractAggregationBuilder...)} instead.
	 */
	@Deprecated
	public NativeSearchQueryBuilder addAggregation(AbstractAggregationBuilder<?> aggregationBuilder) {
		this.aggregationBuilders.add(aggregationBuilder);
		return this;
	}

	/**
	 * @since 4.3
	 */
	public NativeSearchQueryBuilder withAggregations(Collection<AbstractAggregationBuilder<?>> aggregationBuilders) {
		this.aggregationBuilders.addAll(aggregationBuilders);
		return this;
	}

	/**
	 * @since 4.3
	 */
	public NativeSearchQueryBuilder withAggregations(AbstractAggregationBuilder<?>... aggregationBuilders) {
		Collections.addAll(this.aggregationBuilders, aggregationBuilders);
		return this;
	}

	/**
	 * @deprecated use {@link #withPipelineAggregations(PipelineAggregationBuilder...)} instead.
	 */
	@Deprecated
	public NativeSearchQueryBuilder addAggregation(PipelineAggregationBuilder pipelineAggregationBuilder) {
		this.pipelineAggregationBuilders.add(pipelineAggregationBuilder);
		return this;
	}

	/**
	 * @since 4.3
	 */
	public NativeSearchQueryBuilder withPipelineAggregations(
			Collection<PipelineAggregationBuilder> pipelineAggregationBuilders) {
		this.pipelineAggregationBuilders.addAll(pipelineAggregationBuilders);
		return this;
	}

	/**
	 * @since 4.3
	 */
	public NativeSearchQueryBuilder withPipelineAggregations(PipelineAggregationBuilder... pipelineAggregationBuilders) {
		Collections.addAll(this.pipelineAggregationBuilders, pipelineAggregationBuilders);
		return this;
	}

	public NativeSearchQueryBuilder withHighlightBuilder(HighlightBuilder highlightBuilder) {
		this.highlightBuilder = highlightBuilder;
		return this;
	}

	public NativeSearchQueryBuilder withHighlightFields(HighlightBuilder.Field... highlightFields) {
		Collections.addAll(this.highlightFields, highlightFields);
		return this;
	}

	/**
	 * @since 4.3
	 */
	public NativeSearchQueryBuilder withHighlightFields(Collection<HighlightBuilder.Field> highlightFields) {
		this.highlightFields.addAll(highlightFields);
		return this;
	}

	public NativeSearchQueryBuilder withSearchTemplate(SearchTemplateRequestBuilder searchTemplateBuilder) {
		this.searchTemplateBuilder = searchTemplateBuilder;
		return this;
	}

	public NativeSearchQueryBuilder withStoredFields(Collection<String> storedFields) {

		if (this.storedFields == null) {
			this.storedFields = new ArrayList<>(storedFields);
		} else {
			this.storedFields.addAll(storedFields);
		}
		return this;
	}

	public NativeSearchQueryBuilder withStoredFields(String... storedFields) {

		if (this.storedFields == null) {
			this.storedFields = new ArrayList<>(storedFields.length);
		}
		Collections.addAll(this.storedFields, storedFields);
		return this;
	}

	public NativeSearchQueryBuilder withSearchType(SearchType searchType) {
		this.searchType = searchType;
		return this;
	}

	public NativeSearchQueryBuilder withIndicesOptions(IndicesOptions indicesOptions) {
		this.indicesOptions = indicesOptions;
		return this;
	}

	/**
	 * @since 4.2
	 */
	public NativeSearchQueryBuilder withTrackTotalHits(Boolean trackTotalHits) {
		this.trackTotalHits = trackTotalHits;
		return this;
	}

	public NativeSearchQueryBuilder withTimeout(Duration timeout) {
		this.timeout = timeout;
		return this;
	}

	public NativeSearchQueryBuilder withRescorerQuery(RescorerQuery rescorerQuery) {
		this.rescorerQueries.add(rescorerQuery);
		return this;
	}

	/**
	 * @since 4.3
	 */
	public NativeSearchQueryBuilder withSuggestBuilder(SuggestBuilder suggestBuilder) {
		this.suggestBuilder = suggestBuilder;
		return this;
	}

	public NativeSearchQueryBuilder withSearchAfter(List<Object> searchAfter) {
		if (searchAfter != null && searchAfter.isEmpty()) {
			return this;
		}

		this.searchAfter = searchAfter;
		return this;
	}

	public NativeSearchQuery build() {

		NativeSearchQuery nativeSearchQuery = new NativeSearchQuery( //
				this, //
				queryBuilder, //
				filterBuilder, //
				sortBuilders, //
				highlightBuilder, //
				highlightFields.toArray(new HighlightBuilder.Field[highlightFields.size()]));

		if (storedFields != null) {
			nativeSearchQuery.setStoredFields(storedFields);
		}

		if (searchTemplateBuilder != null) {
			nativeSearchQuery.setSearchTemplate(searchTemplateBuilder);
		}

		if (!isEmpty(scriptFields)) {
			nativeSearchQuery.setScriptFields(scriptFields);
		}

		if (collapseBuilder != null) {
			nativeSearchQuery.setCollapseBuilder(collapseBuilder);
		}

		if (!isEmpty(aggregationBuilders)) {
			nativeSearchQuery.setAggregations(aggregationBuilders);
		}

		if (!isEmpty(pipelineAggregationBuilders)) {
			nativeSearchQuery.setPipelineAggregations(pipelineAggregationBuilders);
		}

		if (searchType != null) {
			nativeSearchQuery.setSearchType(Query.SearchType.valueOf(searchType.name()));
		}

		if (indicesOptions != null) {
			nativeSearchQuery.setIndicesOptions(indicesOptions);
		}

		nativeSearchQuery.setTrackTotalHits(trackTotalHits);

		if (timeout != null) {
			nativeSearchQuery.setTimeout(timeout);
		}

		if (!isEmpty(rescorerQueries)) {
			nativeSearchQuery.setRescorerQueries(rescorerQueries);
		}

		if (suggestBuilder != null) {
			nativeSearchQuery.setSuggestBuilder(suggestBuilder);
		}

		if (searchAfter != null) {
			nativeSearchQuery.setSearchAfter(searchAfter);
		}

		return nativeSearchQuery;
	}
}
