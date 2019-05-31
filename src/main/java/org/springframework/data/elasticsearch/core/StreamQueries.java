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

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.Function;

import org.springframework.data.util.CloseableIterator;

/**
 * Utility to support streaming queries.
 *
 * @author Mark Paluch
 * @since 3.2
 */
abstract class StreamQueries {

	/**
	 * Stream query results using {@link ScrolledPage}.
	 *
	 * @param page the initial page.
	 * @param continueFunction continuation function accepting the current scrollId.
	 * @param clearScroll cleanup function accepting the current scrollId.
	 * @param <T>
	 * @return the {@link CloseableIterator}.
	 */
	static <T> CloseableIterator<T> streamResults(ScrolledPage<T> page,
			Function<String, ScrolledPage<T>> continueFunction, Consumer<String> clearScroll) {

		return new CloseableIterator<T>() {

			/** As we couldn't retrieve single result with scroll, store current hits. */
			private volatile Iterator<T> currentHits = page.iterator();

			/** The scroll id. */
			private volatile String scrollId = page.getScrollId();

			/** If stream is finished (ie: cluster returns no results. */
			private volatile boolean finished = !currentHits.hasNext();

			@Override
			public void close() {
				try {
					// Clear scroll on cluster only in case of error (cause elasticsearch auto clear scroll when it's done)
					if (!finished && scrollId != null && currentHits != null && currentHits.hasNext()) {
						clearScroll.accept(scrollId);
					}
				} finally {
					currentHits = null;
					scrollId = null;
				}
			}

			@Override
			public boolean hasNext() {
				// Test if stream is finished
				if (finished) {
					return false;
				}
				// Test if it remains hits
				if (currentHits == null || !currentHits.hasNext()) {
					// Do a new request
					ScrolledPage<T> scroll = continueFunction.apply(scrollId);
					// Save hits and scroll id
					currentHits = scroll.iterator();
					finished = !currentHits.hasNext();
					scrollId = scroll.getScrollId();
				}
				return currentHits.hasNext();
			}

			@Override
			public T next() {
				if (hasNext()) {
					return currentHits.next();
				}
				throw new NoSuchElementException();
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException("remove");
			}
		};
	}

	// utility constructor
	private StreamQueries() {}
}
