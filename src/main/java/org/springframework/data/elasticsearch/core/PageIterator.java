/*
 * Copyright 2013-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.elasticsearch.core;

import java.util.*;
import java.util.function.*;

import org.springframework.data.util.*;

/**
 * Iterator through elastic scroll pages.
 * 
 * @author Nikita Guchakov
 */
class PageIterator<T> implements CloseableIterator<T> {

	private final Function<String, ScrolledPage<T>> pageRetriever;
	private final Consumer<String> onClear;

	/**
	 * As we couldn't retrieve single result with scroll, store current hits.
	 */
	private volatile Iterator<T> currentHits;

	/**
	 * The scroll id.
	 */
	private volatile String scrollId;

	/**
	 * If stream is finished (ie: cluster returns no results.
	 */
	private volatile boolean finished;

	PageIterator(ScrolledPage<T> page, Function<String, ScrolledPage<T>> pageRetriever, // scrollId ->
			Consumer<String> onClear // scrollId ->
	) {
		this.onClear = onClear;
		this.pageRetriever = pageRetriever;
		currentHits = page.iterator();
		scrollId = page.getScrollId();
		finished = !currentHits.hasNext();
	}

	@Override
	public void close() {
		try {
			// Clear scroll on cluster only in case of error (cause elasticsearch auto clear scroll when it's done)
			if (!finished && scrollId != null && currentHits != null && currentHits.hasNext()) {
				onClear.accept(scrollId);
			}
		} finally {
			currentHits = null;
			scrollId = null;
		}
	}

	@Override
	public boolean hasNext() {
		if (finished) {
			return false;
		}
		// Test if it remains hits
		if (currentHits == null || !currentHits.hasNext()) {
			final ScrolledPage<T> scroll = pageRetriever.apply(scrollId);
			currentHits = scroll.iterator();
			scrollId = scroll.getScrollId();
			finished = !currentHits.hasNext();
		}
		return currentHits.hasNext();
	}

	@Override
	public T next() {
		if (!hasNext()) {
			throw new NoSuchElementException();
		}
		return currentHits.next();
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException("remove");
	}
}
