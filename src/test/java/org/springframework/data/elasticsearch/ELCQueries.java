/*
// * Copyright 2022-2023 the original author or authors.
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
package org.springframework.data.elasticsearch;

import static org.springframework.data.elasticsearch.client.elc.Queries.*;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;

import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.Queries;
import org.springframework.data.elasticsearch.core.query.BaseQueryBuilder;
import org.springframework.data.elasticsearch.core.query.Query;

/**
 * Class providing some queries for the new Elasticsearch client needed in different tests.
 *
 * @author Peter-Josef Meisch
 * @since 4.4
 * @deprecated since 5.1, use the corresponding methods from {@link Queries}.
 */
@Deprecated(forRemoval = true)
public final class ELCQueries {

	private ELCQueries() {}

	public static Query getTermsAggsQuery(String aggsName, String aggsField) {
		return NativeQuery.builder() //
				.withQuery(Queries.matchAllQueryAsQuery()) //
				.withAggregation(aggsName, Aggregation.of(a -> a //
						.terms(ta -> ta.field(aggsField)))) //
				.withMaxResults(0) //
				.build();
	}

	public static Query queryWithIds(String... ids) {
		return NativeQuery.builder().withIds(ids).build();
	}

	public static BaseQueryBuilder<?, ?> getBuilderWithMatchAllQuery() {
		return NativeQuery.builder().withQuery(matchAllQueryAsQuery());
	}

	public static BaseQueryBuilder<?, ?> getBuilderWithTermQuery(String field, String value) {
		return NativeQuery.builder().withQuery(termQueryAsQuery(field, value));
	}
}
