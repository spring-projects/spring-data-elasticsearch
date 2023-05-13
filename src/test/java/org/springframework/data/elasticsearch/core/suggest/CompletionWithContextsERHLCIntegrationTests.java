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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.elasticsearch.search.suggest.completion.CompletionSuggestionBuilder;
import org.elasticsearch.search.suggest.completion.context.CategoryQueryContext;
import org.elasticsearch.xcontent.ToXContent;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.client.erhlc.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.junit.jupiter.ElasticsearchRestTemplateConfiguration;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Peter-Josef Meisch
 */
@SuppressWarnings("deprecation")
@ContextConfiguration(classes = { CompletionWithContextsERHLCIntegrationTests.Config.class })
public class CompletionWithContextsERHLCIntegrationTests extends CompletionWithContextsIntegrationTests {
	@Configuration
	@Import({ ElasticsearchRestTemplateConfiguration.class })
	static class Config {
		@Bean
		IndexNameProvider indexNameProvider() {
			return new IndexNameProvider("completion-context-es7");
		}
	}

	@Override
	@NotNull
	protected Query getSearchQuery(String suggestionName, String category) {

		CompletionSuggestionBuilder completionSuggestionFuzzyBuilder = SuggestBuilders.completionSuggestion("suggest")
				.prefix("m", Fuzziness.AUTO);

		Map<String, List<? extends ToXContent>> contextMap = new HashMap<>();
		List<CategoryQueryContext> contexts = new ArrayList<>(1);

		CategoryQueryContext queryContext = CategoryQueryContext.builder().setCategory(category).build();
		contexts.add(queryContext);
		contextMap.put(ContextCompletionEntity.LANGUAGE_CATEGORY, contexts);

		completionSuggestionFuzzyBuilder.contexts(contextMap);

		var suggestBuilder = new SuggestBuilder().addSuggestion(suggestionName, completionSuggestionFuzzyBuilder);
			return new NativeSearchQueryBuilder().withSuggestBuilder(suggestBuilder).build();
	}

}
