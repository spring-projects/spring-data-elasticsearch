/*
 * Copyright 2021 the original author or authors.
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

import org.springframework.data.elasticsearch.support.DefaultStringObjectMap;

import java.util.Map;

/**
 * class defining the settings for an index.
 *
 * @author Peter-Josef Meisch
 * @since 4.2
 */
public class Settings extends DefaultStringObjectMap<Settings> {

	public Settings() {
	}

	public Settings(Map<String, Object> map) {
		super(map);
	}

	/**
	 * Creates a {@link Settings} object from the given JSON String
	 * @param json must not be {@literal null}
	 * @return Settings object
	 */
	public static Settings parse(String json) {
		return new Settings().fromJson(json);
	}

	@Override
	public String toString() {
		return "Settings: " + toJson();
	}
}
