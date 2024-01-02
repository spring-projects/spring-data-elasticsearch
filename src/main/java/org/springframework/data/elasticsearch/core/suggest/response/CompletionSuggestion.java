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
package org.springframework.data.elasticsearch.core.suggest.response;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.document.SearchDocument;
import org.springframework.data.elasticsearch.support.ScoreDoc;
import org.springframework.lang.Nullable;

/**
 * @author Peter-Josef Meisch
 * @since 4.3
 */
public class CompletionSuggestion<T> extends Suggest.Suggestion<CompletionSuggestion.Entry<T>> {

	public CompletionSuggestion(String name, int size, List<Entry<T>> entries) {
		super(name, size, entries);
	}

	public static class Entry<T> extends Suggest.Suggestion.Entry<Entry.Option<T>> {

		public Entry(String text, int offset, int length, List<Option<T>> options) {
			super(text, offset, length, options);
		}

		public static class Option<T> extends Suggest.Suggestion.Entry.Option {

			private final Map<String, Set<String>> contexts;
			private final ScoreDoc scoreDoc;
			@Nullable private final SearchDocument searchDocument;
			@Nullable private final T hitEntity;
			@Nullable private SearchHit<T> searchHit;

			public Option(String text, @Nullable String highlighted, @Nullable Double score, Boolean collateMatch,
					Map<String, Set<String>> contexts, ScoreDoc scoreDoc, @Nullable SearchDocument searchDocument,
					@Nullable T hitEntity) {
				super(text, highlighted, score, collateMatch);
				this.contexts = contexts;
				this.scoreDoc = scoreDoc;
				this.searchDocument = searchDocument;
				this.hitEntity = hitEntity;
			}

			public Map<String, Set<String>> getContexts() {
				return contexts;
			}

			public ScoreDoc getScoreDoc() {
				return scoreDoc;
			}

			@Nullable
			public SearchHit<T> getSearchHit() {
				return searchHit;
			}

			public void updateSearchHit(BiFunction<SearchDocument, T, SearchHit<T>> mapper) {
				searchHit = mapper.apply(searchDocument, hitEntity);
			}
		}
	}
}
