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

import java.time.Duration;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * @author Peter-Josef Meisch
 * @since 5.1
 */
public record PutComponentTemplateRequest(String name, @Nullable Long version, @Nullable Boolean create,
		@Nullable Duration masterTimeout, ComponentTemplateRequestData template) {
	public PutComponentTemplateRequest {
		Assert.notNull(name, "name must not be null");
		Assert.notNull(template, "template must not be null");
	}

	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder {
		@Nullable private String name;
		@Nullable private Long version;
		@Nullable private Boolean create;
		@Nullable private Duration masterTimeout;
		@Nullable private ComponentTemplateRequestData template;

		public Builder withName(String name) {
			this.name = name;
			return this;
		}

		public Builder withVersion(Long version) {
			this.version = version;
			return this;
		}

		public Builder withCreate(Boolean create) {
			this.create = create;
			return this;
		}

		public Builder withMasterTimeout(Duration masterTimeout) {
			this.masterTimeout = masterTimeout;
			return this;
		}

		public Builder withTemplateData(ComponentTemplateRequestData template) {
			this.template = template;
			return this;
		}

		public PutComponentTemplateRequest build() {

			Assert.notNull(name, "name must not be null");
			Assert.notNull(template, "template must not be null");

			return new PutComponentTemplateRequest(name, version, create, masterTimeout, template);
		}
	}
}
