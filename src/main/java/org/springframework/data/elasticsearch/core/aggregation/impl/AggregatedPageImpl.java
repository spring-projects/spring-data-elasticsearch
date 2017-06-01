/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.data.elasticsearch.core.aggregation.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.Aggregations;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.FacetedPageImpl;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;

/**
 * @author Petar Tahchiev
 * @author Artur Konczak
 * @author Mohsin Husen
 */
public class AggregatedPageImpl<T> extends FacetedPageImpl<T> implements AggregatedPage<T> {

	private Aggregations aggregations;
	private Map<String, Aggregation> mapOfAggregations = new HashMap<>();
    private String scrollId;

	public AggregatedPageImpl(List<T> content) {
		super(content);
	}

	public AggregatedPageImpl(List<T> content, String scrollId) {
		super(content);
		this.scrollId = scrollId;
	}

	public AggregatedPageImpl(List<T> content, Pageable pageable, long total) {
		super(content, pageable, total);
	}

	public AggregatedPageImpl(List<T> content, Pageable pageable, long total, String scrollId) {
		super(content, pageable, total);
		this.scrollId = scrollId;
	}

	public AggregatedPageImpl(List<T> content, Pageable pageable, long total, Aggregations aggregations) {
		super(content, pageable, total);
		this.aggregations = aggregations;
		if (aggregations != null) {
			for (Aggregation aggregation : aggregations) {
				mapOfAggregations.put(aggregation.getName(), aggregation);
			}
		}
	}

	public AggregatedPageImpl(List<T> content, Pageable pageable, long total, Aggregations aggregations, String scrollId) {
		super(content, pageable, total);
		this.aggregations = aggregations;
		this.scrollId = scrollId;
		if (aggregations != null) {
			for (Aggregation aggregation : aggregations) {
				mapOfAggregations.put(aggregation.getName(), aggregation);
			}
		}
	}

	@Override
	public boolean hasAggregations() {
		return aggregations != null && mapOfAggregations.size() > 0;
	}

	@Override
	public Aggregations getAggregations() {
		return aggregations;
	}

	@Override
	public Aggregation getAggregation(String name) {
		return aggregations == null ? null : aggregations.get(name);
	}

	@Override
	public String getScrollId() {
		return scrollId;
	}
}
