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

import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.StatsBucketAggregate;

import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.QueryBuilders;
import org.springframework.data.elasticsearch.core.AggregationsContainer;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.junit.jupiter.ElasticsearchTemplateConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Peter-Josef Meisch
 * @since 4.4
 */
@ContextConfiguration(classes = { AggregationELCIntegrationTests.Config.class })
public class AggregationELCIntegrationTests extends AggregationIntegrationTests {

	@Configuration
	@Import({ ElasticsearchTemplateConfiguration.class })
	@EnableElasticsearchRepositories(considerNestedRepositories = true)
	static class Config {
		@Bean
		IndexNameProvider indexNameProvider() {
			return new IndexNameProvider("aggs");
		}
	}

	@Override
	protected Query getTermsAggsQuery(String aggsName, String aggsField) {
		return NativeQuery.builder() //
				.withQuery(QueryBuilders.matchAllQueryAsQuery()) //
				.withAggregation(aggsName, Aggregation.of(a -> a //
						.terms(ta -> ta.field(aggsField)))) //
				.withMaxResults(0) //
				.build();
	}

	@Override
	protected void assertThatAggsHasResult(AggregationsContainer<?> aggregationsContainer, String aggsName) {
		Map<String, Aggregate> aggregations = ((ElasticsearchAggregations) aggregationsContainer).aggregations();
		assertThat(aggregations).containsKey(aggsName);
	}

	@Override
	protected Query getPipelineAggsQuery(String aggsName, String aggsField, String aggsNamePipeline, String bucketsPath) {
		return NativeQuery.builder() //
				.withQuery(QueryBuilders.matchAllQueryAsQuery()) //
				.withAggregation(aggsName, Aggregation.of(a -> a //
						.terms(ta -> ta.field(aggsField)))) //
				.withAggregation(aggsNamePipeline, Aggregation.of(a -> a //
						.statsBucket(sb -> sb.bucketsPath(bp -> bp.single(bucketsPath))))) //
				.withMaxResults(0) //
				.build();
	}

	@Override
	protected void assertThatPipelineAggsAreCorrect(AggregationsContainer<?> aggregationsContainer, String aggsName,
			String pipelineAggsName) {
		Map<String, Aggregate> aggregations = ((ElasticsearchAggregations) aggregationsContainer).aggregations();
		assertThat(aggregations).containsKey(aggsName);
		Aggregate aggregate = aggregations.get(pipelineAggsName);
		assertThat(aggregate.isStatsBucket()).isTrue();
		StatsBucketAggregate statsBucketAggregate = aggregate.statsBucket();
		assertThat(statsBucketAggregate.min()).isEqualTo(1.0);
		assertThat(statsBucketAggregate.max()).isEqualTo(3.0);
		assertThat(statsBucketAggregate.avg()).isEqualTo(2.0);
		assertThat(statsBucketAggregate.sum()).isEqualTo(6.0);
		assertThat(statsBucketAggregate.count()).isEqualTo(3L);
	}
}
