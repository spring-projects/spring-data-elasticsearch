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
package org.springframework.data.elasticsearch.core.bulk;

import static org.assertj.core.api.Assertions.*;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.elasticsearch.core.query.BulkOptions;

/**
 * @author Ralph Ursprung
 */
class BulkIngesterOptionsUnitTests {

	@Test // #2071
	@DisplayName("should have the defaults of the Elasticsearch bulk ingester")
	void shouldHaveTheDefaultsOfTheElasticsearchBulkIngester() {

		final var options = BulkIngesterOptions.defaultOptions();

		assertThat(options.getMaxOperations()).isEqualTo(1000);
		assertThat(options.getMaxSize()).isEqualTo(5L * 1024 * 1024);
		assertThat(options.getMaxConcurrentRequests()).isEqualTo(1);
		assertThat(options.getFlushInterval()).isNull();
		assertThat(options.getBulkOptions()).isSameAs(BulkOptions.defaultOptions());
	}

	@Test // #2071
	@DisplayName("should build options with the given values")
	void shouldBuildOptionsWithTheGivenValues() {

		final var bulkOptions = BulkOptions.builder().withPipeline("pipeline").build();

		final var options = BulkIngesterOptions.builder() //
				.withMaxOperations(10) //
				.withMaxSize(1024) //
				.withMaxConcurrentRequests(3) //
				.withFlushInterval(Duration.ofSeconds(2)) //
				.withBulkOptions(bulkOptions) //
				.build();

		assertThat(options.getMaxOperations()).isEqualTo(10);
		assertThat(options.getMaxSize()).isEqualTo(1024);
		assertThat(options.getMaxConcurrentRequests()).isEqualTo(3);
		assertThat(options.getFlushInterval()).isEqualTo(Duration.ofSeconds(2));
		assertThat(options.getBulkOptions()).isSameAs(bulkOptions);
	}

	@Test // #2071
	@DisplayName("should allow to disable the flush interval")
	void shouldAllowToDisableTheFlushInterval() {

		final var options = BulkIngesterOptions.builder() //
				.withFlushInterval(Duration.ofSeconds(2)) //
				.withFlushInterval(null) //
				.build();

		assertThat(options.getFlushInterval()).isNull();
	}

	@Test // #2071
	@DisplayName("should reject invalid values")
	void shouldRejectInvalidValues() {

		final var builder = BulkIngesterOptions.builder();

		assertThatIllegalArgumentException().isThrownBy(() -> builder.withMaxOperations(0));
		assertThatIllegalArgumentException().isThrownBy(() -> builder.withMaxSize(0));
		assertThatIllegalArgumentException().isThrownBy(() -> builder.withMaxConcurrentRequests(0));
		assertThatIllegalArgumentException().isThrownBy(() -> builder.withFlushInterval(Duration.ofNanos(1)));
		assertThatIllegalArgumentException().isThrownBy(() -> builder.withFlushInterval(Duration.ofSeconds(-1)));
	}
}
