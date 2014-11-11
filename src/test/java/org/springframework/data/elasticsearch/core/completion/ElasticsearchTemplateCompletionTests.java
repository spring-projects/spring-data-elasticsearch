/*
 * Copyright 2013 the original author or authors.
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

import org.elasticsearch.action.suggest.SuggestResponse;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.elasticsearch.search.suggest.completion.CompletionSuggestionFuzzyBuilder;
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
import static org.hamcrest.Matchers.is;
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
	elasticsearchTemplate.refresh(CompletionEntity.class, true);
	elasticsearchTemplate.putMapping(CompletionEntity.class);

	List<IndexQuery> indexQueries = new ArrayList<IndexQuery>();
	indexQueries.add(new CompletionEntityBuilder("1").name("Rizwan Idrees").suggest(new String[]{"Rizwan Idrees"}).buildIndex());
	indexQueries.add(new CompletionEntityBuilder("2").name("Franck Marchand").suggest(new String[]{"Franck", "Marchand"}).buildIndex());
	indexQueries.add(new CompletionEntityBuilder("3").name("Mohsin Husen").suggest(new String[]{"Mohsin", "Husen"}, "Mohsin Husen").buildIndex());
	indexQueries.add(new CompletionEntityBuilder("4").name("Artur Konczak").suggest(new String[]{"Artur", "Konczak"}, "Artur Konczak", null, 60).buildIndex());

	elasticsearchTemplate.bulkIndex(indexQueries);
	elasticsearchTemplate.refresh(CompletionEntity.class, true);
    }

    private void loadAnnotatedCompletionObjectEntities() {
	elasticsearchTemplate.deleteIndex(AnnotatedCompletionEntity.class);
	elasticsearchTemplate.createIndex(AnnotatedCompletionEntity.class);
	elasticsearchTemplate.refresh(AnnotatedCompletionEntity.class, true);
	elasticsearchTemplate.putMapping(AnnotatedCompletionEntity.class);

	NonDocumentEntity nonDocumentEntity = new NonDocumentEntity();
	nonDocumentEntity.setSomeField1("foo");
	nonDocumentEntity.setSomeField2("bar");

	List<IndexQuery> indexQueries = new ArrayList<IndexQuery>();
	indexQueries.add(new AnnotatedCompletionEntityBuilder("1").name("Franck Marchand").suggest(new String[]{"Franck", "Marchand"}).buildIndex());
	indexQueries.add(new AnnotatedCompletionEntityBuilder("2").name("Mohsin Husen").suggest(new String[]{"Mohsin", "Husen"}, "Mohsin Husen").buildIndex());
	indexQueries.add(new AnnotatedCompletionEntityBuilder("3").name("Rizwan Idrees").suggest(new String[]{"Rizwan", "Idrees"}, "Rizwan Idrees", "Payload test").buildIndex());
	indexQueries.add(new AnnotatedCompletionEntityBuilder("4").name("Artur Konczak").suggest(new String[]{"Artur", "Konczak"}, "Artur Konczak", nonDocumentEntity, 60).buildIndex());

	elasticsearchTemplate.bulkIndex(indexQueries);
	elasticsearchTemplate.refresh(AnnotatedCompletionEntity.class, true);
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
    public void shouldFindSuggestionsForGivenCriteriaQueryUsingCompletionObjectEntity() {
	//given
	loadCompletionObjectEntities();
	CompletionSuggestionFuzzyBuilder completionSuggestionFuzzyBuilder = new CompletionSuggestionFuzzyBuilder("test-suggest")
		.text("m")
		.field("suggest");

	//when
	SuggestResponse suggestResponse = elasticsearchTemplate.suggest(completionSuggestionFuzzyBuilder, CompletionEntity.class);
	CompletionSuggestion completionSuggestion = suggestResponse.getSuggest().getSuggestion("test-suggest");
	List<CompletionSuggestion.Entry.Option> options = completionSuggestion.getEntries().get(0).getOptions();

	//then
	assertThat(options.size(), is(2));
	assertEquals("Marchand", options.get(0).getText().string());
    }

    @Test
    public void shouldFindSuggestionsForGivenCriteriaQueryUsingloadAnnotatedCompletionObjectEntity() {
	//given
	loadCompletionObjectEntities();
	CompletionSuggestionFuzzyBuilder completionSuggestionFuzzyBuilder = new CompletionSuggestionFuzzyBuilder("test-suggest")
		.text("m")
		.field("suggest");

	//when
	SuggestResponse suggestResponse = elasticsearchTemplate.suggest(completionSuggestionFuzzyBuilder, CompletionEntity.class);
	CompletionSuggestion completionSuggestion = suggestResponse.getSuggest().getSuggestion("test-suggest");
	List<CompletionSuggestion.Entry.Option> options = completionSuggestion.getEntries().get(0).getOptions();

	//then
	assertThat(options.size(), is(2));
	assertEquals("Marchand", options.get(0).getText().string());
    }
}
