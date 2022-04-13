/*
 * Copyright 2022 the original author or authors.
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

import org.springframework.data.elasticsearch.core.ScriptType;
import org.springframework.lang.Nullable;

/**
 * value class combining script information.
 *
 * @author Peter-Josef Meisch
 * @since 4.4
 */
public final class ScriptData {
	@Nullable private final ScriptType type;
	@Nullable private final String language;
	@Nullable private final String script;
	@Nullable private final String scriptName;
	@Nullable private final Map<String, Object> params;

	public ScriptData(@Nullable ScriptType type, @Nullable String language, @Nullable String script,
			@Nullable String scriptName, @Nullable Map<String, Object> params) {

		this.type = type;
		this.language = language;
		this.script = script;
		this.scriptName = scriptName;
		this.params = params;
	}

	@Nullable
	public ScriptType getType() {
		return type;
	}

	@Nullable
	public String getLanguage() {
		return language;
	}

	@Nullable
	public String getScript() {
		return script;
	}

	@Nullable
	public String getScriptName() {
		return scriptName;
	}

	@Nullable
	public Map<String, Object> getParams() {
		return params;
	}
}
