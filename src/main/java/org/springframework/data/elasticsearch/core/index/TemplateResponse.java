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

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * @author Peter-Josef Meisch
 * @since 5.1
 */
public record TemplateResponse(String name, @Nullable Long version, @Nullable TemplateResponseData templateData) {
	public TemplateResponse {
		Assert.notNull(name, "name must not be null");
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		@Nullable private String name;
		@Nullable private Long version;
		@Nullable private TemplateResponseData templateData;

		public Builder withName(String name) {
			this.name = name;
			return this;
		}

		public Builder withVersion(Long version) {
			this.version = version;
			return this;
		}

		public Builder withTemplateData(TemplateResponseData templateData) {
			this.templateData = templateData;
			return this;
		}

		public TemplateResponse build() {
			return new TemplateResponse(name, version, templateData);
		}
	}
}
