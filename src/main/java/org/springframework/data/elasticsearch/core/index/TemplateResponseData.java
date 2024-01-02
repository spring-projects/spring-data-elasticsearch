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
import java.util.Map;

import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * @author Peter-Josef Meisch
 * @since 5.1
 */
public record TemplateResponseData(@Nullable Document mapping, @Nullable Settings settings,
		Map<String, AliasData> aliases, List<String> composedOf) {

	public TemplateResponseData {
		Assert.notNull(aliases, "aliases must not be null");
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		@Nullable private Document mapping;
		@Nullable private Settings settings;
		@Nullable private Map<String, AliasData> aliases;

		@Nullable private List<String> composedOf;

		public Builder withMapping(@Nullable Document mapping) {
			this.mapping = mapping;
			return this;
		}

		public Builder withSettings(@Nullable Settings settings) {
			this.settings = settings;
			return this;
		}

		public Builder withAliases(@Nullable Map<String, AliasData> aliases) {
			this.aliases = aliases;
			return this;
		}

		public Builder withComposedOf(@Nullable List<String> composedOf) {
			this.composedOf = composedOf;
			return this;
		}

		public TemplateResponseData build() {
			return new TemplateResponseData(mapping, settings, aliases != null ? aliases : Map.of(),
					composedOf != null ? composedOf : List.of());
		}
	}
}
