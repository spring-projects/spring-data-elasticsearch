/*
 * Copyright 2016-2024 the original author or authors.
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
package org.springframework.data.elasticsearch.repository.query.keywords;

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.lang.Nullable;

/**
 * base class for query keyword tests. Implemented by subclasses using ElasticsearchClient and ElasticsearchRestClient
 * based repositories.
 *
 * @author Artur Konczak
 * @author Christoph Strobl
 * @author Peter-Josef Meisch
 */
@SpringIntegrationTest
abstract class QueryKeywordsIntegrationTests {

	@Autowired private ProductRepository repository;
	@Autowired ElasticsearchOperations operations;
	@Autowired IndexNameProvider indexNameProvider;

	@BeforeEach
	public void before() {
		indexNameProvider.increment();
		operations.indexOps(Product.class).createWithMapping();

		Product product1 = new Product("1", "Sugar", "Cane sugar", 1.0f, false, "sort5");
		Product product2 = new Product("2", "Sugar", "Cane sugar", 1.2f, true, "sort4");
		Product product3 = new Product("3", "Sugar", "Beet sugar", 1.1f, true, "sort3");
		Product product4 = new Product("4", "Salt", "Rock salt", 1.9f, true, "sort2");
		Product product5 = new Product("5", "Salt", "Sea salt", 2.1f, false, "sort1");
		Product product6 = new Product("6", null, "no name", 3.4f, false, "sort6");
		Product product7 = new Product("7", "", "empty name", 3.4f, false, "sort7");

		repository.saveAll(Arrays.asList(product1, product2, product3, product4, product5, product6, product7));
	}

	@Test
	@Order(Integer.MAX_VALUE)
	void cleanup() {
		operations.indexOps(IndexCoordinates.of(indexNameProvider.getPrefix() + "*")).delete();
	}

	@Test
	public void shouldSupportAND() {

		assertThat(repository.findByNameAndText("Sugar", "Cane sugar")).hasSize(2);
		assertThat(repository.findByNameAndPrice("Sugar", 1.1f)).hasSize(1);
	}

	@Test
	public void shouldSupportOR() {

		assertThat(repository.findByNameOrPrice("Sugar", 1.9f)).hasSize(4);
		assertThat(repository.findByNameOrText("Salt", "Beet sugar")).hasSize(3);
	}

	@Test
	public void shouldSupportTrueAndFalse() {

		assertThat(repository.findByAvailableTrue()).hasSize(3);
		assertThat(repository.findByAvailableFalse()).hasSize(4);
	}

	@Test
	public void shouldSupportInAndNotInAndNot() {

		assertThat(repository.findByPriceIn(Arrays.asList(1.2f, 1.1f))).hasSize(2);
		assertThat(repository.findByPriceNotIn(Arrays.asList(1.2f, 1.1f))).hasSize(5);
		assertThat(repository.findByPriceNot(1.2f)).hasSize(6);
	}

	@Test // DATAES-171
	public void shouldWorkWithNotIn() {

		assertThat(repository.findByIdNotIn(Arrays.asList("2", "3"))).hasSize(5);
	}

	@Test
	public void shouldSupportBetween() {

		assertThat(repository.findByPriceBetween(1.0f, 2.0f)).hasSize(4);
	}

	@Test
	public void shouldSupportLessThanAndGreaterThan() {

		assertThat(repository.findByPriceLessThan(1.1f)).hasSize(1);
		assertThat(repository.findByPriceLessThanEqual(1.1f)).hasSize(2);

		assertThat(repository.findByPriceGreaterThan(1.9f)).hasSize(3);
		assertThat(repository.findByPriceGreaterThanEqual(1.9f)).hasSize(4);
	}

	@Test // DATAES-615
	public void shouldSupportSortOnStandardFieldWithCriteria() {
		List<String> sortedIds = repository.findAllByNameOrderByText("Salt").stream() //
				.map(it -> it.id).collect(Collectors.toList());

		assertThat(sortedIds).containsExactly("4", "5");
	}

	@Test // DATAES-615
	public void shouldSupportSortOnFieldWithCustomFieldNameWithCriteria() {

		List<String> sortedIds = repository.findAllByNameOrderBySortName("Sugar").stream() //
				.map(it -> it.id).collect(Collectors.toList());

		assertThat(sortedIds).containsExactly("3", "2", "1");
	}

	@Test // DATAES-615
	public void shouldSupportSortOnStandardFieldWithoutCriteria() {
		List<String> sortedIds = repository.findAllByOrderByText().stream() //
				.map(it -> it.text).collect(Collectors.toList());

		assertThat(sortedIds).containsExactly("Beet sugar", "Cane sugar", "Cane sugar", "Rock salt", "Sea salt",
				"empty name", "no name");
	}

	@Test // DATAES-615
	public void shouldSupportSortOnFieldWithCustomFieldNameWithoutCriteria() {

		List<String> sortedIds = repository.findAllByOrderBySortName().stream() //
				.map(it -> it.id).collect(Collectors.toList());

		assertThat(sortedIds).containsExactly("5", "4", "3", "2", "1", "6", "7");
	}

	@Test // DATAES-178
	public void shouldReturnOneWithFindFirst() {

		Product product = repository.findFirstByName("Sugar");

		assertThat(product.name).isEqualTo("Sugar");
	}

	@Test // DATAES-178
	public void shouldReturnOneWithFindTop() {

		Product product = repository.findTopByName("Sugar");

		assertThat(product.name).isEqualTo("Sugar");
	}

	@Test // DATAES-178
	public void shouldReturnTwoWithFindFirst2() {

		List<Product> products = repository.findFirst2ByName("Sugar");

		assertThat(products).hasSize(2);
		products.forEach(product -> assertThat(product.name).isEqualTo("Sugar"));
	}

	@Test // DATAES-178
	public void shouldReturnTwoWithFindTop2() {

		List<Product> products = repository.findTop2ByName("Sugar");

		assertThat(products).hasSize(2);
		products.forEach(product -> assertThat(product.name).isEqualTo("Sugar"));
	}

	@Test // DATAES-239
	void shouldSearchForNullValues() {
		final List<Product> products = repository.findByName(null);

		assertThat(products).hasSize(1);
		assertThat(products.get(0).getId()).isEqualTo("6");
	}

	@Test // DATAES-239
	void shouldDeleteWithNullValues() {
		repository.deleteByName(null);

		long count = repository.count();
		assertThat(count).isEqualTo(6);
	}

	@Test // DATAES-937
	@DisplayName("should return empty list on findById with empty input list")
	void shouldReturnEmptyListOnFindByIdWithEmptyInputList() {

		Iterable<Product> products = repository.findAllById(new ArrayList<>());

		assertThat(products).isEmpty();
	}

	@Test // DATAES-937
	@DisplayName("should return empty list on derived method with empty input list")
	void shouldReturnEmptyListOnDerivedMethodWithEmptyInputList() {

		Iterable<Product> products = repository.findAllByNameIn(new ArrayList<>());

		assertThat(products).isEmpty();
	}

	@Test // #1909
	@DisplayName("should find by property exists")
	void shouldFindByPropertyExists() {

		SearchHits<Product> searchHits = repository.findByNameExists();

		assertThat(searchHits.getTotalHits()).isEqualTo(6);
	}

	@Test // #1909
	@DisplayName("should find by property is not null")
	void shouldFindByPropertyIsNotNull() {

		SearchHits<Product> searchHits = repository.findByNameIsNotNull();

		assertThat(searchHits.getTotalHits()).isEqualTo(6);
	}

	@Test // #1909
	@DisplayName("should find by property is null")
	void shouldFindByPropertyIsNull() {

		SearchHits<Product> searchHits = repository.findByNameIsNull();

		assertThat(searchHits.getTotalHits()).isEqualTo(1);
	}

	@Test // #1909
	@DisplayName("should find by empty property")
	void shouldFindByEmptyProperty() {

		SearchHits<Product> searchHits = repository.findByNameEmpty();

		assertThat(searchHits.getTotalHits()).isEqualTo(1);
	}

	@Test // #1909
	@DisplayName("should find by non-empty property")
	void shouldFindByNonEmptyProperty() {

		SearchHits<Product> searchHits = repository.findByNameNotEmpty();

		assertThat(searchHits.getTotalHits()).isEqualTo(5);
	}

	@Test // #2162
	@DisplayName("should run exists query")
	void shouldRunExistsQuery() {

		Boolean existsCaneSugar = repository.existsByText("Cane sugar");
		Boolean existsSand = repository.existsByText("Sand");

		assertThat(existsCaneSugar).isTrue();
		assertThat(existsSand).isFalse();
	}

	@SuppressWarnings("unused")
	@Document(indexName = "#{@indexNameProvider.indexName()}")
	static class Product {
		@Nullable
		@Id private String id;
		@Nullable private String name;
		@Nullable
		@Field(type = FieldType.Keyword) private String text;
		@Nullable
		@Field(type = FieldType.Float) private Float price;
		@Nullable private boolean available;
		@Nullable
		@Field(name = "sort-name", type = FieldType.Keyword) private String sortName;

		public Product(@Nullable String id, @Nullable String name, @Nullable String text, @Nullable Float price,
				boolean available, @Nullable String sortName) {
			this.id = id;
			this.name = name;
			this.text = text;
			this.price = price;
			this.available = available;
			this.sortName = sortName;
		}

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
		public String getText() {
			return text;
		}

		public void setText(@Nullable String text) {
			this.text = text;
		}

		@Nullable
		public Float getPrice() {
			return price;
		}

		public void setPrice(@Nullable Float price) {
			this.price = price;
		}

		public boolean isAvailable() {
			return available;
		}

		public void setAvailable(boolean available) {
			this.available = available;
		}

		@Nullable
		public String getSortName() {
			return sortName;
		}

		public void setSortName(@Nullable String sortName) {
			this.sortName = sortName;
		}
	}

	interface ProductRepository extends ElasticsearchRepository<Product, String> {

		List<Product> findByName(@Nullable String name);

		List<Product> findByNameAndText(String name, String text);

		List<Product> findByNameAndPrice(String name, Float price);

		List<Product> findByNameOrText(String name, String text);

		List<Product> findByNameOrPrice(String name, Float price);

		List<Product> findByAvailableTrue();

		List<Product> findByAvailableFalse();

		List<Product> findByPriceIn(List<Float> floats);

		List<Product> findByPriceNotIn(List<Float> floats);

		List<Product> findByPriceNot(float v);

		List<Product> findByPriceBetween(float v, float v1);

		List<Product> findByPriceLessThan(float v);

		List<Product> findByPriceLessThanEqual(float v);

		List<Product> findByPriceGreaterThan(float v);

		List<Product> findByPriceGreaterThanEqual(float v);

		List<Product> findByIdNotIn(List<String> strings);

		List<Product> findAllByNameOrderByText(String name);

		List<Product> findAllByNameOrderBySortName(String name);

		List<Product> findAllByOrderByText();

		List<Product> findAllByOrderBySortName();

		Product findFirstByName(String name);

		Product findTopByName(String name);

		List<Product> findFirst2ByName(String name);

		List<Product> findTop2ByName(String name);

		void deleteByName(@Nullable String name);

		List<Product> findAllByNameIn(List<String> names);

		SearchHits<Product> findByNameExists();

		SearchHits<Product> findByNameIsNull();

		SearchHits<Product> findByNameIsNotNull();

		SearchHits<Product> findByNameEmpty();

		SearchHits<Product> findByNameNotEmpty();

		Boolean existsByText(String text);
	}

}
