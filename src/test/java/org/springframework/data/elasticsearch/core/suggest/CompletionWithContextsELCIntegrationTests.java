/*
 * Copyright 2023-2024 the original author or authors.
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

import co.elastic.clients.elasticsearch.core.search.CompletionContext;
import co.elastic.clients.elasticsearch.core.search.FieldSuggester;
import co.elastic.clients.elasticsearch.core.search.SuggestFuzziness;
import co.elastic.clients.elasticsearch.core.search.Suggester;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.junit.jupiter.ElasticsearchTemplateConfiguration;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Peter-Josef Meisch
 * @since 5.1
 */
@ContextConfiguration(classes = { CompletionWithContextsELCIntegrationTests.Config.class })
public class CompletionWithContextsELCIntegrationTests extends CompletionWithContextsIntegrationTests {
	@Configuration
	@Import({ ElasticsearchTemplateConfiguration.class })
	static class Config {
		@Bean
		IndexNameProvider indexNameProvider() {
			return new IndexNameProvider("completion-context");
		}
	}

	@Override
	protected Query getSearchQuery(String suggestionName, String category) {
		return NativeQuery.builder() //
				.withSuggester(Suggester.of(s -> s //
						.suggesters(//
								suggestionName, //
								FieldSuggester.of(fs -> fs //
										.prefix("m") //
										.completion(cs -> cs //
												.field("suggest") //
												.fuzzy(SuggestFuzziness.of(f -> f.fuzziness("AUTO"))) //
												.contexts( //
														ContextCompletionEntity.LANGUAGE_CATEGORY, //
														List.of(CompletionContext.of(cc -> cc //
																.context(cb -> cb.category(category)) //
														)) //
												) //
										) //
								) //
						))) //
				.build();
	}
}
