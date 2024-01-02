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

import org.springframework.lang.Nullable;

import java.util.List;

/**
 * @author Peter-Josef Meisch
 */
public class TermSuggestion extends Suggest.Suggestion<TermSuggestion.Entry> {

	@Nullable private final SortBy sort;

	public TermSuggestion(String name, int size, List<Entry> entries, @Nullable SortBy sort) {
		super(name, size, entries);
		this.sort = sort;
	}

	@Nullable
	public SortBy getSort() {
		return sort;
	}

	public static class Entry extends Suggest.Suggestion.Entry<Entry.Option> {

		public Entry(String text, int offset, int length, List<Option> options) {
			super(text, offset, length, options);
		}

		public static class Option extends Suggest.Suggestion.Entry.Option {

			private final int freq;

			public Option(String text, @Nullable String highlighted, double score, @Nullable Boolean collateMatch, int freq) {
				super(text, highlighted, score, collateMatch);
				this.freq = freq;
			}

			public int getFreq() {
				return freq;
			}
		}
	}
}
