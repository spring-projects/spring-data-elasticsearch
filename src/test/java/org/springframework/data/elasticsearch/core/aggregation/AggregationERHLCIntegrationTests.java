/*
 * Copyright 2019-2022 the original author or authors.
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
package org.springframework.data.elasticsearch.core.aggregation;

import static org.assertj.core.api.Assertions.*;
import static org.elasticsearch.index.query.QueryBuilders.*;
import static org.elasticsearch.search.aggregations.AggregationBuilders.*;
import static org.elasticsearch.search.aggregations.PipelineAggregatorBuilders.*;

import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.pipeline.ParsedStatsBucket;
import org.elasticsearch.search.aggregations.pipeline.StatsBucket;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.client.erhlc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.erhlc.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.AggregationsContainer;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.junit.jupiter.ElasticsearchRestTemplateConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Peter-Josef Meisch
 */
@ContextConfiguration(classes = { AggregationERHLCIntegrationTests.Config.class })
public class AggregationERHLCIntegrationTests extends AggregationIntegrationTests {

	@Configuration
	@Import({ ElasticsearchRestTemplateConfiguration.class })
	@EnableElasticsearchRepositories(considerNestedRepositories = true)
	static class Config {
		@Bean
		IndexNameProvider indexNameProvider() {
			return new IndexNameProvider("aggs-es7");
		}
	}

	protected Query getTermsAggsQuery(String aggsName, String aggsField) {
		return new NativeSearchQueryBuilder() //
				.withQuery(matchAllQuery()) //
				.withSearchType(SearchType.DEFAULT) //
				.withAggregations(terms(aggsName).field(aggsField)) //
				.withMaxResults(0) //
				.build();
	}

	protected void assertThatAggsHasResult(AggregationsContainer<?> aggregationsContainer, String aggsName) {
		Aggregations aggregations = ((ElasticsearchAggregations) aggregationsContainer).aggregations();
		assertThat(aggregations.asMap().get(aggsName)).isNotNull();
	}

	protected Query getPipelineAggsQuery(String aggsName, String aggsField, String aggsNamePipeline, String bucketsPath) {
		return new NativeSearchQueryBuilder() //
				.withQuery(matchAllQuery()) //
				.withSearchType(SearchType.DEFAULT) //
				.withAggregations(terms(aggsName).field(aggsField)) //
				.withPipelineAggregations(statsBucket(aggsNamePipeline, bucketsPath)) //
				.withMaxResults(0) //
				.build();
	}

	protected void assertThatPipelineAggsAreCorrect(AggregationsContainer<?> aggregationsContainer, String aggsName,
			String pipelineAggsName) {
		Aggregations aggregations = ((ElasticsearchAggregations) aggregationsContainer).aggregations();

		assertThat(aggregations.asMap().get(aggsName)).isNotNull();
		Aggregation keyword_bucket_stats = aggregations.asMap().get(pipelineAggsName);
		assertThat(keyword_bucket_stats).isInstanceOf(StatsBucket.class);
		if (keyword_bucket_stats instanceof ParsedStatsBucket statsBucket) {
			// Rest client
			assertThat(statsBucket.getMin()).isEqualTo(1.0);
			assertThat(statsBucket.getMax()).isEqualTo(3.0);
			assertThat(statsBucket.getAvg()).isEqualTo(2.0);
			assertThat(statsBucket.getSum()).isEqualTo(6.0);
			assertThat(statsBucket.getCount()).isEqualTo(3L);
		}
	}

}
