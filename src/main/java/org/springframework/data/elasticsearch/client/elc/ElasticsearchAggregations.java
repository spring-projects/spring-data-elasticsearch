/*
 * Copyright 2021-2022 the original author or authors.
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

import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;

import java.util.Map;

import org.springframework.data.elasticsearch.core.AggregationsContainer;

/**
 * AggregationsContainer implementation for the Elasticsearch aggregations.
 *
 * @author Peter-Josef Meisch
 * @since 4.4
 */
public class ElasticsearchAggregations implements AggregationsContainer<Map<String, Aggregate>> {

	private final Map<String, Aggregate> aggregations;

	public ElasticsearchAggregations(Map<String, Aggregate> aggregations) {
		this.aggregations = aggregations;
	}

	@Override
	public Map<String, Aggregate> aggregations() {
		return aggregations;
	}
}
