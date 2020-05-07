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
package org.springframework.data.elasticsearch.client.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.search.Scroll;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * Mutable state object holding scrollId to be used for {@link SearchScrollRequest#scroll(Scroll)}
 *
 * @author Christoph Strobl
 * @author Peter-Josef Meisch
 * @since 3.2
 */
public class ScrollState {

	private final Object lock = new Object();

	private final Set<String> pastIds = new LinkedHashSet<>();
	@Nullable private String scrollId;

	public ScrollState() {}

	public ScrollState(String scrollId) {
		updateScrollId(scrollId);
	}

	@Nullable
	public String getScrollId() {
		return scrollId;
	}

	public List<String> getScrollIds() {

		synchronized (lock) {
			return Collections.unmodifiableList(new ArrayList<>(pastIds));
		}
	}

	public void updateScrollId(String scrollId) {

		if (StringUtils.hasText(scrollId)) {

			synchronized (lock) {

				this.scrollId = scrollId;
				pastIds.add(scrollId);
			}
		}
	}
}
