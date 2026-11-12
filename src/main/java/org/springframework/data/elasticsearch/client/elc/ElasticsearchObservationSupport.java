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

import io.micrometer.observation.Observation;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;

/**
 * Observation lifecycle shared by ELC templates and their child templates.
 *
 * @author Ralph Ursprung
 * @author Peter-Josef Meisch
 * @since 6.2
 */
@FunctionalInterface
interface ElasticsearchObservationSupport {

	Observation createObservation(final ElasticsearchOperationName operation, @Nullable final IndexCoordinates index,
			@Nullable final Integer batchSize);

	default <T> T observe(final ElasticsearchOperationName operation, @Nullable final IndexCoordinates index,
			@Nullable final Integer batchSize, final Supplier<T> action) {
		return createObservation(operation, index, batchSize).observe(action);
	}
}
