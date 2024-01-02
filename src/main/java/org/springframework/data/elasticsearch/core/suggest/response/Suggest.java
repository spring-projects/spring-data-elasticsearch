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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.lang.Nullable;

/**
 * Class structure mirroring the Elasticsearch classes for a suggest response.
 *
 * @author Peter-Josef Meisch
 * @since 4.3
 */
public class Suggest {
	private final List<Suggestion<? extends Suggestion.Entry<? extends Suggestion.Entry.Option>>> suggestions;
	private final Map<String, Suggestion<? extends Suggestion.Entry<? extends Suggestion.Entry.Option>>> suggestionsMap;
	private final boolean hasScoreDocs;

	public Suggest(List<Suggestion<? extends Suggestion.Entry<? extends Suggestion.Entry.Option>>> suggestions,
			boolean hasScoreDocs) {
		this.suggestions = suggestions;
		this.suggestionsMap = new HashMap<>();
		suggestions.forEach(suggestion -> suggestionsMap.put(suggestion.getName(), suggestion));
		this.hasScoreDocs = hasScoreDocs;
	}

	public List<Suggestion<? extends Suggestion.Entry<? extends Suggestion.Entry.Option>>> getSuggestions() {
		return suggestions;
	}

	public Suggestion<? extends Suggestion.Entry<? extends Suggestion.Entry.Option>> getSuggestion(String name) {
		return suggestionsMap.get(name);
	}

	public boolean hasScoreDocs() {
		return hasScoreDocs;
	}

	public abstract static class Suggestion<E extends Suggestion.Entry<? extends Suggestion.Entry.Option>> {
		private final String name;
		private final int size;
		private final List<E> entries;

		public Suggestion(String name, int size, List<E> entries) {
			this.name = name;
			this.size = size;
			this.entries = entries;
		}

		public String getName() {
			return name;
		}

		public int getSize() {
			return size;
		}

		public List<E> getEntries() {
			return entries;
		}

		public abstract static class Entry<O extends Entry.Option> {
			private final String text;
			private final int offset;
			private final int length;
			private final List<O> options;

			public Entry(String text, int offset, int length, List<O> options) {
				this.text = text;
				this.offset = offset;
				this.length = length;
				this.options = options;
			}

			public String getText() {
				return text;
			}

			public int getOffset() {
				return offset;
			}

			public int getLength() {
				return length;
			}

			public List<O> getOptions() {
				return options;
			}

			public abstract static class Option {
				private final String text;
				@Nullable private final String highlighted;
				@Nullable private final Double score;
				@Nullable private final Boolean collateMatch;

				public Option(String text, @Nullable String highlighted, @Nullable Double score,
						@Nullable Boolean collateMatch) {
					this.text = text;
					this.highlighted = highlighted;
					this.score = score;
					this.collateMatch = collateMatch;
				}

				public String getText() {
					return text;
				}

				@Nullable
				public String getHighlighted() {
					return highlighted;
				}

				@Nullable
				public Double getScore() {
					return score;
				}

				@Nullable
				public Boolean getCollateMatch() {
					return collateMatch;
				}
			}
		}
	}
}
