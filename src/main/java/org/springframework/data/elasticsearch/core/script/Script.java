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
package org.springframework.data.elasticsearch.core.script;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * @author Peter-Josef Meisch
 * @since 5.1
 */
public record Script(String id, String language, String source) {
	public Script {

		Assert.notNull(id, "id must not be null");
		Assert.notNull(language, "language must not be null");
		Assert.notNull(source, "source must not be null");

	}

	public static ScriptBuilder builder() {
		return new ScriptBuilder();
	}

	public static final class ScriptBuilder {
		@Nullable private String id;
		@Nullable private String language;

		@Nullable private String source;

		private ScriptBuilder() {}

		public ScriptBuilder withId(String id) {

			Assert.notNull(id, "id must not be null");

			this.id = id;
			return this;
		}

		public ScriptBuilder withLanguage(String language) {

			Assert.notNull(language, "language must not be null");

			this.language = language;
			return this;
		}

		public ScriptBuilder withSource(String source) {

			Assert.notNull(source, "source must not be null");

			this.source = source;
			return this;
		}

		public Script build() {
			return new Script(id, language, source);
		}
	}
}
