/*
 * Copyright 2020-2024 the original author or authors.
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

import java.util.Iterator;
import java.util.List;

import org.springframework.data.elasticsearch.core.suggest.response.Suggest;
import org.springframework.data.util.Streamable;
import org.springframework.lang.Nullable;

/**
 * Encapsulates a list of {@link SearchHit}s with additional information from the search.
 *
 * @param <T> the result data class.
 * @author Sascha Woo
 * @author Haibo Liu
 * @since 4.0
 */
public interface SearchHits<T> extends Streamable<SearchHit<T>> {

	/**
	 * @return the aggregations.
	 */
	@Nullable
	AggregationsContainer<?> getAggregations();

	/**
	 * @return the maximum score
	 */
	float getMaxScore();

	/**
	 * @param index position in List.
	 * @return the {@link SearchHit} at position {index}
	 * @throws IndexOutOfBoundsException on invalid index
	 */
	SearchHit<T> getSearchHit(int index);

	/**
	 * @return the contained {@link SearchHit}s.
	 */
	List<SearchHit<T>> getSearchHits();

	/**
	 * @return the number of total hits.
	 */
	long getTotalHits();

	/**
	 * @return the relation for the total hits
	 */
	TotalHitsRelation getTotalHitsRelation();

	/**
	 * @return true if aggregations are available
	 */
	default boolean hasAggregations() {
		return getAggregations() != null;
	}

	/**
	 * @return whether the {@link SearchHits} has search hits.
	 */
	default boolean hasSearchHits() {
		return !getSearchHits().isEmpty();
	}

	/**
	 * @return the suggest response
	 * @since 4.3
	 */
	@Nullable
	Suggest getSuggest();

	/**
	 * @return wether the {@link SearchHits} has a suggest response.
	 * @since 4.3
	 */
	default boolean hasSuggest() {
		return getSuggest() != null;
	}

	/**
	 * @return an iterator for {@link SearchHit}
	 */
	default Iterator<SearchHit<T>> iterator() {
		return getSearchHits().iterator();
	}

	/**
	 * When doing a search with a point in time, the response contains a new point in time id value.
	 *
	 * @return the new point in time id, if one was returned from Elasticsearch
	 * @since 5.0
	 */
	@Nullable
	String getPointInTimeId();

	/**
	 * @return shard statistics for the search hit.
	 */
	@Nullable
	SearchShardStatistics getSearchShardStatistics();
}
