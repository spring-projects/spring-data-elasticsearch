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
package org.springframework.data.elasticsearch.client.elc;

import co.elastic.clients.elasticsearch._types.ShardFailure;
import co.elastic.clients.elasticsearch._types.ShardStatistics;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.SearchTemplateResponse;
import co.elastic.clients.elasticsearch.core.search.CompletionSuggest;
import co.elastic.clients.elasticsearch.core.search.CompletionSuggestOption;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.search.ResponseBody;
import co.elastic.clients.elasticsearch.core.search.Suggestion;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import co.elastic.clients.json.JsonpMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.data.elasticsearch.core.SearchShardStatistics;
import org.springframework.data.elasticsearch.core.TotalHitsRelation;
import org.springframework.data.elasticsearch.core.document.SearchDocument;
import org.springframework.data.elasticsearch.core.document.SearchDocumentResponse;
import org.springframework.data.elasticsearch.core.suggest.response.CompletionSuggestion;
import org.springframework.data.elasticsearch.core.suggest.response.PhraseSuggestion;
import org.springframework.data.elasticsearch.core.suggest.response.Suggest;
import org.springframework.data.elasticsearch.core.suggest.response.TermSuggestion;
import org.springframework.data.elasticsearch.support.ScoreDoc;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * Factory class to create {@link SearchDocumentResponse} instances.
 *
 * @author Peter-Josef Meisch
 * @author Haibo Liu
 * @since 4.4
 */
class SearchDocumentResponseBuilder {

	private static final Log LOGGER = LogFactory.getLog(SearchDocumentResponseBuilder.class);

	/**
	 * creates a SearchDocumentResponse from the {@link SearchResponse}
	 *
	 * @param responseBody the Elasticsearch response body
	 * @param entityCreator function to create an entity from a {@link SearchDocument}
	 * @param jsonpMapper to map JsonData objects
	 * @return the SearchDocumentResponse
	 */
	public static <T> SearchDocumentResponse from(ResponseBody<EntityAsMap> responseBody,
			SearchDocumentResponse.EntityCreator<T> entityCreator, JsonpMapper jsonpMapper) {

		Assert.notNull(responseBody, "responseBody must not be null");
		Assert.notNull(entityCreator, "entityCreator must not be null");
		Assert.notNull(jsonpMapper, "jsonpMapper must not be null");

		HitsMetadata<EntityAsMap> hitsMetadata = responseBody.hits();
		String scrollId = responseBody.scrollId();
		Map<String, Aggregate> aggregations = responseBody.aggregations();
		Map<String, List<Suggestion<EntityAsMap>>> suggest = responseBody.suggest();
		var pointInTimeId = responseBody.pitId();
		var shards = responseBody.shards();

		return from(hitsMetadata, shards, scrollId, pointInTimeId, aggregations, suggest, entityCreator, jsonpMapper);
	}

	/**
	 * creates a SearchDocumentResponse from the {@link SearchTemplateResponse}
	 *
	 * @param response the Elasticsearch response body
	 * @param entityCreator function to create an entity from a {@link SearchDocument}
	 * @param jsonpMapper to map JsonData objects
	 * @return the SearchDocumentResponse
	 * @since 5.1
	 */
	public static <T> SearchDocumentResponse from(SearchTemplateResponse<EntityAsMap> response,
			SearchDocumentResponse.EntityCreator<T> entityCreator, JsonpMapper jsonpMapper) {

		Assert.notNull(response, "response must not be null");
		Assert.notNull(entityCreator, "entityCreator must not be null");
		Assert.notNull(jsonpMapper, "jsonpMapper must not be null");

		var shards = response.shards();
		var hitsMetadata = response.hits();
		var scrollId = response.scrollId();
		var aggregations = response.aggregations();
		var suggest = response.suggest();
		var pointInTimeId = response.pitId();

		return from(hitsMetadata, shards, scrollId, pointInTimeId, aggregations, suggest, entityCreator, jsonpMapper);
	}

	/**
	 * creates a {@link SearchDocumentResponseBuilder} from {@link HitsMetadata} with the given scrollId aggregations and
	 * suggestES
	 *
	 * @param <T> entity type
	 * @param hitsMetadata the {@link HitsMetadata} to process
	 * @param scrollId scrollId
	 * @param aggregations aggregations
	 * @param suggestES the suggestion response from Elasticsearch
	 * @param entityCreator function to create an entity from a {@link SearchDocument}, needed in mapping the suggest data
	 * @param jsonpMapper to map JsonData objects
	 * @return the {@link SearchDocumentResponse}
	 */
	public static <T> SearchDocumentResponse from(HitsMetadata<?> hitsMetadata, @Nullable ShardStatistics shards,
			@Nullable String scrollId, @Nullable String pointInTimeId, @Nullable Map<String, Aggregate> aggregations,
			Map<String, List<Suggestion<EntityAsMap>>> suggestES, SearchDocumentResponse.EntityCreator<T> entityCreator,
			JsonpMapper jsonpMapper) {

		Assert.notNull(hitsMetadata, "hitsMetadata must not be null");

		long totalHits;
		String totalHitsRelation;

		TotalHits responseTotalHits = hitsMetadata.total();
		if (responseTotalHits != null) {
			totalHits = responseTotalHits.value();
			totalHitsRelation = switch (responseTotalHits.relation().jsonValue()) {
				case "eq" -> TotalHitsRelation.EQUAL_TO.name();
				case "gte" -> TotalHitsRelation.GREATER_THAN_OR_EQUAL_TO.name();
				default -> TotalHitsRelation.OFF.name();
			};
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

		Suggest suggest = suggestFrom(suggestES, entityCreator);

		SearchShardStatistics shardStatistics = shards != null ? shardsFrom(shards) : null;

		return new SearchDocumentResponse(totalHits, totalHitsRelation, maxScore, scrollId, pointInTimeId, searchDocuments,
				aggregationsContainer, suggest, shardStatistics);
	}

	private static SearchShardStatistics shardsFrom(ShardStatistics shards) {
		List<ShardFailure> failures = shards.failures();
		List<SearchShardStatistics.Failure> searchFailures = failures.stream().map(f -> SearchShardStatistics.Failure
				.of(f.index(), f.node(), f.status(), f.shard(), null, ResponseConverter.toErrorCause(f.reason()))).toList();
		return SearchShardStatistics.of(shards.failed(), shards.successful(), shards.total(), shards.skipped(),
				searchFailures);
	}

	@Nullable
	private static <T> Suggest suggestFrom(Map<String, List<Suggestion<EntityAsMap>>> suggestES,
			SearchDocumentResponse.EntityCreator<T> entityCreator) {

		if (CollectionUtils.isEmpty(suggestES)) {
			return null;
		}

		List<Suggest.Suggestion<? extends Suggest.Suggestion.Entry<? extends Suggest.Suggestion.Entry.Option>>> suggestions = new ArrayList<>();

		suggestES.forEach((name, suggestionsES) -> {

			if (!suggestionsES.isEmpty()) {
				// take the type from the first entry
				switch (suggestionsES.get(0)._kind()) {
					case Term -> {
						suggestions.add(getTermSuggestion(name, suggestionsES));
						break;
					}
					case Phrase -> {
						suggestions.add(getPhraseSuggestion(name, suggestionsES));
						break;
					}
					case Completion -> {
						suggestions.add(getCompletionSuggestion(name, suggestionsES, entityCreator));
						break;
					}
					default -> {}
				}
			}
		});

		// todo: hasScoreDocs checks if any one
		boolean hasScoreDocs = false;

		return new Suggest(suggestions, hasScoreDocs);
	}

	private static TermSuggestion getTermSuggestion(String name, List<Suggestion<EntityAsMap>> suggestionsES) {

		List<TermSuggestion.Entry> entries = new ArrayList<>();
		suggestionsES.forEach(suggestionES -> {
			var termSuggest = suggestionES.term();
			var termSuggestOptions = termSuggest.options();
			List<TermSuggestion.Entry.Option> options = new ArrayList<>();
			termSuggestOptions.forEach(optionES -> options.add(new TermSuggestion.Entry.Option(optionES.text(), null,
					optionES.score(), null, Math.toIntExact(optionES.freq()))));
			entries.add(new TermSuggestion.Entry(termSuggest.text(), termSuggest.offset(), termSuggest.length(), options));
		});
		return new TermSuggestion(name, suggestionsES.size(), entries, null);
	}

	private static PhraseSuggestion getPhraseSuggestion(String name, List<Suggestion<EntityAsMap>> suggestionsES) {

		List<PhraseSuggestion.Entry> entries = new ArrayList<>();
		suggestionsES.forEach(suggestionES -> {
			var phraseSuggest = suggestionES.phrase();
			var phraseSuggestOptions = phraseSuggest.options();
			List<PhraseSuggestion.Entry.Option> options = new ArrayList<>();
			phraseSuggestOptions.forEach(optionES -> options.add(new PhraseSuggestion.Entry.Option(optionES.text(),
					optionES.highlighted(), optionES.score(), optionES.collateMatch())));
			entries.add(new PhraseSuggestion.Entry(phraseSuggest.text(), phraseSuggest.offset(), phraseSuggest.length(),
					options, null));
		});
		return new PhraseSuggestion(name, suggestionsES.size(), entries);
	}

	private static <T> CompletionSuggestion<T> getCompletionSuggestion(String name,
			List<Suggestion<EntityAsMap>> suggestionsES, SearchDocumentResponse.EntityCreator<T> entityCreator) {
		List<CompletionSuggestion.Entry<T>> entries = new ArrayList<>();
		suggestionsES.forEach(suggestionES -> {
			CompletionSuggest<EntityAsMap> completionSuggest = suggestionES.completion();
			List<CompletionSuggestion.Entry.Option<T>> options = new ArrayList<>();
			List<CompletionSuggestOption<EntityAsMap>> optionsES = completionSuggest.options();
			optionsES.forEach(optionES -> {
				SearchDocument searchDocument = (optionES.source() != null) ? DocumentAdapters.from(optionES) : null;
				T hitEntity = null;

				if (searchDocument != null) {
					try {
						hitEntity = entityCreator.apply(searchDocument).get();
					} catch (Exception e) {
						if (LOGGER.isWarnEnabled()) {
							LOGGER.warn("Error creating entity from SearchDocument: " + e.getMessage());
						}
					}
				}

				Map<String, Set<String>> contexts = new HashMap<>();
				optionES.contexts().forEach((key, contextList) -> contexts.put(key,
						contextList.stream().map(context -> context._get().toString()).collect(Collectors.toSet())));

				// response from the new client does not have a doc and shardindex as the ScoreDoc from the old client responses

				options.add(new CompletionSuggestion.Entry.Option<>(optionES.text(), null, optionES.score(),
						optionES.collateMatch() != null ? optionES.collateMatch() : false, contexts,
						new ScoreDoc(optionES.score() != null ? optionES.score() : Double.NaN, null, null), searchDocument,
						hitEntity));
			});

			entries.add(new CompletionSuggestion.Entry<>(completionSuggest.text(), completionSuggest.offset(),
					completionSuggest.length(), options));
		});
		return new CompletionSuggestion<>(name, suggestionsES.size(), entries);
	}
}
