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
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.CompletionField;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.suggest.response.CompletionSuggestion;
import org.springframework.data.elasticsearch.core.suggest.response.Suggest;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.lang.Nullable;

/**
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Franck Marchand
 * @author Artur Konczak
 * @author Mewes Kochheim
 * @author Peter-Josef Meisch
 */
@SpringIntegrationTest
public abstract class CompletionIntegrationTests {

	@Autowired private ElasticsearchOperations operations;
	@Autowired private IndexNameProvider indexNameProvider;

	@BeforeEach
	void setup() {
		indexNameProvider.increment();
		operations.indexOps(CompletionEntity.class).createWithMapping();
		operations.indexOps(AnnotatedCompletionEntity.class).createWithMapping();
	}

	@Test
	@Order(java.lang.Integer.MAX_VALUE)
	void cleanup() {
		operations.indexOps(IndexCoordinates.of(indexNameProvider.getPrefix() + "*")).delete();
	}

	private void loadCompletionObjectEntities() {

		List<IndexQuery> indexQueries = new ArrayList<>();
		indexQueries.add(new CompletionEntityBuilder("1").name("Rizwan Idrees").suggest("Rizwan Idrees").buildIndex());
		indexQueries
				.add(new CompletionEntityBuilder("2").name("Franck Marchand").suggest("Franck", "Marchand").buildIndex());
		indexQueries.add(new CompletionEntityBuilder("3").name("Mohsin Husen").suggest("Mohsin", "Husen").buildIndex());
		indexQueries.add(new CompletionEntityBuilder("4").name("Artur Konczak").suggest("Artur", "Konczak").buildIndex());

		operations.bulkIndex(indexQueries, CompletionEntity.class);
	}

	private void loadAnnotatedCompletionObjectEntities() {

		NonDocumentEntity nonDocumentEntity = new NonDocumentEntity();
		nonDocumentEntity.setSomeField1("foo");
		nonDocumentEntity.setSomeField2("bar");

		List<IndexQuery> indexQueries = new ArrayList<>();
		indexQueries.add(
				new AnnotatedCompletionEntityBuilder("1").name("Franck Marchand").suggest("Franck", "Marchand").buildIndex());
		indexQueries
				.add(new AnnotatedCompletionEntityBuilder("2").name("Mohsin Husen").suggest("Mohsin", "Husen").buildIndex());
		indexQueries
				.add(new AnnotatedCompletionEntityBuilder("3").name("Rizwan Idrees").suggest("Rizwan", "Idrees").buildIndex());
		indexQueries
				.add(new AnnotatedCompletionEntityBuilder("4").name("Artur Konczak").suggest("Artur", "Konczak").buildIndex());

		operations.bulkIndex(indexQueries, AnnotatedCompletionEntity.class);
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
				.suggest(new String[] { "Mewes Kochheim4" }, 4444).buildIndex());

		operations.bulkIndex(indexQueries, AnnotatedCompletionEntity.class);
	}

	@Test
	public void shouldFindSuggestionsForGivenCriteriaQueryUsingCompletionEntity() {

		loadCompletionObjectEntities();
		Query query = getSuggestQuery("test-suggest", "suggest", "m");

		SearchHits<CompletionEntity> searchHits = operations.search(query, CompletionEntity.class);

		assertThat(searchHits.hasSuggest()).isTrue();
		Suggest suggest = searchHits.getSuggest();
		// noinspection ConstantConditions
		Suggest.Suggestion<? extends Suggest.Suggestion.Entry<? extends Suggest.Suggestion.Entry.Option>> suggestion = suggest
				.getSuggestion("test-suggest");
		assertThat(suggestion).isNotNull();
		assertThat(suggestion).isInstanceOf(CompletionSuggestion.class);
		// noinspection unchecked
		List<CompletionSuggestion.Entry.Option<AnnotatedCompletionEntity>> options = ((CompletionSuggestion<AnnotatedCompletionEntity>) suggestion)
				.getEntries().get(0).getOptions();
		assertThat(options).hasSize(2);
		assertThat(options.get(0).getText()).isIn("Marchand", "Mohsin");
		assertThat(options.get(1).getText()).isIn("Marchand", "Mohsin");
	}

	protected abstract Query getSuggestQuery(String suggestionName, String fieldName, String prefix);

	@Test // DATAES-754
	void shouldRetrieveEntityWithCompletion() {
		loadCompletionObjectEntities();
		operations.get("1", CompletionEntity.class);
	}

	@Test
	public void shouldFindSuggestionsForGivenCriteriaQueryUsingAnnotatedCompletionEntity() {

		loadAnnotatedCompletionObjectEntities();
		Query query = getSuggestQuery("test-suggest", "suggest", "m");

		SearchHits<AnnotatedCompletionEntity> searchHits = operations.search(query, AnnotatedCompletionEntity.class);

		assertThat(searchHits.hasSuggest()).isTrue();
		Suggest suggest = searchHits.getSuggest();
		// noinspection ConstantConditions
		Suggest.Suggestion<? extends Suggest.Suggestion.Entry<? extends Suggest.Suggestion.Entry.Option>> suggestion = suggest
				.getSuggestion("test-suggest");
		assertThat(suggestion).isNotNull();
		assertThat(suggestion).isInstanceOf(CompletionSuggestion.class);
		// noinspection unchecked
		List<CompletionSuggestion.Entry.Option<AnnotatedCompletionEntity>> options = ((CompletionSuggestion<AnnotatedCompletionEntity>) suggestion)
				.getEntries().get(0).getOptions();
		assertThat(options).hasSize(2);
		assertThat(options.get(0).getText()).isIn("Marchand", "Mohsin");
		assertThat(options.get(1).getText()).isIn("Marchand", "Mohsin");
	}

	@Test
	public void shouldFindSuggestionsWithWeightsForGivenCriteriaQueryUsingAnnotatedCompletionEntity() {

		loadAnnotatedCompletionObjectEntitiesWithWeights();
		Query query = getSuggestQuery("test-suggest", "suggest", "m");

		SearchHits<AnnotatedCompletionEntity> searchHits = operations.search(query, AnnotatedCompletionEntity.class);

		assertThat(searchHits.hasSuggest()).isTrue();
		Suggest suggest = searchHits.getSuggest();
		// noinspection ConstantConditions
		Suggest.Suggestion<? extends Suggest.Suggestion.Entry<? extends Suggest.Suggestion.Entry.Option>> suggestion = suggest
				.getSuggestion("test-suggest");
		assertThat(suggestion).isNotNull();
		assertThat(suggestion).isInstanceOf(CompletionSuggestion.class);
		// noinspection unchecked
		List<CompletionSuggestion.Entry.Option<AnnotatedCompletionEntity>> options = ((CompletionSuggestion<AnnotatedCompletionEntity>) suggestion)
				.getEntries().get(0).getOptions();

		assertThat(options).hasSize(4);
		for (CompletionSuggestion.Entry.Option<AnnotatedCompletionEntity> option : options) {
			switch (option.getText()) {
				case "Mewes Kochheim1" -> assertThat(option.getScore()).isEqualTo(4);
				case "Mewes Kochheim2" -> assertThat(option.getScore()).isEqualTo(1);
				case "Mewes Kochheim3" -> assertThat(option.getScore()).isEqualTo(2);
				case "Mewes Kochheim4" -> assertThat(option.getScore()).isEqualTo(4444);
				default -> fail("Unexpected option");
			}
		}
	}

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

	@Document(indexName = "#{@indexNameProvider.indexName()}")
	static class CompletionEntity {

		@Nullable
		@Id private String id;

		@Nullable private String name;

		@Nullable
		@CompletionField(maxInputLength = 100) private Completion suggest;

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

	static class CompletionEntityBuilder {

		private final CompletionEntity result;

		public CompletionEntityBuilder(String id) {
			result = new CompletionEntity(id);
		}

		public CompletionEntityBuilder name(String name) {
			result.setName(name);
			return this;
		}

		public CompletionEntityBuilder suggest(String... input) {
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

	@Document(indexName = "#{@indexNameProvider.indexName()}-annotated")
	static class AnnotatedCompletionEntity {

		@Nullable
		@Id private String id;
		@Nullable private String name;
		@Nullable
		@CompletionField(maxInputLength = 100) private Completion suggest;

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

		private final AnnotatedCompletionEntity result;

		public AnnotatedCompletionEntityBuilder(String id) {
			result = new AnnotatedCompletionEntity(id);
		}

		public AnnotatedCompletionEntityBuilder name(String name) {
			result.setName(name);
			return this;
		}

		public AnnotatedCompletionEntityBuilder suggest(String... input) {
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
