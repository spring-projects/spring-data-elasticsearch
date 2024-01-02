/*
 * Copyright 2020-2024 the original author or authors.
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
package org.springframework.data.elasticsearch;

import org.springframework.dao.UncategorizedDataAccessException;
import org.springframework.lang.Nullable;

/**
 * @author Peter-Josef Meisch
 * @since 4.0
 */
public class UncategorizedElasticsearchException extends UncategorizedDataAccessException {

	/**
	 * the response status code from Elasticsearch if available
	 *
	 * @since 4.4
	 */
	@Nullable private final Integer statusCode;

	/**
	 * The response body from Elasticsearch if available
	 *
	 * @since 4.4
	 */
	@Nullable final String responseBody;

	public UncategorizedElasticsearchException(String msg) {
		this(msg, null);
	}

	public UncategorizedElasticsearchException(String msg, @Nullable Throwable cause) {
		this(msg, null, null, cause);
	}

	public UncategorizedElasticsearchException(String msg, @Nullable Integer statusCode, @Nullable String responseBody,
			@Nullable Throwable cause) {
		super(msg, cause);
		this.statusCode = statusCode;
		this.responseBody = responseBody;
	}

	/**
	 * @since 4.4
	 */
	@Nullable
	public Integer getStatusCode() {
		return statusCode;
	}

	/**
	 * @since 4.4
	 */
	@Nullable
	public String getResponseBody() {
		return responseBody;
	}
}
