/*
 * Copyright 2022-2025 the original author or authors.
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
package org.springframework.data.elasticsearch.repository.support;

import co.elastic.clients.elasticsearch.core.search.FieldCollapse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.Queries;
import org.springframework.data.elasticsearch.junit.jupiter.ReactiveElasticsearchTemplateConfiguration;
import org.springframework.data.elasticsearch.repositories.custommethod.QueryParameter;
import org.springframework.data.elasticsearch.repository.config.EnableReactiveElasticsearchRepositories;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.test.context.ContextConfiguration;
import reactor.test.StepVerifier;

/**
 * @author Peter-Josef Meisch
 * @author Haibo Liu
 * @since 4.4
 */
@ContextConfiguration(classes = { SimpleReactiveElasticsearchRepositoryELCIntegrationTests.Config.class })
public class SimpleReactiveElasticsearchRepositoryELCIntegrationTests
		extends SimpleReactiveElasticsearchRepositoryIntegrationTests {

	@Configuration
	@Import({ ReactiveElasticsearchTemplateConfiguration.class })
	@EnableReactiveElasticsearchRepositories(considerNestedRepositories = true)
	static class Config {
		@Bean
		IndexNameProvider indexNameProvider() {
			return new IndexNameProvider("simple-reactive-repository");
		}

		/**
		 * a normal bean referenced by SpEL in query
		 */
		@Bean
		QueryParameter queryParameter() {
			return new QueryParameter("message");
		}
	}

	/**
	 * search_after is used by the reactive search operation, it normally always adds _shard_doc as a tiebreaker sort
	 * parameter. This must not be done when a collapse field is used as sort field, as in that case the collapse field
	 * must be the only sort field.
	 */
	@Test // #2935
	@DisplayName("should use collapse_field for search_after in pit search")
	void shouldUseCollapseFieldForSearchAfterI() {
		var entity = new SampleEntity();
		entity.setId("42");
		entity.setMessage("m");
		entity.setKeyword("kw");
		repository.save(entity).block();

		var query = NativeQuery.builder()
				.withQuery(Queries.matchAllQueryAsQuery())
				.withPageable(Pageable.unpaged())
				.withFieldCollapse(FieldCollapse.of(fcb -> fcb
						.field("keyword")))
				.withSort(Sort.by("keyword"))
				.build();

		operations.search(query, SampleEntity.class)
				.as(StepVerifier::create)
				.expectNextCount(1)
				.verifyComplete();
	}


}
