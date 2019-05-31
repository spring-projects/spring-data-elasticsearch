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

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.elasticsearch.search.suggest.SuggestionBuilder;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.elasticsearch.search.suggest.completion.CompletionSuggestionBuilder;
import org.elasticsearch.search.suggest.completion.context.CategoryQueryContext;
import org.elasticsearch.search.suggest.completion.context.ContextMapping;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.CompletionContext;
import org.springframework.data.elasticsearch.annotations.CompletionField;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.utils.IndexInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Robert Gruendler
 * @author Peter-Josef Meisch
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:elasticsearch-template-test.xml")
public class ElasticsearchTemplateCompletionWithContextsTests {

	@Autowired private ElasticsearchTemplate elasticsearchTemplate;

	private void loadContextCompletionObjectEntities() {

		IndexInitializer.init(elasticsearchTemplate, ContextCompletionEntity.class);

		NonDocumentEntity nonDocumentEntity = new NonDocumentEntity();
		nonDocumentEntity.setSomeField1("foo");
		nonDocumentEntity.setSomeField2("bar");

		List<IndexQuery> indexQueries = new ArrayList<>();

		Map<String, List<String>> context1 = new HashMap<>();
		context1.put(ContextCompletionEntity.LANGUAGE_CATEGORY, Arrays.asList("java", "elastic"));
		indexQueries.add(new ContextCompletionEntityBuilder("1").name("Rizwan Idrees")
				.suggest(new String[] { "Rizwan Idrees" }, context1).buildIndex());

		Map<String, List<String>> context2 = new HashMap<>();
		context2.put(ContextCompletionEntity.LANGUAGE_CATEGORY, Arrays.asList("kotlin", "mongo"));
		indexQueries.add(new ContextCompletionEntityBuilder("2").name("Franck Marchand")
				.suggest(new String[] { "Franck", "Marchand" }, context2).buildIndex());

		Map<String, List<String>> context3 = new HashMap<>();
		context3.put(ContextCompletionEntity.LANGUAGE_CATEGORY, Arrays.asList("kotlin", "elastic"));
		indexQueries.add(new ContextCompletionEntityBuilder("3").name("Mohsin Husen")
				.suggest(new String[] { "Mohsin", "Husen" }, context3).buildIndex());

		Map<String, List<String>> context4 = new HashMap<>();
		context4.put(ContextCompletionEntity.LANGUAGE_CATEGORY, Arrays.asList("java", "kotlin", "redis"));
		indexQueries.add(new ContextCompletionEntityBuilder("4").name("Artur Konczak")
				.suggest(new String[] { "Artur", "Konczak" }, context4).buildIndex());

		elasticsearchTemplate.bulkIndex(indexQueries);
		elasticsearchTemplate.refresh(ContextCompletionEntity.class);
	}

	@Test
	public void shouldPutMappingForGivenEntity() throws Exception {

		// given
		Class<?> entity = ContextCompletionEntity.class;
		elasticsearchTemplate.createIndex(entity);

		// when
		assertThat(elasticsearchTemplate.putMapping(entity)).isTrue();
	}

	@Test // DATAES-536
	public void shouldFindSuggestionsForGivenCriteriaQueryUsingContextCompletionEntityOfMongo() {

		// given
		loadContextCompletionObjectEntities();
		SuggestionBuilder completionSuggestionFuzzyBuilder = SuggestBuilders.completionSuggestion("suggest").prefix("m",
				Fuzziness.AUTO);

		Map<String, List<? extends ToXContent>> contextMap = new HashMap<>();
		List<CategoryQueryContext> contexts = new ArrayList<>(1);

		CategoryQueryContext.Builder builder = CategoryQueryContext.builder();
		builder.setCategory("mongo");
		CategoryQueryContext queryContext = builder.build();
		contexts.add(queryContext);
		contextMap.put(ContextCompletionEntity.LANGUAGE_CATEGORY, contexts);

		((CompletionSuggestionBuilder) completionSuggestionFuzzyBuilder).contexts(contextMap);

		// when
		SearchResponse suggestResponse = elasticsearchTemplate.suggest(
				new SuggestBuilder().addSuggestion("test-suggest", completionSuggestionFuzzyBuilder),
				ContextCompletionEntity.class);
		assertThat(suggestResponse.getSuggest()).isNotNull();
		CompletionSuggestion completionSuggestion = suggestResponse.getSuggest().getSuggestion("test-suggest");
		List<CompletionSuggestion.Entry.Option> options = completionSuggestion.getEntries().get(0).getOptions();

		// then
		assertThat(options).hasSize(1);
		assertThat(options.get(0).getText().string()).isEqualTo("Marchand");
	}

	@Test // DATAES-536
	public void shouldFindSuggestionsForGivenCriteriaQueryUsingContextCompletionEntityOfElastic() {

		// given
		loadContextCompletionObjectEntities();
		SuggestionBuilder completionSuggestionFuzzyBuilder = SuggestBuilders.completionSuggestion("suggest").prefix("m",
				Fuzziness.AUTO);

		Map<String, List<? extends ToXContent>> contextMap = new HashMap<>();
		List<CategoryQueryContext> contexts = new ArrayList<>(1);

		CategoryQueryContext.Builder builder = CategoryQueryContext.builder();
		builder.setCategory("elastic");
		CategoryQueryContext queryContext = builder.build();
		contexts.add(queryContext);
		contextMap.put(ContextCompletionEntity.LANGUAGE_CATEGORY, contexts);

		((CompletionSuggestionBuilder) completionSuggestionFuzzyBuilder).contexts(contextMap);

		// when
		SearchResponse suggestResponse = elasticsearchTemplate.suggest(
				new SuggestBuilder().addSuggestion("test-suggest", completionSuggestionFuzzyBuilder),
				ContextCompletionEntity.class);
		assertThat(suggestResponse.getSuggest()).isNotNull();
		CompletionSuggestion completionSuggestion = suggestResponse.getSuggest().getSuggestion("test-suggest");
		List<CompletionSuggestion.Entry.Option> options = completionSuggestion.getEntries().get(0).getOptions();

		// then
		assertThat(options).hasSize(1);
		assertThat(options.get(0).getText().string()).isEqualTo("Mohsin");
	}

	@Test // DATAES-536
	public void shouldFindSuggestionsForGivenCriteriaQueryUsingContextCompletionEntityOfKotlin() {

		// given
		loadContextCompletionObjectEntities();
		SuggestionBuilder completionSuggestionFuzzyBuilder = SuggestBuilders.completionSuggestion("suggest").prefix("m",
				Fuzziness.AUTO);

		Map<String, List<? extends ToXContent>> contextMap = new HashMap<>();
		List<CategoryQueryContext> contexts = new ArrayList<>(1);

		CategoryQueryContext.Builder builder = CategoryQueryContext.builder();
		builder.setCategory("kotlin");
		CategoryQueryContext queryContext = builder.build();
		contexts.add(queryContext);
		contextMap.put(ContextCompletionEntity.LANGUAGE_CATEGORY, contexts);

		((CompletionSuggestionBuilder) completionSuggestionFuzzyBuilder).contexts(contextMap);

		// when
		SearchResponse suggestResponse = elasticsearchTemplate.suggest(
				new SuggestBuilder().addSuggestion("test-suggest", completionSuggestionFuzzyBuilder),
				ContextCompletionEntity.class);
		assertThat(suggestResponse.getSuggest()).isNotNull();
		CompletionSuggestion completionSuggestion = suggestResponse.getSuggest().getSuggestion("test-suggest");
		List<CompletionSuggestion.Entry.Option> options = completionSuggestion.getEntries().get(0).getOptions();

		// then
		assertThat(options).hasSize(2);
		assertThat(options.get(0).getText().string()).isIn("Marchand", "Mohsin");
		assertThat(options.get(1).getText().string()).isIn("Marchand", "Mohsin");
	}

	/**
	 * @author Rizwan Idrees
	 * @author Mohsin Husen
	 */
	static class NonDocumentEntity {

		@Id private String someId;
		private String someField1;
		private String someField2;

		public String getSomeField1() {
			return someField1;
		}

		public void setSomeField1(String someField1) {
			this.someField1 = someField1;
		}

		public String getSomeField2() {
			return someField2;
		}

		public void setSomeField2(String someField2) {
			this.someField2 = someField2;
		}
	}

	/**
	 * @author Mewes Kochheim
	 * @author Robert Gruendler
	 */
	@Document(indexName = "test-index-context-completion", type = "context-completion-type", shards = 1, replicas = 0,
			refreshInterval = "-1")
	static class ContextCompletionEntity {

		public static final String LANGUAGE_CATEGORY = "language";
		@Id private String id;
		private String name;

		@CompletionField(maxInputLength = 100, contexts = {
				@CompletionContext(name = LANGUAGE_CATEGORY, type = ContextMapping.Type.CATEGORY) }) private Completion suggest;

		private ContextCompletionEntity() {}

		public ContextCompletionEntity(String id) {
			this.id = id;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Completion getSuggest() {
			return suggest;
		}

		public void setSuggest(Completion suggest) {
			this.suggest = suggest;
		}
	}

	/**
	 * @author Robert Gruendler
	 */
	static class ContextCompletionEntityBuilder {

		private ContextCompletionEntity result;

		public ContextCompletionEntityBuilder(String id) {
			result = new ContextCompletionEntity(id);
		}

		public ContextCompletionEntityBuilder name(String name) {
			result.setName(name);
			return this;
		}

		public ContextCompletionEntityBuilder suggest(String[] input, Map<String, List<String>> contexts) {
			Completion suggest = new Completion(input);
			suggest.setContexts(contexts);

			result.setSuggest(suggest);
			return this;
		}

		public ContextCompletionEntity build() {
			return result;
		}

		public IndexQuery buildIndex() {
			IndexQuery indexQuery = new IndexQuery();
			indexQuery.setId(result.getId());
			indexQuery.setObject(result);
			return indexQuery;
		}
	}

}
