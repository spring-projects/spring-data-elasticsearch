/*
 * Copyright 2025-present the original author or authors.
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
package org.springframework.data.elasticsearch.core.query;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.elasticsearch.core.query.BaseQuery.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

class BaseQueryTests {

	private static final String MATCH_ALL_QUERY = "{\"match_all\":{}}";

	@Test // #3127
	@DisplayName("query with no Pageable and no maxResults requests 10 docs from 0")
	void queryWithNoPageableAndNoMaxResultsRequests10DocsFrom0() {

		var query = StringQuery.builder(MATCH_ALL_QUERY)
				.build();

		var requestSize = query.getRequestSize();

		assertThat(requestSize).isEqualTo(10);
	}

	@Test // #3127
	@DisplayName("query with a Pageable and no MaxResults request with values from Pageable")
	void queryWithAPageableAndNoMaxResultsRequestWithValuesFromPageable() {
		var query = StringQuery.builder(MATCH_ALL_QUERY)
				.withPageable(Pageable.ofSize(42))
				.build();

		var requestSize = query.getRequestSize();

		assertThat(requestSize).isEqualTo(42);
	}

	@Test // #3127
	@DisplayName("query with no Pageable and maxResults requests maxResults")
	void queryWithNoPageableAndMaxResultsRequestsMaxResults() {

		var query = StringQuery.builder(MATCH_ALL_QUERY)
				.withMaxResults(12_345)
				.build();

		var requestSize = query.getRequestSize();

		assertThat(requestSize).isEqualTo(12_345);
	}

	@Test // #3127
	@DisplayName("query with Pageable and maxResults requests with values from Pageable if Pageable is less than maxResults")
	void queryWithPageableAndMaxResultsRequestsWithValuesFromPageableIfPageableIsLessThanMaxResults() {

		var query = StringQuery.builder(MATCH_ALL_QUERY)
				.withPageable(Pageable.ofSize(42))
				.withMaxResults(123)
				.build();

		var requestSize = query.getRequestSize();

		assertThat(requestSize).isEqualTo(42);
	}

	@Test // #3127
	@DisplayName("query with Pageable and maxResults requests with values from maxResults if Pageable is more than maxResults")
	void queryWithPageableAndMaxResultsRequestsWithValuesFromMaxResultsIfPageableIsMoreThanMaxResults() {

		var query = StringQuery.builder(MATCH_ALL_QUERY)
				.withPageable(Pageable.ofSize(420))
				.withMaxResults(123)
				.build();

		var requestSize = query.getRequestSize();

		assertThat(requestSize).isEqualTo(123);
	}

	@Test // #3127
	@DisplayName("query with explicit unpaged request and no maxResults requests max request window size")
	void queryWithExplicitUnpagedRequestAndNoMaxResultsRequestsMaxRequestWindowSize() {

		var query = StringQuery.builder(MATCH_ALL_QUERY)
				.withPageable(Pageable.unpaged())
				.build();

		var requestSize = query.getRequestSize();

		assertThat(requestSize).isEqualTo(INDEX_MAX_RESULT_WINDOW);
	}
}
