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
package org.springframework.data.elasticsearch.core;

import static org.springframework.data.elasticsearch.client.elc.QueryBuilders.*;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;

import org.junit.jupiter.api.DisplayName;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.query.BaseQueryBuilder;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.junit.jupiter.ElasticsearchTemplateConfiguration;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Farid Faoudi
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

	@Override
	public boolean usesNewElasticsearchClient() {
		return true;
	}

	@Override
	protected Query queryWithIds(String... ids) {
		return NativeQuery.builder().withIds(ids).build();
	}

	@Override
	protected Query getTermQuery(String field, String value) {
		return NativeQuery.builder().withQuery(termQueryAsQuery(field, value)).build();
	}

	@Override
	protected BaseQueryBuilder<?, ?> getBuilderWithMatchAllQuery() {
		return NativeQuery.builder().withQuery(matchAllQueryAsQuery());
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
}
