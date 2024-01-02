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
package org.springframework.data.elasticsearch.core.suggest;

import co.elastic.clients.elasticsearch.core.search.FieldSuggester;
import co.elastic.clients.elasticsearch.core.search.SuggestFuzziness;
import co.elastic.clients.elasticsearch.core.search.Suggester;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.junit.jupiter.ReactiveElasticsearchTemplateConfiguration;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Peter-Josef Meisch
 * @since 4.4
 */
@ContextConfiguration(classes = { ReactiveSuggestELCIntegrationTests.Config.class })
public class ReactiveSuggestELCIntegrationTests extends ReactiveSuggestIntegrationTests {

	@Configuration
	@Import({ ReactiveElasticsearchTemplateConfiguration.class })
	static class Config {
		@Bean
		IndexNameProvider indexNameProvider() {
			return new IndexNameProvider("reactive-template-suggest");
		}
	}

	@Override
	protected Query getSuggestQuery(String suggestionName, String fieldName, String prefix) {
		return NativeQuery.builder() //
				.withSuggester(Suggester.of(s -> s //
						.suggesters(suggestionName, FieldSuggester.of(fs -> fs //
								.prefix(prefix) //
								.completion(cs -> cs //
										.field(fieldName) //
										.fuzzy(SuggestFuzziness.of(f -> f //
												// NOTE we currently need to set all these values to their default as the client code does not
												// have them nullable
												.fuzziness("AUTO") //
												.minLength(3) //
												.prefixLength(1) //
												.transpositions(true) //
												.unicodeAware(false))))//
						))) //
				).build();
	}
}
