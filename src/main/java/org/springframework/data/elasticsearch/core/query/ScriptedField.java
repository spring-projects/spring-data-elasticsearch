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

import org.springframework.util.Assert;

/**
 * Class defining a scripted field to be used in a {@link Query}. Must be set by using the builder for a query.
 *
 * @author Peter-Josef Meisch
 * @since 4.4
 */
public class ScriptedField {

	private final String fieldName;
	private final ScriptData scriptData;

	/**
	 * @since 5.2
	 */
	public static ScriptedField of(String fieldName, ScriptData scriptData) {
		return new ScriptedField(fieldName, scriptData);
	}

	public ScriptedField(String fieldName, ScriptData scriptData) {

		Assert.notNull(fieldName, "fieldName must not be null");
		Assert.notNull(scriptData, "scriptData must not be null");

		this.fieldName = fieldName;
		this.scriptData = scriptData;
	}

	public String getFieldName() {
		return fieldName;
	}

	public ScriptData getScriptData() {
		return scriptData;
	}
}
