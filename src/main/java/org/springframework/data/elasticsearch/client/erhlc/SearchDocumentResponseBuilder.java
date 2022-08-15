/*
 * Copyright 2022 the original author or authors.
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
package org.springframework.data.elasticsearch.client.erhlc;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.search.TotalHits;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregations;
import org.springframework.data.elasticsearch.core.document.SearchDocument;
import org.springframework.data.elasticsearch.core.document.SearchDocumentResponse;
import org.springframework.data.elasticsearch.core.suggest.response.CompletionSuggestion;
import org.springframework.data.elasticsearch.core.suggest.response.PhraseSuggestion;
import org.springframework.data.elasticsearch.core.suggest.response.SortBy;
import org.springframework.data.elasticsearch.core.suggest.response.Suggest;
import org.springframework.data.elasticsearch.core.suggest.response.TermSuggestion;
import org.springframework.data.elasticsearch.support.ScoreDoc;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Factory class to create {@link SearchDocumentResponse} instances.
 *
 * @author Peter-Josef Meisch
 * @since 4.0
 * @deprecated since 5.0
 */
@Deprecated
public class SearchDocumentResponseBuilder {

	private static final Log LOGGER = LogFactory.getLog(SearchDocumentResponse.class);

	/**
	 * creates a SearchDocumentResponse from the {@link SearchResponse}
	 *
	 * @param searchResponse must not be {@literal null}
	 * @param entityCreator function to create an entity from a {@link SearchDocument}
	 * @param <T> entity type
	 * @return the SearchDocumentResponse
	 */
	public static <T> SearchDocumentResponse from(SearchResponse searchResponse,
			SearchDocumentResponse.EntityCreator<T> entityCreator) {

		Assert.notNull(searchResponse, "searchResponse must not be null");

		SearchHits searchHits = searchResponse.getHits();
		String scrollId = searchResponse.getScrollId();
		Aggregations aggregations = searchResponse.getAggregations();
		org.elasticsearch.search.suggest.Suggest suggest = searchResponse.getSuggest();

		return from(searchHits, scrollId, aggregations, suggest, entityCreator);
	}

	/**
	 * creates a {@link SearchDocumentResponseBuilder} from {@link SearchHits} with the given scrollId aggregations and
	 * suggest
	 *
	 * @param searchHits the {@link SearchHits} to process
	 * @param scrollId scrollId
	 * @param aggregations aggregations
	 * @param suggestES the suggestion response from Elasticsearch
	 * @param entityCreator function to create an entity from a {@link SearchDocument}
	 * @param <T> entity type
	 * @return the {@link SearchDocumentResponse}
	 * @since 4.3
	 */
	public static <T> SearchDocumentResponse from(SearchHits searchHits, @Nullable String scrollId,
			@Nullable Aggregations aggregations, @Nullable org.elasticsearch.search.suggest.Suggest suggestES,
			SearchDocumentResponse.EntityCreator<T> entityCreator) {

		TotalHits responseTotalHits = searchHits.getTotalHits();

		long totalHits;
		String totalHitsRelation;

		if (responseTotalHits != null) {
			totalHits = responseTotalHits.value;
			totalHitsRelation = responseTotalHits.relation.name();
		} else {
			totalHits = searchHits.getHits().length;
			totalHitsRelation = "OFF";
		}

		float maxScore = searchHits.getMaxScore();

		List<SearchDocument> searchDocuments = new ArrayList<>();
		for (SearchHit searchHit : searchHits) {
			if (searchHit != null) {
				searchDocuments.add(DocumentAdapters.from(searchHit));
			}
		}

		ElasticsearchAggregations aggregationsContainer = aggregations != null ? new ElasticsearchAggregations(aggregations)
				: null;
		Suggest suggest = suggestFrom(suggestES, entityCreator);

		// no pointInTimeId for the deprecated implementation
		return new SearchDocumentResponse(totalHits, totalHitsRelation, maxScore, scrollId, null, searchDocuments,
				aggregationsContainer, suggest);
	}

	@Nullable
	private static <T> Suggest suggestFrom(@Nullable org.elasticsearch.search.suggest.Suggest suggestES,
			SearchDocumentResponse.EntityCreator<T> entityCreator) {

		if (suggestES == null) {
			return null;
		}

		List<Suggest.Suggestion<? extends Suggest.Suggestion.Entry<? extends Suggest.Suggestion.Entry.Option>>> suggestions = new ArrayList<>();

		for (org.elasticsearch.search.suggest.Suggest.Suggestion<? extends org.elasticsearch.search.suggest.Suggest.Suggestion.Entry<? extends org.elasticsearch.search.suggest.Suggest.Suggestion.Entry.Option>> suggestionES : suggestES) {

			if (suggestionES instanceof org.elasticsearch.search.suggest.term.TermSuggestion termSuggestionES) {

				List<TermSuggestion.Entry> entries = new ArrayList<>();
				for (org.elasticsearch.search.suggest.term.TermSuggestion.Entry entryES : termSuggestionES) {

					List<TermSuggestion.Entry.Option> options = new ArrayList<>();
					for (org.elasticsearch.search.suggest.term.TermSuggestion.Entry.Option optionES : entryES) {
						options.add(new TermSuggestion.Entry.Option(textToString(optionES.getText()),
								textToString(optionES.getHighlighted()), optionES.getScore(), optionES.collateMatch(),
								optionES.getFreq()));
					}

					entries.add(new TermSuggestion.Entry(textToString(entryES.getText()), entryES.getOffset(),
							entryES.getLength(), options));
				}

				suggestions.add(new TermSuggestion(termSuggestionES.getName(), termSuggestionES.getSize(), entries,
						suggestFrom(termSuggestionES.getSort())));
			}

			if (suggestionES instanceof org.elasticsearch.search.suggest.phrase.PhraseSuggestion phraseSuggestionES) {

				List<PhraseSuggestion.Entry> entries = new ArrayList<>();
				for (org.elasticsearch.search.suggest.phrase.PhraseSuggestion.Entry entryES : phraseSuggestionES) {

					List<PhraseSuggestion.Entry.Option> options = new ArrayList<>();
					for (org.elasticsearch.search.suggest.phrase.PhraseSuggestion.Entry.Option optionES : entryES) {
						options.add(new PhraseSuggestion.Entry.Option(textToString(optionES.getText()),
								textToString(optionES.getHighlighted()), (double) optionES.getScore(), optionES.collateMatch()));
					}

					entries.add(new PhraseSuggestion.Entry(textToString(entryES.getText()), entryES.getOffset(),
							entryES.getLength(), options, entryES.getCutoffScore()));
				}

				suggestions.add(new PhraseSuggestion(phraseSuggestionES.getName(), phraseSuggestionES.getSize(), entries));
			}

			if (suggestionES instanceof org.elasticsearch.search.suggest.completion.CompletionSuggestion completionSuggestionES) {

				List<CompletionSuggestion.Entry<T>> entries = new ArrayList<>();
				for (org.elasticsearch.search.suggest.completion.CompletionSuggestion.Entry entryES : completionSuggestionES) {

					List<CompletionSuggestion.Entry.Option<T>> options = new ArrayList<>();
					for (org.elasticsearch.search.suggest.completion.CompletionSuggestion.Entry.Option optionES : entryES) {
						SearchDocument searchDocument = optionES.getHit() != null ? DocumentAdapters.from(optionES.getHit()) : null;
						T hitEntity = null;

						if (searchDocument != null) {
							try {
								hitEntity = entityCreator.apply(searchDocument).get();
							} catch (Exception e) {
								if (LOGGER.isWarnEnabled()) {
									LOGGER.warn("Error creating entity from SearchDocument");
								}
							}
						}

						options.add(new CompletionSuggestion.Entry.Option<>(textToString(optionES.getText()),
								textToString(optionES.getHighlighted()), (double) optionES.getScore(), optionES.collateMatch(),
								optionES.getContexts(), scoreDocFrom(optionES.getDoc()), searchDocument, hitEntity));
					}

					entries.add(new CompletionSuggestion.Entry<>(textToString(entryES.getText()), entryES.getOffset(),
							entryES.getLength(), options));
				}

				suggestions.add(
						new CompletionSuggestion<>(completionSuggestionES.getName(), completionSuggestionES.getSize(), entries));
			}
		}

		return new Suggest(suggestions, suggestES.hasScoreDocs());
	}

	private static SortBy suggestFrom(org.elasticsearch.search.suggest.SortBy sort) {
		return SortBy.valueOf(sort.name().toUpperCase());
	}

	@Nullable
	private static ScoreDoc scoreDocFrom(@Nullable org.apache.lucene.search.ScoreDoc scoreDoc) {

		if (scoreDoc == null) {
			return null;
		}

		return new ScoreDoc(scoreDoc.score, scoreDoc.doc, scoreDoc.shardIndex);
	}

	private static String textToString(@Nullable Text text) {
		return text != null ? text.string() : "";
	}
}
