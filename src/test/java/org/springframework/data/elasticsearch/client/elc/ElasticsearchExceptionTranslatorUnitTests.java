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

import static org.assertj.core.api.Assertions.*;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.ErrorResponse;
import co.elastic.clients.json.jackson.Jackson3JsonpMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

/**
 * @author Minju Kim
 */
class ElasticsearchExceptionTranslatorUnitTests {

	private final ElasticsearchExceptionTranslator translator = new ElasticsearchExceptionTranslator(
			new Jackson3JsonpMapper());

	@Test // #3364
	@DisplayName("should translate conflict on create of existing document to DuplicateKeyException")
	void shouldTranslateConflictOnCreateOfExistingDocumentToDuplicateKeyException() {

		var exception = new ElasticsearchException("es/index", ErrorResponse.of(r -> r //
				.status(409) //
				.error(e -> e //
						.type("version_conflict_engine_exception") //
						.reason("[42]: version conflict, document already exists (current version [1])"))));

		assertThatThrownBy(() -> translator.translateException(exception)).isInstanceOf(DuplicateKeyException.class)
				.hasCause(exception);
	}
}
