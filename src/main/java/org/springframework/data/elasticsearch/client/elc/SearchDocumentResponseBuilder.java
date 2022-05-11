/*
 * Copyright 2021-2022 the original author or authors.
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
package org.springframework.data.elasticsearch.client.elc;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.search.ResponseBody;
import co.elastic.clients.elasticsearch.core.search.Suggestion;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import co.elastic.clients.json.JsonpMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.elasticsearch.search.SearchHits;
import org.springframework.data.elasticsearch.core.TotalHitsRelation;
import org.springframework.data.elasticsearch.core.document.SearchDocument;
import org.springframework.data.elasticsearch.core.document.SearchDocumentResponse;
import org.springframework.data.elasticsearch.core.suggest.response.Suggest;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Factory class to create {@link SearchDocumentResponse} instances.
 *
 * @author Peter-Josef Meisch
 * @since 4.4
 */
class SearchDocumentResponseBuilder {
	/**
	 * creates a SearchDocumentResponse from the {@link SearchResponse}
	 *
	 * @param responseBody the Elasticsearch response body
	 * @param entityCreator function to create an entity from a {@link SearchDocument}
	 * @param jsonpMapper to map JsonData objects
	 * @return the SearchDocumentResponse
	 */
	@SuppressWarnings("DuplicatedCode")
	public static <T> SearchDocumentResponse from(ResponseBody<EntityAsMap> responseBody,
			SearchDocumentResponse.EntityCreator<T> entityCreator, JsonpMapper jsonpMapper) {

		Assert.notNull(responseBody, "responseBody must not be null");
		Assert.notNull(entityCreator, "entityCreator must not be null");

		HitsMetadata<EntityAsMap> hitsMetadata = responseBody.hits();
		String scrollId = responseBody.scrollId();
		Map<String, Aggregate> aggregations = responseBody.aggregations();
		Map<String, List<Suggestion<EntityAsMap>>> suggest = responseBody.suggest();

		return from(hitsMetadata, scrollId, aggregations, suggest, entityCreator, jsonpMapper);
	}

	/**
	 * creates a {@link SearchDocumentResponseBuilder} from {@link SearchHits} with the given scrollId aggregations and
	 * suggestES
	 *
	 * @param <T> entity type
	 * @param hitsMetadata the {@link SearchHits} to process
	 * @param scrollId scrollId
	 * @param aggregations aggregations
	 * @param suggestES the suggestion response from Elasticsearch
	 * @param entityCreator function to create an entity from a {@link SearchDocument}, needed in mapping the suggest data
	 * @param jsonpMapper to map JsonData objects
	 * @return the {@link SearchDocumentResponse}
	 */
	public static <T> SearchDocumentResponse from(HitsMetadata<?> hitsMetadata, @Nullable String scrollId,
			Map<String, Aggregate> aggregations, Map<String, List<Suggestion<EntityAsMap>>> suggestES,
			SearchDocumentResponse.EntityCreator<T> entityCreator, JsonpMapper jsonpMapper) {

		Assert.notNull(hitsMetadata, "hitsMetadata must not be null");

		long totalHits;
		String totalHitsRelation;

		TotalHits responseTotalHits = hitsMetadata.total();
		if (responseTotalHits != null) {
			totalHits = responseTotalHits.value();
			switch (responseTotalHits.relation().jsonValue()) {
				case "eq":
					totalHitsRelation = TotalHitsRelation.EQUAL_TO.name();
					break;
				case "gte":
					totalHitsRelation = TotalHitsRelation.GREATER_THAN_OR_EQUAL_TO.name();
					break;
				default:
					totalHitsRelation = TotalHitsRelation.OFF.name();
			}
		} else {
			totalHits = hitsMetadata.hits().size();
			totalHitsRelation = "OFF";
		}

		float maxScore = hitsMetadata.maxScore() != null ? hitsMetadata.maxScore().floatValue() : Float.NaN;

		List<SearchDocument> searchDocuments = new ArrayList<>();
		for (Hit<?> hit : hitsMetadata.hits()) {
			searchDocuments.add(DocumentAdapters.from(hit, jsonpMapper));
		}

		ElasticsearchAggregations aggregationsContainer = aggregations != null ? new ElasticsearchAggregations(aggregations)
				: null;

		// todo #2154
		Suggest suggest = null;

		return new SearchDocumentResponse(totalHits, totalHitsRelation, maxScore, scrollId, searchDocuments,
				aggregationsContainer, suggest);
	}
}
