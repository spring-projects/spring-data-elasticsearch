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

import org.springframework.dao.DataRetrievalFailureException;

import java.util.Map;

/**
 * @author Peter-Josef Meisch
 * @author Illia Ulianov
 * @since 4.1
 */
public class BulkFailureException extends DataRetrievalFailureException {
	private final Map<String, FailureDetails> failedDocuments;

	public BulkFailureException(String msg, Map<String, FailureDetails> failedDocuments) {
		super(msg);
		this.failedDocuments = failedDocuments;
	}

	public Map<String, FailureDetails> getFailedDocuments() {
		return failedDocuments;
	}

	/**
	 * Details about a document saving failure.
	 *
	 * @author Illia Ulianov
	 * @since 5.2
	 */
	public record FailureDetails(Integer status, String errorMessage) {
	}
}
