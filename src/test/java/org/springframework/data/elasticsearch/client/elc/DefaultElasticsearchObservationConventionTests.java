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

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.micrometer.observation.Observation;

/**
 * Unit tests for {@link DefaultElasticsearchObservationConvention}.
 *
 * @author maryantocinn
 */
class DefaultElasticsearchObservationConventionTests {

	private final DefaultElasticsearchObservationConvention convention = DefaultElasticsearchObservationConvention.INSTANCE;

	@Test
	@DisplayName("should return observation name matching ObservationDocumentation")
	void shouldReturnCorrectName() {
		assertThat(convention.getName()).isEqualTo("spring.data.elasticsearch.command");
	}

	@Test
	@DisplayName("should support ElasticsearchObservationContext")
	void shouldSupportElasticsearchContext() {

		ElasticsearchObservationContext context = new ElasticsearchObservationContext(ElasticsearchOperationName.SEARCH,
				IndexCoordinates.of("my-index"));

		assertThat(convention.supportsContext(context)).isTrue();
	}

	@Test
	@DisplayName("should not support unrelated context")
	void shouldNotSupportUnrelatedContext() {

		Observation.Context otherContext = new Observation.Context();

		assertThat(convention.supportsContext(otherContext)).isFalse();
	}

	// region contextual name

	@Test
	@DisplayName("contextual name should be 'operation index' when index is present")
	void contextualNameWithIndex() {

		ElasticsearchObservationContext context = new ElasticsearchObservationContext(ElasticsearchOperationName.SEARCH,
				IndexCoordinates.of("products"));

		assertThat(convention.getContextualName(context)).isEqualTo("search products");
	}

	@Test
	@DisplayName("contextual name should be just the operation when index is null")
	void contextualNameWithoutIndex() {

		ElasticsearchObservationContext context = new ElasticsearchObservationContext(ElasticsearchOperationName.SEARCH,
				null);

		assertThat(convention.getContextualName(context)).isEqualTo("search");
	}

	@Test
	@DisplayName("contextual name should include comma-joined indices for multi-index operations")
	void contextualNameWithMultipleIndices() {

		ElasticsearchObservationContext context = new ElasticsearchObservationContext(ElasticsearchOperationName.SEARCH,
				IndexCoordinates.of("index-a", "index-b"));

		assertThat(convention.getContextualName(context)).isEqualTo("search index-a,index-b");
	}

	// endregion

	// region low cardinality key values

	@Test
	@DisplayName("should always include spring.data.operation")
	void shouldIncludeRequiredKeyValues() {

		ElasticsearchObservationContext context = new ElasticsearchObservationContext(ElasticsearchOperationName.GET, null);

		KeyValues keyValues = convention.getLowCardinalityKeyValues(context);

		assertThat(keyValues).contains(KeyValue.of("spring.data.operation", "get"));
	}

	@Test
	@DisplayName("should include spring.data.collection when index is present")
	void shouldIncludeCollectionWhenIndexPresent() {

		ElasticsearchObservationContext context = new ElasticsearchObservationContext(ElasticsearchOperationName.SEARCH,
				IndexCoordinates.of("products"));

		KeyValues keyValues = convention.getLowCardinalityKeyValues(context);

		assertThat(keyValues).contains(KeyValue.of("spring.data.collection", "products"));
	}

	@Test
	@DisplayName("should not include spring.data.collection when index is null")
	void shouldNotIncludeCollectionWhenNull() {

		ElasticsearchObservationContext context = new ElasticsearchObservationContext(ElasticsearchOperationName.SEARCH,
				null);

		KeyValues keyValues = convention.getLowCardinalityKeyValues(context);

		assertThat(keyValues.stream().map(KeyValue::getKey)).doesNotContain("spring.data.collection");
	}

	@Test
	@DisplayName("should include spring.data.batch.size as high cardinality when batch size is set")
	void shouldIncludeBatchSizeAsHighCardinality() {

		ElasticsearchObservationContext context = new ElasticsearchObservationContext(ElasticsearchOperationName.BULK,
				IndexCoordinates.of("products"));
		context.setBatchSize(5);

		KeyValues highCardValues = convention.getHighCardinalityKeyValues(context);

		assertThat(highCardValues).contains(KeyValue.of("spring.data.batch.size", "5"));
	}

	@Test
	@DisplayName("should not include spring.data.batch.size in low cardinality key values")
	void shouldNotIncludeBatchSizeInLowCardinality() {

		ElasticsearchObservationContext context = new ElasticsearchObservationContext(ElasticsearchOperationName.BULK,
				IndexCoordinates.of("products"));
		context.setBatchSize(5);

		KeyValues keyValues = convention.getLowCardinalityKeyValues(context);

		assertThat(keyValues.stream().map(KeyValue::getKey)).doesNotContain("spring.data.batch.size");
	}

	@Test
	@DisplayName("should return empty high cardinality key values when batch size is null")
	void shouldReturnEmptyHighCardinalityWhenNoBatchSize() {

		ElasticsearchObservationContext context = new ElasticsearchObservationContext(ElasticsearchOperationName.SEARCH,
				IndexCoordinates.of("products"));

		KeyValues highCardValues = convention.getHighCardinalityKeyValues(context);

		assertThat(highCardValues.stream().map(KeyValue::getKey)).doesNotContain("spring.data.batch.size");
	}

	@Test
	@DisplayName("should produce correct key values for a full bulk operation")
	void shouldProduceCorrectKeyValuesForBulk() {

		ElasticsearchObservationContext context = new ElasticsearchObservationContext(ElasticsearchOperationName.BULK,
				IndexCoordinates.of("orders"));
		context.setBatchSize(100);

		KeyValues lowCardValues = convention.getLowCardinalityKeyValues(context);
		KeyValues highCardValues = convention.getHighCardinalityKeyValues(context);

		assertThat(lowCardValues).contains(KeyValue.of("spring.data.operation", "bulk"),
				KeyValue.of("spring.data.collection", "orders"));
		assertThat(highCardValues).contains(KeyValue.of("spring.data.batch.size", "100"));
	}

	@Test
	@DisplayName("should produce correct key values for each operation name")
	void shouldProduceCorrectOperationNames() {

		for (ElasticsearchOperationName operationName : ElasticsearchOperationName.values()) {
			ElasticsearchObservationContext context = new ElasticsearchObservationContext(operationName,
					IndexCoordinates.of("test-index"));

			KeyValues keyValues = convention.getLowCardinalityKeyValues(context);

			assertThat(keyValues).contains(KeyValue.of("spring.data.operation", operationName.getValue()));
		}
	}

	// endregion
}
