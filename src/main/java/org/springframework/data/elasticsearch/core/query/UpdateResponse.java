/*
 * Copyright 2020 the original author or authors.
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
 * Response data from an update request ({@link UpdateQuery}). Currently contains only the result status value from
 * Elasticsearch. Should be extended if further information is needed.
 * 
 * @author Peter-Josef Meisch
 * @since 4.0
 */
public class UpdateResponse {

	private Result result;

	public UpdateResponse(Result result) {

		Assert.notNull(result, "result must not be null");

		this.result = result;
	}

	public Result getResult() {
		return result;
	}

	public enum Result {
		CREATED, UPDATED, DELETED, NOT_FOUND, NOOP;
	}
}
