/*
 * Copyright 2021-2024 the original author or authors.
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
package org.springframework.data.elasticsearch.core.query.scriptedandruntimefields;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.ReadOnlyProperty;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Mapping;
import org.springframework.data.elasticsearch.annotations.ScriptedField;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilterBuilder;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.RuntimeField;
import org.springframework.data.elasticsearch.core.query.ScriptData;
import org.springframework.data.elasticsearch.core.query.ScriptType;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.lang.Nullable;

/**
 * @author Peter-Josef Meisch
 * @author cdalxndr
 */
@SpringIntegrationTest
public abstract class ScriptedAndRuntimeFieldsIntegrationTests {

	@Autowired private ElasticsearchOperations operations;
	@Autowired protected IndexNameProvider indexNameProvider;
	@Autowired private SARRepository repository;

	@BeforeEach
	void setUp() {

		indexNameProvider.increment();
		operations.indexOps(SomethingToBuy.class).createWithMapping();
		operations.indexOps(Person.class).createWithMapping();
		operations.indexOps(SAREntity.class).createWithMapping();
	}

	@Test
	@Order(java.lang.Integer.MAX_VALUE)
	void cleanup() {
		operations.indexOps(IndexCoordinates.of(indexNameProvider.getPrefix() + '*')).delete();
	}

	@Test // #1971
	@DisplayName("should use runtime-field from query in search")
	void shouldUseRuntimeFieldFromQueryInSearch() {

		insert("1", "item 1", 13.5);
		insert("2", "item 2", 15);
		Query query = new CriteriaQuery(new Criteria("priceWithTax").greaterThanEqual(16.5));
		RuntimeField runtimeField = new RuntimeField("priceWithTax", "double", "emit(doc['price'].value * 1.19)");
		query.addRuntimeField(runtimeField);

		SearchHits<SomethingToBuy> searchHits = operations.search(query, SomethingToBuy.class);

		assertThat(searchHits.getTotalHits()).isEqualTo(1);
		var searchHit = searchHits.getSearchHit(0);
		assertThat(searchHit.getId()).isEqualTo("2");
		var foundEntity = searchHit.getContent();
		assertThat(foundEntity.getDescription()).isEqualTo("item 2");
	}

	@Test // #2267
	@DisplayName("should use runtime-field without script")
	void shouldUseRuntimeFieldWithoutScript() {

		insert("1", "11", 10);
		Query query = new CriteriaQuery(new Criteria("description").matches(11.0));
		RuntimeField runtimeField = new RuntimeField("description", "double");
		query.addRuntimeField(runtimeField);

		SearchHits<SomethingToBuy> searchHits = operations.search(query, SomethingToBuy.class);

		assertThat(searchHits.getTotalHits()).isEqualTo(1);
		var searchHit = searchHits.getSearchHit(0);
		assertThat(searchHit.getId()).isEqualTo("1");
		assertThat(searchHit.getContent().getDescription()).isEqualTo("11");
	}

	@Test // #2431
	@DisplayName("should return value from runtime field defined in mapping")
	void shouldReturnValueFromRuntimeFieldDefinedInMapping() {

		var person = new Person();
		var years = 10;
		var birthDate = LocalDate.now().minusDays(years * 365 + 100);
		person.setBirthDate(birthDate);
		operations.save(person);
		var query = Query.findAll();
		query.addFields("age");
		query.addSourceFilter(new FetchSourceFilterBuilder().withIncludes("*").build());

		var searchHits = operations.search(query, Person.class);

		assertThat(searchHits.getTotalHits()).isEqualTo(1);
		var foundPerson = searchHits.getSearchHit(0).getContent();
		assertThat(foundPerson.getAge()).isEqualTo(years);
		assertThat(foundPerson.getBirthDate()).isEqualTo(birthDate);
	}

	@Test // #2035
	@DisplayName("should use repository method with ScriptedField parameters")
	void shouldUseRepositoryMethodWithScriptedFieldParameters() {

		var entity = new SAREntity();
		entity.setId("42");
		entity.setValue(3);

		repository.save(entity);

		org.springframework.data.elasticsearch.core.query.ScriptedField scriptedField1 = getScriptedField("scriptedValue1",
				2);
		org.springframework.data.elasticsearch.core.query.ScriptedField scriptedField2 = getScriptedField("scriptedValue2",
				3);

		var searchHits = repository.findByValue(3, scriptedField1, scriptedField2);

		assertThat(searchHits.getTotalHits()).isEqualTo(1);
		var foundEntity = searchHits.getSearchHit(0).getContent();
		assertThat(foundEntity.value).isEqualTo(3);
		assertThat(foundEntity.getScriptedValue1()).isEqualTo(6);
		assertThat(foundEntity.getScriptedValue2()).isEqualTo(9);
	}

	@NotNull
	private static org.springframework.data.elasticsearch.core.query.ScriptedField getScriptedField(String fieldName,
			int factor) {
		return org.springframework.data.elasticsearch.core.query.ScriptedField.of(
				fieldName,
				ScriptData.of(b -> b
						.withType(ScriptType.INLINE)
						.withScript("doc['value'].size() > 0 ? doc['value'].value * params['factor'] : 0")
						.withParams(Map.of("factor", factor))));
	}

	@Test // #2035
	@DisplayName("should use repository string query method with ScriptedField parameters")
	void shouldUseRepositoryStringQueryMethodWithScriptedFieldParameters() {

		var entity = new SAREntity();
		entity.setId("42");
		entity.setValue(3);

		repository.save(entity);

		org.springframework.data.elasticsearch.core.query.ScriptedField scriptedField1 = getScriptedField("scriptedValue1",
				2);
		org.springframework.data.elasticsearch.core.query.ScriptedField scriptedField2 = getScriptedField("scriptedValue2",
				3);

		var searchHits = repository.findWithScriptedFields(3, scriptedField1, scriptedField2);

		assertThat(searchHits.getTotalHits()).isEqualTo(1);
		var foundEntity = searchHits.getSearchHit(0).getContent();
		assertThat(foundEntity.value).isEqualTo(3);
		assertThat(foundEntity.getScriptedValue1()).isEqualTo(6);
		assertThat(foundEntity.getScriptedValue2()).isEqualTo(9);
	}

	@Test // #2035
	@DisplayName("should use repository method with RuntimeField parameters")
	void shouldUseRepositoryMethodWithRuntimeFieldParameters() {

		var entity = new SAREntity();
		entity.setId("42");
		entity.setValue(3);

		repository.save(entity);

		var runtimeField1 = getRuntimeField("scriptedValue1", 3);
		var runtimeField2 = getRuntimeField("scriptedValue2", 4);

		var searchHits = repository.findByValue(3, runtimeField1, runtimeField2);

		assertThat(searchHits.getTotalHits()).isEqualTo(1);
		var foundEntity = searchHits.getSearchHit(0).getContent();
		assertThat(foundEntity.value).isEqualTo(3);
		assertThat(foundEntity.getScriptedValue1()).isEqualTo(9);
		assertThat(foundEntity.getScriptedValue2()).isEqualTo(12);
	}

	@NotNull
	private static RuntimeField getRuntimeField(String fieldName, int factor) {
		return new RuntimeField(
				fieldName,
				"long",
				String.format("emit(doc['value'].size() > 0 ? doc['value'].value * %d : 0)", factor));
	}

	@Test // #2035
	@DisplayName("should use repository string query method with RuntimeField parameters")
	void shouldUseRepositoryStringQueryMethodWithRuntimeFieldParameters() {

		var entity = new SAREntity();
		entity.setId("42");
		entity.setValue(3);

		repository.save(entity);

		var runtimeField1 = getRuntimeField("scriptedValue1", 3);
		var runtimeField2 = getRuntimeField("scriptedValue2", 4);

		var searchHits = repository.findWithRuntimeFields(3, runtimeField1, runtimeField2);

		assertThat(searchHits.getTotalHits()).isEqualTo(1);
		var foundEntity = searchHits.getSearchHit(0).getContent();
		assertThat(foundEntity.value).isEqualTo(3);
		assertThat(foundEntity.getScriptedValue1()).isEqualTo(9);
		assertThat(foundEntity.getScriptedValue2()).isEqualTo(12);
	}

	private void insert(String id, String description, double price) {
		SomethingToBuy entity = new SomethingToBuy();
		entity.setId(id);
		entity.setDescription(description);
		entity.setPrice(price);
		operations.save(entity);
	}

	@Test // #2303
	@DisplayName("should use parameters for runtime fields in search queries")
	void shouldUseParametersForRuntimeFieldsInSearchQueries() {

		insert("1", "item 1", 80.0);
		insert("2", "item 2", 90.0);

		RuntimeField runtimeField = new RuntimeField(
				"priceWithTax",
				"double",
				"emit(doc['price'].value * params.tax)",
				Map.of("tax", 1.19)
		);
		var query = CriteriaQuery.builder(
				Criteria.where("priceWithTax").greaterThan(100.0))
				.withRuntimeFields(List.of(runtimeField))
				.build();

		var searchHits = operations.search(query, SomethingToBuy.class);

		assertThat(searchHits).hasSize(1);
	}

	@SuppressWarnings("unused")
	@Document(indexName = "#{@indexNameProvider.indexName()}-something-to-by")
	private static class SomethingToBuy {
		private @Id @Nullable String id;

		@Nullable
		@Field(type = FieldType.Text) private String description;

		@Nullable
		@Field(type = FieldType.Double) private Double price;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getDescription() {
			return description;
		}

		public void setDescription(@Nullable String description) {
			this.description = description;
		}

		@Nullable
		public Double getPrice() {
			return price;
		}

		public void setPrice(@Nullable Double price) {
			this.price = price;
		}
	}

	@SuppressWarnings("unused")
	@Document(indexName = "#{@indexNameProvider.indexName()}-person")
	@Mapping(runtimeFieldsPath = "/runtime-fields-person.json")
	public static class Person {
		@Nullable private String id;

		@Field(type = FieldType.Date, format = DateFormat.basic_date)
		@Nullable private LocalDate birthDate;

		@ReadOnlyProperty // do not write to prevent ES from automapping
		@Nullable private Integer age;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public LocalDate getBirthDate() {
			return birthDate;
		}

		public void setBirthDate(@Nullable LocalDate birthDate) {
			this.birthDate = birthDate;
		}

		@Nullable
		public Integer getAge() {
			return age;
		}

		public void setAge(@Nullable Integer age) {
			this.age = age;
		}
	}

	@SuppressWarnings("unused")
	@Document(indexName = "#{@indexNameProvider.indexName()}-sar")
	public static class SAREntity {
		@Nullable private String id;
		@Field(type = FieldType.Integer)
		@Nullable Integer value;
		@ScriptedField
		@Nullable Integer scriptedValue1;
		@ScriptedField
		@Nullable Integer scriptedValue2;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public Integer getValue() {
			return value;
		}

		public void setValue(@Nullable Integer value) {
			this.value = value;
		}

		@Nullable
		public Integer getScriptedValue1() {
			return scriptedValue1;
		}

		public void setScriptedValue1(@Nullable Integer scriptedValue1) {
			this.scriptedValue1 = scriptedValue1;
		}

		@Nullable
		public Integer getScriptedValue2() {
			return scriptedValue2;
		}

		public void setScriptedValue2(@Nullable Integer scriptedValue2) {
			this.scriptedValue2 = scriptedValue2;
		}
	}

	@SuppressWarnings("SpringDataRepositoryMethodReturnTypeInspection")
	public interface SARRepository extends ElasticsearchRepository<SAREntity, String> {
		SearchHits<SAREntity> findByValue(Integer value,
				org.springframework.data.elasticsearch.core.query.ScriptedField scriptedField1,
				org.springframework.data.elasticsearch.core.query.ScriptedField scriptedField2);

		@org.springframework.data.elasticsearch.annotations.Query("""
				{
				   "term": {
					 "value": {
					   "value": "?0"
					 }
				   }
				}
				""")
		SearchHits<SAREntity> findWithScriptedFields(Integer value,
				org.springframework.data.elasticsearch.core.query.ScriptedField scriptedField1,
				org.springframework.data.elasticsearch.core.query.ScriptedField scriptedField2);

		SearchHits<SAREntity> findByValue(Integer value, RuntimeField runtimeField1, RuntimeField runtimeField2);

		@org.springframework.data.elasticsearch.annotations.Query("""
				{
					"term": {
					  "value": {
						"value": "?0"
					  }
					}
				}
				""")
		SearchHits<SAREntity> findWithRuntimeFields(Integer value, RuntimeField runtimeField1, RuntimeField runtimeField2);
	}
}
