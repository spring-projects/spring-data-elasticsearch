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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;

/**
 * Unit tests for {@link ElasticsearchObservationContext}.
 *
 * @author maryantocinn
 */
class ElasticsearchObservationContextTests {

	@Test
	@DisplayName("should carry operation name and index coordinates")
	void shouldCarryOperationNameAndIndex() {

		IndexCoordinates index = IndexCoordinates.of("my-index");
		ElasticsearchObservationContext context = new ElasticsearchObservationContext(ElasticsearchOperationName.SEARCH,
				index);

		assertThat(context.getOperationName()).isEqualTo(ElasticsearchOperationName.SEARCH);
		assertThat(context.getIndexCoordinates()).isEqualTo(index);
		assertThat(context.getIndexName()).isEqualTo("my-index");
	}

	@Test
	@DisplayName("should return null index name when index coordinates are null")
	void shouldReturnNullIndexNameWhenNull() {

		ElasticsearchObservationContext context = new ElasticsearchObservationContext(ElasticsearchOperationName.SEARCH,
				null);

		assertThat(context.getIndexCoordinates()).isNull();
		assertThat(context.getIndexName()).isNull();
	}

	@Test
	@DisplayName("should join multiple index names with comma")
	void shouldJoinMultipleIndexNames() {

		IndexCoordinates index = IndexCoordinates.of("index-a", "index-b", "index-c");
		ElasticsearchObservationContext context = new ElasticsearchObservationContext(ElasticsearchOperationName.SEARCH,
				index);

		assertThat(context.getIndexName()).isEqualTo("index-a,index-b,index-c");
	}

	@Test
	@DisplayName("should store and retrieve batch size")
	void shouldStoreAndRetrieveBatchSize() {

		ElasticsearchObservationContext context = new ElasticsearchObservationContext(ElasticsearchOperationName.BULK,
				IndexCoordinates.of("my-index"));

		assertThat(context.getBatchSize()).isNull();

		context.setBatchSize(42);

		assertThat(context.getBatchSize()).isEqualTo(42);
	}

	@Test
	@DisplayName("should allow null batch size")
	void shouldAllowNullBatchSize() {

		ElasticsearchObservationContext context = new ElasticsearchObservationContext(ElasticsearchOperationName.BULK,
				IndexCoordinates.of("my-index"));
		context.setBatchSize(10);
		context.setBatchSize(null);

		assertThat(context.getBatchSize()).isNull();
	}
}
