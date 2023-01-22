/*
 * Copyright 2021-2023 the original author or authors.
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

import java.time.LocalDate;

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
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilterBuilder;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.lang.Nullable;

/**
 * @author Peter-Josef Meisch
 * @author cdalxndr
 */
@SpringIntegrationTest
public abstract class RuntimeFieldsIntegrationTests {

	@Autowired private ElasticsearchOperations operations;
	@Autowired protected IndexNameProvider indexNameProvider;

	@BeforeEach
	void setUp() {

		indexNameProvider.increment();
		operations.indexOps(SomethingToBuy.class).createWithMapping();
		operations.indexOps(Person.class).createWithMapping();
	}

	@Test
	@Order(java.lang.Integer.MAX_VALUE)
	void cleanup() {
		operations.indexOps(IndexCoordinates.of(indexNameProvider.getPrefix() + "*")).delete();
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
		assertThat(searchHits.getSearchHit(0).getId()).isEqualTo("2");
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
		assertThat(searchHits.getSearchHit(0).getId()).isEqualTo("1");
	}

	@Test // #2431
	@DisplayName("should return value from runtime field defined in mapping")
	void shouldReturnValueFromRuntimeFieldDefinedInMapping() {

		var person = new Person();
		var years = 10;
		person.setBirthDate(LocalDate.now().minusDays(years * 365 + 100));
		operations.save(person);
		var query = Query.findAll();
		query.addFields("age");
		query.addSourceFilter(new FetchSourceFilterBuilder().withIncludes("*").build());

		var searchHits = operations.search(query, Person.class);

		assertThat(searchHits.getTotalHits()).isEqualTo(1);
		assertThat(searchHits.getSearchHit(0).getContent().getAge()).isEqualTo(years);
	}

	private void insert(String id, String description, double price) {
		SomethingToBuy entity = new SomethingToBuy();
		entity.setId(id);
		entity.setDescription(description);
		entity.setPrice(price);
		operations.save(entity);
	}

	@Document(indexName = "#{@indexNameProvider.indexName()}-something")
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

	@Document(indexName = "#{@indexNameProvider.indexName()}-person")
	@Mapping(runtimeFieldsPath = "/runtime-fields-person.json")
	public class Person {
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
}
