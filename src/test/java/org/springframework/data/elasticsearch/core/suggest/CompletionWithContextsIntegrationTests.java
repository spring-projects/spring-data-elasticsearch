/*
 * Copyright 2013-2024 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.CompletionContext;
import org.springframework.data.elasticsearch.annotations.CompletionField;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.suggest.response.Suggest;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.lang.Nullable;

/**
 * @author Robert Gruendler
 * @author Peter-Josef Meisch
 */
@SpringIntegrationTest
public abstract class CompletionWithContextsIntegrationTests {

	private static final String SUGGESTION_NAME = "test-suggest";

	@Autowired private ElasticsearchOperations operations;
	@Autowired private IndexNameProvider indexNameProvider;

	@BeforeEach
	void setup() {
		indexNameProvider.increment();
		operations.indexOps(ContextCompletionEntity.class).createWithMapping();
	}

	@Test
	@Order(java.lang.Integer.MAX_VALUE)
	void cleanup() {
		operations.indexOps(IndexCoordinates.of(indexNameProvider.getPrefix() + "*")).delete();
	}

	private void loadContextCompletionObjectEntities() {

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

		operations.bulkIndex(indexQueries, IndexCoordinates.of(indexNameProvider.indexName()));
		operations.indexOps(ContextCompletionEntity.class).refresh();
	}

	abstract protected Query getSearchQuery(String suggestionName, String category);

	@Test // DATAES-536
	public void shouldFindSuggestionsForGivenCriteriaQueryUsingContextCompletionEntityOfMongo() {

		loadContextCompletionObjectEntities();
		Query query = getSearchQuery(SUGGESTION_NAME, "mongo");

		var searchHits = operations.search(query, ContextCompletionEntity.class);

		assertThat(searchHits.hasSuggest()).isTrue();
		Suggest suggest = searchHits.getSuggest();
		Suggest.Suggestion<? extends Suggest.Suggestion.Entry<? extends Suggest.Suggestion.Entry.Option>> suggestion = suggest
				.getSuggestion(SUGGESTION_NAME);
		assertThat(suggestion).isNotNull();
		assertThat(suggestion)
				.isInstanceOf(org.springframework.data.elasticsearch.core.suggest.response.CompletionSuggestion.class);
		List<org.springframework.data.elasticsearch.core.suggest.response.CompletionSuggestion.Entry.Option<CompletionIntegrationTests.AnnotatedCompletionEntity>> options = ((org.springframework.data.elasticsearch.core.suggest.response.CompletionSuggestion<CompletionIntegrationTests.AnnotatedCompletionEntity>) suggestion)
				.getEntries().get(0).getOptions();
		assertThat(options).hasSize(1);
		assertThat(options.get(0).getText()).isEqualTo("Marchand");
	}

	@Test // DATAES-536
	public void shouldFindSuggestionsForGivenCriteriaQueryUsingContextCompletionEntityOfElastic() {

		loadContextCompletionObjectEntities();
		Query query = getSearchQuery(SUGGESTION_NAME, "elastic");

		var searchHits = operations.search(query, ContextCompletionEntity.class);

		assertThat(searchHits.hasSuggest()).isTrue();
		Suggest suggest = searchHits.getSuggest();
		Suggest.Suggestion<? extends Suggest.Suggestion.Entry<? extends Suggest.Suggestion.Entry.Option>> suggestion = suggest
				.getSuggestion(SUGGESTION_NAME);
		assertThat(suggestion).isNotNull();
		assertThat(suggestion)
				.isInstanceOf(org.springframework.data.elasticsearch.core.suggest.response.CompletionSuggestion.class);
		List<org.springframework.data.elasticsearch.core.suggest.response.CompletionSuggestion.Entry.Option<CompletionIntegrationTests.AnnotatedCompletionEntity>> options = ((org.springframework.data.elasticsearch.core.suggest.response.CompletionSuggestion<CompletionIntegrationTests.AnnotatedCompletionEntity>) suggestion)
				.getEntries().get(0).getOptions();
		assertThat(options).hasSize(1);
		assertThat(options.get(0).getText()).isEqualTo("Mohsin");
	}

	@Test // DATAES-536
	public void shouldFindSuggestionsForGivenCriteriaQueryUsingContextCompletionEntityOfKotlin() {

		loadContextCompletionObjectEntities();
		Query query = getSearchQuery(SUGGESTION_NAME, "kotlin");

		var searchHits = operations.search(query, ContextCompletionEntity.class);

		assertThat(searchHits.hasSuggest()).isTrue();
		Suggest suggest = searchHits.getSuggest();
		Suggest.Suggestion<? extends Suggest.Suggestion.Entry<? extends Suggest.Suggestion.Entry.Option>> suggestion = suggest
				.getSuggestion(SUGGESTION_NAME);
		assertThat(suggestion).isNotNull();
		assertThat(suggestion)
				.isInstanceOf(org.springframework.data.elasticsearch.core.suggest.response.CompletionSuggestion.class);
		List<org.springframework.data.elasticsearch.core.suggest.response.CompletionSuggestion.Entry.Option<CompletionIntegrationTests.AnnotatedCompletionEntity>> options = ((org.springframework.data.elasticsearch.core.suggest.response.CompletionSuggestion<CompletionIntegrationTests.AnnotatedCompletionEntity>) suggestion)
				.getEntries().get(0).getOptions();
		assertThat(options).hasSize(2);
		assertThat(options.get(0).getText()).isIn("Marchand", "Mohsin");
		assertThat(options.get(1).getText()).isIn("Marchand", "Mohsin");
	}

	/**
	 * @author Rizwan Idrees
	 * @author Mohsin Husen
	 */
	static class NonDocumentEntity {

		@Nullable
		@Id private String someId;
		@Nullable private String someField1;
		@Nullable private String someField2;

		@Nullable
		public String getSomeField1() {
			return someField1;
		}

		public void setSomeField1(String someField1) {
			this.someField1 = someField1;
		}

		@Nullable
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
	@Document(indexName = "#{@indexNameProvider.indexName()}")
	static class ContextCompletionEntity {

		public static final String LANGUAGE_CATEGORY = "language";
		@Nullable
		@Id private String id;
		@Nullable private String name;

		@Nullable
		@CompletionField(maxInputLength = 100, contexts = { @CompletionContext(name = LANGUAGE_CATEGORY,
				type = CompletionContext.ContextMappingType.CATEGORY) }) private Completion suggest;

		private ContextCompletionEntity() {}

		public ContextCompletionEntity(String id) {
			this.id = id;
		}

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		@Nullable
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Nullable
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

		private final ContextCompletionEntity result;

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
