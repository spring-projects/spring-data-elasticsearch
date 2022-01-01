/*
 * Copyright 2019-2022 the original author or authors.
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

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
	 * @param maxCount the maximum number of entities to return, a value of 0 means that all available entities are
	 *          returned
	 * @param searchHits the initial hits
	 * @param continueScrollFunction function to continue scrolling applies to the current scrollId.
	 * @param clearScrollConsumer consumer to clear the scroll context by accepting the scrollIds to clear.
	 * @param <T> the entity type
	 * @return the {@link SearchHitsIterator}.
	 */
	static <T> SearchHitsIterator<T> streamResults(int maxCount, SearchScrollHits<T> searchHits,
			Function<String, SearchScrollHits<T>> continueScrollFunction, Consumer<List<String>> clearScrollConsumer) {

		Assert.notNull(searchHits, "searchHits must not be null.");
		Assert.notNull(searchHits.getScrollId(), "scrollId of searchHits must not be null.");
		Assert.notNull(continueScrollFunction, "continueScrollFunction must not be null.");
		Assert.notNull(clearScrollConsumer, "clearScrollConsumer must not be null.");

		AggregationsContainer<?> aggregations = searchHits.getAggregations();
		float maxScore = searchHits.getMaxScore();
		long totalHits = searchHits.getTotalHits();
		TotalHitsRelation totalHitsRelation = searchHits.getTotalHitsRelation();

		return new SearchHitsIterator<T>() {

			private volatile AtomicInteger currentCount = new AtomicInteger();
			private volatile Iterator<SearchHit<T>> currentScrollHits = searchHits.iterator();
			private volatile boolean continueScroll = currentScrollHits.hasNext();
			private volatile ScrollState scrollState = new ScrollState(searchHits.getScrollId());
			private volatile boolean isClosed = false;

			@Override
			public void close() {
				if (!isClosed) {
					clearScrollConsumer.accept(scrollState.getScrollIds());
					isClosed = true;
				}
			}

			@Override
			@Nullable
			public AggregationsContainer<?> getAggregations() {
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

				boolean hasNext = false;

				if (!isClosed && continueScroll && (maxCount <= 0 || currentCount.get() < maxCount)) {

					if (!currentScrollHits.hasNext()) {
						SearchScrollHits<T> nextPage = continueScrollFunction.apply(scrollState.getScrollId());
						currentScrollHits = nextPage.iterator();
						scrollState.updateScrollId(nextPage.getScrollId());
						continueScroll = currentScrollHits.hasNext();
					}
					hasNext = currentScrollHits.hasNext();
				}

				if (!hasNext) {
					close();
				}

				return hasNext;
			}

			@Override
			public SearchHit<T> next() {
				if (hasNext()) {
					currentCount.incrementAndGet();
					return currentScrollHits.next();
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
