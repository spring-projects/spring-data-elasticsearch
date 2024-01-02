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
public class HighlightParameters extends HighlightCommonParameters {
	private final String encoder;
	private final String tagsSchema;

	private HighlightParameters(HighlightParametersBuilder builder) {
		super(builder);
		encoder = builder.encoder;
		tagsSchema = builder.tagsSchema;
	}

	public String getEncoder() {
		return encoder;
	}

	public String getTagsSchema() {
		return tagsSchema;
	}

	public static HighlightParametersBuilder builder() {
		return new HighlightParametersBuilder();
	}

	public static final class HighlightParametersBuilder
			extends HighlightCommonParametersBuilder<HighlightParametersBuilder> {
		private String encoder = "";
		private String tagsSchema = "";

		public HighlightParametersBuilder withEncoder(String encoder) {
			this.encoder = encoder;
			return this;
		}

		public HighlightParametersBuilder withTagsSchema(String tagsSchema) {
			this.tagsSchema = tagsSchema;
			return this;
		}

		@Override
		public HighlightParameters build() {
			return new HighlightParameters(this);
		}
	}
}
