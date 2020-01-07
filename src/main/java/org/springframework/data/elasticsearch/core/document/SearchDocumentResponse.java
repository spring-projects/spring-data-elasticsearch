/*
 * Copyright 2019-2020 the original author or authors.
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
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.lucene.search.TotalHits;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.aggregations.Aggregations;
import org.springframework.util.Assert;

/**
 * This represents the complete search response from Elasticsearch, including the returned documents. Instances must be
 * created with the {@link #from(SearchResponse)} method.
 * 
 * @author Peter-Josef Meisch
 * @since 4.0
 */
public class SearchDocumentResponse {

	private long totalHits;
	private String totalHitsRelation;
	private float maxScore;
	private final String scrollId;
	private final List<SearchDocument> searchDocuments;
	private final Aggregations aggregations;

	private SearchDocumentResponse(long totalHits, String totalHitsRelation, float maxScore, String scrollId,
								   List<SearchDocument> searchDocuments, Aggregations aggregations) {
		this.totalHits = totalHits;
		this.totalHitsRelation = totalHitsRelation;
		this.maxScore = maxScore;
		this.scrollId = scrollId;
		this.searchDocuments = searchDocuments;
		this.aggregations = aggregations;
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

	public String getScrollId() {
		return scrollId;
	}

	public List<SearchDocument> getSearchDocuments() {
		return searchDocuments;
	}

	public Aggregations getAggregations() {
		return aggregations;
	}

	/**
	 * creates a SearchDocumentResponse from the {@link SearchResponse}
	 *
	 * @param searchResponse
	 * 		must not be {@literal null}
	 * @return the SearchDocumentResponse
	 */
	public static SearchDocumentResponse from(SearchResponse searchResponse) {
		Assert.notNull(searchResponse, "searchResponse must not be null");

		TotalHits responseTotalHits = searchResponse.getHits().getTotalHits();
		long totalHits = responseTotalHits.value;
		String totalHitsRelation = responseTotalHits.relation.name();

		float maxScore = searchResponse.getHits().getMaxScore();
		String scrollId = searchResponse.getScrollId();

		List<SearchDocument> searchDocuments = StreamSupport.stream(searchResponse.getHits().spliterator(), false) //
				.filter(Objects::nonNull) //
				.map(DocumentAdapters::from) //
				.collect(Collectors.toList());

		Aggregations aggregations = searchResponse.getAggregations();

		return new SearchDocumentResponse(totalHits, totalHitsRelation, maxScore, scrollId, searchDocuments, aggregations);
	}
}
