/*
 * Copyright 2021-2024 the original author or authors.
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

import reactor.core.publisher.Flux;

import org.springframework.data.elasticsearch.core.suggest.response.Suggest;
import org.springframework.lang.Nullable;

/**
 * Encapsulates a Flux of {@link SearchHit}s with additional information from the search.
 *
 * @param <T> the result data class.
 * @author Peter-Josef Meisch
 * @since 4.4
 */
public interface ReactiveSearchHits<T> {

	/**
	 * @return the aggregations.
	 */
	@Nullable
	AggregationsContainer<?> getAggregations();

	float getMaxScore();

	/**
	 * @return the {@link SearchHit}s from the search result.
	 */
	Flux<SearchHit<T>> getSearchHits();

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
	boolean hasAggregations();

	/**
	 * @return whether the {@link SearchHits} has search hits.
	 */
	boolean hasSearchHits();

	/**
	 * @return the suggest response
	 */
	@Nullable
	Suggest getSuggest();

	/**
	 * @return wether the {@link SearchHits} has a suggest response.
	 */
	boolean hasSuggest();

	/**
	 * When doing a search with a point in time, the response contains a new point in time id value.
	 *
	 * @return the new point in time id, if one was returned from Elasticsearch
	 * @since 5.0
	 */
	@Nullable
	String getPointInTimeId();

}
