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

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.Function;

import org.elasticsearch.search.aggregations.Aggregations;
import org.springframework.data.elasticsearch.client.util.ScrollState;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Utility to support streaming queries.
 *
 * @author Mark Paluch
 * @author Sascha Woo
 * @since 3.2
 */
abstract class StreamQueries {

	/**
	 * Stream query results using {@link SearchScrollHits}.
	 *
	 * @param searchHits the initial hits
	 * @param continueScrollFunction function to continue scrolling applies to the current scrollId.
	 * @param clearScrollConsumer consumer to clear the scroll context by accepting the scrollIds to clear.
	 * @param <T>
	 * @return the {@link SearchHitsIterator}.
	 */
	static <T> SearchHitsIterator<T> streamResults(SearchScrollHits<T> searchHits,
			Function<String, SearchScrollHits<T>> continueScrollFunction, Consumer<List<String>> clearScrollConsumer) {

		Assert.notNull(searchHits, "searchHits must not be null.");
		Assert.notNull(searchHits.getScrollId(), "scrollId of searchHits must not be null.");
		Assert.notNull(continueScrollFunction, "continueScrollFunction must not be null.");
		Assert.notNull(clearScrollConsumer, "clearScrollConsumer must not be null.");

		Aggregations aggregations = searchHits.getAggregations();
		float maxScore = searchHits.getMaxScore();
		long totalHits = searchHits.getTotalHits();
		TotalHitsRelation totalHitsRelation = searchHits.getTotalHitsRelation();

		return new SearchHitsIterator<T>() {

			// As we couldn't retrieve single result with scroll, store current hits.
			private volatile Iterator<SearchHit<T>> scrollHits = searchHits.iterator();
			private volatile boolean continueScroll = scrollHits.hasNext();
			private volatile ScrollState scrollState = new ScrollState(searchHits.getScrollId());

			@Override
			public void close() {

				try {
					clearScrollConsumer.accept(scrollState.getScrollIds());
				} finally {
					scrollHits = null;
					scrollState = null;
				}
			}

			@Override
			@Nullable
			public Aggregations getAggregations() {
				return aggregations;
			}

			@Override
			public float getMaxScore() {
				return maxScore;
			}

			@Override
			public long getTotalHits() {
				return totalHits;
			}

			@Override
			public TotalHitsRelation getTotalHitsRelation() {
				return totalHitsRelation;
			}

			@Override
			public boolean hasNext() {

				if (!continueScroll) {
					return false;
				}

				if (!scrollHits.hasNext()) {
					SearchScrollHits<T> nextPage = continueScrollFunction.apply(scrollState.getScrollId());
					scrollHits = nextPage.iterator();
					scrollState.updateScrollId(nextPage.getScrollId());
					continueScroll = scrollHits.hasNext();
				}

				return scrollHits.hasNext();
			}

			@Override
			public SearchHit<T> next() {
				if (hasNext()) {
					return scrollHits.next();
				}
				throw new NoSuchElementException();
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	// utility constructor
	private StreamQueries() {}
}
