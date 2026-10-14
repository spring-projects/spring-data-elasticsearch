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
package org.springframework.data.elasticsearch.core.query;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * @author Ralph Ursprung
 */
class BulkOptionsUnitTests {

	@Test // #3345
	void shouldHaveNoLimitsByDefault() {

		final var bulkOptions = BulkOptions.defaultOptions();

		assertThat(bulkOptions.getMaxOperations()).isNull();
		assertThat(bulkOptions.getMaxSize()).isNull();
	}

	@Test // #3345
	void shouldSetLimits() {

		final var bulkOptions = BulkOptions.builder() //
				.withMaxOperations(42) //
				.withMaxSize(1024L) //
				.build();

		assertThat(bulkOptions.getMaxOperations()).isEqualTo(42);
		assertThat(bulkOptions.getMaxSize()).isEqualTo(1024L);
	}

	@Test // #3345
	void shouldRejectNonPositiveMaxOperations() {

		final var builder = BulkOptions.builder();

		assertThatIllegalArgumentException().isThrownBy(() -> builder.withMaxOperations(0));
		assertThatIllegalArgumentException().isThrownBy(() -> builder.withMaxOperations(-1));
	}

	@Test // #3345
	void shouldRejectNonPositiveMaxSize() {

		final var builder = BulkOptions.builder();

		assertThatIllegalArgumentException().isThrownBy(() -> builder.withMaxSize(0L));
		assertThatIllegalArgumentException().isThrownBy(() -> builder.withMaxSize(-1L));
	}
}
