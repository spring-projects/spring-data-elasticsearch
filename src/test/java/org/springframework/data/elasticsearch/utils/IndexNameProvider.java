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
package org.springframework.data.elasticsearch.utils;

/**
 * Class providing an index name with a prefix, an index number and a random 6-digit number.
 *
 * @author Peter-Josef Meisch
 */
public class IndexNameProvider {
	private final String prefix;
	private int idx = -1;
	private String indexName;

	public IndexNameProvider() {
		this("index-default");
	}

	public IndexNameProvider(String prefix) {
		this.prefix = prefix;
		increment();
	}

	public void increment() {
		indexName = prefix + '-' + ++idx + '-' + sixDigits();
	}

	public String indexName() {
		return indexName;
	}

	/**
	 * @since 4.4
	 */
	public String getPrefix() {
		return prefix;
	}

	private String sixDigits() {
		return String.valueOf((int) (100000 + Math.random() * 900000));
	}
}
