/*
 * Copyright 2023-2024 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * @author Illia Ulianov
 */
class BulkFailureExceptionTest {

	@Test // #2619
	void shouldCreateBulkException() {
		String documentId = "id1";
		var failureDetails = new BulkFailureException.FailureDetails(409, "conflict");
		var exception = new BulkFailureException("Test message", Map.of(documentId, failureDetails));
		assertThat(exception.getFailedDocuments()).containsEntry(documentId, failureDetails);
	}
}
