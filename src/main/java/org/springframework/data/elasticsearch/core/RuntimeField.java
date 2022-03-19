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
package org.springframework.data.elasticsearch.core;

import java.util.HashMap;
import java.util.Map;

import org.springframework.util.Assert;

/**
 * Defines a runtime field to be added to a Query
 *
 * @author Peter-Josef Meisch
 * @since 4.3
 */
public class RuntimeField {

	private final String name;
	private final String type;
	private final String script;

	public RuntimeField(String name, String type, String script) {

		Assert.notNull(name, "name must not be null");
		Assert.notNull(type, "type must not be null");
		Assert.notNull(script, "script must not be null");

		this.name = name;
		this.type = type;
		this.script = script;
	}

	public String getName() {
		return name;
	}

	/**
	 * @return the mapping as a Map like it is needed for the Elasticsearch client
	 */
	public Map<String, Object> getMapping() {

		Map<String, Object> map = new HashMap<>();
		map.put("type", type);
		map.put("script", script);
		return map;
	}

	/**
	 * @since 4.4
	 */
	public String getType() {
		return type;
	}

	/**
	 * @since 4.4
	 */
	public String getScript() {
		return script;
	}
}
