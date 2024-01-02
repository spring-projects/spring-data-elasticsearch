/*
 * Copyright 2022-2024 the original author or authors.
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
package org.springframework.data.elasticsearch.core.query;

import java.util.Map;
import java.util.function.Function;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * value class combining script information.
 *
 * @author Peter-Josef Meisch
 * @since 4.4
 */
public record ScriptData(ScriptType type, @Nullable String language, @Nullable String script,
		@Nullable String scriptName, @Nullable Map<String, Object> params) {

	public ScriptData(ScriptType type, @Nullable String language, @Nullable String script, @Nullable String scriptName,
			@Nullable Map<String, Object> params) {

		Assert.notNull(type, "type must not be null");

		this.type = type;
		this.language = language;
		this.script = script;
		this.scriptName = scriptName;
		this.params = params;
	}

	/**
	 * @since 5.2
	 */
	public static ScriptData of(ScriptType type, @Nullable String language, @Nullable String script,
			@Nullable String scriptName, @Nullable Map<String, Object> params) {
		return new ScriptData(type, language, script, scriptName, params);
	}

	public static ScriptData of(Function<Builder, Builder> builderFunction) {

		Assert.notNull(builderFunction, "f must not be null");

		return builderFunction.apply(new Builder()).build();
	}

	/**
	 * @since 5.2
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * @since 5.2
	 */
	public static final class Builder {
		@Nullable private ScriptType type;
		@Nullable private String language;
		@Nullable private String script;
		@Nullable private String scriptName;
		@Nullable private Map<String, Object> params;

		private Builder() {}

		public Builder withType(ScriptType type) {

			Assert.notNull(type, "type must not be null");

			this.type = type;
			return this;
		}

		public Builder withLanguage(@Nullable String language) {
			this.language = language;
			return this;
		}

		public Builder withScript(@Nullable String script) {
			this.script = script;
			return this;
		}

		public Builder withScriptName(@Nullable String scriptName) {
			this.scriptName = scriptName;
			return this;
		}

		public Builder withParams(@Nullable Map<String, Object> params) {
			this.params = params;
			return this;
		}

		public ScriptData build() {

			Assert.notNull(type, "type must be set");

			return new ScriptData(type, language, script, scriptName, params);
		}
	}
}
