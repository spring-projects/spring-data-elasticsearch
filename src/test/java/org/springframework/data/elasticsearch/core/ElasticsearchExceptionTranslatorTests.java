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
package org.springframework.data.elasticsearch.core;

import static org.assertj.core.api.Assertions.*;

import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.index.engine.VersionConflictEngineException;
import org.elasticsearch.index.shard.ShardId;
import org.elasticsearch.rest.RestStatus;
import org.junit.jupiter.api.Test;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;

/**
 * @author Roman Puchkovskiy
 * @author Peter-Josef Meisch
 */
class ElasticsearchExceptionTranslatorTests {
	private final ElasticsearchExceptionTranslator translator = new ElasticsearchExceptionTranslator();

	@Test // DATAES-799
	void shouldConvertElasticsearchStatusExceptionWithSeqNoConflictToOptimisticLockingFailureException() {
		ElasticsearchStatusException ex = new ElasticsearchStatusException(
				"Elasticsearch exception [type=version_conflict_engine_exception, reason=[WPUUsXEB6uuA6j8_A7AB]: version conflict, required seqNo [34], primary term [16]. current document has seqNo [35] and primary term [16]]",
				RestStatus.CONFLICT);

		DataAccessException translated = translator.translateExceptionIfPossible(ex);

		assertThat(translated).isInstanceOf(OptimisticLockingFailureException.class);
		assertThat(translated.getMessage()).startsWith("Cannot index a document due to seq_no+primary_term conflict");
		assertThat(translated.getCause()).isSameAs(ex);
	}

	@Test // DATAES-799
	void shouldConvertVersionConflictEngineExceptionWithSeqNoConflictToOptimisticLockingFailureException() {
		VersionConflictEngineException ex = new VersionConflictEngineException(new ShardId("index", "uuid", 1),
				"exception-id",
				"Elasticsearch exception [type=version_conflict_engine_exception, reason=[WPUUsXEB6uuA6j8_A7AB]: version conflict, required seqNo [34], primary term [16]. current document has seqNo [35] and primary term [16]]");

		DataAccessException translated = translator.translateExceptionIfPossible(ex);

		assertThat(translated).isInstanceOf(OptimisticLockingFailureException.class);
		assertThat(translated.getMessage()).startsWith("Cannot index a document due to seq_no+primary_term conflict");
		assertThat(translated.getCause()).isSameAs(ex);
	}

}
