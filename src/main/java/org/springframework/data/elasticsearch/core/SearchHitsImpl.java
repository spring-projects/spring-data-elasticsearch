/*
 * Copyright 2019-2020 the original author or authors.
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

import org.elasticsearch.search.aggregations.Aggregations;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Basic implementation of {@link SearchScrollHits} 
 *
 * @param <T> the result data class.
 * @author Peter-Josef Meisch
 * @author Sascha Woo
 * @since 4.0
 */
public class SearchHitsImpl<T> implements SearchScrollHits<T> {

	private final long totalHits;
	private final TotalHitsRelation totalHitsRelation;
	private final float maxScore;
	private final String scrollId;
	private final List<? extends SearchHit<T>> searchHits;
	private final Aggregations aggregations;

	/**
	 * @param totalHits the number of total hits for the search
	 * @param totalHitsRelation the relation {@see TotalHitsRelation}, must not be {@literal null}
	 * @param maxScore the maximum score
	 * @param scrollId the scroll id if available
	 * @param searchHits must not be {@literal null}
	 * @param aggregations the aggregations if available
	 */
	public SearchHitsImpl(long totalHits, TotalHitsRelation totalHitsRelation, float maxScore, @Nullable String scrollId,
			List<? extends SearchHit<T>> searchHits, @Nullable Aggregations aggregations) {

		Assert.notNull(searchHits, "searchHits must not be null");

		this.totalHits = totalHits;
		this.totalHitsRelation = totalHitsRelation;
		this.maxScore = maxScore;
		this.scrollId = scrollId;
		this.searchHits = searchHits;
		this.aggregations = aggregations;
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
		return Collections.unmodifiableList(searchHits);
	}
	// endregion

	// region SearchHit access
	@Override
	public SearchHit<T> getSearchHit(int index) {
		return searchHits.get(index);
	}
	// endregion

	@Override
	public String toString() {
		return "SearchHits{" + //
				"totalHits=" + totalHits + //
				", totalHitsRelation=" + totalHitsRelation + //
				", maxScore=" + maxScore + //
				", scrollId='" + scrollId + '\'' + //
				", searchHits={" + searchHits.size() + " elements}" + //
				", aggregations=" + aggregations + //
				'}';
	}

	// region aggregations
	@Override
	@Nullable
	public Aggregations getAggregations() {
		return aggregations;
	}
	// endregion
}
