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
package org.springframework.data.elasticsearch.client.elc;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.elasticsearch.core.AggregationsContainer;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * AggregationsContainer implementation for the Elasticsearch aggregations.
 *
 * @author Peter-Josef Meisch
 * @author Sascha Woo
 * @since 4.4
 */
public class ElasticsearchAggregations implements AggregationsContainer<List<ElasticsearchAggregation>> {

	private final List<ElasticsearchAggregation> aggregations;
	private final Map<String, ElasticsearchAggregation> aggregationsAsMap;

	public ElasticsearchAggregations(Map<String, Aggregate> aggregations) {

		Assert.notNull(aggregations, "aggregations must not be null");

		aggregationsAsMap = new HashMap<>();
		aggregations.forEach((name, aggregate) -> aggregationsAsMap //
				.put(name, new ElasticsearchAggregation(new Aggregation(name, aggregate))));

		this.aggregations = new ArrayList<>(aggregationsAsMap.values());
	}

	@Override
	public List<ElasticsearchAggregation> aggregations() {
		return aggregations;
	}

	/**
	 * @return the {@link ElasticsearchAggregation}s keyed by aggregation name.
	 */
	public Map<String, ElasticsearchAggregation> aggregationsAsMap() {
		return aggregationsAsMap;
	}

	/**
	 * Returns the aggregation that is associated with the specified name.
	 *
	 * @param name the name of the aggregation
	 * @return the aggregation or {@literal null} if not found
	 */
	@Nullable
	public ElasticsearchAggregation get(String name) {

		Assert.notNull(name, "name must not be null");

		return aggregationsAsMap.get(name);
	}

}
