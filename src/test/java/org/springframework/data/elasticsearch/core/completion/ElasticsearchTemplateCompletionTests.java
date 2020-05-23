/*
 * Copyright 2013-2020 the original author or authors.
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
import java.util.List;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.elasticsearch.search.suggest.SuggestionBuilder;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.CompletionField;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.core.AbstractElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.junit.jupiter.ElasticsearchRestTemplateConfiguration;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.data.elasticsearch.utils.IndexInitializer;
import org.springframework.lang.Nullable;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Franck Marchand
 * @author Artur Konczak
 * @author Mewes Kochheim
 * @author Peter-Josef Meisch
 */
@SpringIntegrationTest
@ContextConfiguration(classes = { ElasticsearchTemplateCompletionTests.Config.class })
public class ElasticsearchTemplateCompletionTests {

	@Configuration
	@Import({ ElasticsearchRestTemplateConfiguration.class })
	static class Config {}

	@Autowired private ElasticsearchOperations operations;

	@BeforeEach
	private void setup() {
		IndexInitializer.init(operations.indexOps(CompletionEntity.class));
		IndexInitializer.init(operations.indexOps(AnnotatedCompletionEntity.class));
	}

	@AfterEach
	void after() {
		operations.indexOps(CompletionEntity.class).delete();
		operations.indexOps(AnnotatedCompletionEntity.class).delete();
	}

	private void loadCompletionObjectEntities() {

		List<IndexQuery> indexQueries = new ArrayList<>();
		indexQueries.add(
				new CompletionEntityBuilder("1").name("Rizwan Idrees").suggest(new String[] { "Rizwan Idrees" }).buildIndex());
		indexQueries.add(new CompletionEntityBuilder("2").name("Franck Marchand")
				.suggest(new String[] { "Franck", "Marchand" }).buildIndex());
		indexQueries.add(
				new CompletionEntityBuilder("3").name("Mohsin Husen").suggest(new String[] { "Mohsin", "Husen" }).buildIndex());
		indexQueries.add(new CompletionEntityBuilder("4").name("Artur Konczak").suggest(new String[] { "Artur", "Konczak" })
				.buildIndex());

		IndexCoordinates index = IndexCoordinates.of("test-index-core-completion");
		operations.bulkIndex(indexQueries, index);
		operations.indexOps(CompletionEntity.class).refresh();
	}

	private void loadAnnotatedCompletionObjectEntities() {

		NonDocumentEntity nonDocumentEntity = new NonDocumentEntity();
		nonDocumentEntity.setSomeField1("foo");
		nonDocumentEntity.setSomeField2("bar");

		List<IndexQuery> indexQueries = new ArrayList<>();
		indexQueries.add(new AnnotatedCompletionEntityBuilder("1").name("Franck Marchand")
				.suggest(new String[] { "Franck", "Marchand" }).buildIndex());
		indexQueries.add(new AnnotatedCompletionEntityBuilder("2").name("Mohsin Husen")
				.suggest(new String[] { "Mohsin", "Husen" }).buildIndex());
		indexQueries.add(new AnnotatedCompletionEntityBuilder("3").name("Rizwan Idrees")
				.suggest(new String[] { "Rizwan", "Idrees" }).buildIndex());
		indexQueries.add(new AnnotatedCompletionEntityBuilder("4").name("Artur Konczak")
				.suggest(new String[] { "Artur", "Konczak" }).buildIndex());

		operations.bulkIndex(indexQueries, IndexCoordinates.of("test-index-annotated-completion"));
		operations.indexOps(AnnotatedCompletionEntity.class).refresh();
	}

	private void loadAnnotatedCompletionObjectEntitiesWithWeights() {

		List<IndexQuery> indexQueries = new ArrayList<>();
		indexQueries.add(new AnnotatedCompletionEntityBuilder("1").name("Mewes Kochheim1")
				.suggest(new String[] { "Mewes Kochheim1" }, 4).buildIndex());
		indexQueries.add(new AnnotatedCompletionEntityBuilder("2").name("Mewes Kochheim2")
				.suggest(new String[] { "Mewes Kochheim2" }, 1).buildIndex());
		indexQueries.add(new AnnotatedCompletionEntityBuilder("3").name("Mewes Kochheim3")
				.suggest(new String[] { "Mewes Kochheim3" }, 2).buildIndex());
		indexQueries.add(new AnnotatedCompletionEntityBuilder("4").name("Mewes Kochheim4")
				.suggest(new String[] { "Mewes Kochheim4" }, Integer.MAX_VALUE).buildIndex());

		operations.bulkIndex(indexQueries, IndexCoordinates.of("test-index-annotated-completion"));
		operations.indexOps(AnnotatedCompletionEntity.class).refresh();
	}

	@Test
	public void shouldFindSuggestionsForGivenCriteriaQueryUsingCompletionEntity() {

		// given
		loadCompletionObjectEntities();

		SuggestionBuilder completionSuggestionFuzzyBuilder = SuggestBuilders.completionSuggestion("suggest").prefix("m",
				Fuzziness.AUTO);

		// when
		SearchResponse suggestResponse = ((AbstractElasticsearchTemplate) operations).suggest(
				new SuggestBuilder().addSuggestion("test-suggest", completionSuggestionFuzzyBuilder),
				IndexCoordinates.of("test-index-core-completion"));
		CompletionSuggestion completionSuggestion = suggestResponse.getSuggest().getSuggestion("test-suggest");
		List<CompletionSuggestion.Entry.Option> options = completionSuggestion.getEntries().get(0).getOptions();

		// then
		assertThat(options).hasSize(2);
		assertThat(options.get(0).getText().string()).isIn("Marchand", "Mohsin");
		assertThat(options.get(1).getText().string()).isIn("Marchand", "Mohsin");
	}

	@Test // DATAES-754
	void shouldRetrieveEntityWithCompletion() {
		loadCompletionObjectEntities();
		IndexCoordinates index = IndexCoordinates.of("test-index-core-completion");
		operations.get("1", CompletionEntity.class, index);
	}

	@Test
	public void shouldFindSuggestionsForGivenCriteriaQueryUsingAnnotatedCompletionEntity() {

		// given
		loadAnnotatedCompletionObjectEntities();
		SuggestionBuilder completionSuggestionFuzzyBuilder = SuggestBuilders.completionSuggestion("suggest").prefix("m",
				Fuzziness.AUTO);

		// when
		SearchResponse suggestResponse = ((AbstractElasticsearchTemplate) operations).suggest(
				new SuggestBuilder().addSuggestion("test-suggest", completionSuggestionFuzzyBuilder),
				IndexCoordinates.of("test-index-annotated-completion"));
		CompletionSuggestion completionSuggestion = suggestResponse.getSuggest().getSuggestion("test-suggest");
		List<CompletionSuggestion.Entry.Option> options = completionSuggestion.getEntries().get(0).getOptions();

		// then
		assertThat(options).hasSize(2);
		assertThat(options.get(0).getText().string()).isIn("Marchand", "Mohsin");
		assertThat(options.get(1).getText().string()).isIn("Marchand", "Mohsin");
	}

	@Test
	public void shouldFindSuggestionsWithWeightsForGivenCriteriaQueryUsingAnnotatedCompletionEntity() {

		// given
		loadAnnotatedCompletionObjectEntitiesWithWeights();
		SuggestionBuilder completionSuggestionFuzzyBuilder = SuggestBuilders.completionSuggestion("suggest").prefix("m",
				Fuzziness.AUTO);

		// when
		SearchResponse suggestResponse = ((AbstractElasticsearchTemplate) operations).suggest(
				new SuggestBuilder().addSuggestion("test-suggest", completionSuggestionFuzzyBuilder),
				IndexCoordinates.of("test-index-annotated-completion"));
		CompletionSuggestion completionSuggestion = suggestResponse.getSuggest().getSuggestion("test-suggest");
		List<CompletionSuggestion.Entry.Option> options = completionSuggestion.getEntries().get(0).getOptions();

		// then
		assertThat(options).hasSize(4);
		for (CompletionSuggestion.Entry.Option option : options) {
			switch (option.getText().string()) {
				case "Mewes Kochheim1":
					assertThat(option.getScore()).isEqualTo(4);
					break;
				case "Mewes Kochheim2":
					assertThat(option.getScore()).isEqualTo(1);
					break;
				case "Mewes Kochheim3":
					assertThat(option.getScore()).isEqualTo(2);
					break;
				case "Mewes Kochheim4":
					assertThat(option.getScore()).isEqualTo(Integer.MAX_VALUE);
					break;
				default:
					fail("Unexpected option");
					break;
			}
		}
	}

	/**
	 * @author Rizwan Idrees
	 * @author Mohsin Husen
	 */
	static class NonDocumentEntity {

		@Nullable @Id private String someId;
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
	 */
	@Document(indexName = "test-index-core-completion", replicas = 0, refreshInterval = "-1")
	static class CompletionEntity {

		@Nullable @Id private String id;

		@Nullable private String name;

		@Nullable @CompletionField(maxInputLength = 100) private Completion suggest;

		private CompletionEntity() {}

		public CompletionEntity(String id) {
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
	 * @author Mewes Kochheim
	 */
	static class CompletionEntityBuilder {

		private CompletionEntity result;

		public CompletionEntityBuilder(String id) {
			result = new CompletionEntity(id);
		}

		public CompletionEntityBuilder name(String name) {
			result.setName(name);
			return this;
		}

		public CompletionEntityBuilder suggest(String[] input) {
			return suggest(input, null);
		}

		public CompletionEntityBuilder suggest(String[] input, Integer weight) {
			Completion suggest = new Completion(input);
			suggest.setWeight(weight);

			result.setSuggest(suggest);
			return this;
		}

		public CompletionEntity build() {
			return result;
		}

		public IndexQuery buildIndex() {
			IndexQuery indexQuery = new IndexQuery();
			indexQuery.setId(result.getId());
			indexQuery.setObject(result);
			return indexQuery;
		}
	}

	/**
	 * @author Mewes Kochheim
	 */
	@Document(indexName = "test-index-annotated-completion", replicas = 0, refreshInterval = "-1")
	static class AnnotatedCompletionEntity {

		@Nullable @Id private String id;
		@Nullable private String name;
		@Nullable @CompletionField(maxInputLength = 100) private Completion suggest;

		private AnnotatedCompletionEntity() {}

		public AnnotatedCompletionEntity(String id) {
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
	 * @author Franck Marchand
	 * @author Mohsin Husen
	 * @author Mewes Kochheim
	 */
	static class AnnotatedCompletionEntityBuilder {

		private AnnotatedCompletionEntity result;

		public AnnotatedCompletionEntityBuilder(String id) {
			result = new AnnotatedCompletionEntity(id);
		}

		public AnnotatedCompletionEntityBuilder name(String name) {
			result.setName(name);
			return this;
		}

		public AnnotatedCompletionEntityBuilder suggest(String[] input) {
			return suggest(input, null);
		}

		public AnnotatedCompletionEntityBuilder suggest(String[] input, Integer weight) {
			Completion suggest = new Completion(input);
			suggest.setWeight(weight);
			result.setSuggest(suggest);
			return this;
		}

		public AnnotatedCompletionEntity build() {
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
