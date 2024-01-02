/*
 * Copyright 2020-2024 the original author or authors.
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

import java.util.Map;

import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.lang.Nullable;

/**
 * Data returned for template information retrieval.
 *
 * @author Peter-Josef Meisch
 */
public class TemplateData {
	@Nullable private final String[] indexPatterns;
	@Nullable Settings settings;
	@Nullable Document mapping;
	@Nullable private final Map<String, AliasData> aliases;
	int order;
	@Nullable Integer version;

	private TemplateData(@Nullable String[] indexPatterns, @Nullable Settings settings, @Nullable Document mapping,
			@Nullable Map<String, AliasData> aliases, int order, @Nullable Integer version) {
		this.indexPatterns = indexPatterns;
		this.settings = settings;
		this.mapping = mapping;
		this.order = order;
		this.version = version;
		this.aliases = aliases;
	}

	public static TemplateDataBuilder builder() {
		return new TemplateDataBuilder();
	}

	@Nullable
	public String[] getIndexPatterns() {
		return indexPatterns;
	}

	@Nullable
	public Settings getSettings() {
		return settings;
	}

	@Nullable
	public Document getMapping() {
		return mapping;
	}

	@Nullable
	public Map<String, AliasData> getAliases() {
		return aliases;
	}

	public int getOrder() {
		return order;
	}

	@Nullable
	public Integer getVersion() {
		return version;
	}

	public static final class TemplateDataBuilder {
		@Nullable Settings settings;
		@Nullable Document mapping;
		int order;
		@Nullable Integer version;
		@Nullable private String[] indexPatterns;
		@Nullable private Map<String, AliasData> aliases;

		private TemplateDataBuilder() {}

		public TemplateDataBuilder withIndexPatterns(String... indexPatterns) {
			this.indexPatterns = indexPatterns;
			return this;
		}

		public TemplateDataBuilder withSettings(Map<String, Object> settings) {
			this.settings = new Settings(settings);
			return this;
		}

		public TemplateDataBuilder withMapping(Document mapping) {
			this.mapping = mapping;
			return this;
		}

		public TemplateDataBuilder withOrder(int order) {
			this.order = order;
			return this;
		}

		public TemplateDataBuilder withVersion(Integer version) {
			this.version = version;
			return this;
		}

		public TemplateDataBuilder withAliases(Map<String, AliasData> aliases) {
			this.aliases = aliases;
			return this;
		}

		public TemplateData build() {
			return new TemplateData(indexPatterns, settings, mapping, aliases, order, version);
		}
	}
}
