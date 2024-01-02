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
package org.springframework.data.elasticsearch.core.query.highlight;

/**
 * @author Peter-Josef Meisch
 * @since 4.3
 */
public class HighlightFieldParameters extends HighlightCommonParameters {
	private final int fragmentOffset;
	private final String[] matchedFields;

	private HighlightFieldParameters(HighlightFieldParametersBuilder builder) {
		super(builder);
		fragmentOffset = builder.fragmentOffset;
		matchedFields = builder.matchedFields;
	}

	public int getFragmentOffset() {
		return fragmentOffset;
	}

	public String[] getMatchedFields() {
		return matchedFields;
	}

	public static HighlightFieldParametersBuilder builder() {
		return new HighlightFieldParametersBuilder();
	}

	public static final class HighlightFieldParametersBuilder
			extends HighlightCommonParametersBuilder<HighlightFieldParametersBuilder> {
		private int fragmentOffset = -1;
		private String[] matchedFields = new String[0];

		public HighlightFieldParametersBuilder withFragmentOffset(int fragmentOffset) {
			this.fragmentOffset = fragmentOffset;
			return this;
		}

		public HighlightFieldParametersBuilder withMatchedFields(String... matchedFields) {
			this.matchedFields = matchedFields;
			return this;
		}

		@Override
		public HighlightFieldParameters build() {
			return new HighlightFieldParameters(this);
		}
	}
}
