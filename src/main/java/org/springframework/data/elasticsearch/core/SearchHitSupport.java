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

import reactor.core.publisher.Flux;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;
import org.springframework.data.repository.util.ReactiveWrappers;
import org.springframework.data.util.CloseableIterator;
import org.springframework.lang.Nullable;

/**
 * Utility class with helper methods for working with {@link SearchHit}.
 * 
 * @author Peter-Josef Meisch
 * @author Sascha Woo
 * @author Roman Puchkovskiy
 * @since 4.0
 */
public final class SearchHitSupport {

	private SearchHitSupport() {}

	/**
	 * unwraps the data contained in a SearchHit for different types containing SearchHits if possible
	 * 
	 * @param result the object, list, page or whatever containing SearchHit objects
	 * @return a corresponding object where the SearchHits are replaced by their content if possible, otherwise the
	 *         original object
	 */
	public static Object unwrapSearchHits(Object result) {

		if (result == null) {
			return result;
		}

		if (result instanceof SearchHit<?>) {
			return ((SearchHit<?>) result).getContent();
		}

		if (result instanceof List<?>) {
			return ((List<?>) result).stream() //
					.map(SearchHitSupport::unwrapSearchHits) //
					.collect(Collectors.toList());
		}

		if (result instanceof AggregatedPage<?>) {
			AggregatedPage<?> page = (AggregatedPage<?>) result;
			List<?> list = page.getContent().stream().map(SearchHitSupport::unwrapSearchHits).collect(Collectors.toList());
			return new AggregatedPageImpl<>(list, page.getPageable(), page.getTotalElements(), page.getAggregations(),
					page.getScrollId(), page.getMaxScore());

		}

		if (result instanceof Stream<?>) {
			return ((Stream<?>) result).map(SearchHitSupport::unwrapSearchHits);
		}

		if (result instanceof SearchHits<?>) {
			SearchHits<?> searchHits = (SearchHits<?>) result;
			return unwrapSearchHits(searchHits.getSearchHits());
		}

		if (result instanceof SearchHitsIterator<?>) {
			return unwrapSearchHitsIterator((SearchHitsIterator<?>) result);
		}

		if (ReactiveWrappers.isAvailable(ReactiveWrappers.ReactiveLibrary.PROJECT_REACTOR)) {

			if (result instanceof Flux) {
				Flux<?> flux = (Flux<?>) result;
				return flux.map(SearchHitSupport::unwrapSearchHits);
			}
		}

		return result;
	}

	private static CloseableIterator<?> unwrapSearchHitsIterator(SearchHitsIterator<?> iterator) {

		return new CloseableIterator<Object>() {
			@Override
			public boolean hasNext() {
				return iterator.hasNext();
			}

			@Override
			public Object next() {
				return unwrapSearchHits(iterator.next());
			}

			@Override
			public void close() {
				iterator.close();
			}
		};
	}

	/**
	 * Builds an {@link AggregatedPage} with the {@link SearchHit} objects from a {@link SearchHits} object.
	 * 
	 * @param searchHits, must not be {@literal null}.
	 * @param pageable, must not be {@literal null}.
	 * @return the created Page
	 * @deprecated since 4.0, will be removed in a future version.
	 */
	@Deprecated
	public static <T> AggregatedPage<SearchHit<T>> page(SearchHits<T> searchHits, Pageable pageable) {
		return new AggregatedPageImpl<>( //
				searchHits.getSearchHits(), //
				pageable, //
				searchHits.getTotalHits(), //
				searchHits.getAggregations(), //
				null, //
				searchHits.getMaxScore());
	}

	public static <T> SearchPage<T> searchPageFor(SearchHits<T> searchHits, @Nullable Pageable pageable) {
		return new SearchPageImpl<>(searchHits, (pageable != null) ? pageable : Pageable.unpaged());
	}

	/**
	 * SearchPage implementation.
	 * 
	 * @param <T>
	 */
	static class SearchPageImpl<T> extends PageImpl<SearchHit<T>> implements SearchPage<T> {

		private final SearchHits<T> searchHits;

		public SearchPageImpl(SearchHits<T> searchHits, Pageable pageable) {
			super(searchHits.getSearchHits(), pageable, searchHits.getTotalHits());
			this.searchHits = searchHits;
		}

		@Override
		public SearchHits<T> getSearchHits() {
			return searchHits;
		}
	}
}
