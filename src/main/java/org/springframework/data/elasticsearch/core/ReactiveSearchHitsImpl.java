/*
 * Copyright 2021-2024 the original author or authors.
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

import reactor.core.publisher.Flux;

import org.springframework.data.elasticsearch.core.suggest.response.Suggest;
import org.springframework.lang.Nullable;

/**
 * @author Peter-Josef Meisch
 * @since 4.4
 */
public class ReactiveSearchHitsImpl<T> implements ReactiveSearchHits<T> {

	protected final SearchHits<T> delegate;

	public ReactiveSearchHitsImpl(SearchHits<T> delegate) {
		this.delegate = delegate;
	}

	@Override
	public long getTotalHits() {
		return delegate.getTotalHits();
	}

	@Override
	public TotalHitsRelation getTotalHitsRelation() {
		return delegate.getTotalHitsRelation();
	}

	@Override
	public boolean hasAggregations() {
		return delegate.hasAggregations();
	}

	@Override
	@Nullable
	public AggregationsContainer<?> getAggregations() {
		return delegate.getAggregations();
	}

	@Override
	public float getMaxScore() {
		return delegate.getMaxScore();
	}

	@Override
	public boolean hasSearchHits() {
		return delegate.hasSearchHits();
	}

	@Override
	public Flux<SearchHit<T>> getSearchHits() {
		return Flux.defer(() -> Flux.fromIterable(delegate.getSearchHits()));
	}

	@Override
	@Nullable
	public Suggest getSuggest() {
		return delegate.getSuggest();
	}

	@Override
	public boolean hasSuggest() {
		return delegate.hasSuggest();
	}

	/**
	 * @since 5.0
	 */
	@Nullable
	@Override
	public String getPointInTimeId() {
		return delegate.getPointInTimeId();
	}
}
