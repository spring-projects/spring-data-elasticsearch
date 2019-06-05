/*
 * Copyright 2014-2019 the original author or authors.
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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.webbeans.cditest.CdiTestContainer;
import org.apache.webbeans.cditest.CdiTestContainerLoader;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;

/**
 * @author Mohsin Husen
 * @author Mark Paluch
 * @author Christoph Strobl
 * @author Peter-Josef Meisch
 */
public class CdiRepositoryTests {

	private static CdiTestContainer cdiContainer;
	private CdiProductRepository repository;
	private SamplePersonRepository personRepository;
	private QualifiedProductRepository qualifiedProductRepository;

	@BeforeClass
	public static void init() throws Exception {

		cdiContainer = CdiTestContainerLoader.getCdiContainer();
		cdiContainer.startApplicationScope();
		cdiContainer.bootContainer();
	}

	@AfterClass
	public static void shutdown() throws Exception {

		cdiContainer.stopContexts();
		cdiContainer.shutdownContainer();
	}

	@Before
	public void setUp() {

		CdiRepositoryClient client = cdiContainer.getInstance(CdiRepositoryClient.class);
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

	/**
	 * @author Mohsin Husen
	 * @author Artur Konczak
	 */
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	@Document(indexName = "test-index-product-cdi-repository", type = "test-product-type", shards = 1, replicas = 0,
			refreshInterval = "-1")
	static class Product {

		@Id private String id;

		private List<String> title;

		private String name;

		private String description;

		private String text;

		private List<String> categories;

		private Float weight;

		@Field(type = FieldType.Float) private Float price;

		private Integer popularity;

		private boolean available;

		private String location;

		private Date lastModified;
	}

	@Data
	@Document(indexName = "test-index-person-cdi-repository", type = "user", shards = 1, replicas = 0,
			refreshInterval = "-1")
	static class Person {

		@Id private String id;

		private String name;

		@Field(type = FieldType.Nested) private List<Car> car;

		@Field(type = FieldType.Nested, includeInParent = true) private List<Book> books;

	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	@Document(indexName = "test-index-book-cdi-repository", type = "book", shards = 1, replicas = 0,
			refreshInterval = "-1")
	static class Book {

		@Id private String id;
		private String name;
		@Field(type = FieldType.Object) private Author author;
		@Field(type = FieldType.Nested) private Map<Integer, Collection<String>> buckets = new HashMap<>();
		@MultiField(mainField = @Field(type = FieldType.Text, analyzer = "whitespace"),
				otherFields = { @InnerField(suffix = "prefix", type = FieldType.Text, analyzer = "stop",
						searchAnalyzer = "standard") }) private String description;
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	static class Car {

		private String name;
		private String model;
	}

	@Data
	static class Author {

		private String id;
		private String name;
	}

}
