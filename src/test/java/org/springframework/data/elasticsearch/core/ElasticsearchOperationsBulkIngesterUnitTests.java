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
package org.springframework.data.elasticsearch.core;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.elasticsearch.client.UnsupportedClientOperationException;
import org.springframework.data.elasticsearch.core.bulk.BulkIngesterListener;
import org.springframework.data.elasticsearch.core.bulk.BulkIngesterOptions;
import org.springframework.data.elasticsearch.core.bulk.BulkOperationResult;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;

/**
 * @author Ralph Ursprung
 */
class ElasticsearchOperationsBulkIngesterUnitTests {

	private final BulkIngesterListener<String> listener = new BulkIngesterListener<>() {
		@Override
		public void afterBulk(long executionId, List<BulkOperationResult<String>> results) {}

		@Override
		public void afterBulk(long executionId, List<@Nullable String> contexts, RuntimeException failure) {}
	};

	@Test // #2071
	@DisplayName("should throw UnsupportedClientOperationException by default")
	void shouldThrowUnsupportedClientOperationExceptionByDefault() {

		final var operations = mock(ElasticsearchOperations.class, CALLS_REAL_METHODS);

		assertThatThrownBy(() -> operations.bulkIngester(BulkIngesterOptions.defaultOptions(), IndexCoordinates.of("foo"),
				listener)).isInstanceOf(UnsupportedClientOperationException.class).hasMessageContaining("bulkIngester");
	}

	@Test // #2071
	@DisplayName("should use default options and the index of the entity class in the overloads")
	void shouldUseDefaultOptionsAndTheIndexOfTheEntityClassInTheOverloads() {

		final var operations = mock(ElasticsearchOperations.class, CALLS_REAL_METHODS);
		final var index = IndexCoordinates.of("entity-index");
		doReturn(index).when(operations).getIndexCoordinatesFor(String.class);
		doReturn(null).when(operations).bulkIngester(any(BulkIngesterOptions.class), any(IndexCoordinates.class),
				any(BulkIngesterListener.class));

		operations.bulkIngester(String.class, listener);
		operations.bulkIngester(IndexCoordinates.of("foo"), listener);

		verify(operations).bulkIngester(BulkIngesterOptions.defaultOptions(), index, listener);
		verify(operations).bulkIngester(BulkIngesterOptions.defaultOptions(), IndexCoordinates.of("foo"), listener);
	}
}
