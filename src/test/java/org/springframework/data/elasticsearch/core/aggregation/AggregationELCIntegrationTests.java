/*
 * Copyright 2019-2024 the original author or authors.
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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregation;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.Queries;
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
		return Queries.getTermsAggsQuery(aggsName, aggsField);
	}

	@Override
	protected void assertThatAggsHasResult(AggregationsContainer<?> aggregationsContainer, String aggsName) {
		List<ElasticsearchAggregation> aggregations = ((ElasticsearchAggregations) aggregationsContainer).aggregations();
		List<String> aggNames = aggregations.stream() //
				.map(ElasticsearchAggregation::aggregation) //
				.map(org.springframework.data.elasticsearch.client.elc.Aggregation::getName) //
				.collect(Collectors.toList());
		assertThat(aggNames).contains(aggsName);
	}

	@Override
	protected Query getPipelineAggsQuery(String aggsName, String aggsField, String aggsNamePipeline, String bucketsPath) {
		return NativeQuery.builder() //
				.withQuery(Queries.matchAllQueryAsQuery()) //
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
		Map<String, Aggregate> aggregates = ((ElasticsearchAggregations) aggregationsContainer).aggregations().stream() //
				.map(ElasticsearchAggregation::aggregation) //
				.collect(Collectors.toMap(org.springframework.data.elasticsearch.client.elc.Aggregation::getName,
						org.springframework.data.elasticsearch.client.elc.Aggregation::getAggregate));

		assertThat(aggregates).containsKey(aggsName);
		Aggregate aggregate = aggregates.get(pipelineAggsName);
		assertThat(aggregate.isStatsBucket()).isTrue();
		StatsBucketAggregate statsBucketAggregate = aggregate.statsBucket();
		assertThat(statsBucketAggregate.min()).isEqualTo(1.0);
		assertThat(statsBucketAggregate.max()).isEqualTo(3.0);
		assertThat(statsBucketAggregate.avg()).isEqualTo(2.0);
		assertThat(statsBucketAggregate.sum()).isEqualTo(6.0);
		assertThat(statsBucketAggregate.count()).isEqualTo(3L);
	}
}
