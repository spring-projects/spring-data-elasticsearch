/*
 * Copyright 2017-2020 the original author or authors.
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

import static java.util.Optional.*;

import java.util.List;

import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.Aggregations;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.document.SearchDocumentResponse;
import org.springframework.lang.Nullable;

/**
 * @author Petar Tahchiev
 * @author Artur Konczak
 * @author Mohsin Husen
 * @author Sascha Woo
 * @author Peter-Josef Meisch
 */
public class AggregatedPageImpl<T> extends PageImpl<T> implements AggregatedPage<T> {

	@Nullable private Aggregations aggregations;
	@Nullable private String scrollId;
	private float maxScore;

	private static Pageable pageableOrUnpaged(@Nullable Pageable pageable) {
		return ofNullable(pageable).orElse(Pageable.unpaged());
	}

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
		super(content, pageableOrUnpaged(pageable), total);
	}

	public AggregatedPageImpl(List<T> content, Pageable pageable, long total, float maxScore) {
		super(content, pageableOrUnpaged(pageable), total);
		this.maxScore = maxScore;
	}

	public AggregatedPageImpl(List<T> content, Pageable pageable, long total, String scrollId) {
		super(content, pageableOrUnpaged(pageable), total);
		this.scrollId = scrollId;
	}

	public AggregatedPageImpl(List<T> content, Pageable pageable, long total, String scrollId, float maxScore) {
		this(content, pageableOrUnpaged(pageable), total, scrollId);
		this.maxScore = maxScore;
	}

	public AggregatedPageImpl(List<T> content, Pageable pageable, long total, @Nullable Aggregations aggregations) {
		super(content, pageableOrUnpaged(pageable), total);
		this.aggregations = aggregations;
	}

	public AggregatedPageImpl(List<T> content, Pageable pageable, long total, @Nullable Aggregations aggregations,
			float maxScore) {
		this(content, pageableOrUnpaged(pageable), total, aggregations);
		this.maxScore = maxScore;
	}

	public AggregatedPageImpl(List<T> content, Pageable pageable, long total, @Nullable Aggregations aggregations,
			String scrollId) {
		this(content, pageableOrUnpaged(pageable), total, aggregations);
		this.scrollId = scrollId;
	}

	public AggregatedPageImpl(List<T> content, Pageable pageable, long total, @Nullable Aggregations aggregations,
			String scrollId, float maxScore) {
		this(content, pageableOrUnpaged(pageable), total, aggregations, scrollId);
		this.maxScore = maxScore;
	}

	public AggregatedPageImpl(List<T> content, Pageable pageable, SearchDocumentResponse response) {
		this(content, pageableOrUnpaged(pageable), response.getTotalHits(), response.getAggregations(),
				response.getScrollId(), response.getMaxScore());
	}

	@Override
	public boolean hasAggregations() {
		return aggregations != null;
	}

	@Override
	@Nullable
	public Aggregations getAggregations() {
		return aggregations;
	}

	@Override
	@Nullable
	public Aggregation getAggregation(String name) {
		return aggregations == null ? null : aggregations.get(name);
	}

	@Nullable
	@Override
	public String getScrollId() {
		return scrollId;
	}

	@Override
	public float getMaxScore() {
		return maxScore;
	}
}
