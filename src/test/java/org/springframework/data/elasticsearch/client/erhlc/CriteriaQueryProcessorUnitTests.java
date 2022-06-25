/*
 * Copyright 2020-2022 the original author or authors.
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
package org.springframework.data.elasticsearch.client.erhlc;

import static org.skyscreamer.jsonassert.JSONAssert.*;

import org.json.JSONException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.elasticsearch.core.query.Criteria;

/**
 * @author Peter-Josef Meisch
 */
@SuppressWarnings("ConstantConditions")
class CriteriaQueryProcessorUnitTests {

	private final CriteriaQueryProcessor queryProcessor = new CriteriaQueryProcessor();

	@Test // DATAES-706
	void shouldProcessTwoCriteriaWithAnd() throws JSONException {

		String expected = "{\n" + //
				"  \"bool\": {\n" + //
				"    \"must\": [\n" + //
				"      {\n" + //
				"        \"query_string\": {\n" + //
				"          \"query\": \"value1\",\n" + //
				"          \"fields\": [\n" + //
				"            \"field1^1.0\"\n" + //
				"          ]\n" + //
				"        }\n" + //
				"      },\n" + //
				"      {\n" + //
				"        \"query_string\": {\n" + //
				"          \"query\": \"value2\",\n" + //
				"          \"fields\": [\n" + //
				"            \"field2^1.0\"\n" + //
				"          ]\n" + //
				"        }\n" + //
				"      }\n" + //
				"    ]\n" + //
				"  }\n" + //
				"}"; //

		Criteria criteria = new Criteria("field1").is("value1").and("field2").is("value2");

		String query = queryProcessor.createQuery(criteria).toString();

		assertEquals(expected, query, false);
	}

	@Test // DATAES-706
	void shouldProcessTwoCriteriaWithOr() throws JSONException {

		String expected = "{\n" + //
				"  \"bool\": {\n" + //
				"    \"should\": [\n" + //
				"      {\n" + //
				"        \"query_string\": {\n" + //
				"          \"query\": \"value1\",\n" + //
				"          \"fields\": [\n" + //
				"            \"field1^1.0\"\n" + //
				"          ]\n" + //
				"        }\n" + //
				"      },\n" + //
				"      {\n" + //
				"        \"query_string\": {\n" + //
				"          \"query\": \"value2\",\n" + //
				"          \"fields\": [\n" + //
				"            \"field2^1.0\"\n" + //
				"          ]\n" + //
				"        }\n" + //
				"      }\n" + //
				"    ]\n" + //
				"  }\n" + //
				"}"; //

		Criteria criteria = new Criteria("field1").is("value1").or("field2").is("value2");

		String query = queryProcessor.createQuery(criteria).toString();

		assertEquals(expected, query, false);
	}

	@Test // DATAES-706
	void shouldProcessMixedCriteriaWithOrAnd() throws JSONException {

		String expected = "{\n" + //
				"  \"bool\": {\n" + //
				"    \"must\": [\n" + //
				"      {\n" + //
				"        \"query_string\": {\n" + //
				"          \"query\": \"value1\",\n" + //
				"          \"fields\": [\n" + //
				"            \"field1^1.0\"\n" + //
				"          ]\n" + //
				"        }\n" + //
				"      },\n" + //
				"      {\n" + //
				"        \"query_string\": {\n" + //
				"          \"query\": \"value3\",\n" + //
				"          \"fields\": [\n" + //
				"            \"field3^1.0\"\n" + //
				"          ]\n" + //
				"        }\n" + //
				"      }\n" + //
				"    ],\n" + //
				"    \"should\": [\n" + //
				"      {\n" + //
				"        \"query_string\": {\n" + //
				"          \"query\": \"value2\",\n" + //
				"          \"fields\": [\n" + //
				"            \"field2^1.0\"\n" + //
				"          ]\n" + //
				"        }\n" + //
				"      },\n" + //
				"      {\n" + //
				"        \"query_string\": {\n" + //
				"          \"query\": \"value4\",\n" + //
				"          \"fields\": [\n" + //
				"            \"field4^1.0\"\n" + //
				"          ]\n" + //
				"        }\n" + //
				"      }\n" + //
				"    ]\n" + //
				"  }\n" + //
				"}\n"; //

		Criteria criteria = new Criteria("field1").is("value1") //
				.or("field2").is("value2") //
				.and("field3").is("value3") //
				.or("field4").is("value4"); //

		String query = queryProcessor.createQuery(criteria).toString();

		assertEquals(expected, query, false);
	}

	@Test // DATAES-706
	void shouldAddSubQuery() throws JSONException {

		String expected = "{\n" + //
				"  \"bool\": {\n" + //
				"    \"must\": [\n" + //
				"      {\n" + //
				"        \"query_string\": {\n" + //
				"          \"query\": \"Miller\",\n" + //
				"          \"fields\": [\n" + //
				"            \"lastName^1.0\"\n" + //
				"          ]\n" + //
				"        }\n" + //
				"      },\n" + //
				"      {\n" + //
				"        \"bool\": {\n" + //
				"          \"should\": [\n" + //
				"            {\n" + //
				"              \"query_string\": {\n" + //
				"                \"query\": \"John\",\n" + //
				"                \"fields\": [\n" + //
				"                  \"firstName^1.0\"\n" + //
				"                ]\n" + //
				"              }\n" + //
				"            },\n" + //
				"            {\n" + //
				"              \"query_string\": {\n" + //
				"                \"query\": \"Jack\",\n" + //
				"                \"fields\": [\n" + //
				"                  \"firstName^1.0\"\n" + //
				"                ]\n" + //
				"              }\n" + //
				"            }\n" + //
				"          ]\n" + //
				"        }\n" + //
				"      }\n" + //
				"    ]\n" + //
				"  }\n" + //
				"}"; //

		Criteria criteria = new Criteria("lastName").is("Miller")
				.subCriteria(new Criteria().or("firstName").is("John").or("firstName").is("Jack"));

		String query = queryProcessor.createQuery(criteria).toString();

		assertEquals(expected, query, false);
	}

	@Test // DATAES-706
	void shouldProcessNestedSubCriteria() throws JSONException {

		String expected = "{\n" + //
				"  \"bool\": {\n" + //
				"    \"should\": [\n" + //
				"      {\n" + //
				"        \"bool\": {\n" + //
				"          \"must\": [\n" + //
				"            {\n" + //
				"              \"query_string\": {\n" + //
				"                \"query\": \"Miller\",\n" + //
				"                \"fields\": [\n" + //
				"                  \"lastName^1.0\"\n" + //
				"                ]\n" + //
				"              }\n" + //
				"            },\n" + //
				"            {\n" + //
				"              \"bool\": {\n" + //
				"                \"should\": [\n" + //
				"                  {\n" + //
				"                    \"query_string\": {\n" + //
				"                      \"query\": \"Jack\",\n" + //
				"                      \"fields\": [\n" + //
				"                        \"firstName^1.0\"\n" + //
				"                      ]\n" + //
				"                    }\n" + //
				"                  },\n" + //
				"                  {\n" + //
				"                    \"query_string\": {\n" + //
				"                      \"query\": \"John\",\n" + //
				"                      \"fields\": [\n" + //
				"                        \"firstName^1.0\"\n" + //
				"                      ]\n" + //
				"                    }\n" + //
				"                  }\n" + //
				"                ]\n" + //
				"              }\n" + //
				"            }\n" + //
				"          ]\n" + //
				"        }\n" + //
				"      },\n" + //
				"      {\n" + //
				"        \"bool\": {\n" + //
				"          \"must\": [\n" + //
				"            {\n" + //
				"              \"query_string\": {\n" + //
				"                \"query\": \"Smith\",\n" + //
				"                \"fields\": [\n" + //
				"                  \"lastName^1.0\"\n" + //
				"                ]\n" + //
				"              }\n" + //
				"            },\n" + //
				"            {\n" + //
				"              \"bool\": {\n" + //
				"                \"should\": [\n" + //
				"                  {\n" + //
				"                    \"query_string\": {\n" + //
				"                      \"query\": \"Emma\",\n" + //
				"                      \"fields\": [\n" + //
				"                        \"firstName^1.0\"\n" + //
				"                      ]\n" + //
				"                    }\n" + //
				"                  },\n" + //
				"                  {\n" + //
				"                    \"query_string\": {\n" + //
				"                      \"query\": \"Lucy\",\n" + //
				"                      \"fields\": [\n" + //
				"                        \"firstName^1.0\"\n" + //
				"                      ]\n" + //
				"                    }\n" + //
				"                  }\n" + //
				"                ]\n" + //
				"              }\n" + //
				"            }\n" + //
				"          ]\n" + //
				"        }\n" + //
				"      }\n" + //
				"    ]\n" + //
				"  }\n" + //
				"}"; //

		Criteria criteria = Criteria.or()
				.subCriteria(new Criteria("lastName").is("Miller")
						.subCriteria(new Criteria().or("firstName").is("John").or("firstName").is("Jack")))
				.subCriteria(new Criteria("lastName").is("Smith")
						.subCriteria(new Criteria().or("firstName").is("Emma").or("firstName").is("Lucy")));

		String query = queryProcessor.createQuery(criteria).toString();

		assertEquals(expected, query, false);
	}

	@Test // DATAES-706
	void shouldBuildMatchQuery() throws JSONException {

		String expected = "{\n" + //
				"  \"bool\" : {\n" + //
				"    \"must\" : [\n" + //
				"      {\n" + //
				"        \"match\" : {\n" + //
				"          \"field1\" : {\n" + //
				"            \"query\" : \"value1 value2\",\n" + //
				"            \"operator\" : \"OR\"\n" + //
				"          }\n" + //
				"        }\n" + //
				"      }\n" + //
				"    ]\n" + //
				"  }\n" + //
				"}\n"; //

		Criteria criteria = new Criteria("field1").matches("value1 value2");

		String query = queryProcessor.createQuery(criteria).toString();

		assertEquals(expected, query, false);
	}

	@Test // DATAES-706
	void shouldBuildMatchAllQuery() throws JSONException {

		String expected = "{\n" + //
				"  \"bool\" : {\n" + //
				"    \"must\" : [\n" + //
				"      {\n" + //
				"        \"match\" : {\n" + //
				"          \"field1\" : {\n" + //
				"            \"query\" : \"value1 value2\",\n" + //
				"            \"operator\" : \"AND\"\n" + //
				"          }\n" + //
				"        }\n" + //
				"      }\n" + //
				"    ]\n" + //
				"  }\n" + //
				"}\n"; //

		Criteria criteria = new Criteria("field1").matchesAll("value1 value2");

		String query = queryProcessor.createQuery(criteria).toString();

		assertEquals(expected, query, false);
	}

	@Test // #1753
	@DisplayName("should build nested query")
	void shouldBuildNestedQuery() throws JSONException {

		String expected = "{\n" + //
				"  \"bool\" : {\n" + //
				"    \"must\" : [\n" + //
				"      {\n" + //
				"        \"nested\" : {\n" + //
				"          \"query\" : {\n" + //
				"            \"query_string\" : {\n" + //
				"              \"query\" : \"murphy\",\n" + //
				"              \"fields\" : [\n" + //
				"                \"houses.inhabitants.lastName^1.0\"\n" + //
				"              ]\n" + //
				"            }\n" + //
				"          },\n" + //
				"          \"path\" : \"houses.inhabitants\"\n" + //
				"        }\n" + //
				"      }\n" + //
				"    ]\n" + //
				"  }\n" + //
				"}"; //

		Criteria criteria = new Criteria("houses.inhabitants.lastName").is("murphy");
		criteria.getField().setPath("houses.inhabitants");

		String query = queryProcessor.createQuery(criteria).toString();

		assertEquals(expected, query, false);
	}

	@Test // #1909
	@DisplayName("should build query for empty property")
	void shouldBuildQueryForEmptyProperty() throws JSONException {

		String expected = "{\n" + //
				"  \"bool\" : {\n" + //
				"    \"must\" : [\n" + //
				"      {\n" + //
				"        \"bool\" : {\n" + //
				"          \"must\" : [\n" + //
				"            {\n" + //
				"              \"exists\" : {\n" + //
				"                \"field\" : \"lastName\"" + //
				"              }\n" + //
				"            }\n" + //
				"          ],\n" + //
				"          \"must_not\" : [\n" + //
				"            {\n" + //
				"              \"wildcard\" : {\n" + //
				"                \"lastName\" : {\n" + //
				"                  \"wildcard\" : \"*\"" + //
				"                }\n" + //
				"              }\n" + //
				"            }\n" + //
				"          ]\n" + //
				"        }\n" + //
				"      }\n" + //
				"    ]\n" + //
				"  }\n" + //
				"}"; //

		Criteria criteria = new Criteria("lastName").empty();

		String query = queryProcessor.createQuery(criteria).toString();

		assertEquals(expected, query, false);
	}

	@Test // #1909
	@DisplayName("should build query for non-empty property")
	void shouldBuildQueryForNonEmptyProperty() throws JSONException {

		String expected = "{\n" + //
				"  \"bool\" : {\n" + //
				"    \"must\" : [\n" + //
				"      {\n" + //
				"        \"wildcard\" : {\n" + //
				"          \"lastName\" : {\n" + //
				"            \"wildcard\" : \"*\"\n" + //
				"          }\n" + //
				"        }\n" + //
				"      }\n" + //
				"    ]\n" + //
				"  }\n" + //
				"}\n"; //

		Criteria criteria = new Criteria("lastName").notEmpty();

		String query = queryProcessor.createQuery(criteria).toString();

		assertEquals(expected, query, false);
	}
}
