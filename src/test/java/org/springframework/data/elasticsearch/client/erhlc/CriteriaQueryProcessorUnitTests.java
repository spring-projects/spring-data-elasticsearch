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

		String expected = """
				{
				  "bool": {
				    "must": [
				      {
				        "query_string": {
				          "query": "value1",
				          "fields": [
				            "field1^1.0"
				          ]
				        }
				      },
				      {
				        "query_string": {
				          "query": "value2",
				          "fields": [
				            "field2^1.0"
				          ]
				        }
				      }
				    ]
				  }
				}"""; //

		Criteria criteria = new Criteria("field1").is("value1").and("field2").is("value2");

		String query = queryProcessor.createQuery(criteria).toString();

		assertEquals(expected, query, false);
	}

	@Test // DATAES-706
	void shouldProcessTwoCriteriaWithOr() throws JSONException {

		String expected = """
				{
				  "bool": {
				    "should": [
				      {
				        "query_string": {
				          "query": "value1",
				          "fields": [
				            "field1^1.0"
				          ]
				        }
				      },
				      {
				        "query_string": {
				          "query": "value2",
				          "fields": [
				            "field2^1.0"
				          ]
				        }
				      }
				    ]
				  }
				}"""; //

		Criteria criteria = new Criteria("field1").is("value1").or("field2").is("value2");

		String query = queryProcessor.createQuery(criteria).toString();

		assertEquals(expected, query, false);
	}

	@Test // DATAES-706
	void shouldProcessMixedCriteriaWithOrAnd() throws JSONException {

		String expected = """
				{
				  "bool": {
				    "must": [
				      {
				        "query_string": {
				          "query": "value1",
				          "fields": [
				            "field1^1.0"
				          ]
				        }
				      },
				      {
				        "query_string": {
				          "query": "value3",
				          "fields": [
				            "field3^1.0"
				          ]
				        }
				      }
				    ],
				    "should": [
				      {
				        "query_string": {
				          "query": "value2",
				          "fields": [
				            "field2^1.0"
				          ]
				        }
				      },
				      {
				        "query_string": {
				          "query": "value4",
				          "fields": [
				            "field4^1.0"
				          ]
				        }
				      }
				    ]
				  }
				}
				"""; //

		Criteria criteria = new Criteria("field1").is("value1") //
				.or("field2").is("value2") //
				.and("field3").is("value3") //
				.or("field4").is("value4"); //

		String query = queryProcessor.createQuery(criteria).toString();

		assertEquals(expected, query, false);
	}

	@Test // DATAES-706
	void shouldAddSubQuery() throws JSONException {

		String expected = """
				{
				  "bool": {
				    "must": [
				      {
				        "query_string": {
				          "query": "Miller",
				          "fields": [
				            "lastName^1.0"
				          ]
				        }
				      },
				      {
				        "bool": {
				          "should": [
				            {
				              "query_string": {
				                "query": "John",
				                "fields": [
				                  "firstName^1.0"
				                ]
				              }
				            },
				            {
				              "query_string": {
				                "query": "Jack",
				                "fields": [
				                  "firstName^1.0"
				                ]
				              }
				            }
				          ]
				        }
				      }
				    ]
				  }
				}"""; //

		Criteria criteria = new Criteria("lastName").is("Miller")
				.subCriteria(new Criteria().or("firstName").is("John").or("firstName").is("Jack"));

		String query = queryProcessor.createQuery(criteria).toString();

		assertEquals(expected, query, false);
	}

	@Test // DATAES-706
	void shouldProcessNestedSubCriteria() throws JSONException {

		String expected = """
				{
				  "bool": {
				    "should": [
				      {
				        "bool": {
				          "must": [
				            {
				              "query_string": {
				                "query": "Miller",
				                "fields": [
				                  "lastName^1.0"
				                ]
				              }
				            },
				            {
				              "bool": {
				                "should": [
				                  {
				                    "query_string": {
				                      "query": "Jack",
				                      "fields": [
				                        "firstName^1.0"
				                      ]
				                    }
				                  },
				                  {
				                    "query_string": {
				                      "query": "John",
				                      "fields": [
				                        "firstName^1.0"
				                      ]
				                    }
				                  }
				                ]
				              }
				            }
				          ]
				        }
				      },
				      {
				        "bool": {
				          "must": [
				            {
				              "query_string": {
				                "query": "Smith",
				                "fields": [
				                  "lastName^1.0"
				                ]
				              }
				            },
				            {
				              "bool": {
				                "should": [
				                  {
				                    "query_string": {
				                      "query": "Emma",
				                      "fields": [
				                        "firstName^1.0"
				                      ]
				                    }
				                  },
				                  {
				                    "query_string": {
				                      "query": "Lucy",
				                      "fields": [
				                        "firstName^1.0"
				                      ]
				                    }
				                  }
				                ]
				              }
				            }
				          ]
				        }
				      }
				    ]
				  }
				}"""; //

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

		String expected = """
				{
				  "bool" : {
				    "must" : [
				      {
				        "match" : {
				          "field1" : {
				            "query" : "value1 value2",
				            "operator" : "OR"
				          }
				        }
				      }
				    ]
				  }
				}
				"""; //

		Criteria criteria = new Criteria("field1").matches("value1 value2");

		String query = queryProcessor.createQuery(criteria).toString();

		assertEquals(expected, query, false);
	}

	@Test // DATAES-706
	void shouldBuildMatchAllQuery() throws JSONException {

		String expected = """
				{
				  "bool" : {
				    "must" : [
				      {
				        "match" : {
				          "field1" : {
				            "query" : "value1 value2",
				            "operator" : "AND"
				          }
				        }
				      }
				    ]
				  }
				}
				"""; //

		Criteria criteria = new Criteria("field1").matchesAll("value1 value2");

		String query = queryProcessor.createQuery(criteria).toString();

		assertEquals(expected, query, false);
	}

	@Test // #1753
	@DisplayName("should build nested query")
	void shouldBuildNestedQuery() throws JSONException {

		String expected = """
				{
				  "bool" : {
				    "must" : [
				      {
				        "nested" : {
				          "query" : {
				            "query_string" : {
				              "query" : "murphy",
				              "fields" : [
				                "houses.inhabitants.lastName^1.0"
				              ]
				            }
				          },
				          "path" : "houses.inhabitants"
				        }
				      }
				    ]
				  }
				}"""; //

		Criteria criteria = new Criteria("houses.inhabitants.lastName").is("murphy");
		criteria.getField().setPath("houses.inhabitants");

		String query = queryProcessor.createQuery(criteria).toString();

		assertEquals(expected, query, false);
	}

	@Test // #1909
	@DisplayName("should build query for empty property")
	void shouldBuildQueryForEmptyProperty() throws JSONException {

		String expected = """
				{
				  "bool" : {
				    "must" : [
				      {
				        "bool" : {
				          "must" : [
				            {
				              "exists" : {
				                "field" : "lastName"              }
				            }
				          ],
				          "must_not" : [
				            {
				              "wildcard" : {
				                "lastName" : {
				                  "wildcard" : "*"                }
				              }
				            }
				          ]
				        }
				      }
				    ]
				  }
				}"""; //

		Criteria criteria = new Criteria("lastName").empty();

		String query = queryProcessor.createQuery(criteria).toString();

		assertEquals(expected, query, false);
	}

	@Test // #1909
	@DisplayName("should build query for non-empty property")
	void shouldBuildQueryForNonEmptyProperty() throws JSONException {

		String expected = """
				{
				  "bool" : {
				    "must" : [
				      {
				        "wildcard" : {
				          "lastName" : {
				            "wildcard" : "*"
				          }
				        }
				      }
				    ]
				  }
				}
				"""; //

		Criteria criteria = new Criteria("lastName").notEmpty();

		String query = queryProcessor.createQuery(criteria).toString();

		assertEquals(expected, query, false);
	}
}
