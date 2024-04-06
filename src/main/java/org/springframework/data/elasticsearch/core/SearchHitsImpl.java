/*
 * Copyright 2019-2024 the original author or authors.
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
package org.springframework.data.elasticsearch.core;

import java.util.Collections;
import java.util.List;

import org.springframework.data.elasticsearch.core.suggest.response.Suggest;
import org.springframework.data.util.Lazy;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Basic implementation of {@link SearchScrollHits}
 *
 * @param <T> the result data class.
 * @author Peter-Josef Meisch
 * @author Sascha Woo
 * @author Haibo Liu
 * @since 4.0
 */
public class SearchHitsImpl<T> implements SearchScrollHits<T> {

	private final long totalHits;
	private final TotalHitsRelation totalHitsRelation;
	private final float maxScore;
	@Nullable private final String scrollId;
	private final List<? extends SearchHit<T>> searchHits;
	private final Lazy<List<SearchHit<T>>> unmodifiableSearchHits;
	@Nullable private final AggregationsContainer<?> aggregations;
	@Nullable private final Suggest suggest;
	@Nullable private final String pointInTimeId;
	@Nullable private final SearchShardStatistics searchShardStatistics;

	/**
	 * @param totalHits the number of total hits for the search
	 * @param totalHitsRelation the relation {@see TotalHitsRelation}, must not be {@literal null}
	 * @param maxScore the maximum score
	 * @param scrollId the scroll id if available
	 * @param searchHits must not be {@literal null}
	 * @param aggregations the aggregations if available
	 */
	public SearchHitsImpl(long totalHits, TotalHitsRelation totalHitsRelation, float maxScore, @Nullable String scrollId,
			@Nullable String pointInTimeId, List<? extends SearchHit<T>> searchHits,
			@Nullable AggregationsContainer<?> aggregations, @Nullable Suggest suggest,
			@Nullable SearchShardStatistics searchShardStatistics) {

		Assert.notNull(searchHits, "searchHits must not be null");

		this.totalHits = totalHits;
		this.totalHitsRelation = totalHitsRelation;
		this.maxScore = maxScore;
		this.scrollId = scrollId;
		this.pointInTimeId = pointInTimeId;
		this.searchHits = searchHits;
		this.aggregations = aggregations;
		this.suggest = suggest;
		this.unmodifiableSearchHits = Lazy.of(() -> Collections.unmodifiableList(searchHits));
		this.searchShardStatistics = searchShardStatistics;
	}

	// region getter
	@Override
	public long getTotalHits() {
		return totalHits;
	}

	@Override
	public TotalHitsRelation getTotalHitsRelation() {
		return totalHitsRelation;
	}

	@Override
	public float getMaxScore() {
		return maxScore;
	}

	@Override
	@Nullable
	public String getScrollId() {
		return scrollId;
	}

	@Override
	public List<SearchHit<T>> getSearchHits() {
		return unmodifiableSearchHits.get();
	}

	@Override
	public SearchHit<T> getSearchHit(int index) {
		return searchHits.get(index);
	}

	@Override
	@Nullable
	public AggregationsContainer<?> getAggregations() {
		return aggregations;
	}

	@Override
	@Nullable
	public Suggest getSuggest() {
		return suggest;
	}

	@Nullable
	@Override
	public String getPointInTimeId() {
		return pointInTimeId;
	}

	@Override
	public SearchShardStatistics getSearchShardStatistics() {
		return searchShardStatistics;
	}

	@Override
	public String toString() {
		return "SearchHits{" + //
				"totalHits=" + totalHits + //
				", totalHitsRelation=" + totalHitsRelation + //
				", maxScore=" + maxScore + //
				", scrollId='" + scrollId + '\'' + //
				", pointInTimeId='" + pointInTimeId + '\'' + //
				", searchHits={" + searchHits.size() + " elements}" + //
				", aggregations=" + aggregations + //
				", shardStatistics=" + searchShardStatistics + //
				'}';
	}
}
