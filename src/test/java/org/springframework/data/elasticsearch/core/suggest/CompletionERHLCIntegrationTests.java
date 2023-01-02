/*
 * Copyright 2019-2023 the original author or authors.
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

import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.client.erhlc.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.junit.jupiter.ElasticsearchRestTemplateConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Peter-Josef Meisch
 */
@ContextConfiguration(classes = { CompletionERHLCIntegrationTests.Config.class })
public class CompletionERHLCIntegrationTests extends CompletionIntegrationTests {

	@Configuration
	@Import({ ElasticsearchRestTemplateConfiguration.class })
	@EnableElasticsearchRepositories(considerNestedRepositories = true)
	static class Config {
		@Bean
		IndexNameProvider indexNameProvider() {
			return new IndexNameProvider("completion-es7");
		}
	}

	@Override
	protected Query getSuggestQuery(String suggestionName, String fieldName, String prefix) {
		return new NativeSearchQueryBuilder() //
				.withSuggestBuilder(new SuggestBuilder() //
						.addSuggestion(suggestionName, //
								SuggestBuilders.completionSuggestion(fieldName) //
										.prefix(prefix, Fuzziness.AUTO))) //
				.build(); //
	}

}
