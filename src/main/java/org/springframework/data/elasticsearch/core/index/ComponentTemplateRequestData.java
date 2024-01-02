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

import java.util.Map;

import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * A component template to be used in a component template request.
 *
 * @author Peter-Josef Meisch
 * @since 5.1
 */
public record ComponentTemplateRequestData(@Nullable Settings settings, @Nullable Document mapping,
		@Nullable AliasActions aliasActions, @Nullable Boolean allowAutoCreate) {

	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder {
		@Nullable private Settings settings;
		@Nullable private Document mapping;
		@Nullable private AliasActions aliasActions;
		@Nullable private Boolean allowAutoCreate;

		public Builder withSettings(Map<String, Object> settings) {
			this.settings = new Settings(settings);
			return this;
		}

		public Builder withMapping(Document mapping) {
			this.mapping = mapping;
			return this;
		}

		public Builder withAliasActions(AliasActions aliasActions) {

			aliasActions.getActions().forEach(action -> Assert.isTrue(action instanceof AliasAction.Add,
					"only alias add actions are allowed in templates"));

			this.aliasActions = aliasActions;
			return this;
		}

		public Builder withAllowAutoCreate(@Nullable Boolean allowAutoCreate) {
			this.allowAutoCreate = allowAutoCreate;
			return this;
		}

		public ComponentTemplateRequestData build() {
			return new ComponentTemplateRequestData(settings, mapping, aliasActions, allowAutoCreate);
		}
	}
}
