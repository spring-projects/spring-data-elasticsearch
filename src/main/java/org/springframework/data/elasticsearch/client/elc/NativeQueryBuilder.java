/*
 * Copyright 2022 the original author or authors.
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

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.search.Suggester;
import co.elastic.clients.util.ObjectBuilder;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import org.springframework.data.elasticsearch.core.query.BaseQueryBuilder;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * @author Peter-Josef Meisch
 * @since 4.4
 */
public class NativeQueryBuilder extends BaseQueryBuilder<NativeQuery, NativeQueryBuilder> {

	@Nullable private Query query;
	private final Map<String, Aggregation> aggregations = new LinkedHashMap<>();
	@Nullable private Suggester suggester;

	public NativeQueryBuilder() {
	}

	@Nullable
	public Query getQuery() {
		return query;
	}

	public NativeQueryBuilder withQuery(Query query) {

		Assert.notNull(query, "query must not be null");

		this.query = query;
		return this;
	}

	public NativeQueryBuilder withQuery(Function<Query.Builder, ObjectBuilder<Query>> fn) {

		Assert.notNull(fn, "fn must not be null");

		return withQuery(fn.apply(new Query.Builder()).build());
	}

	public NativeQueryBuilder withAggregation(String name, Aggregation aggregation) {

		Assert.notNull(name, "name must not be null");
		Assert.notNull(aggregation, "aggregation must not be null");

		this.aggregations.put(name, aggregation);
		return this;
	}

	public NativeQueryBuilder withSuggester(@Nullable Suggester suggester) {
		this.suggester = suggester;
		return this;
	}

	public NativeQuery build() {
		NativeQuery nativeQuery = new NativeQuery(this);
		nativeQuery.setAggregations(aggregations);
		nativeQuery.setSuggester(suggester);

		return nativeQuery;
	}
}
