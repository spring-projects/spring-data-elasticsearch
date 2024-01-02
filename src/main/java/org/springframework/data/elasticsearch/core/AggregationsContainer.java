/*
 * Copyright 2021-2024 the original author or authors.
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
package org.springframework.data.elasticsearch.core;

/**
 * Aggregations container used in the Spring Data Elasticsearch API. The concrete implementations must be provided by
 * the code handling the direct communication with Elasticsearch.
 *
 * @author Peter-Josef Meisch
 * @param <T> the aggregations class from the used client implementation.
 * @since 4.3
 */
public interface AggregationsContainer<T> {
	/**
	 * @return the concrete aggregations implementation
	 */
	T aggregations();
}
