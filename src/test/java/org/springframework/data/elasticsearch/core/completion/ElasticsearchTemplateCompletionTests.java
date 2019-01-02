/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.elasticsearch.core.completion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.elasticsearch.search.suggest.SuggestionBuilder;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.elasticsearch.search.suggest.completion.CompletionSuggestionBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.entities.NonDocumentEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;

/**
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Franck Marchand
 * @author Artur Konczak
 * @author Mewes Kochheim
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:elasticsearch-template-test.xml")
public class ElasticsearchTemplateCompletionTests {

	@Autowired
	private ElasticsearchTemplate elasticsearchTemplate;

	private void loadCompletionObjectEntities() {
		elasticsearchTemplate.deleteIndex(CompletionEntity.class);
		elasticsearchTemplate.createIndex(CompletionEntity.class);
		elasticsearchTemplate.refresh(CompletionEntity.class);
		elasticsearchTemplate.putMapping(CompletionEntity.class);

		List<IndexQuery> indexQueries = new ArrayList<>();
		indexQueries.add(new CompletionEntityBuilder("1").name("Rizwan Idrees").suggest(new String[]{"Rizwan Idrees"}).buildIndex());
		indexQueries.add(new CompletionEntityBuilder("2").name("Franck Marchand").suggest(new String[]{"Franck", "Marchand"}).buildIndex());
		indexQueries.add(new CompletionEntityBuilder("3").name("Mohsin Husen").suggest(new String[]{"Mohsin", "Husen"}).buildIndex());
		indexQueries.add(new CompletionEntityBuilder("4").name("Artur Konczak").suggest(new String[]{"Artur", "Konczak"}).buildIndex());

		elasticsearchTemplate.bulkIndex(indexQueries);
		elasticsearchTemplate.refresh(CompletionEntity.class);
	}

	private void loadAnnotatedCompletionObjectEntities() {
		elasticsearchTemplate.deleteIndex(AnnotatedCompletionEntity.class);
		elasticsearchTemplate.createIndex(AnnotatedCompletionEntity.class);
		elasticsearchTemplate.refresh(AnnotatedCompletionEntity.class);
		elasticsearchTemplate.putMapping(AnnotatedCompletionEntity.class);

		NonDocumentEntity nonDocumentEntity = new NonDocumentEntity();
		nonDocumentEntity.setSomeField1("foo");
		nonDocumentEntity.setSomeField2("bar");

		List<IndexQuery> indexQueries = new ArrayList<>();
		indexQueries.add(new AnnotatedCompletionEntityBuilder("1").name("Franck Marchand").suggest(new String[]{"Franck", "Marchand"}).buildIndex());
        indexQueries.add(new AnnotatedCompletionEntityBuilder("2").name("Mohsin Husen").suggest(new String[]{"Mohsin", "Husen"}).buildIndex());
        indexQueries.add(new AnnotatedCompletionEntityBuilder("3").name("Rizwan Idrees").suggest(new String[]{"Rizwan", "Idrees"}).buildIndex());
        indexQueries.add(new AnnotatedCompletionEntityBuilder("4").name("Artur Konczak").suggest(new String[]{"Artur", "Konczak"}).buildIndex());

		elasticsearchTemplate.bulkIndex(indexQueries);
		elasticsearchTemplate.refresh(AnnotatedCompletionEntity.class);
	}

	private void loadAnnotatedCompletionObjectEntitiesWithWeights() {
		elasticsearchTemplate.deleteIndex(AnnotatedCompletionEntity.class);
		elasticsearchTemplate.createIndex(AnnotatedCompletionEntity.class);
		elasticsearchTemplate.putMapping(AnnotatedCompletionEntity.class);
		elasticsearchTemplate.refresh(AnnotatedCompletionEntity.class);

		List<IndexQuery> indexQueries = new ArrayList<>();
		indexQueries.add(new AnnotatedCompletionEntityBuilder("1").name("Mewes Kochheim1").suggest(new String[]{"Mewes Kochheim1"},4).buildIndex());
		indexQueries.add(new AnnotatedCompletionEntityBuilder("2").name("Mewes Kochheim2").suggest(new String[]{"Mewes Kochheim2"}, 1).buildIndex());
		indexQueries.add(new AnnotatedCompletionEntityBuilder("3").name("Mewes Kochheim3").suggest(new String[]{"Mewes Kochheim3"}, 2).buildIndex());
		indexQueries.add(new AnnotatedCompletionEntityBuilder("4").name("Mewes Kochheim4").suggest(new String[]{"Mewes Kochheim4"}, Integer.MAX_VALUE).buildIndex());

		elasticsearchTemplate.bulkIndex(indexQueries);
		elasticsearchTemplate.refresh(AnnotatedCompletionEntity.class);
	}

	@Test
	public void shouldPutMappingForGivenEntity() throws Exception {
		//given
		Class entity = CompletionEntity.class;
		elasticsearchTemplate.createIndex(entity);

		//when
		assertThat(elasticsearchTemplate.putMapping(entity), is(true));
	}

	@Test
	public void shouldFindSuggestionsForGivenCriteriaQueryUsingCompletionEntity() {
		//given
		loadCompletionObjectEntities();

		SuggestionBuilder completionSuggestionFuzzyBuilder = SuggestBuilders.completionSuggestion("suggest").prefix("m", Fuzziness.AUTO);

		//when
		final SearchResponse  suggestResponse = elasticsearchTemplate.suggest(new SuggestBuilder().addSuggestion("test-suggest",completionSuggestionFuzzyBuilder), CompletionEntity.class);
		CompletionSuggestion completionSuggestion = suggestResponse.getSuggest().getSuggestion("test-suggest");
		List<CompletionSuggestion.Entry.Option> options = completionSuggestion.getEntries().get(0).getOptions();

		//then
		assertThat(options.size(), is(2));
		assertThat(options.get(0).getText().string(), isOneOf("Marchand", "Mohsin"));
		assertThat(options.get(1).getText().string(), isOneOf("Marchand", "Mohsin"));
	}

	@Test
	public void shouldFindSuggestionsForGivenCriteriaQueryUsingAnnotatedCompletionEntity() {
		//given
		loadAnnotatedCompletionObjectEntities();
		SuggestionBuilder completionSuggestionFuzzyBuilder = SuggestBuilders.completionSuggestion("suggest").prefix("m", Fuzziness.AUTO);

		//when
		final SearchResponse suggestResponse  = elasticsearchTemplate.suggest(new SuggestBuilder().addSuggestion("test-suggest", completionSuggestionFuzzyBuilder), CompletionEntity.class);
		CompletionSuggestion completionSuggestion = suggestResponse.getSuggest().getSuggestion("test-suggest");
		List<CompletionSuggestion.Entry.Option> options = completionSuggestion.getEntries().get(0).getOptions();

		//then
		assertThat(options.size(), is(2));
		assertThat(options.get(0).getText().string(), isOneOf("Marchand", "Mohsin"));
		assertThat(options.get(1).getText().string(), isOneOf("Marchand", "Mohsin"));
	}

	@Test
	public void shouldFindSuggestionsWithWeightsForGivenCriteriaQueryUsingAnnotatedCompletionEntity() {
		//given
		loadAnnotatedCompletionObjectEntitiesWithWeights();
		SuggestionBuilder completionSuggestionFuzzyBuilder = SuggestBuilders.completionSuggestion("suggest").prefix("m", Fuzziness.AUTO);

		//when
		final SearchResponse  suggestResponse = elasticsearchTemplate.suggest(new SuggestBuilder().addSuggestion("test-suggest",completionSuggestionFuzzyBuilder), AnnotatedCompletionEntity.class);
		CompletionSuggestion completionSuggestion = suggestResponse.getSuggest().getSuggestion("test-suggest");
		List<CompletionSuggestion.Entry.Option> options = completionSuggestion.getEntries().get(0).getOptions();

		//then
		assertThat(options.size(), is(4));
		for (CompletionSuggestion.Entry.Option option : options) {
			if (option.getText().string().equals("Mewes Kochheim1")) {
				assertEquals(4, option.getScore(), 0);
			} else if (option.getText().string().equals("Mewes Kochheim2")) {
				assertEquals(1, option.getScore(), 0);
			} else if (option.getText().string().equals("Mewes Kochheim3")) {
				assertEquals(2, option.getScore(), 0);
			} else if (option.getText().string().equals("Mewes Kochheim4")) {
				assertEquals(Integer.MAX_VALUE, option.getScore(), 0);
			} else {
				fail("Unexpected option");
			}
		}
	}
}
