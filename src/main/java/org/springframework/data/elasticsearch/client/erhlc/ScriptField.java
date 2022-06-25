/*
 * Copyright 2021-2022 the original author or authors.
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
package org.springframework.data.elasticsearch.client.erhlc;

import org.elasticsearch.script.Script;

/**
 * @author Ryan Murfitt
 * @author Artur Konczak
 * @deprecated since 5.0
 */
@Deprecated
public class ScriptField {

	private final String fieldName;
	private final Script script;

	public ScriptField(String fieldName, Script script) {
		this.fieldName = fieldName;
		this.script = script;
	}

	public String fieldName() {
		return fieldName;
	}

	public Script script() {
		return script;
	}
}
