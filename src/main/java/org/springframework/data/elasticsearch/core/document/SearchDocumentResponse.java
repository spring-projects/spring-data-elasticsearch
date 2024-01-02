/*
 * Copyright 2019-2024 the original author or authors.
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
package org.springframework.data.elasticsearch.core.document;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import org.springframework.data.elasticsearch.core.AggregationsContainer;
import org.springframework.data.elasticsearch.core.SearchShardStatistics;
import org.springframework.data.elasticsearch.core.suggest.response.Suggest;
import org.springframework.lang.Nullable;

/**
 * This represents the complete search response from Elasticsearch, including the returned documents.
 *
 * @author Peter-Josef Meisch
 * @author Haibo Liu
 * @since 4.0
 */
public class SearchDocumentResponse {

	private final long totalHits;
	private final String totalHitsRelation;
	private final float maxScore;
	@Nullable private final String scrollId;
	private final List<SearchDocument> searchDocuments;
	@Nullable private final AggregationsContainer<?> aggregations;
	@Nullable private final Suggest suggest;

	@Nullable String pointInTimeId;
	@Nullable private final SearchShardStatistics searchShardStatistics;

	public SearchDocumentResponse(long totalHits, String totalHitsRelation, float maxScore, @Nullable String scrollId,
			@Nullable String pointInTimeId, List<SearchDocument> searchDocuments,
			@Nullable AggregationsContainer<?> aggregationsContainer, @Nullable Suggest suggest,
			@Nullable SearchShardStatistics searchShardStatistics) {
		this.totalHits = totalHits;
		this.totalHitsRelation = totalHitsRelation;
		this.maxScore = maxScore;
		this.scrollId = scrollId;
		this.pointInTimeId = pointInTimeId;
		this.searchDocuments = searchDocuments;
		this.aggregations = aggregationsContainer;
		this.suggest = suggest;
		this.searchShardStatistics = searchShardStatistics;
	}

	public long getTotalHits() {
		return totalHits;
	}

	public String getTotalHitsRelation() {
		return totalHitsRelation;
	}

	public float getMaxScore() {
		return maxScore;
	}

	@Nullable
	public String getScrollId() {
		return scrollId;
	}

	public List<SearchDocument> getSearchDocuments() {
		return searchDocuments;
	}

	@Nullable
	public AggregationsContainer<?> getAggregations() {
		return aggregations;
	}

	@Nullable
	public Suggest getSuggest() {
		return suggest;
	}

	/**
	 * @since 5.0
	 */
	@Nullable
	public String getPointInTimeId() {
		return pointInTimeId;
	}

	@Nullable
	public SearchShardStatistics getSearchShardStatistics() {
		return searchShardStatistics;
	}

	/**
	 * A function to convert a {@link SearchDocument} async into an entity. Asynchronous so that it can be used from the
	 * imperative and the reactive code.
	 *
	 * @param <T> the entity type
	 */
	@FunctionalInterface
	public interface EntityCreator<T> extends Function<SearchDocument, CompletableFuture<T>> {}

}
