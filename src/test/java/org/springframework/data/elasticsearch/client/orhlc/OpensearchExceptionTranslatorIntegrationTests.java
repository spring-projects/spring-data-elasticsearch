/*
 * Copyright 2020-2022 the original author or authors.
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
package org.springframework.data.elasticsearch.client.orhlc;

import static org.assertj.core.api.Assertions.*;

import org.opensearch.OpenSearchStatusException;
import org.opensearch.index.engine.VersionConflictEngineException;
import org.opensearch.index.shard.ShardId;
import org.opensearch.rest.RestStatus;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.elasticsearch.EnabledIfOpensearch;
import org.springframework.data.elasticsearch.junit.jupiter.Tags;

/**
 * @author Roman Puchkovskiy
 * @author Peter-Josef Meisch
 * @author Andriy Redko
 */
@EnabledIfOpensearch
@Tag(Tags.INTEGRATION_TEST)
class OpensearchExceptionTranslatorIntegrationTests {
	private final OpensearchExceptionTranslator translator = new OpensearchExceptionTranslator();

	@Test // DATAES-799
	void shouldConvertOpensearchStatusExceptionWithSeqNoConflictToOptimisticLockingFailureException() {
		OpenSearchStatusException ex = new OpenSearchStatusException(
				"Opensearch exception [type=version_conflict_engine_exception, reason=[WPUUsXEB6uuA6j8_A7AB]: version conflict, required seqNo [34], primary term [16]. current document has seqNo [35] and primary term [16]]",
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
				"Opensearch exception [type=version_conflict_engine_exception, reason=[WPUUsXEB6uuA6j8_A7AB]: version conflict, required seqNo [34], primary term [16]. current document has seqNo [35] and primary term [16]]");

		DataAccessException translated = translator.translateExceptionIfPossible(ex);

		assertThat(translated).isInstanceOf(OptimisticLockingFailureException.class);
		assertThat(translated.getMessage()).startsWith("Cannot index a document due to seq_no+primary_term conflict");
		assertThat(translated.getCause()).isSameAs(ex);
	}

}
