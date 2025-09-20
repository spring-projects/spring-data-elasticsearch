/*
 * Copyright 2019-2025 the original author or authors.
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

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @author Peter-Josef Meisch
 */
class CriteriaTest {

	@Test // #3159
	@DisplayName("should not slow down on calculating hashcode for long criteria chains")
	void shouldNotSlowDownOnCalculatingHashcodeForLongCriteriaChains() {
		assertTimeoutPreemptively(Duration.of(1, ChronoUnit.SECONDS), () -> {
			var criteria = new Criteria();
			var size = 5000;
			for (int i = 1; i <= size; i++) {
				criteria = criteria.or("field-" + i).contains("value-" + i);
			}
			final var criteriaChain = criteria.getCriteriaChain();
			assertEquals(size, criteriaChain.size());
			final var hashCode = Integer.valueOf(criteria.hashCode());
		});
	}
}
