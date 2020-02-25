/*
 * Copyright 2013-2020 the original author or authors.
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

import static java.util.Collections.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.support.IndicesOptions;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * AbstractQuery
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Mark Paluch
 * @author Alen Turkovic
 * @author Sascha Woo
 * @author Farid Azaza
 * @author Peter-Josef Meisch
 */
abstract class AbstractQuery implements Query {

	protected Pageable pageable = DEFAULT_PAGE;
	@Nullable protected Sort sort;
	protected List<String> fields = new ArrayList<>();
	@Nullable protected SourceFilter sourceFilter;
	protected float minScore;
	@Nullable protected Collection<String> ids;
	@Nullable protected String route;
	protected SearchType searchType = SearchType.DFS_QUERY_THEN_FETCH;
	@Nullable protected IndicesOptions indicesOptions;
	protected boolean trackScores;
	@Nullable protected String preference;
	@Nullable protected Integer maxResults;
	@Nullable protected HighlightQuery highlightQuery;
	private boolean trackTotalHits = false;
	@Nullable private Duration scrollTime;

	@Override
	@Nullable
	public Sort getSort() {
		return this.sort;
	}

	@Override
	public Pageable getPageable() {
		return this.pageable;
	}

	@Override
	public final <T extends Query> T setPageable(Pageable pageable) {

		Assert.notNull(pageable, "Pageable must not be null!");

		this.pageable = pageable;
		return (T) this.addSort(pageable.getSort());
	}

	@Override
	public void addFields(String... fields) {
		addAll(this.fields, fields);
	}

	@Override
	public List<String> getFields() {
		return fields;
	}

	@Override
	public void addSourceFilter(SourceFilter sourceFilter) {
		this.sourceFilter = sourceFilter;
	}

	@Nullable
	@Override
	public SourceFilter getSourceFilter() {
		return sourceFilter;
	}

	@Override
	@SuppressWarnings("unchecked")
	public final <T extends Query> T addSort(Sort sort) {
		if (sort == null) {
			return (T) this;
		}

		if (this.sort == null) {
			this.sort = sort;
		} else {
			this.sort = this.sort.and(sort);
		}

		return (T) this;
	}

	@Override
	public float getMinScore() {
		return minScore;
	}

	public void setMinScore(float minScore) {
		this.minScore = minScore;
	}

	@Nullable
	@Override
	public Collection<String> getIds() {
		return ids;
	}

	public void setIds(Collection<String> ids) {
		this.ids = ids;
	}

	@Nullable
	@Override
	public String getRoute() {
		return route;
	}

	public void setRoute(String route) {
		this.route = route;
	}

	public void setSearchType(SearchType searchType) {
		this.searchType = searchType;
	}

	@Override
	public SearchType getSearchType() {
		return searchType;
	}

	@Nullable
	@Override
	public IndicesOptions getIndicesOptions() {
		return indicesOptions;
	}

	public void setIndicesOptions(IndicesOptions indicesOptions) {
		this.indicesOptions = indicesOptions;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.core.query.Query#getTrackScores()
	 */
	@Override
	public boolean getTrackScores() {
		return trackScores;
	}

	/**
	 * Configures whether to track scores.
	 *
	 * @param trackScores
	 * @since 3.1
	 */
	public void setTrackScores(boolean trackScores) {
		this.trackScores = trackScores;
	}

	@Nullable
	@Override
	public String getPreference() {
		return preference;
	}

	@Override
	public void setPreference(String preference) {
		this.preference = preference;
	}

	@Override
	public boolean isLimiting() {
		return maxResults != null;
	}

	@Nullable
	@Override
	public Integer getMaxResults() {
		return maxResults;
	}

	public void setMaxResults(Integer maxResults) {
		this.maxResults = maxResults;
	}

	@Override
	public void setHighlightQuery(HighlightQuery highlightQuery) {
		this.highlightQuery = highlightQuery;
	}

	@Override
	public Optional<HighlightQuery> getHighlightQuery() {
		return Optional.ofNullable(highlightQuery);
	}

	@Override
	public void setTrackTotalHits(boolean trackTotalHits) {
		this.trackTotalHits = trackTotalHits;
	}

	@Override
	public boolean getTrackTotalHits() {
		return trackTotalHits;
	}

	@Nullable
	@Override
	public Duration getScrollTime() {
		return scrollTime;
	}

	@Override
	public void setScrollTime(@Nullable Duration scrollTime) {
		this.scrollTime = scrollTime;
	}
}
