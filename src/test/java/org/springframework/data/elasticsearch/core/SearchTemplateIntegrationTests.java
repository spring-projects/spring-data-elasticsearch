/*
 * Copyright 2022 the original author or authors.
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
 */
@SpringIntegrationTest
public abstract class SearchTemplateIntegrationTests {

	private static final String SCRIPT = """
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
	private Script script = Script.builder() //
			.withId("testScript") //
			.withLanguage("mustache") //
			.withSource(SCRIPT) //
			.build();

	@Autowired ElasticsearchOperations operations;
	@Autowired IndexNameProvider indexNameProvider;
	@Nullable IndexOperations indexOperations;

	@BeforeEach
	void setUp() {
		indexNameProvider.increment();
		indexOperations = operations.indexOps(Person.class);
		indexOperations.createWithMapping();
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

		var success = operations.putScript(script);
		assertThat(success).isTrue();

		var savedScript = operations.getScript(script.id());
		assertThat(savedScript).isNotNull();
		assertThat(savedScript.id()).isEqualTo(script.id());
		assertThat(savedScript.language()).isEqualTo(script.language());
		assertEquals(savedScript.source(), script.source(), false);

		success = operations.deleteScript(script.id());
		assertThat(success).isTrue();

		savedScript = operations.getScript(script.id());
		assertThat(savedScript).isNull();

		assertThatThrownBy(() -> operations.deleteScript(script.id())) //
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test // #1891
	@DisplayName("should search with template")
	void shouldSearchWithTemplate() {

		var success = operations.putScript(script);
		assertThat(success).isTrue();

		operations.save( //
				new Person("1", "John", "Smith"), //
				new Person("2", "Willy", "Smith"), //
				new Person("3", "John", "Myers"));
		var query = SearchTemplateQuery.builder() //
				.withId(script.id()) //
				.withParams(Map.of("firstName", "John")) //
				.build();

		var searchHits = operations.search(query, Person.class);

		assertThat(searchHits.getTotalHits()).isEqualTo(2);

		success = operations.deleteScript(script.id());
		assertThat(success).isTrue();
	}

	@Document(indexName = "#{@indexNameProvider.indexName()}")
	record Person( //
			@Nullable @Id String id, //
			@Field(type = FieldType.Text) String firstName, //
			@Field(type = FieldType.Text) String lastName //
	) {
	}
}
