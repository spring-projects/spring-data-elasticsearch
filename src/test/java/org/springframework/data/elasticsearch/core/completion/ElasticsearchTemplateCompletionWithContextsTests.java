/*
 * Copyright 2013-2019 the original author or authors.
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
package org.springframework.data.elasticsearch.core.completion;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.elasticsearch.search.suggest.SuggestionBuilder;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.elasticsearch.search.suggest.completion.CompletionSuggestionBuilder;
import org.elasticsearch.search.suggest.completion.context.CategoryQueryContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.entities.NonDocumentEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * @author Robert Gruendler
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:elasticsearch-template-test.xml")
public class ElasticsearchTemplateCompletionWithContextsTests {

	@Autowired
	private ElasticsearchTemplate elasticsearchTemplate;

	private void loadContextCompletionObjectEntities() {
		elasticsearchTemplate.deleteIndex(ContextCompletionEntity.class);
		elasticsearchTemplate.createIndex(ContextCompletionEntity.class);
		elasticsearchTemplate.refresh(ContextCompletionEntity.class);
		elasticsearchTemplate.putMapping(ContextCompletionEntity.class);

		NonDocumentEntity nonDocumentEntity = new NonDocumentEntity();
		nonDocumentEntity.setSomeField1("foo");
		nonDocumentEntity.setSomeField2("bar");

		List<IndexQuery> indexQueries = new ArrayList<>();

		Map<String, List<String>> context1 = new HashMap<>();
		context1.put(ContextCompletionEntity.LANGUAGE_CATEGORY, Arrays.asList("java", "elastic"));
		indexQueries.add(new ContextCompletionEntityBuilder("1").name("Rizwan Idrees").suggest(new String[]{"Rizwan Idrees"}, context1).buildIndex());

		Map<String, List<String>> context2 = new HashMap<>();
		context2.put(ContextCompletionEntity.LANGUAGE_CATEGORY, Arrays.asList("kotlin", "mongo"));
		indexQueries.add(new ContextCompletionEntityBuilder("2").name("Franck Marchand").suggest(new String[]{"Franck", "Marchand"}, context2).buildIndex());

		Map<String, List<String>> context3 = new HashMap<>();
		context3.put(ContextCompletionEntity.LANGUAGE_CATEGORY, Arrays.asList("kotlin", "elastic"));
		indexQueries.add(new ContextCompletionEntityBuilder("3").name("Mohsin Husen").suggest(new String[]{"Mohsin", "Husen"}, context3).buildIndex());

		Map<String, List<String>> context4 = new HashMap<>();
		context4.put(ContextCompletionEntity.LANGUAGE_CATEGORY, Arrays.asList("java", "kotlin", "redis"));
		indexQueries.add(new ContextCompletionEntityBuilder("4").name("Artur Konczak").suggest(new String[]{"Artur", "Konczak"}, context4).buildIndex());

		elasticsearchTemplate.bulkIndex(indexQueries);
		elasticsearchTemplate.refresh(ContextCompletionEntity.class);
	}

	@Test
	public void shouldPutMappingForGivenEntity() throws Exception {
		//given
		Class entity = ContextCompletionEntity.class;
		elasticsearchTemplate.createIndex(entity);

		//when
		assertThat(elasticsearchTemplate.putMapping(entity), is(true));
	}

	@Test // DATAES-536
	public void shouldFindSuggestionsForGivenCriteriaQueryUsingContextCompletionEntityOfMongo() {
		//given
		loadContextCompletionObjectEntities();
		SuggestionBuilder completionSuggestionFuzzyBuilder = SuggestBuilders.completionSuggestion("suggest").prefix("m", Fuzziness.AUTO);

		Map<String, List<? extends ToXContent>> contextMap = new HashMap<>();
		List<CategoryQueryContext> contexts = new ArrayList<>(1);

		final CategoryQueryContext.Builder builder = CategoryQueryContext.builder();
		builder.setCategory("mongo");
		CategoryQueryContext queryContext = builder.build();
		contexts.add(queryContext);
		contextMap.put(ContextCompletionEntity.LANGUAGE_CATEGORY, contexts);

		((CompletionSuggestionBuilder) completionSuggestionFuzzyBuilder).contexts(contextMap);

		//when
		final SearchResponse suggestResponse  = elasticsearchTemplate.suggest(new SuggestBuilder().addSuggestion("test-suggest", completionSuggestionFuzzyBuilder), ContextCompletionEntity.class);
		assertNotNull(suggestResponse.getSuggest());
		CompletionSuggestion completionSuggestion = suggestResponse.getSuggest().getSuggestion("test-suggest");
		List<CompletionSuggestion.Entry.Option> options = completionSuggestion.getEntries().get(0).getOptions();

		//then
		assertThat(options.size(), is(1));
		assertThat(options.get(0).getText().string(), isOneOf("Marchand"));
	}

	@Test // DATAES-536
	public void shouldFindSuggestionsForGivenCriteriaQueryUsingContextCompletionEntityOfElastic() {
		//given
		loadContextCompletionObjectEntities();
		SuggestionBuilder completionSuggestionFuzzyBuilder = SuggestBuilders.completionSuggestion("suggest").prefix("m", Fuzziness.AUTO);

		Map<String, List<? extends ToXContent>> contextMap = new HashMap<>();
		List<CategoryQueryContext> contexts = new ArrayList<>(1);

		final CategoryQueryContext.Builder builder = CategoryQueryContext.builder();
		builder.setCategory("elastic");
		CategoryQueryContext queryContext = builder.build();
		contexts.add(queryContext);
		contextMap.put(ContextCompletionEntity.LANGUAGE_CATEGORY, contexts);

		((CompletionSuggestionBuilder) completionSuggestionFuzzyBuilder).contexts(contextMap);

		//when
		final SearchResponse suggestResponse  = elasticsearchTemplate.suggest(new SuggestBuilder().addSuggestion("test-suggest", completionSuggestionFuzzyBuilder), ContextCompletionEntity.class);
		assertNotNull(suggestResponse.getSuggest());
		CompletionSuggestion completionSuggestion = suggestResponse.getSuggest().getSuggestion("test-suggest");
		List<CompletionSuggestion.Entry.Option> options = completionSuggestion.getEntries().get(0).getOptions();

		//then
		assertThat(options.size(), is(1));
		assertThat(options.get(0).getText().string(), isOneOf( "Mohsin"));
	}

	@Test // DATAES-536
	public void shouldFindSuggestionsForGivenCriteriaQueryUsingContextCompletionEntityOfKotlin() {
		//given
		loadContextCompletionObjectEntities();
		SuggestionBuilder completionSuggestionFuzzyBuilder = SuggestBuilders.completionSuggestion("suggest").prefix("m", Fuzziness.AUTO);

		Map<String, List<? extends ToXContent>> contextMap = new HashMap<>();
		List<CategoryQueryContext> contexts = new ArrayList<>(1);

		final CategoryQueryContext.Builder builder = CategoryQueryContext.builder();
		builder.setCategory("kotlin");
		CategoryQueryContext queryContext = builder.build();
		contexts.add(queryContext);
		contextMap.put(ContextCompletionEntity.LANGUAGE_CATEGORY, contexts);

		((CompletionSuggestionBuilder) completionSuggestionFuzzyBuilder).contexts(contextMap);

		//when
		final SearchResponse suggestResponse  = elasticsearchTemplate.suggest(new SuggestBuilder().addSuggestion("test-suggest", completionSuggestionFuzzyBuilder), ContextCompletionEntity.class);
		assertNotNull(suggestResponse.getSuggest());
		CompletionSuggestion completionSuggestion = suggestResponse.getSuggest().getSuggestion("test-suggest");
		List<CompletionSuggestion.Entry.Option> options = completionSuggestion.getEntries().get(0).getOptions();

		//then
		assertThat(options.size(), is(2));
		assertThat(options.get(0).getText().string(), isOneOf("Marchand", "Mohsin"));
		assertThat(options.get(1).getText().string(), isOneOf("Marchand", "Mohsin"));
	}
}
