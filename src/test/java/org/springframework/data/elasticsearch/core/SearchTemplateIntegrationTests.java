/*
 * Copyright 2022-2024 the original author or authors.
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
package org.springframework.data.elasticsearch.core;

import static org.assertj.core.api.Assertions.*;
import static org.skyscreamer.jsonassert.JSONAssert.*;

import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.ResourceNotFoundException;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.SearchTemplateQuery;
import org.springframework.data.elasticsearch.core.script.Script;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.lang.Nullable;

/**
 * Integration tests search template API.
 *
 * @author Peter-Josef Meisch
 * @author Haibo Liu
 */
@SpringIntegrationTest
public abstract class SearchTemplateIntegrationTests {

	private static final String SEARCH_FIRSTNAME = """
			{
				"query": {
					"bool": {
						"must": [
							{
								"match": {
									"firstName": "{{firstName}}"
								}
							}
						]
					}
				},
				"from": 0,
				"size": 100
			  }
			""";

	private static final String SEARCH_LASTNAME = """
			{
				"query": {
					"bool": {
						"must": [
							{
								"match": {
									"lastName": "{{lastName}}"
								}
							}
						]
					}
				},
				"from": 0,
				"size": 100
			  }
			""";

	private static final Script SCRIPT_SEARCH_FIRSTNAME = Script.builder() //
			.withId("searchFirstName") //
			.withLanguage("mustache") //
			.withSource(SEARCH_FIRSTNAME) //
			.build();

	private static final Script SCRIPT_SEARCH_LASTNAME = Script.builder() //
			.withId("searchLastName") //
			.withLanguage("mustache") //
			.withSource(SEARCH_LASTNAME) //
			.build();

	@Autowired ElasticsearchOperations operations;
	@Autowired IndexNameProvider indexNameProvider;
	IndexOperations personIndexOperations, studentIndexOperations;

	@BeforeEach
	void setUp() {
		indexNameProvider.increment();
		personIndexOperations = operations.indexOps(Person.class);
		personIndexOperations.createWithMapping();
		studentIndexOperations = operations.indexOps(Student.class);
		studentIndexOperations.createWithMapping();

		operations.save( //
				new Person("1", "John", "Smith"), //
				new Person("2", "Willy", "Smith"), //
				new Person("3", "John", "Myers"));

		operations.save(
				new Student("1", "Joey", "Dunlop"), //
				new Student("2", "Michael", "Dunlop"));
	}

	@Test
	@Order(Integer.MAX_VALUE)
	void cleanup() {
		operations.indexOps(IndexCoordinates.of(indexNameProvider.getPrefix() + '*')).delete();
	}

	@Test // #1891
	@DisplayName("should store, retrieve and delete template script")
	void shouldStoreAndRetrieveAndDeleteTemplateScript() throws JSONException {
		// we do all in this test because scripts aren't stored in an index but in the cluster and we need to clenaup.

		var success = operations.putScript(SCRIPT_SEARCH_FIRSTNAME);
		assertThat(success).isTrue();

		var savedScript = operations.getScript(SCRIPT_SEARCH_FIRSTNAME.id());
		assertThat(savedScript).isNotNull();
		assertThat(savedScript.id()).isEqualTo(SCRIPT_SEARCH_FIRSTNAME.id());
		assertThat(savedScript.language()).isEqualTo(SCRIPT_SEARCH_FIRSTNAME.language());
		assertEquals(savedScript.source(), SCRIPT_SEARCH_FIRSTNAME.source(), false);

		success = operations.deleteScript(SCRIPT_SEARCH_FIRSTNAME.id());
		assertThat(success).isTrue();

		savedScript = operations.getScript(SCRIPT_SEARCH_FIRSTNAME.id());
		assertThat(savedScript).isNull();

		assertThatThrownBy(() -> operations.deleteScript(SCRIPT_SEARCH_FIRSTNAME.id())) //
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test // #1891
	@DisplayName("should search with template")
	void shouldSearchWithTemplate() {
		var success = operations.putScript(SCRIPT_SEARCH_FIRSTNAME);
		assertThat(success).isTrue();

		var query = SearchTemplateQuery.builder() //
				.withId(SCRIPT_SEARCH_FIRSTNAME.id()) //
				.withParams(Map.of("firstName", "John")) //
				.build();

		var searchHits = operations.search(query, Person.class);

		assertThat(searchHits.getTotalHits()).isEqualTo(2);

		success = operations.deleteScript(SCRIPT_SEARCH_FIRSTNAME.id());
		assertThat(success).isTrue();
	}

	@Test // #2704
	@DisplayName("should search with template multisearch")
	void shouldSearchWithTemplateMultiSearch() {
		var success = operations.putScript(SCRIPT_SEARCH_FIRSTNAME);
		assertThat(success).isTrue();

		var q1 = SearchTemplateQuery.builder() //
				.withId(SCRIPT_SEARCH_FIRSTNAME.id()) //
				.withParams(Map.of("firstName", "John")) //
				.build();
		var q2 = SearchTemplateQuery.builder() //
				.withId(SCRIPT_SEARCH_FIRSTNAME.id()) //
				.withParams(Map.of("firstName", "Willy")) //
				.build();

		var multiSearchHits = operations.multiSearch(List.of(q1, q2), Person.class);

		assertThat(multiSearchHits.size()).isEqualTo(2);
		assertThat(multiSearchHits.get(0).getTotalHits()).isEqualTo(2);
		assertThat(multiSearchHits.get(1).getTotalHits()).isEqualTo(1);

		assertThat(multiSearchHits.get(0).getSearchHits())
				.extracting(SearchHit::getContent)
				.extracting(Person::lastName)
				.contains("Smith", "Myers");
		assertThat(multiSearchHits.get(1).getSearchHits())
				.extracting(SearchHit::getContent)
				.extracting(Person::lastName)
				.containsExactly("Smith");

		success = operations.deleteScript(SCRIPT_SEARCH_FIRSTNAME.id());
		assertThat(success).isTrue();
	}

	@Test // #2704
	@DisplayName("should search with template multisearch including different scripts")
	void shouldSearchWithTemplateMultiSearchIncludingDifferentScripts() {
		assertThat(operations.putScript(SCRIPT_SEARCH_FIRSTNAME)).isTrue();
		assertThat(operations.putScript(SCRIPT_SEARCH_LASTNAME)).isTrue();

		var q1 = SearchTemplateQuery.builder() //
				.withId(SCRIPT_SEARCH_FIRSTNAME.id()) //
				.withParams(Map.of("firstName", "John")) //
				.build();
		var q2 = SearchTemplateQuery.builder() //
				.withId(SCRIPT_SEARCH_LASTNAME.id()) //
				.withParams(Map.of("lastName", "smith")) //
				.build();

		var multiSearchHits = operations.multiSearch(List.of(q1, q2), Person.class);

		assertThat(multiSearchHits.size()).isEqualTo(2);
		assertThat(multiSearchHits.get(0).getTotalHits()).isEqualTo(2);
		assertThat(multiSearchHits.get(1).getTotalHits()).isEqualTo(2);

		assertThat(multiSearchHits.get(0).getSearchHits())
				.extracting(SearchHit::getContent)
				.extracting(Person::lastName)
				.contains("Smith", "Myers");
		assertThat(multiSearchHits.get(1).getSearchHits())
				.extracting(SearchHit::getContent)
				.extracting(Person::firstName)
				.contains("John", "Willy");

		assertThat(operations.deleteScript(SCRIPT_SEARCH_FIRSTNAME.id())).isTrue();
		assertThat(operations.deleteScript(SCRIPT_SEARCH_LASTNAME.id())).isTrue();
	}

	@Test // #2704
	@DisplayName("should search with template multisearch with multiple classes")
	void shouldSearchWithTemplateMultiSearchWithMultipleClasses() {
		assertThat(operations.putScript(SCRIPT_SEARCH_FIRSTNAME)).isTrue();
		assertThat(operations.putScript(SCRIPT_SEARCH_LASTNAME)).isTrue();

		var q1 = SearchTemplateQuery.builder() //
				.withId(SCRIPT_SEARCH_FIRSTNAME.id()) //
				.withParams(Map.of("firstName", "John")) //
				.build();
		var q2 = SearchTemplateQuery.builder() //
				.withId(SCRIPT_SEARCH_FIRSTNAME.id()) //
				.withParams(Map.of("firstName", "Joey")) //
				.build();

		// search with multiple classes
		var multiSearchHits = operations.multiSearch(List.of(q1, q2), List.of(Person.class, Student.class));

		assertThat(multiSearchHits.size()).isEqualTo(2);
		assertThat(multiSearchHits.get(0).getTotalHits()).isEqualTo(2);
		assertThat(multiSearchHits.get(1).getTotalHits()).isEqualTo(1);

		assertThat(multiSearchHits.get(0).getSearchHits())
				// type casting is needed here
				.extracting(hits -> (Person) hits.getContent())
				.extracting(Person::lastName)
				.contains("Smith", "Myers");
		assertThat(multiSearchHits.get(1).getSearchHits())
				// type casting is needed here
				.extracting(hits -> (Student) hits.getContent())
				.extracting(Student::lastName)
				.containsExactly("Dunlop");

		assertThat(operations.deleteScript(SCRIPT_SEARCH_FIRSTNAME.id())).isTrue();
		assertThat(operations.deleteScript(SCRIPT_SEARCH_LASTNAME.id())).isTrue();
	}

	@Test // #2704
	@DisplayName("should search with template multisearch with multiple index coordinates")
	void shouldSearchWithTemplateMultiSearchWithMultipleIndexCoordinates() {
		assertThat(operations.putScript(SCRIPT_SEARCH_FIRSTNAME)).isTrue();
		assertThat(operations.putScript(SCRIPT_SEARCH_LASTNAME)).isTrue();

		var q1 = SearchTemplateQuery.builder() //
				.withId(SCRIPT_SEARCH_FIRSTNAME.id()) //
				.withParams(Map.of("firstName", "John")) //
				.build();
		var q2 = SearchTemplateQuery.builder() //
				.withId(SCRIPT_SEARCH_LASTNAME.id()) //
				.withParams(Map.of("lastName", "Dunlop")) //
				.build();

		// search with multiple index coordinates
		var multiSearchHits = operations.multiSearch(
				List.of(q1, q2),
				List.of(Person.class, Student.class),
				List.of(IndexCoordinates.of(indexNameProvider.indexName() + "-person"),
						IndexCoordinates.of(indexNameProvider.indexName() + "-student")));

		assertThat(multiSearchHits.size()).isEqualTo(2);
		assertThat(multiSearchHits.get(0).getTotalHits()).isEqualTo(2);
		assertThat(multiSearchHits.get(1).getTotalHits()).isEqualTo(2);

		assertThat(multiSearchHits.get(0).getSearchHits())
				// type casting is needed here
				.extracting(hits -> (Person) hits.getContent())
				.extracting(Person::lastName)
				.contains("Smith", "Myers");
		assertThat(multiSearchHits.get(1).getSearchHits())
				// type casting is needed here
				.extracting(hits -> (Student) hits.getContent())
				.extracting(Student::firstName)
				.contains("Joey", "Michael");

		assertThat(operations.deleteScript(SCRIPT_SEARCH_FIRSTNAME.id())).isTrue();
		assertThat(operations.deleteScript(SCRIPT_SEARCH_LASTNAME.id())).isTrue();
	}

	@Document(indexName = "#{@indexNameProvider.indexName()}-person")
	record Person( //
			@Nullable @Id String id, //
			@Field(type = FieldType.Text) String firstName, //
			@Field(type = FieldType.Text) String lastName //
	) {
	}

	@Document(indexName = "#{@indexNameProvider.indexName()}-student")
	record Student( //
			@Nullable @Id String id, //
			@Field(type = FieldType.Text) String firstName, //
			@Field(type = FieldType.Text) String lastName //
	) {
	}
}
