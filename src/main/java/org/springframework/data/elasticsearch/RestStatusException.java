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
package org.springframework.data.elasticsearch;

import org.springframework.dao.DataAccessException;

/**
 * Exception class for REST status exceptions independent from the used client/backend.
 *
 * @author Peter-Josef Meisch
 * @since 4.3
 */
public class RestStatusException extends DataAccessException {

	// we do not use a dedicated status class from Elasticsearch, OpenSearch, Spring web or webflux here
	private final int status;

	public RestStatusException(int status, String msg) {
		super(msg);
		this.status = status;
	}

	public RestStatusException(int status, String msg, Throwable cause) {
		super(msg, cause);
		this.status = status;
	}

	public int getStatus() {
		return status;
	}

	@Override
	public String toString() {
		return "RestStatusException{" + "status=" + status + "} " + super.toString();
	}
}
