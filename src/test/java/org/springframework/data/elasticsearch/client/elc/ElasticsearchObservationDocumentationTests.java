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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import io.micrometer.common.docs.KeyName;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistryAssert;

/**
 * Tests for {@link ElasticsearchObservation} and the end-to-end observation lifecycle.
 *
 * @author maryantocinn
 */
class ElasticsearchObservationDocumentationTests {

	@Test
	@DisplayName("observation name should be 'spring.data.elasticsearch.command'")
	void shouldHaveCorrectObservationName() {
		assertThat(ElasticsearchObservation.ELASTICSEARCH_COMMAND_OBSERVATION.getName()).isEqualTo(
				"spring.data.elasticsearch.command");
	}

	@Test
	@DisplayName("default convention should be DefaultElasticsearchObservationConvention")
	void shouldHaveCorrectDefaultConvention() {
		assertThat(ElasticsearchObservation.ELASTICSEARCH_COMMAND_OBSERVATION.getDefaultConvention()).isEqualTo(
				DefaultElasticsearchObservationConvention.class);
	}

	@Test
	@DisplayName("should declare all expected low cardinality key names")
	void shouldDeclareExpectedLowCardinalityKeyNames() {

		KeyName[] keyNames = ElasticsearchObservation.ELASTICSEARCH_COMMAND_OBSERVATION.getLowCardinalityKeyNames();
		List<String> keyStrings = Arrays.stream(keyNames).map(KeyName::asString).collect(Collectors.toList());

		assertThat(keyStrings).containsExactlyInAnyOrder("spring.data.operation", "spring.data.collection");
	}

	@Test
	@DisplayName("should declare spring.data.batch.size as high cardinality key name")
	void shouldDeclareHighCardinalityKeyNames() {

		KeyName[] keyNames = ElasticsearchObservation.ELASTICSEARCH_COMMAND_OBSERVATION.getHighCardinalityKeyNames();
		List<String> keyStrings = Arrays.stream(keyNames).map(KeyName::asString).collect(Collectors.toList());

		assertThat(keyStrings).containsExactly("spring.data.batch.size");
	}

	@Test
	@DisplayName("should record observation with correct key values using TestObservationRegistry")
	void shouldRecordObservationWithKeyValues() {

		TestObservationRegistry registry = TestObservationRegistry.create();

		ElasticsearchObservationContext context = new ElasticsearchObservationContext(ElasticsearchOperationName.SEARCH,
				IndexCoordinates.of("products"));

		Observation observation = ElasticsearchObservation.ELASTICSEARCH_COMMAND_OBSERVATION.observation(null,
				DefaultElasticsearchObservationConvention.INSTANCE, () -> context, registry);

		observation.start();
		observation.stop();

		TestObservationRegistryAssert.assertThat(registry).doesNotHaveAnyRemainingCurrentObservation()
				.hasObservationWithNameEqualTo("spring.data.elasticsearch.command").that()
				.hasLowCardinalityKeyValue("spring.data.operation", "search")
				.hasLowCardinalityKeyValue("spring.data.collection", "products").hasContextualNameEqualTo("search products");
	}

	@Test
	@DisplayName("should record observation without index when null")
	void shouldRecordObservationWithoutIndex() {

		TestObservationRegistry registry = TestObservationRegistry.create();

		ElasticsearchObservationContext context = new ElasticsearchObservationContext(ElasticsearchOperationName.DELETE,
				null);

		Observation observation = ElasticsearchObservation.ELASTICSEARCH_COMMAND_OBSERVATION.observation(null,
				DefaultElasticsearchObservationConvention.INSTANCE, () -> context, registry);

		observation.start();
		observation.stop();

		TestObservationRegistryAssert.assertThat(registry)
				.hasObservationWithNameEqualTo("spring.data.elasticsearch.command").that()
				.hasLowCardinalityKeyValue("spring.data.operation", "delete")
				.doesNotHaveLowCardinalityKeyValue("spring.data.collection", "products").hasContextualNameEqualTo("delete");
	}

	@Test
	@DisplayName("should record observation with batch size for bulk operations")
	void shouldRecordObservationWithBatchSize() {

		TestObservationRegistry registry = TestObservationRegistry.create();

		ElasticsearchObservationContext context = new ElasticsearchObservationContext(ElasticsearchOperationName.BULK,
				IndexCoordinates.of("logs"));
		context.setBatchSize(25);

		Observation observation = ElasticsearchObservation.ELASTICSEARCH_COMMAND_OBSERVATION.observation(null,
				DefaultElasticsearchObservationConvention.INSTANCE, () -> context, registry);

		observation.start();
		observation.stop();

		TestObservationRegistryAssert.assertThat(registry)
				.hasObservationWithNameEqualTo("spring.data.elasticsearch.command").that()
				.hasLowCardinalityKeyValue("spring.data.operation", "bulk")
				.hasHighCardinalityKeyValue("spring.data.batch.size", "25").hasContextualNameEqualTo("bulk logs");
	}

	@Test
	@DisplayName("should record error on observation")
	void shouldRecordErrorOnObservation() {

		TestObservationRegistry registry = TestObservationRegistry.create();

		ElasticsearchObservationContext context = new ElasticsearchObservationContext(ElasticsearchOperationName.SEARCH,
				IndexCoordinates.of("products"));

		Observation observation = ElasticsearchObservation.ELASTICSEARCH_COMMAND_OBSERVATION.observation(null,
				DefaultElasticsearchObservationConvention.INSTANCE, () -> context, registry);

		RuntimeException error = new RuntimeException("connection refused");
		observation.start();
		observation.error(error);
		observation.stop();

		TestObservationRegistryAssert.assertThat(registry)
				.hasObservationWithNameEqualTo("spring.data.elasticsearch.command").that()
				.hasLowCardinalityKeyValue("spring.data.operation", "search").hasBeenStopped();
	}

	@Test
	@DisplayName("NOOP registry should not record any observations")
	void noopRegistryShouldNotRecord() {

		ElasticsearchObservationContext context = new ElasticsearchObservationContext(ElasticsearchOperationName.SEARCH,
				IndexCoordinates.of("products"));

		Observation observation = ElasticsearchObservation.ELASTICSEARCH_COMMAND_OBSERVATION.observation(null,
				DefaultElasticsearchObservationConvention.INSTANCE, () -> context, ObservationRegistry.NOOP);

		observation.start();
		observation.stop();

		assertThat(observation.isNoop()).isTrue();
	}

	@Test
	@DisplayName("custom convention should override default key values")
	void customConventionShouldOverride() {

		TestObservationRegistry registry = TestObservationRegistry.create();

		ElasticsearchObservationConvention customConvention = new ElasticsearchObservationConvention() {
			@Override
			public String getName() {
				return "custom.elasticsearch.command";
			}

			@Override
			public String getContextualName(ElasticsearchObservationContext context) {
				return "custom " + context.getOperationName().getValue();
			}
		};

		ElasticsearchObservationContext context = new ElasticsearchObservationContext(ElasticsearchOperationName.SEARCH,
				IndexCoordinates.of("products"));

		Observation observation = ElasticsearchObservation.ELASTICSEARCH_COMMAND_OBSERVATION.observation(customConvention,
				DefaultElasticsearchObservationConvention.INSTANCE, () -> context, registry);

		observation.start();
		observation.stop();

		TestObservationRegistryAssert.assertThat(registry).hasObservationWithNameEqualTo("custom.elasticsearch.command")
				.that().hasContextualNameEqualTo("custom search");
	}
}
