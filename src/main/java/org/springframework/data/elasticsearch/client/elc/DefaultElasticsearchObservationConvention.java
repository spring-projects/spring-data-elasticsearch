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

import io.micrometer.common.KeyValues;

/**
 * Default {@link ElasticsearchObservationConvention} implementation.
 *
 * @author maryantocinn
 * @since 6.1
 */
public class DefaultElasticsearchObservationConvention implements ElasticsearchObservationConvention {

	public static final DefaultElasticsearchObservationConvention INSTANCE = new DefaultElasticsearchObservationConvention();

	@Override
	public String getName() {
		return ElasticsearchObservation.ELASTICSEARCH_COMMAND_OBSERVATION.getName();
	}

	@Override
	public String getContextualName(ElasticsearchObservationContext context) {

		String indexName = context.getIndexName();
		if (indexName != null) {
			return context.getOperationName().getValue() + " " + indexName;
		}
		return context.getOperationName().getValue();
	}

	@Override
	public KeyValues getLowCardinalityKeyValues(final ElasticsearchObservationContext context) {

		return KeyValues.of(
				ElasticsearchObservation.LowCardinalityKeyNames.OPERATION.withValue(context.getOperationName().getValue()));
	}

	@Override
	public KeyValues getHighCardinalityKeyValues(final ElasticsearchObservationContext context) {

		var keyValues = KeyValues.empty();

		final var indexName = context.getIndexName();
		if (indexName != null) {
			keyValues = keyValues.and(ElasticsearchObservation.HighCardinalityKeyNames.COLLECTION.withValue(indexName));
		}

		final var batchSize = context.getBatchSize();
		if (batchSize != null) {
			keyValues = keyValues.and(
					ElasticsearchObservation.HighCardinalityKeyNames.BATCH_SIZE.withValue(String.valueOf(batchSize)));
		}

		return keyValues;
	}
}
