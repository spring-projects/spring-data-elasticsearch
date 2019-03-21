/*
 * Copyright 2014-2019 the original author or authors.
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

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.lang.Nullable;

/**
 * @author Artur Konczak
 * @author Petar Tahchiev
 * @author Christoph Strobl
 */
public interface SearchResultMapper {

	<T> AggregatedPage<T> mapResults(SearchResponse response, Class<T> clazz, Pageable pageable);

	/**
	 * Map a single {@link SearchHit} to the given {@link Class type}.
	 *
	 * @param searchHit must not be {@literal null}.
	 * @param type must not be {@literal null}.
	 * @param <T>
	 * @return can be {@literal null}.
	 * @since 3.2
	 */
	@Nullable
	<T> T mapSearchHit(SearchHit searchHit, Class<T> type);
}
