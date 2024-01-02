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
 * @since 4.3
 */
public class PhraseSuggestion extends Suggest.Suggestion<PhraseSuggestion.Entry> {

	public PhraseSuggestion(String name, int size, List<Entry> entries) {
		super(name, size, entries);
	}

	public static class Entry extends Suggest.Suggestion.Entry<Entry.Option> {

		@Nullable private final Double cutoffScore;

		public Entry(String text, int offset, int length, List<Option> options, @Nullable Double cutoffScore) {
			super(text, offset, length, options);
			this.cutoffScore = cutoffScore;
		}

		@Nullable
		public Double getCutoffScore() {
			return cutoffScore;
		}

		public static class Option extends Suggest.Suggestion.Entry.Option {

			public Option(String text, String highlighted, @Nullable Double score, @Nullable Boolean collateMatch) {
				super(text, highlighted, score, collateMatch);
			}
		}
	}
}
