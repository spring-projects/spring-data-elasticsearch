/*
 * Copyright 2019-2022 the original author or authors.
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

import java.time.LocalDate;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.assertj.core.api.SoftAssertions;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;
import org.springframework.data.elasticsearch.core.convert.GeoConverters;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.geo.GeoJson;
import org.springframework.data.elasticsearch.core.geo.GeoJsonPoint;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilterBuilder;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.SourceFilter;
import org.springframework.lang.Nullable;

/**
 * Tests for the mapping of {@link CriteriaQuery} by a
 * {@link org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter}. In the same package as
 * {@link CriteriaQueryProcessor} as this is needed to get the String representation to assert.
 *
 * @author Peter-Josef Meisch
 * @author Sascha Woo
 * @author vdisk
 */
public class CriteriaQueryMappingUnitTests {

	MappingElasticsearchConverter mappingElasticsearchConverter;

	// region setup
	@BeforeEach
	void setUp() {
		SimpleElasticsearchMappingContext mappingContext = new SimpleElasticsearchMappingContext();
		mappingContext.setInitialEntitySet(Collections.singleton(Person.class));
		mappingContext.afterPropertiesSet();

		mappingElasticsearchConverter = new MappingElasticsearchConverter(mappingContext, new GenericConversionService());
		mappingElasticsearchConverter.afterPropertiesSet();

	}
	// endregion

	// region tests
	@Test // DATAES-716
	void shouldMapNamesAndConvertValuesInCriteriaQuery() throws JSONException {

		// use POJO properties and types in the query building
		CriteriaQuery criteriaQuery = new CriteriaQuery( //
				new Criteria("birthDate") //
						.between(LocalDate.of(1989, 11, 9), LocalDate.of(1990, 11, 9)) //
						.or("birthDate").is(LocalDate.of(2019, 12, 28)) //
		);

		// mapped field name and converted parameter
		String expected = '{' + //
				"  \"bool\" : {" + //
				"    \"should\" : [" + //
				"      {" + //
				"        \"range\" : {" + //
				"          \"birth-date\" : {" + //
				"            \"from\" : \"09.11.1989\"," + //
				"            \"to\" : \"09.11.1990\"," + //
				"            \"include_lower\" : true," + //
				"            \"include_upper\" : true" + //
				"          }" + //
				"        }" + //
				"      }," + //
				"      {" + //
				"        \"query_string\" : {" + //
				"          \"query\" : \"28.12.2019\"," + //
				"          \"fields\" : [" + //
				"            \"birth-date^1.0\"" + //
				"          ]" + //
				"        }" + //
				"      }" + //
				"    ]" + //
				"  }" + //
				'}'; //

		mappingElasticsearchConverter.updateQuery(criteriaQuery, Person.class);
		String queryString = new CriteriaQueryProcessor().createQuery(criteriaQuery.getCriteria()).toString();

		assertEquals(expected, queryString, false);
	}

	@Test // #1668
	void shouldMapNamesAndConvertValuesInCriteriaQueryForSubCriteria() throws JSONException {

		// use POJO properties and types in the query building
		CriteriaQuery criteriaQuery = new CriteriaQuery( //
				Criteria.or().subCriteria(Criteria.where("birthDate") //
						.between(LocalDate.of(1989, 11, 9), LocalDate.of(1990, 11, 9))) //
						.subCriteria(Criteria.where("birthDate").is(LocalDate.of(2019, 12, 28))) //
		);

		// mapped field name and converted parameter
		String expected = """
				{
				  "bool" : {
				    "should" : [
				      {
				        "bool" : {
				          "must" : [
				            {
				              "range" : {
				                "birth-date" : {
				                  "from" : "09.11.1989",
				                  "to" : "09.11.1990",
				                  "include_lower" : true,
				                  "include_upper" : true,
				                  "boost" : 1.0
				                }
				              }
				            }
				          ],
				          "adjust_pure_negative" : true,
				          "boost" : 1.0
				        }
				      },
				      {
				        "bool" : {
				          "must" : [
				            {
				              "query_string" : {
				                "query" : "28.12.2019",
				                "fields" : [
				                  "birth-date^1.0"
				                ],
				                "type" : "best_fields",
				                "default_operator" : "and",
				                "max_determinized_states" : 10000,
				                "enable_position_increments" : true,
				                "fuzziness" : "AUTO",
				                "fuzzy_prefix_length" : 0,
				                "fuzzy_max_expansions" : 50,
				                "phrase_slop" : 0,
				                "escape" : false,
				                "auto_generate_synonyms_phrase_query" : true,
				                "fuzzy_transpositions" : true,
				                "boost" : 1.0
				              }
				            }
				          ],
				          "adjust_pure_negative" : true,
				          "boost" : 1.0
				        }
				      }
				    ],
				    "adjust_pure_negative" : true,
				    "boost" : 1.0
				  }
				}"""; //

		mappingElasticsearchConverter.updateQuery(criteriaQuery, Person.class);
		String queryString = new CriteriaQueryProcessor().createQuery(criteriaQuery.getCriteria()).toString();

		assertEquals(expected, queryString, false);
	}

	@Test // #1668
	void shouldMapNamesAndConvertValuesInCriteriaQueryForSubCriteriaWithDate() throws JSONException {
		// use POJO properties and types in the query building
		CriteriaQuery criteriaQuery = new CriteriaQuery( //
				Criteria.or().subCriteria(Criteria.where("birthDate") //
						.between(LocalDate.of(1989, 11, 9), LocalDate.of(1990, 11, 9))) //
						.subCriteria(Criteria.where("createdDate").is(new Date(383745721653L))) //
		);

		// mapped field name and converted parameter
		String expected = """
				{
				  "bool" : {
				    "should" : [
				      {
				        "bool" : {
				          "must" : [
				            {
				              "range" : {
				                "birth-date" : {
				                  "from" : "09.11.1989",
				                  "to" : "09.11.1990",
				                  "include_lower" : true,
				                  "include_upper" : true,
				                  "boost" : 1.0
				                }
				              }
				            }
				          ],
				          "adjust_pure_negative" : true,
				          "boost" : 1.0
				        }
				      },
				      {
				        "bool" : {
				          "must" : [
				            {
				              "query_string" : {
				                "query" : "383745721653",
				                "fields" : [
				                  "created-date^1.0"
				                ],
				                "type" : "best_fields",
				                "default_operator" : "and",
				                "max_determinized_states" : 10000,
				                "enable_position_increments" : true,
				                "fuzziness" : "AUTO",
				                "fuzzy_prefix_length" : 0,
				                "fuzzy_max_expansions" : 50,
				                "phrase_slop" : 0,
				                "escape" : false,
				                "auto_generate_synonyms_phrase_query" : true,
				                "fuzzy_transpositions" : true,
				                "boost" : 1.0
				              }
				            }
				          ],
				          "adjust_pure_negative" : true,
				          "boost" : 1.0
				        }
				      }
				    ],
				    "adjust_pure_negative" : true,
				    "boost" : 1.0
				  }
				}"""; //

		mappingElasticsearchConverter.updateQuery(criteriaQuery, Person.class);
		String queryString = new CriteriaQueryProcessor().createQuery(criteriaQuery.getCriteria()).toString();

		assertEquals(expected, queryString, false);
	}

	@Test // DATAES-706
	void shouldMapNamesAndValuesInSubCriteriaQuery() throws JSONException {

		CriteriaQuery criteriaQuery = new CriteriaQuery( //
				new Criteria("firstName").matches("John") //
						.subCriteria(new Criteria("birthDate") //
								.between(LocalDate.of(1989, 11, 9), LocalDate.of(1990, 11, 9)) //
								.or("birthDate").is(LocalDate.of(2019, 12, 28))));

		String expected = """
				{
				  "bool": {
				    "must": [
				      {
				        "match": {
				          "first-name": {
				            "query": "John"
				          }
				        }
				      },
				      {
				        "bool": {
				          "should": [
				            {
				              "range": {
				                "birth-date": {
				                  "from": "09.11.1989",
				                  "to": "09.11.1990",
				                  "include_lower": true,
				                  "include_upper": true
				                }
				              }
				            },
				            {
				              "query_string": {
				                "query": "28.12.2019",
				                "fields": [
				                  "birth-date^1.0"
				                ]
				              }
				            }
				          ]
				        }
				      }
				    ]
				  }
				}
				"""; //

		mappingElasticsearchConverter.updateQuery(criteriaQuery, Person.class);
		String queryString = new CriteriaQueryProcessor().createQuery(criteriaQuery.getCriteria()).toString();

		assertEquals(expected, queryString, false);
	}

	@Test // DATAES-931
	@DisplayName("should map names in GeoJson query")
	void shouldMapNamesInGeoJsonQuery() throws JSONException {

		GeoJsonPoint geoJsonPoint = GeoJsonPoint.of(1.2, 3.4);
		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("geoShapeField").intersects(geoJsonPoint));
		String base64Query = getBase64EncodedGeoShapeQuery(geoJsonPoint, "geo-shape-field", "intersects");

		String expected = "{\n" + //
				"  \"wrapper\": {\n" + //
				"    \"query\": \"" + base64Query + "\"\n" + //
				"  }\n" + //
				"}\n"; //

		mappingElasticsearchConverter.updateQuery(criteriaQuery, GeoShapeEntity.class);
		String queryString = new CriteriaFilterProcessor().createFilter(criteriaQuery.getCriteria()).toString();

		assertEquals(expected, queryString, false);
	}

	@Test // #1753
	@DisplayName("should map names and value in nested entities")
	void shouldMapNamesAndValueInNestedEntities() throws JSONException {

		String expected = """
				{
				  "bool": {
				    "must": [
				      {
				        "nested": {
				          "query": {
				            "query_string": {
				              "query": "03.10.1999",
				              "fields": [
				                "per-sons.birth-date^1.0"
				              ]
				            }
				          },
				          "path": "per-sons"
				        }
				      }
				    ]
				  }
				}
				"""; //

		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("persons.birthDate").is(LocalDate.of(1999, 10, 3)));
		mappingElasticsearchConverter.updateQuery(criteriaQuery, House.class);
		String queryString = new CriteriaQueryProcessor().createQuery(criteriaQuery.getCriteria()).toString();

		assertEquals(expected, queryString, false);
	}

	@Test // #1753
	@DisplayName("should map names and value in nested entities with sub-fields")
	void shouldMapNamesAndValueInNestedEntitiesWithSubfields() throws JSONException {

		String expected = """
				{
				  "bool": {
				    "must": [
				      {
				        "nested": {
				          "query": {
				            "query_string": {
				              "query": "Foobar",
				              "fields": [
				                "per-sons.nick-name.keyword^1.0"
				              ]
				            }
				          },
				          "path": "per-sons"
				        }
				      }
				    ]
				  }
				}
				"""; //

		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("persons.nickName.keyword").is("Foobar"));
		mappingElasticsearchConverter.updateQuery(criteriaQuery, House.class);
		String queryString = new CriteriaQueryProcessor().createQuery(criteriaQuery.getCriteria()).toString();

		assertEquals(expected, queryString, false);
	}

	@Test // #1761
	@DisplayName("should map names and value in object entities")
	void shouldMapNamesAndValueInObjectEntities() throws JSONException {

		String expected = """
				{
				  "bool": {
				    "must": [
				      {
				         "query_string": {
				              "query": "03.10.1999",
				              "fields": [
				                "per-sons.birth-date^1.0"
				              ]
				            }
				      }
				    ]
				  }
				}
				"""; //

		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("persons.birthDate").is(LocalDate.of(1999, 10, 3)));
		mappingElasticsearchConverter.updateQuery(criteriaQuery, ObjectWithPerson.class);
		String queryString = new CriteriaQueryProcessor().createQuery(criteriaQuery.getCriteria()).toString();

		assertEquals(expected, queryString, false);
	}

	@Test // #1778
	@DisplayName("should map names in source fields and SourceFilters")
	void shouldMapNamesInSourceFieldsAndSourceFilters() {

		Query query = Query.findAll();
		// Note: we don't care if these filters make sense here, this test is only about name mapping
		query.addFields("firstName", "lastName");
		query.addSourceFilter(new FetchSourceFilterBuilder().withIncludes("firstName").withExcludes("lastName").build());

		mappingElasticsearchConverter.updateQuery(query, Person.class);

		SoftAssertions softly = new SoftAssertions();
		softly.assertThat(query.getFields()).containsExactly("first-name", "last-name");
		SourceFilter sourceFilter = query.getSourceFilter();
		softly.assertThat(sourceFilter).isNotNull();
		softly.assertThat(sourceFilter.getIncludes()).containsExactly("first-name");
		softly.assertThat(sourceFilter.getExcludes()).containsExactly("last-name");
		softly.assertAll();
	}

	@Test
	@DisplayName("should map names in source stored fields")
	void shouldMapNamesInSourceStoredFields() {

		Query query = Query.findAll();
		query.addStoredFields("firstName", "lastName");

		mappingElasticsearchConverter.updateQuery(query, Person.class);

		SoftAssertions softly = new SoftAssertions();
		List<String> storedFields = query.getStoredFields();
		softly.assertThat(storedFields).isNotNull();
		softly.assertThat(storedFields).containsExactly("first-name", "last-name");
		softly.assertAll();
	}

	// endregion

	// region helper functions
	private String getBase64EncodedGeoShapeQuery(GeoJson<?> geoJson, String elasticFieldName, String relation) {
		return Base64.getEncoder()
				.encodeToString(("{\"geo_shape\": {\""
						+ elasticFieldName + "\": {\"shape\": " + Document
								.from(Objects.requireNonNull(GeoConverters.GeoJsonToMapConverter.INSTANCE.convert(geoJson))).toJson()
						+ ", \"relation\": \"" + relation + "\"}}}").getBytes());
	}
	// endregion

	// region test entities
	static class Person {
		@Nullable
		@Id String id;
		@Nullable
		@Field(name = "first-name") String firstName;
		@Nullable
		@Field(name = "last-name") String lastName;
		@Nullable
		@MultiField(mainField = @Field(name = "nick-name"),
				otherFields = { @InnerField(suffix = "keyword", type = FieldType.Keyword) }) String nickName;
		@Nullable
		@Field(name = "created-date", type = FieldType.Date, format = DateFormat.epoch_millis) Date createdDate;
		@Nullable
		@Field(name = "birth-date", type = FieldType.Date, format = {}, pattern = "dd.MM.uuuu") LocalDate birthDate;
	}

	static class House {
		@Nullable
		@Id String id;
		@Nullable
		@Field(name = "per-sons", type = FieldType.Nested) List<Person> persons;
	}

	static class ObjectWithPerson {
		@Nullable
		@Id String id;
		@Nullable
		@Field(name = "per-sons", type = FieldType.Object) List<Person> persons;
	}

	static class GeoShapeEntity {
		@Nullable
		@Field(name = "geo-shape-field") GeoJson<?> geoShapeField;
	}
	// endregion
}
