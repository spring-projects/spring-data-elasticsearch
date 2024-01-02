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
package org.springframework.data.elasticsearch.core;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.elasticsearch.client.elc.Queries.*;
import static org.springframework.data.elasticsearch.utils.IndexBuilder.*;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionBoostMode;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionScoreMode;
import co.elastic.clients.elasticsearch.core.search.FieldCollapse;
import co.elastic.clients.json.JsonData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.client.elc.Queries;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.BaseQueryBuilder;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilterBuilder;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.RescorerQuery;
import org.springframework.data.elasticsearch.core.query.ScriptData;
import org.springframework.data.elasticsearch.core.query.ScriptType;
import org.springframework.data.elasticsearch.core.query.ScriptedField;
import org.springframework.data.elasticsearch.junit.jupiter.ElasticsearchTemplateConfiguration;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.lang.Nullable;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Farid Faoudi
 * @author Sascha Woo
 * @since 4.4
 */
@ContextConfiguration(classes = { ElasticsearchELCIntegrationTests.Config.class })
@DisplayName("Using Elasticsearch Client")
public class ElasticsearchELCIntegrationTests extends ElasticsearchIntegrationTests {

	@Configuration
	@Import({ ElasticsearchTemplateConfiguration.class })
	static class Config {
		@Bean
		IndexNameProvider indexNameProvider() {
			return new IndexNameProvider("integration");
		}
	}

	@Test // #2263
	public void shouldSortResultsBySortOptions() {

		List<IndexQuery> indexQueries = new ArrayList<>();

		indexQueries.add(buildIndex(SampleEntity.builder().id("1").message("ab xz").build()));
		indexQueries.add(buildIndex(SampleEntity.builder().id("2").message("bc").build()));
		indexQueries.add(buildIndex(SampleEntity.builder().id("3").message("ac xz hi").build()));

		operations.bulkIndex(indexQueries, IndexCoordinates.of(indexNameProvider.indexName()));

		NativeQuery query = NativeQuery.builder().withSort(b -> b.field(fb -> fb.field("message").order(SortOrder.Asc)))
				.build();

		SearchHits<SampleEntity> searchHits = operations.search(query, SampleEntity.class,
				IndexCoordinates.of(indexNameProvider.indexName()));

		assertThat(searchHits.getSearchHits()) //
				.satisfiesExactly(e -> assertThat(e.getId()).isEqualTo("1"), e -> assertThat(e.getId()).isEqualTo("3"),
						e -> assertThat(e.getId()).isEqualTo("2"));
	}

	@Override
	protected Query queryWithIds(String... ids) {
		return Queries.queryWithIds(ids);
	}

	@Override
	protected Query getTermQuery(String field, String value) {
		return NativeQuery.builder().withQuery(termQueryAsQuery(field, value)).build();
	}

	@Override
	protected BaseQueryBuilder<?, ?> getBuilderWithMatchAllQuery() {
		return Queries.getBuilderWithMatchAllQuery();
	}

	@Override
	protected BaseQueryBuilder<?, ?> getBuilderWithMatchQuery(String field, String value) {
		return NativeQuery.builder().withQuery(matchQueryAsQuery(field, value, null, null));
	}

	@Override
	protected BaseQueryBuilder<?, ?> getBuilderWithTermQuery(String field, String value) {
		return NativeQuery.builder().withQuery(termQueryAsQuery(field, value));
	}

	@Override
	protected BaseQueryBuilder<?, ?> getBuilderWithWildcardQuery(String field, String value) {
		return NativeQuery.builder().withQuery(wildcardQueryAsQuery(field, value));
	}

	@Override
	protected Query getBoolQueryWithWildcardsFirstMustSecondShouldAndMinScore(String firstField, String firstValue,
			String secondField, String secondValue, float minScore) {

		return NativeQuery.builder().withQuery(q -> q //
				.bool(BoolQuery.of(b -> b //
						.must(m -> m.wildcard(w1 -> w1.field(firstField).wildcard(firstValue))) //
						.should(s -> s.wildcard(w2 -> w2.field(secondField).wildcard(secondValue)))))) //
				.withMinScore(minScore) //
				.build();
	}

	@Override
	protected Query getQueryWithCollapse(String collapseField, @Nullable String innerHits, @Nullable Integer size) {
		return NativeQuery.builder() //
				.withQuery(matchAllQueryAsQuery()) //
				.withFieldCollapse(FieldCollapse.of(fc -> {
					fc.field(collapseField);

					if (innerHits != null) {
						fc.innerHits(ih -> ih.name(innerHits).size(size));
					}
					return fc;
				})).build();
	}

	@Override
	protected Query getMatchAllQueryWithFilterForId(String id) {
		return NativeQuery.builder() //
				.withQuery(matchAllQueryAsQuery()) //
				.withFilter(termQueryAsQuery("id", id)) //
				.build();
	}

	@Override
	protected Query getQueryForParentId(String type, String id, @Nullable String route) {

		NativeQueryBuilder queryBuilder = NativeQuery.builder() //
				.withQuery(qb -> qb //
						.parentId(p -> p.type(type).id(id)) //
				);

		if (route != null) {
			queryBuilder.withRoute(route);
		}

		return queryBuilder.build();
	}

	@Override
	protected Query getMatchAllQueryWithIncludesAndInlineExpressionScript(@Nullable String includes, String fieldName,
			String script, Map<String, Object> params) {

		NativeQueryBuilder nativeQueryBuilder = NativeQuery.builder().withQuery(matchAllQueryAsQuery());

		if (includes != null) {
			nativeQueryBuilder.withSourceFilter(new FetchSourceFilterBuilder().withIncludes(includes).build());
		}

		return nativeQueryBuilder.withScriptedField(new ScriptedField( //
				fieldName, //
				new ScriptData(ScriptType.INLINE, "expression", script, null, params))) //
				.build();
	}

	@Override
	protected Query getQueryWithRescorer() {

		return NativeQuery.builder() //
				.withQuery(q -> q //
						.bool(b -> b //
								.filter(f -> f.exists(e -> e.field("rate"))) //
								.should(s -> s.term(t -> t.field("message").value("message"))) //
						)) //
				.withRescorerQuery( //
						new RescorerQuery(NativeQuery.builder() //
								.withQuery(q -> q //
										.functionScore(fs -> fs //
												.functions(f1 -> f1 //
														.filter(matchAllQueryAsQuery()) //
														.weight(1.0) //
														.gauss(d -> d //
																.field("rate") //
																.placement(dp -> dp //
																		.origin(JsonData.of(0)) //
																		.scale(JsonData.of(10)) //
																		.decay(0.5)) //
														)) //
												.functions(f2 -> f2 //
														.filter(matchAllQueryAsQuery()).weight(100.0) //
														.gauss(d -> d //
																.field("rate") //
																.placement(dp -> dp //
																		.origin(JsonData.of(0)) //
																		.scale(JsonData.of(10)) //
																		.decay(0.5)) //

														)) //
												.scoreMode(FunctionScoreMode.Sum) //
												.maxBoost(80.0) //
												.boostMode(FunctionBoostMode.Replace)) //
								) //
								.build() //
						) //
								.withScoreMode(RescorerQuery.ScoreMode.Max) //
								.withWindowSize(100)) //
				.build();
	}
}
