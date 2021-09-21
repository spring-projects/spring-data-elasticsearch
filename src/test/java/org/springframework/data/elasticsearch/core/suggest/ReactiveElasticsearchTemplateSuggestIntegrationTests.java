/*
 * Copyright 2021 the original author or authors.
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

import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.CompletionField;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.suggest.response.CompletionSuggestion;
import org.springframework.data.elasticsearch.core.suggest.response.Suggest;
import org.springframework.data.elasticsearch.junit.jupiter.ReactiveElasticsearchRestTemplateConfiguration;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.lang.Nullable;

/**
 * @author Peter-Josef Meisch
 */
@SuppressWarnings("SpringJavaAutowiredMembersInspection")
@SpringIntegrationTest
public class ReactiveElasticsearchTemplateSuggestIntegrationTests {
	@Configuration
	@Import({ ReactiveElasticsearchRestTemplateConfiguration.class })
	static class Config {
		@Bean
		IndexNameProvider indexNameProvider() {
			return new IndexNameProvider("reactive-template-suggest");
		}
	}

	@Autowired private ReactiveElasticsearchOperations operations;

	@Autowired private IndexNameProvider indexNameProvider;

	// region Setup
	@BeforeEach
	public void beforeEach() {

		indexNameProvider.increment();
		operations.indexOps(CompletionEntity.class).createWithMapping().block();
	}

	@Test // #1302
	@DisplayName("should do some test")
	void shouldDoSomeTest() {

		assertThat(operations).isNotNull();
	}

	@Test // #1302
	@DisplayName("should find suggestions for given prefix completion")
	void shouldFindSuggestionsForGivenPrefixCompletion() {

		loadCompletionObjectEntities();

		NativeSearchQuery query = new NativeSearchQueryBuilder().withSuggestBuilder(new SuggestBuilder()
				.addSuggestion("test-suggest", SuggestBuilders.completionSuggestion("suggest").prefix("m", Fuzziness.AUTO)))
				.build();

		operations.suggest(query, CompletionEntity.class) //
				.as(StepVerifier::create) //
				.assertNext(suggest -> {
					Suggest.Suggestion<? extends Suggest.Suggestion.Entry<? extends Suggest.Suggestion.Entry.Option>> suggestion = suggest
							.getSuggestion("test-suggest");
					assertThat(suggestion).isNotNull();
					assertThat(suggestion).isInstanceOf(CompletionSuggestion.class);
					// noinspection unchecked
					List<CompletionSuggestion.Entry.Option<ElasticsearchTemplateCompletionTests.AnnotatedCompletionEntity>> options = ((CompletionSuggestion<ElasticsearchTemplateCompletionTests.AnnotatedCompletionEntity>) suggestion)
							.getEntries().get(0).getOptions();
					assertThat(options).hasSize(2);
					assertThat(options.get(0).getText()).isIn("Marchand", "Mohsin");
					assertThat(options.get(1).getText()).isIn("Marchand", "Mohsin");

				}) //
				.verifyComplete();
	}

	// region helper functions
	private void loadCompletionObjectEntities() {

		CompletionEntity rizwan_idrees = new CompletionEntityBuilder("1").name("Rizwan Idrees")
				.suggest(new String[] { "Rizwan Idrees" }).build();
		CompletionEntity franck_marchand = new CompletionEntityBuilder("2").name("Franck Marchand")
				.suggest(new String[] { "Franck", "Marchand" }).build();
		CompletionEntity mohsin_husen = new CompletionEntityBuilder("3").name("Mohsin Husen")
				.suggest(new String[] { "Mohsin", "Husen" }).build();
		CompletionEntity artur_konczak = new CompletionEntityBuilder("4").name("Artur Konczak")
				.suggest(new String[] { "Artur", "Konczak" }).build();
		List<CompletionEntity> entities = new ArrayList<>(
				Arrays.asList(rizwan_idrees, franck_marchand, mohsin_husen, artur_konczak));
		IndexCoordinates index = IndexCoordinates.of(indexNameProvider.indexName());
		operations.saveAll(entities, index).blockLast();
	}
	// endregion

	// region Entities
	@Document(indexName = "#{@indexNameProvider.indexName()}")
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
	// endregion
}
