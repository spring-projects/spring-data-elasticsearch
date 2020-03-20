/*
 * Copyright 2020 the original author or authors.
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

import org.elasticsearch.search.aggregations.Aggregations;
import org.springframework.data.util.CloseableIterator;
import org.springframework.lang.Nullable;

/**
 * A {@link SearchHitsIterator} encapsulates {@link SearchHit} results that can be wrapped in a Java 8
 * {@link java.util.stream.Stream}.
 *
 * @author Sascha Woo
 * @param <T>
 * @since 4.0
 */
public interface SearchHitsIterator<T> extends CloseableIterator<SearchHit<T>> {

	/**
	 * @return the aggregations.
	 */
	@Nullable
	Aggregations getAggregations();

	/**
	 * @return the maximum score
	 */
	float getMaxScore();

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

}
