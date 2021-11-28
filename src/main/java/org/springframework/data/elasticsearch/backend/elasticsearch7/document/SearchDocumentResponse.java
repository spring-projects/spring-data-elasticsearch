/*
 * Copyright 2019-2021 the original author or authors.
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
package org.springframework.data.elasticsearch.backend.elasticsearch7.document;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.apache.lucene.search.TotalHits;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregations;
import org.springframework.data.elasticsearch.backend.elasticsearch7.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.core.AggregationsContainer;
import org.springframework.data.elasticsearch.core.document.SearchDocument;
import org.springframework.data.elasticsearch.core.suggest.response.CompletionSuggestion;
import org.springframework.data.elasticsearch.core.suggest.response.PhraseSuggestion;
import org.springframework.data.elasticsearch.core.suggest.response.SortBy;
import org.springframework.data.elasticsearch.core.suggest.response.Suggest;
import org.springframework.data.elasticsearch.core.suggest.response.TermSuggestion;
import org.springframework.data.elasticsearch.support.ScoreDoc;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * This represents the complete search response from Elasticsearch, including the returned documents. Instances must be
 * created with the {@link #from(SearchResponse,Function)} method.
 *
 * @author Peter-Josef Meisch
 * @since 4.4
 */
public class SearchDocumentResponse {

	private final long totalHits;
	private final String totalHitsRelation;
	private final float maxScore;
	private final String scrollId;
	private final List<SearchDocument> searchDocuments;
	@Nullable private final AggregationsContainer<?> aggregations;
	@Nullable private final Suggest suggest;

	private SearchDocumentResponse(long totalHits, String totalHitsRelation, float maxScore, String scrollId,
			List<SearchDocument> searchDocuments, @Nullable Aggregations aggregations, @Nullable Suggest suggest) {
		this.totalHits = totalHits;
		this.totalHitsRelation = totalHitsRelation;
		this.maxScore = maxScore;
		this.scrollId = scrollId;
		this.searchDocuments = searchDocuments;
		this.aggregations = aggregations != null ? new ElasticsearchAggregations(aggregations) : null;
		this.suggest = suggest;
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

	@Nullable
	public AggregationsContainer<?> getAggregations() {
		return aggregations;
	}

	@Nullable
	public Suggest getSuggest() {
		return suggest;
	}

	/**
	 * creates a SearchDocumentResponse from the {@link SearchResponse}
	 *
	 * @param searchResponse must not be {@literal null}
	 * @param entityCreator function to create an entity from a {@link SearchDocument}
	 * @param <T> entity type
	 * @return the SearchDocumentResponse
	 */
	public static <T> SearchDocumentResponse from(SearchResponse searchResponse,
			Function<SearchDocument, T> entityCreator) {

		Assert.notNull(searchResponse, "searchResponse must not be null");

		SearchHits searchHits = searchResponse.getHits();
		String scrollId = searchResponse.getScrollId();
		Aggregations aggregations = searchResponse.getAggregations();
		org.elasticsearch.search.suggest.Suggest suggest = searchResponse.getSuggest();

		return from(searchHits, scrollId, aggregations, suggest, entityCreator);
	}

	/**
	 * creates a {@link SearchDocumentResponse} from {@link SearchHits} with the given scrollId aggregations and suggest
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
			Function<SearchDocument, T> entityCreator) {

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

		Suggest suggest = suggestFrom(suggestES, entityCreator);
		return new SearchDocumentResponse(totalHits, totalHitsRelation, maxScore, scrollId, searchDocuments, aggregations,
				suggest);
	}

	@Nullable
	private static <T> Suggest suggestFrom(@Nullable org.elasticsearch.search.suggest.Suggest suggestES,
			Function<SearchDocument, T> entityCreator) {

		if (suggestES == null) {
			return null;
		}

		List<Suggest.Suggestion<? extends Suggest.Suggestion.Entry<? extends Suggest.Suggestion.Entry.Option>>> suggestions = new ArrayList<>();

		for (org.elasticsearch.search.suggest.Suggest.Suggestion<? extends org.elasticsearch.search.suggest.Suggest.Suggestion.Entry<? extends org.elasticsearch.search.suggest.Suggest.Suggestion.Entry.Option>> suggestionES : suggestES) {

			if (suggestionES instanceof org.elasticsearch.search.suggest.term.TermSuggestion) {
				org.elasticsearch.search.suggest.term.TermSuggestion termSuggestionES = (org.elasticsearch.search.suggest.term.TermSuggestion) suggestionES;

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

			if (suggestionES instanceof org.elasticsearch.search.suggest.phrase.PhraseSuggestion) {
				org.elasticsearch.search.suggest.phrase.PhraseSuggestion phraseSuggestionES = (org.elasticsearch.search.suggest.phrase.PhraseSuggestion) suggestionES;

				List<PhraseSuggestion.Entry> entries = new ArrayList<>();
				for (org.elasticsearch.search.suggest.phrase.PhraseSuggestion.Entry entryES : phraseSuggestionES) {

					List<PhraseSuggestion.Entry.Option> options = new ArrayList<>();
					for (org.elasticsearch.search.suggest.phrase.PhraseSuggestion.Entry.Option optionES : entryES) {
						options.add(new PhraseSuggestion.Entry.Option(textToString(optionES.getText()),
								textToString(optionES.getHighlighted()), optionES.getScore(), optionES.collateMatch()));
					}

					entries.add(new PhraseSuggestion.Entry(textToString(entryES.getText()), entryES.getOffset(),
							entryES.getLength(), options, entryES.getCutoffScore()));
				}

				suggestions.add(new PhraseSuggestion(phraseSuggestionES.getName(), phraseSuggestionES.getSize(), entries));
			}

			if (suggestionES instanceof org.elasticsearch.search.suggest.completion.CompletionSuggestion) {
				org.elasticsearch.search.suggest.completion.CompletionSuggestion completionSuggestionES = (org.elasticsearch.search.suggest.completion.CompletionSuggestion) suggestionES;

				List<CompletionSuggestion.Entry<T>> entries = new ArrayList<>();
				for (org.elasticsearch.search.suggest.completion.CompletionSuggestion.Entry entryES : completionSuggestionES) {

					List<CompletionSuggestion.Entry.Option<T>> options = new ArrayList<>();
					for (org.elasticsearch.search.suggest.completion.CompletionSuggestion.Entry.Option optionES : entryES) {
						SearchDocument searchDocument = optionES.getHit() != null ? DocumentAdapters.from(optionES.getHit()) : null;
						T hitEntity = searchDocument != null ? entityCreator.apply(searchDocument) : null;
						options.add(new CompletionSuggestion.Entry.Option<T>(textToString(optionES.getText()),
								textToString(optionES.getHighlighted()), optionES.getScore(), optionES.collateMatch(),
								optionES.getContexts(), scoreDocFrom(optionES.getDoc()), searchDocument, hitEntity));
					}

					entries.add(new CompletionSuggestion.Entry<T>(textToString(entryES.getText()), entryES.getOffset(),
							entryES.getLength(), options));
				}

				suggestions.add(
						new CompletionSuggestion<T>(completionSuggestionES.getName(), completionSuggestionES.getSize(), entries));
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
