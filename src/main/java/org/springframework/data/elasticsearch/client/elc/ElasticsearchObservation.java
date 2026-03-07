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

import io.micrometer.common.docs.KeyName;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;
import io.micrometer.observation.docs.ObservationDocumentation;

/**
 * {@link ObservationDocumentation} for Spring Data Elasticsearch template operations.
 *
 * @author maryantocinn
 * @since 6.1
 */
public enum ElasticsearchObservation implements ObservationDocumentation {

	/**
	 * Timer created around a Spring Data Elasticsearch template operation.
	 */
	ELASTICSEARCH_COMMAND_OBSERVATION {
		@Override
		public String getName() {
			return "spring.data.elasticsearch.command";
		}

		@Override
		public Class<? extends ObservationConvention<? extends Observation.Context>> getDefaultConvention() {
			return DefaultElasticsearchObservationConvention.class;
		}

		@Override
		public KeyName[] getLowCardinalityKeyNames() {
			return LowCardinalityKeyNames.values();
		}

		@Override
		public KeyName[] getHighCardinalityKeyNames() {
			return HighCardinalityKeyNames.values();
		}
	};

	/**
	 * Low cardinality key names for Spring Data Elasticsearch observations. These become metric dimensions and MUST be
	 * present on every observation to satisfy backends like Prometheus that require consistent tag key sets.
	 */
	enum LowCardinalityKeyNames implements KeyName {

		/**
		 * The Spring Data operation being performed (e.g., save, search, delete, bulk).
		 */
		OPERATION {
			@Override
			public String asString() {
				return "spring.data.operation";
			}
		},

		/**
		 * The target collection (index) name. Only present when the operation targets a specific index.
		 */
		COLLECTION {
			@Override
			public String asString() {
				return "spring.data.collection";
			}
		}
	}

	/**
	 * High cardinality key names for Spring Data Elasticsearch observations. These appear only on traces/spans, not on
	 * metrics, because their values are unbounded or optional per operation.
	 */
	enum HighCardinalityKeyNames implements KeyName {

		/**
		 * The number of operations included in a batch (bulk) request. Only present for bulk operations.
		 */
		BATCH_SIZE {
			@Override
			public String asString() {
				return "spring.data.batch.size";
			}
		}
	}
}
