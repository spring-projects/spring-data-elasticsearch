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

import reactor.core.publisher.Flux;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;

/**
 * Utility class with helper methods for working with {@link SearchHit}.
 * 
 * @author Peter-Josef Meisch
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
			List<?> list = page.getContent().stream().map(o -> unwrapSearchHits(o)).collect(Collectors.toList());
			return new AggregatedPageImpl<>(list, null, page.getTotalElements(), page.getAggregations(), page.getScrollId(),
					page.getMaxScore());

		}

		if (result instanceof Stream<?>) {
			return ((Stream<?>) result).map(SearchHitSupport::unwrapSearchHits);
		}

		if (result instanceof SearchHits<?>) {
			SearchHits<?> searchHits = (SearchHits<?>) result;
			return unwrapSearchHits(searchHits.getSearchHits());
		}

		if (result instanceof Flux) {
			Flux<?> flux = (Flux<?>) result;
			return flux.map(SearchHitSupport::unwrapSearchHits);
		}

		return result;
	}

	/**
	 * Builds an {@link AggregatedPage} with the {@link SearchHit} objects from a {@link SearchHits} object.
	 * 
	 * @param searchHits, must not be {@literal null}.
	 * @param pageable, must not be {@literal null}.
	 * @return the created Page
	 */
	public static <T> AggregatedPage<SearchHit<T>> page(SearchHits<T> searchHits, Pageable pageable) {
		return new AggregatedPageImpl<>(searchHits.getSearchHits(), pageable, searchHits.getTotalHits(),
				searchHits.getAggregations(), searchHits.getScrollId(), searchHits.getMaxScore());
	}
}
