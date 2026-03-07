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

import org.jspecify.annotations.Nullable;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;

/**
 * {@link Observation.Context} for Spring Data Elasticsearch operations. One instance is created per observed operation.
 * It carries contextual data that conventions use to produce observation names and key-values.
 *
 * @author maryantocinn
 * @since 6.1
 */
public class ElasticsearchObservationContext extends Observation.Context {

	private final ElasticsearchOperationName operationName;
	@Nullable private final IndexCoordinates indexCoordinates;
	@Nullable private Integer batchSize;

	public ElasticsearchObservationContext(ElasticsearchOperationName operationName,
			@Nullable IndexCoordinates indexCoordinates) {
		this.operationName = operationName;
		this.indexCoordinates = indexCoordinates;
	}

	/**
	 * @return the Spring Data operation being performed.
	 */
	public ElasticsearchOperationName getOperationName() {
		return operationName;
	}

	/**
	 * @return the target index coordinates, or {@literal null} if the operation is not index-specific.
	 */
	@Nullable
	public IndexCoordinates getIndexCoordinates() {
		return indexCoordinates;
	}

	/**
	 * @return the comma-joined index name(s), or {@literal null} if no index coordinates are set.
	 */
	@Nullable
	public String getIndexName() {
		return indexCoordinates != null ? String.join(",", indexCoordinates.getIndexNames()) : null;
	}

	/**
	 * @return the batch size, or {@literal null} if not a batch operation.
	 */
	@Nullable
	public Integer getBatchSize() {
		return batchSize;
	}

	/**
	 * Set the number of operations included in a batch (bulk) request.
	 *
	 * @param batchSize the batch size, can be {@literal null}
	 */
	public void setBatchSize(@Nullable Integer batchSize) {
		this.batchSize = batchSize;
	}
}
