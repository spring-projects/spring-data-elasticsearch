/*
 * Copyright 2021-2024 the original author or authors.
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
package org.springframework.data.elasticsearch.annotations;

/**
 * Values for the {@code dynamic} mapping parameter.
 *
 * @author Sascha Woo
 * @since 4.3
 */
public enum Dynamic {
	/**
	 * New fields are added to the mapping.
	 */
	TRUE("true"),
	/**
	 * New fields are added to the mapping as
	 * <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/runtime.html">runtime fields</a>. These
	 * fields are not indexed, and are loaded from {@code _source} at query time.
	 */
	RUNTIME("runtime"),
	/**
	 * New fields are ignored. These fields will not be indexed or searchable, but will still appear in the
	 * {@code _source} field of returned hits. These fields will not be added to the mapping, and new fields must be added
	 * explicitly.
	 */
	FALSE("false"),
	/**
	 * If new fields are detected, an exception is thrown and the document is rejected. New fields must be explicitly
	 * added to the mapping.
	 */
	STRICT("strict"),
	/**
	 * Inherit the dynamic setting from their parent object or from the mapping type.
	 */
	INHERIT("inherit");

	private final String mappedName;

	Dynamic(String mappedName) {
		this.mappedName = mappedName;
	}

	public String getMappedName() {
		return mappedName;
	}
}
