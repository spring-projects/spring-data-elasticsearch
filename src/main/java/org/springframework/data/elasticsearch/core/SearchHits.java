/*
 * Copyright 2019 the original author or authors.
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
import java.util.Iterator;
import java.util.List;

import org.elasticsearch.search.aggregations.Aggregations;
import org.springframework.data.util.Streamable;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Encapsulates a list of {@link SearchHit}s with additional information from the search.
 *
 * @param <T> the result data class.
 * @author Peter-Josef Meisch
 * @since 4.0
 */
public class SearchHits<T> implements Streamable<SearchHit<T>> {

	private final long totalHits;
	private final float maxScore;
	private final String scrollId;
	private final List<? extends SearchHit<T>> searchHits;
	private final Aggregations aggregations;

	/**
	 * @param totalHits
	 * @param maxScore
	 * @param searchHits must not be {@literal null}
	 * @param aggregations
	 */
	public SearchHits(long totalHits, float maxScore, @Nullable String scrollId,  List<? extends SearchHit<T>> searchHits,
			@Nullable Aggregations aggregations) {

		Assert.notNull(searchHits, "searchHits must not be null");

		this.totalHits = totalHits;
		this.maxScore = maxScore;
		this.scrollId = scrollId;
		this.searchHits = searchHits;
		this.aggregations = aggregations;
	}

	@Override
	public Iterator<SearchHit<T>> iterator() {
		return (Iterator<SearchHit<T>>) searchHits.iterator();
	}

	/**
	 * @return the number of total hits.
	 */
	// region getter
	public long getTotalHits() {
		return totalHits;
	}

	/**
	 * @return the maximum score
	 */
	public float getMaxScore() {
		return maxScore;
	}

	/**
	 * @return the scroll id
	 */
	@Nullable
	public String getScrollId() {
		return scrollId;
	}

	/**
	 * @return the contained {@link SearchHit}s.
	 */
	public List<SearchHit<T>> getSearchHits() {
		return Collections.unmodifiableList(searchHits);
	}
	// endregion

	// region SearchHit access
	/**
	 * @param index position in List.
	 * @return the {@link SearchHit} at position {index}
	 * @throws IndexOutOfBoundsException on invalid index
	 */
	public SearchHit<T> getSearchHit(int index) {
		return searchHits.get(index);
	}
	// endregion

	@Override
	public String toString() {
		return "SearchHits{" +
				"totalHits=" + totalHits +
				", maxScore=" + maxScore +
				", scrollId='" + scrollId + '\'' +
				", searchHits=" + StringUtils.collectionToCommaDelimitedString(searchHits) +
				", aggregations=" + aggregations +
				'}';
	}

	/**
	 * @return true if aggregations are available
	 */
	// region aggregations
	public boolean hasAggregations() {
		return aggregations != null;
	}

	/**
	 * @return the aggregations.
	 */
	@Nullable
	public Aggregations getAggregations() {
		return aggregations;
	}
	// endregion

}
