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
import static org.springframework.data.elasticsearch.core.IndexOperationsAdapter.*;

import reactor.test.StepVerifier;

import java.util.Arrays;
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
 * Integration tests for the point in time API.
 *
 * @author Peter-Josef Meisch
 * @since 5.1
 */
@SpringIntegrationTest
public abstract class ReactiveSearchTemplateIntegrationTests {
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
	private final Script script = Script.builder() //
			.withId("testScript") //
			.withLanguage("mustache") //
			.withSource(SCRIPT) //
			.build();

	@Autowired ReactiveElasticsearchOperations operations;
	@Autowired IndexNameProvider indexNameProvider;

	@BeforeEach
	void setUp() {
		indexNameProvider.increment();
		blocking(operations.indexOps(Person.class)).createWithMapping();
	}

	@Test
	@Order(Integer.MAX_VALUE)
	void cleanup() {
		blocking(operations.indexOps(IndexCoordinates.of(indexNameProvider.getPrefix() + '*'))).delete();
	}

	@Test // #1891
	@DisplayName("should store, retrieve and delete template script")
	void shouldStoreAndRetrieveAndDeleteTemplateScript() throws JSONException {

		// we do all in this test because scripts aren't stored in an index but in the cluster and we need to clenaup.

		var success = operations.putScript(script).block();
		assertThat(success).isTrue();

		var savedScript = operations.getScript(script.id()).block();
		assertThat(savedScript).isNotNull();
		assertThat(savedScript.id()).isEqualTo(script.id());
		assertThat(savedScript.language()).isEqualTo(script.language());
		assertEquals(savedScript.source(), script.source(), false);

		success = operations.deleteScript(script.id()).block();
		assertThat(success).isTrue();

		savedScript = operations.getScript(script.id()).block();
		assertThat(savedScript).isNull();

		operations.deleteScript(script.id()) //
				.as(StepVerifier::create) //
				.verifyError(ResourceNotFoundException.class);
	}

	@Test // #1891
	@DisplayName("should search with template")
	void shouldSearchWithTemplate() {

		var success = operations.putScript(script).block();
		assertThat(success).isTrue();

		operations.saveAll( //
				Arrays.asList(new Person("1", "John", "Smith"), //
						new Person("2", "Willy", "Smith"), //
						new Person("3", "John", "Myers")), //
				Person.class).blockLast();
		var query = SearchTemplateQuery.builder() //
				.withId(script.id()) //
				.withParams(Map.of("firstName", "John")) //
				.build();

		operations.search(query, Person.class)//
				.as(StepVerifier::create) //
				.expectNextCount(2L) //
				.verifyComplete();

		success = operations.deleteScript(script.id()).block();
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
