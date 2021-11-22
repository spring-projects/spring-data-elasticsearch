/*
 * Copyright 2014-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.elasticsearch.repositories.cdi;

import static org.assertj.core.api.Assertions.*;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.enterprise.inject.se.SeContainer;
import jakarta.enterprise.inject.se.SeContainerInitializer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;
import org.springframework.data.elasticsearch.junit.jupiter.IntegrationTest;
import org.springframework.lang.Nullable;

/**
 * @author Mohsin Husen
 * @author Mark Paluch
 * @author Christoph Strobl
 * @author Peter-Josef Meisch
 */
@IntegrationTest
public class CdiRepositoryTests {

	@SuppressWarnings("NotNullFieldNotInitialized") private static SeContainer container;

	// @Nullable private static CdiTestContainer cdiContainer;
	private CdiProductRepository repository;
	private SamplePersonRepository personRepository;
	private QualifiedProductRepository qualifiedProductRepository;

	@BeforeAll
	public static void init() throws Exception {

		container = SeContainerInitializer.newInstance() //
				.disableDiscovery()//
				.addPackages(CdiRepositoryTests.class) //
				.initialize();
	}

	@AfterAll
	public static void shutdown() throws Exception {
		container.close();
	}

	@BeforeEach
	public void setUp() {

		CdiRepositoryClient client = container.select(CdiRepositoryClient.class).get();
		repository = client.getRepository();
		personRepository = client.getSamplePersonRepository();
		repository.deleteAll();
		qualifiedProductRepository = client.getQualifiedProductRepository();
		qualifiedProductRepository.deleteAll();
	}

	@Test
	public void testCdiRepository() {

		assertThat(repository).isNotNull();

		Product bean = new Product();
		bean.setId("id-1");
		bean.setName("cidContainerTest-1");

		repository.save(bean);

		assertThat(repository.existsById(bean.getId())).isTrue();

		Optional<Product> retrieved = repository.findById(bean.getId());

		assertThat(retrieved).isPresent();
		retrieved.ifPresent(product -> {
			assertThat(bean.getId()).isEqualTo(product.getId());
			assertThat(bean.getName()).isEqualTo(product.getName());
		});

		assertThat(repository.count()).isEqualTo(1);

		assertThat(repository.existsById(bean.getId())).isTrue();

		repository.delete(bean);

		assertThat(repository.count()).isEqualTo(0);
		retrieved = repository.findById(bean.getId());
		assertThat(retrieved).isNotPresent();
	}

	@Test // DATAES-234
	public void testQualifiedCdiRepository() {
		assertThat(qualifiedProductRepository).isNotNull();

		Product bean = new Product();
		bean.setId("id-1");
		bean.setName("cidContainerTest-1");

		qualifiedProductRepository.save(bean);

		assertThat(qualifiedProductRepository.existsById(bean.getId())).isTrue();

		Optional<Product> retrieved = qualifiedProductRepository.findById(bean.getId());

		assertThat(retrieved).isPresent();
		retrieved.ifPresent(product -> {
			assertThat(bean.getId()).isEqualTo(product.getId());
			assertThat(bean.getName()).isEqualTo(product.getName());
		});

		assertThat(qualifiedProductRepository.count()).isEqualTo(1);

		assertThat(qualifiedProductRepository.existsById(bean.getId())).isTrue();

		qualifiedProductRepository.delete(bean);

		assertThat(qualifiedProductRepository.count()).isEqualTo(0);
		retrieved = qualifiedProductRepository.findById(bean.getId());
		assertThat(retrieved).isNotPresent();
	}

	@Test // DATAES-113
	public void returnOneFromCustomImpl() {

		assertThat(personRepository.returnOne()).isEqualTo(1);
	}

	@Document(indexName = "test-index-product-cdi-repository")
	static class Product {
		@Nullable
		@Id private String id;
		@Nullable private List<String> title;
		@Nullable private String name;
		@Nullable private String description;
		@Nullable private String text;
		@Nullable private List<String> categories;
		@Nullable private Float weight;
		@Nullable
		@Field(type = FieldType.Float) private Float price;
		@Nullable private Integer popularity;
		@Nullable private boolean available;
		@Nullable private String location;
		@Nullable private Date lastModified;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public List<String> getTitle() {
			return title;
		}

		public void setTitle(@Nullable List<String> title) {
			this.title = title;
		}

		@Nullable
		public String getName() {
			return name;
		}

		public void setName(@Nullable String name) {
			this.name = name;
		}

		@Nullable
		public String getDescription() {
			return description;
		}

		public void setDescription(@Nullable String description) {
			this.description = description;
		}

		@Nullable
		public String getText() {
			return text;
		}

		public void setText(@Nullable String text) {
			this.text = text;
		}

		@Nullable
		public List<String> getCategories() {
			return categories;
		}

		public void setCategories(@Nullable List<String> categories) {
			this.categories = categories;
		}

		@Nullable
		public Float getWeight() {
			return weight;
		}

		public void setWeight(@Nullable Float weight) {
			this.weight = weight;
		}

		@Nullable
		public Float getPrice() {
			return price;
		}

		public void setPrice(@Nullable Float price) {
			this.price = price;
		}

		@Nullable
		public Integer getPopularity() {
			return popularity;
		}

		public void setPopularity(@Nullable Integer popularity) {
			this.popularity = popularity;
		}

		public boolean isAvailable() {
			return available;
		}

		public void setAvailable(boolean available) {
			this.available = available;
		}

		@Nullable
		public String getLocation() {
			return location;
		}

		public void setLocation(@Nullable String location) {
			this.location = location;
		}

		@Nullable
		public Date getLastModified() {
			return lastModified;
		}

		public void setLastModified(@Nullable Date lastModified) {
			this.lastModified = lastModified;
		}
	}

	@Document(indexName = "test-index-person-cdi-repository")
	static class Person {

		@Id private String id;

		private String name;

		@Field(type = FieldType.Nested) private List<Car> car;

		@Field(type = FieldType.Nested, includeInParent = true) private List<Book> books;

	}

	@Document(indexName = "test-index-book-cdi-repository")
	static class Book {
		@Nullable
		@Id private String id;
		@Nullable private String name;
		@Nullable
		@Field(type = FieldType.Object) private Author author;
		@Nullable
		@Field(type = FieldType.Nested) private Map<Integer, Collection<String>> buckets = new HashMap<>();
		@Nullable
		@MultiField(mainField = @Field(type = FieldType.Text, analyzer = "whitespace"),
				otherFields = { @InnerField(suffix = "prefix", type = FieldType.Text, analyzer = "stop",
						searchAnalyzer = "standard") }) private String description;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getName() {
			return name;
		}

		public void setName(@Nullable String name) {
			this.name = name;
		}

		@Nullable
		public Author getAuthor() {
			return author;
		}

		public void setAuthor(@Nullable Author author) {
			this.author = author;
		}

		@Nullable
		public Map<Integer, Collection<String>> getBuckets() {
			return buckets;
		}

		public void setBuckets(@Nullable Map<Integer, Collection<String>> buckets) {
			this.buckets = buckets;
		}

		@Nullable
		public String getDescription() {
			return description;
		}

		public void setDescription(@Nullable String description) {
			this.description = description;
		}
	}

	static class Car {
		@Nullable private String name;
		@Nullable private String model;

		@Nullable
		public String getName() {
			return name;
		}

		public void setName(@Nullable String name) {
			this.name = name;
		}

		@Nullable
		public String getModel() {
			return model;
		}

		public void setModel(@Nullable String model) {
			this.model = model;
		}
	}

	private static class Author {
		@Nullable private String id;
		@Nullable private String name;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getName() {
			return name;
		}

		public void setName(@Nullable String name) {
			this.name = name;
		}
	}

}
