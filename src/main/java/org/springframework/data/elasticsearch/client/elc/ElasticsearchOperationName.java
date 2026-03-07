/*
 * Copyright 2026-present the original author or authors.
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
package org.springframework.data.elasticsearch.client.elc;

/**
 * Enumeration of Spring Data Elasticsearch operation names used in observations.
 *
 * @author maryantocinn
 * @since 6.1
 */
public enum ElasticsearchOperationName {

	SAVE("save"), //
	INDEX("index"), //
	GET("get"), //
	MULTI_GET("multiGet"), //
	EXISTS("exists"), //
	DELETE("delete"), //
	DELETE_BY_QUERY("deleteByQuery"), //
	BULK("bulk"), //
	UPDATE("update"), //
	UPDATE_BY_QUERY("updateByQuery"), //
	COUNT("count"), //
	SEARCH("search");

	private final String value;

	ElasticsearchOperationName(String value) {
		this.value = value;
	}

	/**
	 * @return the operation name as a string value used in observation key values.
	 */
	public String getValue() {
		return value;
	}

	@Override
	public String toString() {
		return value;
	}
}
