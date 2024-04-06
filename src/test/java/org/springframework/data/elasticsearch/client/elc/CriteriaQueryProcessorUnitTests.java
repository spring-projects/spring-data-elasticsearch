/*
 * Copyright 2020-2024 the original author or authors.
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
package org.springframework.data.elasticsearch.client.elc;

import static org.skyscreamer.jsonassert.JSONAssert.*;
import static org.springframework.data.elasticsearch.client.elc.JsonUtils.*;

import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;

import org.json.JSONException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.elasticsearch.core.query.Criteria;

/**
 * @author Peter-Josef Meisch
 * @author Ezequiel Antúnez Camacho
 */
@SuppressWarnings("ConstantConditions")
class CriteriaQueryProcessorUnitTests {

	private final JsonpMapper mapper = new JacksonJsonpMapper();

	private final CriteriaQueryProcessor queryProcessor = new CriteriaQueryProcessor();

	@Test // DATAES-706
	void shouldProcessTwoCriteriaWithAnd() throws JSONException {

		String expected = """
				{
					"bool": {
						"must": [
							{
								"query_string": {
									"fields": [
										"field1"
									],
									"query": "value1"
								}
							},
							{
								"query_string": {
									"fields": [
										"field2"
									],
									"query": "value2"
								}
							}
						]
					}
				}

				"""; //

		Criteria criteria = new Criteria("field1").is("value1").and("field2").is("value2");

		var queryString = queryToJson(CriteriaQueryProcessor.createQuery(criteria), mapper);

		assertEquals(expected, queryString, false);
	}

	@Test // DATAES-706
	void shouldProcessTwoCriteriaWithOr() throws JSONException {

		String expected = """
				{
					"bool": {
						"should": [
							{
								"query_string": {
									"fields": [
										"field1"
									],
									"query": "value1"
								}
							},
							{
								"query_string": {
									"fields": [
										"field2"
									],
									"query": "value2"
								}
							}
						]
					}
				}
				""";

		Criteria criteria = new Criteria("field1").is("value1").or("field2").is("value2");

		var queryString = queryToJson(CriteriaQueryProcessor.createQuery(criteria), mapper);

		assertEquals(expected, queryString, false);
	}

	@Test // DATAES-706
	void shouldProcessMixedCriteriaWithOrAnd() throws JSONException {

		String expected = """
				{
					"bool": {
						"must": [
							{
								"query_string": {
									"fields": [
										"field1"
									],
									"query": "value1"
								}
							},
							{
								"query_string": {
									"fields": [
										"field3"
									],
									"query": "value3"
								}
							}
						],
						"should": [
							{
								"query_string": {
									"fields": [
										"field2"
									],
									"query": "value2"
								}
							},
							{
								"query_string": {
									"fields": [
										"field4"
									],
									"query": "value4"
								}
							}
						]
					}
				}
				""";

		Criteria criteria = new Criteria("field1").is("value1") //
				.or("field2").is("value2") //
				.and("field3").is("value3") //
				.or("field4").is("value4"); //

		var queryString = queryToJson(CriteriaQueryProcessor.createQuery(criteria), mapper);

		assertEquals(expected, queryString, false);
	}

	@Test // DATAES-706
	void shouldAddSubQuery() throws JSONException {

		String expected = """
				{
					"bool": {
						"must": [
							{
								"query_string": {
									"fields": [
										"lastName"
									],
									"query": "Miller"
								}
							},
							{
								"bool": {
									"should": [
										{
											"query_string": {
												"fields": [
													"firstName"
												],
												"query": "John"
											}
										},
										{
											"query_string": {
												"fields": [
													"firstName"
												],
												"query": "Jack"
											}
										}
									]
								}
							}
						]
					}
				}""";

		Criteria criteria = new Criteria("lastName").is("Miller")
				.subCriteria(new Criteria().or("firstName").is("John").or("firstName").is("Jack"));

		var queryString = queryToJson(CriteriaQueryProcessor.createQuery(criteria), mapper);

		assertEquals(expected, queryString, false);
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
												"fields": [
													"lastName"
												],
												"query": "Miller"
											}
										},
										{
											"bool": {
												"should": [
													{
														"query_string": {
															"fields": [
																"firstName"
															],
															"query": "John"
														}
													},
													{
														"query_string": {
															"fields": [
																"firstName"
															],
															"query": "Jack"
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
												"fields": [
													"lastName"
												],
												"query": "Smith"
											}
										},
										{
											"bool": {
												"should": [
													{
														"query_string": {
															"fields": [
																"firstName"
															],
															"query": "Emma"
														}
													},
													{
														"query_string": {
															"fields": [
																"firstName"
															],
															"query": "Lucy"
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
				}
				""";

		Criteria criteria = Criteria.or()
				.subCriteria(new Criteria("lastName").is("Miller")
						.subCriteria(new Criteria().or("firstName").is("John").or("firstName").is("Jack")))
				.subCriteria(new Criteria("lastName").is("Smith")
						.subCriteria(new Criteria().or("firstName").is("Emma").or("firstName").is("Lucy")));

		var queryString = queryToJson(CriteriaQueryProcessor.createQuery(criteria), mapper);

		assertEquals(expected, queryString, false);
	}

	@Test // DATAES-706
	void shouldBuildMatchQuery() throws JSONException {

		String expected = """
				 {
					"bool": {
						"must": [
							{
								"match": {
									"field1": {
										"operator": "or",
										"query": "value1 value2"
									}
								}
							}
						]
					}
				}
				""";

		Criteria criteria = new Criteria("field1").matches("value1 value2");

		var queryString = queryToJson(CriteriaQueryProcessor.createQuery(criteria), mapper);

		assertEquals(expected, queryString, false);
	}

	@Test // DATAES-706
	void shouldBuildMatchAllQuery() throws JSONException {

		String expected = """
				{
					"bool": {
						"must": [
							{
								"match": {
									"field1": {
										"operator": "and",
										"query": "value1 value2"
									}
								}
							}
						]
					}
				}""";

		Criteria criteria = new Criteria("field1").matchesAll("value1 value2");

		var queryString = queryToJson(CriteriaQueryProcessor.createQuery(criteria), mapper);

		assertEquals(expected, queryString, false);
	}

	@Test // #1753
	@DisplayName("should build nested query")
	void shouldBuildNestedQuery() throws JSONException {

		String expected = """
				{
					"bool": {
						"must": [
							{
								"nested": {
									"path": "houses.inhabitants",
									"query": {
										"query_string": {
											"fields": [
												"houses.inhabitants.lastName"
											],
											"query": "murphy"
										}
									}
								}
							}
						]
					}
				}""";

		Criteria criteria = new Criteria("houses.inhabitants.lastName").is("murphy");
		criteria.getField().setPath("houses.inhabitants");

		var queryString = queryToJson(CriteriaQueryProcessor.createQuery(criteria), mapper);

		assertEquals(expected, queryString, false);
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

		var queryString = queryToJson(CriteriaQueryProcessor.createQuery(criteria), mapper);

		assertEquals(expected, queryString, false);
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

		var queryString = queryToJson(CriteriaQueryProcessor.createQuery(criteria), mapper);

		assertEquals(expected, queryString, false);
	}

	@Test // #2418
	void shouldBuildRegexpQuery() throws JSONException {
		String expected = """
				 {
					"bool": {
						"must": [
							{
								"regexp": {
									"field1": {
										"value": "[^abc]"
									}
								}
							}
						]
					}
				}
				""";

		Criteria criteria = new Criteria("field1").regexp("[^abc]");

		var queryString = queryToJson(CriteriaQueryProcessor.createQuery(criteria), mapper);

		assertEquals(expected, queryString, false);
	}

    @Test
    void shouldWrapOrCriteria() throws JSONException {
        // Given
        String expected = """
                 {
                    "bool": {
                       "should": [
                          {
                             "query_string": {
                                "analyze_wildcard": true,
                                "fields": [
                                   "field1"
                                ],
                                "query": "*xyz*"
                             }
                          },
                          {
                             "bool": {
                                "must_not": [
                                   {
                                      "query_string": {
                                         "boost": 1.5,
                                         "default_operator": "and",
                                         "fields": [
                                            "field1"
                                         ],
                                         "query": "abc"
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
                                         "analyze_wildcard": true,
                                         "fields": [
                                            "field2"
                                         ],
                                         "query": "elastic*"
                                      }
                                   }
                                ]
                             }
                          }
                       ]
                    }
                 }
                """;

        Criteria criteria = Criteria.where("field1")
                .contains("xyz")
                .or(
                        Criteria.where("field1")
                                .is("abc").not()
                                .boost(1.5f)
                                .subCriteria(
                                        Criteria.where("field2")
                                                .startsWith("elastic")
                                )
                );

        // Then
        String queryString = queryToJson(CriteriaQueryProcessor.createQuery(criteria), mapper);

        assertEquals(expected, queryString, false);
    }

}
