/*
 * Copyright 2016 the original author or authors.
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

import java.util.List;

import org.elasticsearch.search.aggregations.Aggregations;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;

/**
 * Container for scroll results
 *
 * @author Sascha Woo
 */
public class ScrollPageImpl<T> extends AggregatedPageImpl<T> implements ScrollPage<T> {

	private final String scrollId;

	public ScrollPageImpl(List<T> content, Pageable pageable, long total, Aggregations aggregations, String scrollId) {
		super(content, pageable, total, aggregations);
		this.scrollId = scrollId;
	}

	public ScrollPageImpl(List<T> content, Pageable pageable, long total, String scrollId) {
		super(content, pageable, total);
		this.scrollId = scrollId;
	}

	public ScrollPageImpl(List<T> content, String scrollId) {
		super(content);
		this.scrollId = scrollId;
	}

	@Override
	public String getScrollId() {
		return scrollId;
	}
}
