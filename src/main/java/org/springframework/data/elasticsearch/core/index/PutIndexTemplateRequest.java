/*
 * Copyright 2023-2024 the original author or authors.
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
package org.springframework.data.elasticsearch.core.index;

import java.util.List;

import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * @author Peter-Josef Meisch
 * @since 5.1
 */
public record PutIndexTemplateRequest(String name, String[] indexPatterns, @Nullable Settings settings,
		@Nullable Document mapping, @Nullable AliasActions aliasActions, List<String> composedOf) {

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		@Nullable private String name;
		@Nullable private String[] indexPatterns;
		@Nullable private Settings settings;
		@Nullable private Document mapping;
		@Nullable AliasActions aliasActions;

		@Nullable List<String> composedOf;

		public Builder withName(String name) {
			this.name = name;
			return this;
		}

		public Builder withIndexPatterns(String... indexPatterns) {
			this.indexPatterns = indexPatterns;
			return this;
		}

		public Builder withSettings(Settings settings) {
			this.settings = settings;
			return this;
		}

		public Builder withMapping(Document mapping) {
			this.mapping = mapping;
			return this;
		}

		public Builder withAliasActions(AliasActions aliasActions) {
			this.aliasActions = aliasActions;
			return this;
		}

		public Builder withComposedOf(List<String> composedOf) {
			this.composedOf = composedOf;
			return this;
		}

		public PutIndexTemplateRequest build() {

			Assert.notNull(name, "name must not be null");
			Assert.notNull(indexPatterns, "indexPatterns must not be null");
			Assert.isTrue(indexPatterns.length > 0, "indexPatterns must not be empty");

			return new PutIndexTemplateRequest(name, indexPatterns, settings, mapping, aliasActions,
					composedOf != null ? composedOf : List.of());
		}
	}
}
