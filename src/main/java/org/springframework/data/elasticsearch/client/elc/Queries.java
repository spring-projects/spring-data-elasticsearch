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

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.LatLonGeoLocation;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.IdsQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchAllQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryStringQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.WildcardQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.WrapperQuery;
import co.elastic.clients.util.ObjectBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.function.Function;

import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.data.elasticsearch.core.query.BaseQueryBuilder;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Utility class simplifying the creation of some more complex queries and type.
 *
 * @author Peter-Josef Meisch
 * @since 4.4
 */
public final class Queries {

	private Queries() {}

	public static IdsQuery idsQuery(List<String> ids) {

		Assert.notNull(ids, "ids must not be null");

		return IdsQuery.of(i -> i.values(ids));
	}

	public static Query idsQueryAsQuery(List<String> ids) {

		Assert.notNull(ids, "ids must not be null");

		Function<Query.Builder, ObjectBuilder<Query>> builder = b -> b.ids(idsQuery(ids));

		return builder.apply(new Query.Builder()).build();
	}

	public static MatchQuery matchQuery(String fieldName, String query, @Nullable Operator operator,
			@Nullable Float boost) {

		Assert.notNull(fieldName, "fieldName must not be null");
		Assert.notNull(query, "query must not be null");

		return MatchQuery.of(mb -> mb.field(fieldName).query(FieldValue.of(query)).operator(operator).boost(boost));
	}

	public static Query matchQueryAsQuery(String fieldName, String query, @Nullable Operator operator,
			@Nullable Float boost) {

		Function<Query.Builder, ObjectBuilder<Query>> builder = b -> b.match(matchQuery(fieldName, query, operator, boost));

		return builder.apply(new Query.Builder()).build();
	}

	public static MatchAllQuery matchAllQuery() {

		return MatchAllQuery.of(b -> b);
	}

	public static Query matchAllQueryAsQuery() {

		Function<Query.Builder, ObjectBuilder<Query>> builder = b -> b.matchAll(matchAllQuery());

		return builder.apply(new Query.Builder()).build();
	}

	public static QueryStringQuery queryStringQuery(String fieldName, String query, @Nullable Float boost) {
		return queryStringQuery(fieldName, query, null, null, boost);
	}

	public static QueryStringQuery queryStringQuery(String fieldName, String query, Operator defaultOperator,
			@Nullable Float boost) {
		return queryStringQuery(fieldName, query, null, defaultOperator, boost);
	}

	public static QueryStringQuery queryStringQuery(String fieldName, String query, @Nullable Boolean analyzeWildcard,
			@Nullable Float boost) {
		return queryStringQuery(fieldName, query, analyzeWildcard, null, boost);
	}

	public static QueryStringQuery queryStringQuery(String fieldName, String query, @Nullable Boolean analyzeWildcard,
			@Nullable Operator defaultOperator, @Nullable Float boost) {

		Assert.notNull(fieldName, "fieldName must not be null");
		Assert.notNull(query, "query must not be null");

		return QueryStringQuery.of(qs -> qs.fields(fieldName).query(query).analyzeWildcard(analyzeWildcard)
				.defaultOperator(defaultOperator).boost(boost));
	}

	public static TermQuery termQuery(String fieldName, String value) {

		Assert.notNull(fieldName, "fieldName must not be null");
		Assert.notNull(value, "value must not be null");

		return TermQuery.of(t -> t.field(fieldName).value(FieldValue.of(value)));
	}

	public static Query termQueryAsQuery(String fieldName, String value) {

		Function<Query.Builder, ObjectBuilder<Query>> builder = q -> q.term(termQuery(fieldName, value));
		return builder.apply(new Query.Builder()).build();
	}

	public static WildcardQuery wildcardQuery(String field, String value) {

		Assert.notNull(field, "field must not be null");
		Assert.notNull(value, "value must not be null");

		return WildcardQuery.of(w -> w.field(field).wildcard(value));
	}

	public static Query wildcardQueryAsQuery(String field, String value) {
		Function<Query.Builder, ObjectBuilder<Query>> builder = q -> q.wildcard(wildcardQuery(field, value));
		return builder.apply(new Query.Builder()).build();
	}

	public static Query wrapperQueryAsQuery(String query) {

		Function<Query.Builder, ObjectBuilder<Query>> builder = q -> q.wrapper(wrapperQuery(query));

		return builder.apply(new Query.Builder()).build();
	}

	public static WrapperQuery wrapperQuery(String query) {

		Assert.notNull(query, "query must not be null");

		String encodedValue = Base64.getEncoder().encodeToString(query.getBytes(StandardCharsets.UTF_8));

		return WrapperQuery.of(wq -> wq.query(encodedValue));
	}

	public static LatLonGeoLocation latLon(GeoPoint geoPoint) {

		Assert.notNull(geoPoint, "geoPoint must not be null");

		return latLon(geoPoint.getLat(), geoPoint.getLon());
	}

	public static LatLonGeoLocation latLon(double lat, double lon) {
		return LatLonGeoLocation.of(_0 -> _0.lat(lat).lon(lon));
	}

	public static org.springframework.data.elasticsearch.core.query.Query getTermsAggsQuery(String aggsName,
			String aggsField) {
		return NativeQuery.builder() //
				.withQuery(Queries.matchAllQueryAsQuery()) //
				.withAggregation(aggsName, Aggregation.of(a -> a //
						.terms(ta -> ta.field(aggsField)))) //
				.withMaxResults(0) //
				.build();
	}

	public static org.springframework.data.elasticsearch.core.query.Query queryWithIds(String... ids) {
		return NativeQuery.builder().withIds(ids).build();
	}

	public static BaseQueryBuilder<?, ?> getBuilderWithMatchAllQuery() {
		return NativeQuery.builder().withQuery(matchAllQueryAsQuery());
	}

	public static BaseQueryBuilder<?, ?> getBuilderWithTermQuery(String field, String value) {
		return NativeQuery.builder().withQuery(termQueryAsQuery(field, value));
	}
}
