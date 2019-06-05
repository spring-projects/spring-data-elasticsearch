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

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;

/**
 * Adapter utility for {@link SearchResultMapper} that wish to implement a subset of mapping methods. Default
 * implementations throw {@link UnsupportedOperationException}.
 *
 * @author Mark Paluch
 * @since 3.2
 */
abstract class SearchResultMapperAdapter implements SearchResultMapper {

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.core.SearchResultMapper#mapResults(org.elasticsearch.action.search.SearchResponse, java.lang.Class, org.springframework.data.domain.Pageable)
	 */
	@Override
	public <T> AggregatedPage<T> mapResults(SearchResponse response, Class<T> clazz, Pageable pageable) {
		throw new UnsupportedOperationException();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.core.SearchResultMapper#mapSearchHit(org.elasticsearch.search.SearchHit, java.lang.Class)
	 */
	@Override
	public <T> T mapSearchHit(SearchHit searchHit, Class<T> type) {
		throw new UnsupportedOperationException();
	}
}
