/*
 * Copyright 2022-2024 the original author or authors.
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

/**
 * Class to combine an Elasticsearch {@link co.elastic.clients.elasticsearch._types.aggregations.Aggregate} with its
 * name. Necessary as the Elasticsearch Aggregate does not know its name.
 *
 * @author Peter-Josef Meisch
 * @since 4.4
 */
public class Aggregation {

	private final String name;
	private final Aggregate aggregate;

	public Aggregation(String name, Aggregate aggregate) {
		this.name = name;
		this.aggregate = aggregate;
	}

	public String getName() {
		return name;
	}

	public Aggregate getAggregate() {
		return aggregate;
	}
}
