/*
 * Copyright 2022-2023 the original author or authors.
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
import static org.elasticsearch.index.query.QueryBuilders.*;

import org.elasticsearch.index.query.InnerHitBuilder;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.collapse.CollapseBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.client.erhlc.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.BaseQueryBuilder;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.junit.jupiter.ReactiveElasticsearchRestTemplateConfiguration;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.lang.Nullable;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Peter-Josef Meisch
 * @since 4.4
 */
@ContextConfiguration(classes = ReactiveElasticsearchERHLCIntegrationTests.Config.class)
public class ReactiveElasticsearchERHLCIntegrationTests extends ReactiveElasticsearchIntegrationTests {

	@Configuration
	@Import({ ReactiveElasticsearchRestTemplateConfiguration.class })
	static class Config {
		@Bean
		IndexNameProvider indexNameProvider() {
			return new IndexNameProvider("reactive-template-es7");
		}
	}

	@Override
	protected Query getTermsAggsQuery(String aggsName, String aggsField) {
		return new NativeSearchQueryBuilder().withQuery(matchAllQuery())
				.addAggregation(AggregationBuilders.terms("messages").field("message")).build();
	}

	@Override
	protected BaseQueryBuilder<?, ?> getBuilderWithMatchAllQuery() {
		return new NativeSearchQueryBuilder().withQuery(matchAllQuery());

	}

	@Override
	protected BaseQueryBuilder<?, ?> getBuilderWithTermQuery(String field, String value) {
		return new NativeSearchQueryBuilder().withQuery(termQuery(field, value));
	}

	@Override
	protected Query getQueryWithCollapse(String collapseField, @Nullable String innerHits, @Nullable Integer size) {
		CollapseBuilder collapseBuilder = new CollapseBuilder(collapseField);

		if (innerHits != null) {
			InnerHitBuilder innerHitBuilder = new InnerHitBuilder(innerHits);

			if (size != null) {
				innerHitBuilder.setSize(size);
			}

			collapseBuilder.setInnerHits(innerHitBuilder);
		}

		return new NativeSearchQueryBuilder().withQuery(matchAllQuery()).withCollapseBuilder(collapseBuilder).build();
	}

	@Override
	protected Query queryWithIds(String... ids) {
		return new NativeSearchQueryBuilder().withIds(ids).build();
	}

	@Override
	protected <A extends AggregationContainer<?>> void assertThatAggregationsAreCorrect(A aggregationContainer) {
		Aggregation aggregation = (Aggregation) aggregationContainer.aggregation();
		assertThat(aggregation.getName()).isEqualTo("messages");
		assertThat(aggregation instanceof ParsedStringTerms);
		ParsedStringTerms parsedStringTerms = (ParsedStringTerms) aggregation;
		assertThat(parsedStringTerms.getBuckets().size()).isEqualTo(3);
		assertThat(parsedStringTerms.getBucketByKey("message").getDocCount()).isEqualTo(3);
		assertThat(parsedStringTerms.getBucketByKey("some").getDocCount()).isEqualTo(2);
		assertThat(parsedStringTerms.getBucketByKey("other").getDocCount()).isEqualTo(1);
	}

}
