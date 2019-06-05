/*
 * Copyright 2017-2019 the original author or authors.
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
package org.springframework.data.elasticsearch.core.aggregation.impl;

import java.util.List;

import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.Aggregations;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.FacetedPageImpl;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;

/**
 * @author Petar Tahchiev
 * @author Artur Konczak
 * @author Mohsin Husen
 * @author Sascha Woo
 */
public class AggregatedPageImpl<T> extends FacetedPageImpl<T> implements AggregatedPage<T> {

	private Aggregations aggregations;
	private String scrollId;
	private float maxScore;

	public AggregatedPageImpl(List<T> content) {
		super(content);
	}

	public AggregatedPageImpl(List<T> content, float maxScore) {
		super(content);
		this.maxScore = maxScore;
	}

	public AggregatedPageImpl(List<T> content, String scrollId) {
		super(content);
		this.scrollId = scrollId;
	}

	public AggregatedPageImpl(List<T> content, String scrollId, float maxScore) {
		this(content, scrollId);
		this.maxScore = maxScore;
	}

	public AggregatedPageImpl(List<T> content, Pageable pageable, long total) {
		super(content, pageable, total);
	}

	public AggregatedPageImpl(List<T> content, Pageable pageable, long total, float maxScore) {
		super(content, pageable, total);
		this.maxScore = maxScore;
	}

	public AggregatedPageImpl(List<T> content, Pageable pageable, long total, String scrollId) {
		super(content, pageable, total);
		this.scrollId = scrollId;
	}

	public AggregatedPageImpl(List<T> content, Pageable pageable, long total, String scrollId, float maxScore) {
		this(content, pageable, total, scrollId);
		this.maxScore = maxScore;
	}

	public AggregatedPageImpl(List<T> content, Pageable pageable, long total, Aggregations aggregations) {
		super(content, pageable, total);
		this.aggregations = aggregations;
	}

	public AggregatedPageImpl(List<T> content, Pageable pageable, long total, Aggregations aggregations, float maxScore) {
		this(content, pageable, total, aggregations);
		this.maxScore = maxScore;
	}

	public AggregatedPageImpl(List<T> content, Pageable pageable, long total, Aggregations aggregations,
			String scrollId) {
		this(content, pageable, total, aggregations);
		this.scrollId = scrollId;
	}

	public AggregatedPageImpl(List<T> content, Pageable pageable, long total, Aggregations aggregations, String scrollId,
			float maxScore) {
		this(content, pageable, total, aggregations, scrollId);
		this.maxScore = maxScore;
	}

	@Override
	public boolean hasAggregations() {
		return aggregations != null;
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

	@Override
	public float getMaxScore() {
		return maxScore;
	}
}
