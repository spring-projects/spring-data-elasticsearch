/*
 * Copyright 2019-2024 the original author or authors.
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

import java.time.LocalDate;
import java.util.Collections;
import java.util.Date;
import java.util.List;

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
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
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
 * Tests for the mapping of {@link CriteriaQuery} by a {@link MappingElasticsearchConverter}. In the same package as
 * {@link org.springframework.data.elasticsearch.client.elc.CriteriaQueryProcessor} as this is needed to get the String
 * representation to assert.
 *
 * @author Peter-Josef Meisch
 * @author Sascha Woo
 * @author vdisk
 */
public class CriteriaQueryMappingUnitTests {

	private final JsonpMapper mapper = new JacksonJsonpMapper();

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
		var expected = """
				{
					"bool": {
						"should": [
							{
								"range": {
									"birth-date": {
										"gte": "09.11.1989",
										"lte": "09.11.1990"
									}
								}
							},
							{
								"query_string": {
									"default_operator": "and",
									"fields": [
										"birth-date"
									],
									"query": "28.12.2019"
								}
							}
						]
					}
				}
				""";

		mappingElasticsearchConverter.updateQuery(criteriaQuery, Person.class);
		var queryString = queryToJson(CriteriaQueryProcessor.createQuery(criteriaQuery.getCriteria()), mapper);

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
					"bool": {
						"should": [
							{
								"bool": {
									"must": [
										{
											"range": {
												"birth-date": {
													"gte": "09.11.1989",
													"lte": "09.11.1990"
												}
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
												"default_operator": "and",
												"fields": [
													"birth-date"
												],
												"query": "28.12.2019"
											}
										}
									]
								}
							}
						]
					}
				}
				""";

		mappingElasticsearchConverter.updateQuery(criteriaQuery, Person.class);
		var queryString = queryToJson(CriteriaQueryProcessor.createQuery(criteriaQuery.getCriteria()), mapper);

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
					"bool": {
						"should": [
							{
								"bool": {
									"must": [
										{
											"range": {
												"birth-date": {
													"gte": "09.11.1989",
													"lte": "09.11.1990"
												}
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
												"default_operator": "and",
												"fields": [
													"created-date"
												],
												"query": "383745721653"
											}
										}
									]
								}
							}
						]
					}
				}""";

		mappingElasticsearchConverter.updateQuery(criteriaQuery, Person.class);
		var queryString = queryToJson(CriteriaQueryProcessor.createQuery(criteriaQuery.getCriteria()), mapper);

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
													"gte": "09.11.1989",
													"lte": "09.11.1990"
												}
											}
										},
										{
											"query_string": {
												"default_operator": "and",
												"fields": [
													"birth-date"
												],
												"query": "28.12.2019"
											}
										}
									]
								}
							}
						]
					}
				}""";

		mappingElasticsearchConverter.updateQuery(criteriaQuery, Person.class);
		var queryString = queryToJson(CriteriaQueryProcessor.createQuery(criteriaQuery.getCriteria()), mapper);

		assertEquals(expected, queryString, false);
	}

	@Test // DATAES-931
	@DisplayName("should map names in GeoJson query")
	void shouldMapNamesInGeoJsonQuery() throws JSONException {

		GeoJsonPoint geoJsonPoint = GeoJsonPoint.of(1.2, 3.4);
		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("geoShapeField").intersects(geoJsonPoint));

		String expected = """
				 {
					"geo_shape": {
						"geo-shape-field": {
							"shape": {
								"type": "Point",
								"coordinates": [
									1.2,
									3.4
								]
							},
							"relation": "intersects"
						}
					}
				}
				""";

		mappingElasticsearchConverter.updateQuery(criteriaQuery, GeoShapeEntity.class);
		var queryString = queryToJson(CriteriaFilterProcessor.createQuery(criteriaQuery.getCriteria()).get(), mapper);

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
									"path": "per-sons",
									"query": {
										"query_string": {
											"default_operator": "and",
											"fields": [
												"per-sons.birth-date"
											],
											"query": "03.10.1999"
										}
									},
									"score_mode": "avg"
								}
							}
						]
					}
				}
				""";

		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("persons.birthDate").is(LocalDate.of(1999, 10, 3)));
		mappingElasticsearchConverter.updateQuery(criteriaQuery, House.class);
		var queryString = queryToJson(CriteriaQueryProcessor.createQuery(criteriaQuery.getCriteria()), mapper);

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
									"path": "per-sons",
									"query": {
										"query_string": {
											"default_operator": "and",
											"fields": [
												"per-sons.nick-name.keyword"
											],
											"query": "Foobar"
										}
									},
									"score_mode": "avg"
								}
							}
						]
					}
				}""";

		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("persons.nickName.keyword").is("Foobar"));
		mappingElasticsearchConverter.updateQuery(criteriaQuery, House.class);
		var queryString = queryToJson(CriteriaQueryProcessor.createQuery(criteriaQuery.getCriteria()), mapper);

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
									"default_operator": "and",
									"fields": [
										"per-sons.birth-date"
									],
									"query": "03.10.1999"
								}
							}
						]
					}
				}""";

		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("persons.birthDate").is(LocalDate.of(1999, 10, 3)));
		mappingElasticsearchConverter.updateQuery(criteriaQuery, ObjectWithPerson.class);
		var queryString = queryToJson(CriteriaQueryProcessor.createQuery(criteriaQuery.getCriteria()), mapper);

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
