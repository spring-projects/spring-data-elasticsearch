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
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.util.CloseableIterator;
import org.springframework.lang.Nullable;

/**
 * @author Sascha Woo
 */
public class StreamQueriesTest {

	@Test // DATAES-764
	public void shouldCallClearScrollOnIteratorClose() {

		// given
		List<String> results = new ArrayList<>();
		results.add("one");

		ScrolledPage<String> page = new ScrolledPageImpl("1234", results);

		AtomicBoolean clearScrollCalled = new AtomicBoolean(false);

		// when
		CloseableIterator<String> closeableIterator = StreamQueries.streamResults( //
				page, //
				scrollId -> new ScrolledPageImpl(scrollId, Collections.emptyList()), //
				scrollId -> clearScrollCalled.set(true));

		while (closeableIterator.hasNext()) {
			closeableIterator.next();
		}
		closeableIterator.close();

		// then
		assertThat(clearScrollCalled).isTrue();

	}

	private static class ScrolledPageImpl extends PageImpl<String> implements ScrolledPage<String> {

		private String scrollId;

		public ScrolledPageImpl(String scrollId, List<String> content) {
			super(content);
			this.scrollId = scrollId;
		}

		@Override
		@Nullable
		public String getScrollId() {
			return scrollId;
		}
	}
}
