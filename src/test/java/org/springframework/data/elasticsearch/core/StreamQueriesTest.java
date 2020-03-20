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

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.elasticsearch.search.aggregations.Aggregations;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;

/**
 * @author Sascha Woo
 */
public class StreamQueriesTest {

	@Test // DATAES-764
	public void shouldCallClearScrollOnIteratorClose() {

		// given
		List<SearchHit<String>> hits = new ArrayList<>();
		hits.add(new SearchHit<String>(null, 0, null, null, "one"));

		SearchScrollHits<String> searchHits = newSearchScrollHits(hits);

		AtomicBoolean clearScrollCalled = new AtomicBoolean(false);

		// when
		SearchHitsIterator<String> iterator = StreamQueries.streamResults( //
				searchHits, //
				scrollId -> newSearchScrollHits(Collections.emptyList()), //
				scrollId -> clearScrollCalled.set(true));

		while (iterator.hasNext()) {
			iterator.next();
		}
		iterator.close();

		// then
		assertThat(clearScrollCalled).isTrue();

	}

	@Test // DATAES-766
	public void shouldReturnTotalHits() {

		// given
		List<SearchHit<String>> hits = new ArrayList<>();
		hits.add(new SearchHit<String>(null, 0, null, null, "one"));

		SearchScrollHits<String> searchHits = newSearchScrollHits(hits);

		// when
		SearchHitsIterator<String> iterator = StreamQueries.streamResults( //
				searchHits, //
				scrollId -> newSearchScrollHits(Collections.emptyList()), //
				scrollId -> {
				});

		// then
		assertThat(iterator.getTotalHits()).isEqualTo(1);

	}

	private SearchScrollHits<String> newSearchScrollHits(List<SearchHit<String>> hits) {
		return new SearchHitsImpl<String>(hits.size(), TotalHitsRelation.EQUAL_TO, 0, "1234", hits, null);
	}
}
