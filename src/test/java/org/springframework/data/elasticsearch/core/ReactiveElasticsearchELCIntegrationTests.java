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

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.Buckets;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch.core.search.FieldCollapse;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.Aggregation;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregation;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.Queries;
import org.springframework.data.elasticsearch.core.query.BaseQueryBuilder;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.junit.jupiter.ReactiveElasticsearchTemplateConfiguration;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.lang.Nullable;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Peter-Josef Meisch
 * @since 4.4
 */
@ContextConfiguration(classes = ReactiveElasticsearchELCIntegrationTests.Config.class)
public class ReactiveElasticsearchELCIntegrationTests extends ReactiveElasticsearchIntegrationTests {

	@Test // #2745
	@DisplayName("should use sort defined in native unbounded query")
	void shouldUseSortDefinedInNativeUnboundedQuery() {
		var entity1 = randomEntity(null);
		entity1.setRate(7);
		var entity2 = randomEntity(null);
		entity2.setRate(5);
		var entity3 = randomEntity(null);
		entity3.setRate(11);

		operations.saveAll(List.of(entity1, entity2, entity3), SampleEntity.class).blockLast();

		var query = NativeQuery.builder()
				.withQuery(qb -> qb
						.matchAll(m -> m))
				.withSort(sob -> sob
						.field(f -> f
								.field("rate")
								.order(SortOrder.Asc)))
				.withPageable(Pageable.unpaged())
				.build();

		var rates = operations.search(query, SampleEntity.class)
				.map(SearchHit::getContent)
				.map(SampleEntity::getRate)
				.collectList().block();
		assertThat(rates).containsExactly(5, 7, 11);

		query = NativeQuery.builder()
				.withQuery(qb -> qb
						.matchAll(m -> m))
				.withSort(sob -> sob
						.field(f -> f
								.field("rate")
								.order(SortOrder.Desc)))
				.withPageable(Pageable.unpaged())
				.build();

		rates = operations.search(query, SampleEntity.class)
				.map(SearchHit::getContent)
				.map(SampleEntity::getRate)
				.collectList().block();
		assertThat(rates).containsExactly(11, 7, 5);
	}

	@Configuration
	@Import({ ReactiveElasticsearchTemplateConfiguration.class })
	static class Config {
		@Bean
		IndexNameProvider indexNameProvider() {
			return new IndexNameProvider("reactive-template");
		}
	}

	@Override
	protected Query getTermsAggsQuery(String aggsName, String aggsField) {
		return Queries.getTermsAggsQuery(aggsName, aggsField);
	}

	@Override
	protected BaseQueryBuilder<?, ?> getBuilderWithMatchAllQuery() {
		return Queries.getBuilderWithMatchAllQuery();
	}

	@Override
	protected BaseQueryBuilder<?, ?> getBuilderWithTermQuery(String field, String value) {
		return Queries.getBuilderWithTermQuery(field, value);
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
	protected Query queryWithIds(String... ids) {
		return Queries.queryWithIds(ids);
	}

	@Override
	protected <A extends AggregationContainer<?>> void assertThatAggregationsAreCorrect(A aggregationContainer) {
		Aggregation aggregation = ((ElasticsearchAggregation) aggregationContainer).aggregation();
		assertThat(aggregation.getName()).isEqualTo("messages");
		Aggregate aggregate = aggregation.getAggregate();
		assertThat(aggregate.isSterms()).isTrue();
		StringTermsAggregate parsedStringTerms = (StringTermsAggregate) aggregate.sterms();
		Buckets<StringTermsBucket> buckets = parsedStringTerms.buckets();
		assertThat(buckets.isArray()).isTrue();
		List<StringTermsBucket> bucketList = buckets.array();
		assertThat(bucketList.size()).isEqualTo(3);
		AtomicInteger count = new AtomicInteger();
		bucketList.forEach(stringTermsBucket -> {
			if ("message".equals(stringTermsBucket.key().stringValue())) {
				count.getAndIncrement();
				assertThat(stringTermsBucket.docCount()).isEqualTo(3);
			}
			if ("some".equals(stringTermsBucket.key().stringValue())) {
				count.getAndIncrement();
				assertThat(stringTermsBucket.docCount()).isEqualTo(2);
			}
			if ("other".equals(stringTermsBucket.key().stringValue())) {
				count.getAndIncrement();
				assertThat(stringTermsBucket.docCount()).isEqualTo(1);
			}
		});
		assertThat(count.get()).isEqualTo(3);
	}
}
