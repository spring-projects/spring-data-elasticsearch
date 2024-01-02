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

import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.Map;

/**
 * Request to create an index template. This is to create legacy templates (@see
 * https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-templates-v1.html)
 *
 * @author Peter-Josef Meisch
 * @since 4.1
 */
public class PutTemplateRequest {
	private final String name;
	private final String[] indexPatterns;
	@Nullable final private Settings settings;
	@Nullable final private Document mappings;
	@Nullable final AliasActions aliasActions;
	private final int order;
	@Nullable final Integer version;

	private PutTemplateRequest(String name, String[] indexPatterns, @Nullable Settings settings,
			@Nullable Document mappings, @Nullable AliasActions aliasActions, int order, @Nullable Integer version) {
		this.name = name;
		this.indexPatterns = indexPatterns;
		this.settings = settings;
		this.mappings = mappings;
		this.aliasActions = aliasActions;
		this.order = order;
		this.version = version;
	}

	public String getName() {
		return name;
	}

	public String[] getIndexPatterns() {
		return indexPatterns;
	}

	@Nullable
	public Settings getSettings() {
		return settings;
	}

	@Nullable
	public Document getMappings() {
		return mappings;
	}

	@Nullable
	public AliasActions getAliasActions() {
		return aliasActions;
	}

	public int getOrder() {
		return order;
	}

	@Nullable
	public Integer getVersion() {
		return version;
	}

	public static TemplateRequestBuilder builder(String name, String... indexPatterns) {
		return new TemplateRequestBuilder(name, indexPatterns);
	}

	public static final class TemplateRequestBuilder {
		private final String name;
		private final String[] indexPatterns;
		@Nullable private Settings settings;
		@Nullable private Document mappings;
		@Nullable AliasActions aliasActions;
		private int order = 0;
		@Nullable Integer version;

		private TemplateRequestBuilder(String name, String... indexPatterns) {

			Assert.notNull(name, "name must not be null");
			Assert.notNull(indexPatterns, "indexPatterns must not be null");

			this.name = name;
			this.indexPatterns = indexPatterns;
		}

		public TemplateRequestBuilder withSettings(Map<String, Object> settings) {
			this.settings = new Settings(settings);
			return this;
		}

		public TemplateRequestBuilder withMappings(Document mappings) {
			this.mappings = mappings;
			return this;
		}

		public TemplateRequestBuilder withAliasActions(AliasActions aliasActions) {

			aliasActions.getActions().forEach(action -> Assert.isTrue(action instanceof AliasAction.Add,
					"only alias add actions are allowed in templates"));

			this.aliasActions = aliasActions;
			return this;
		}

		public TemplateRequestBuilder withOrder(int order) {
			this.order = order;
			return this;
		}

		public TemplateRequestBuilder withVersion(@Nullable Integer version) {
			this.version = version;
			return this;
		}

		public PutTemplateRequest build() {
			return new PutTemplateRequest(name, indexPatterns, settings, mappings, aliasActions, order, version);
		}
	}
}
