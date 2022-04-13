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

import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.elasticsearch.client.elc.QueryBuilders.*;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.Buckets;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch.core.search.FieldCollapse;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.ELCQueries;
import org.springframework.data.elasticsearch.client.elc.Aggregation;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregation;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
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
		return ELCQueries.getTermsAggsQuery(aggsName, aggsField);
	}

	@Override
	protected BaseQueryBuilder<?, ?> getBuilderWithMatchAllQuery() {
		return ELCQueries.getBuilderWithMatchAllQuery();
	}

	@Override
	protected BaseQueryBuilder<?, ?> getBuilderWithTermQuery(String field, String value) {
		return ELCQueries.getBuilderWithTermQuery(field, value);
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
		return ELCQueries.queryWithIds(ids);
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
			if ("message".equals(stringTermsBucket.key())) {
				count.getAndIncrement();
				assertThat(stringTermsBucket.docCount()).isEqualTo(3);
			}
			if ("some".equals(stringTermsBucket.key())) {
				count.getAndIncrement();
				assertThat(stringTermsBucket.docCount()).isEqualTo(2);
			}
			if ("other".equals(stringTermsBucket.key())) {
				count.getAndIncrement();
				assertThat(stringTermsBucket.docCount()).isEqualTo(1);
			}
		});
		assertThat(count.get()).isEqualTo(3);
	}
}
