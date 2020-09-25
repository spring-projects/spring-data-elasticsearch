/*
 * Copyright 2016-2020 the original author or authors.
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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.elasticsearch.utils.IndexInitializer;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * base class for query keyword tests. Implemented by subclasses using ElasticsearchClient and ElasticsearchRestClient
 * based repositories.
 *
 * @author Artur Konczak
 * @author Christoph Strobl
 * @author Peter-Josef Meisch
 */
@RunWith(SpringRunner.class)
abstract class QueryKeywordsTests {

	@Autowired private ProductRepository repository;

	@Autowired private ElasticsearchOperations elasticsearchTemplate;

	@Before
	public void before() {

		IndexInitializer.init(elasticsearchTemplate, Product.class);

		Product product1 = Product.builder().id("1").name("Sugar").text("Cane sugar").price(1.0f).available(false)
				.sortName("sort5").build();
		Product product2 = Product.builder().id("2").name("Sugar").text("Cane sugar").price(1.2f).available(true)
				.sortName("sort4").build();
		Product product3 = Product.builder().id("3").name("Sugar").text("Beet sugar").price(1.1f).available(true)
				.sortName("sort3").build();
		Product product4 = Product.builder().id("4").name("Salt").text("Rock salt").price(1.9f).available(true)
				.sortName("sort2").build();
		Product product5 = Product.builder().id("5").name("Salt").text("Sea salt").price(2.1f).available(false)
				.sortName("sort1").build();

		repository.saveAll(Arrays.asList(product1, product2, product3, product4, product5));

		elasticsearchTemplate.refresh(Product.class);
	}

	@Test
	public void shouldSupportAND() {

		// given

		// when

		// then
		assertThat(repository.findByNameAndText("Sugar", "Cane sugar")).hasSize(2);
		assertThat(repository.findByNameAndPrice("Sugar", 1.1f)).hasSize(1);
	}

	@Test
	public void shouldSupportOR() {

		// given

		// when

		// then
		assertThat(repository.findByNameOrPrice("Sugar", 1.9f)).hasSize(4);
		assertThat(repository.findByNameOrText("Salt", "Beet sugar")).hasSize(3);
	}

	@Test
	public void shouldSupportTrueAndFalse() {

		// given

		// when

		// then
		assertThat(repository.findByAvailableTrue()).hasSize(3);
		assertThat(repository.findByAvailableFalse()).hasSize(2);
	}

	@Test
	public void shouldSupportInAndNotInAndNot() {

		// given

		// when

		// then
		assertThat(repository.findByPriceIn(Arrays.asList(1.2f, 1.1f))).hasSize(2);
		assertThat(repository.findByPriceNotIn(Arrays.asList(1.2f, 1.1f))).hasSize(3);
		assertThat(repository.findByPriceNot(1.2f)).hasSize(4);
	}

	@Test // DATAES-171
	public void shouldWorkWithNotIn() {

		// given

		// when

		// then
		assertThat(repository.findByIdNotIn(Arrays.asList("2", "3"))).hasSize(3);
	}

	@Test
	public void shouldSupportBetween() {

		// given

		// when

		// then
		assertThat(repository.findByPriceBetween(1.0f, 2.0f)).hasSize(4);
	}

	@Test
	public void shouldSupportLessThanAndGreaterThan() {

		// given

		// when

		// then
		assertThat(repository.findByPriceLessThan(1.1f)).hasSize(1);
		assertThat(repository.findByPriceLessThanEqual(1.1f)).hasSize(2);

		assertThat(repository.findByPriceGreaterThan(1.9f)).hasSize(1);
		assertThat(repository.findByPriceGreaterThanEqual(1.9f)).hasSize(2);
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

		assertThat(sortedIds).containsExactly("Beet sugar", "Cane sugar", "Cane sugar", "Rock salt", "Sea salt");
	}

	@Test // DATAES-615
	public void shouldSupportSortOnFieldWithCustomFieldNameWithoutCriteria() {

		List<String> sortedIds = repository.findAllByOrderBySortName().stream() //
				.map(it -> it.id).collect(Collectors.toList());

		assertThat(sortedIds).containsExactly("5", "4", "3", "2", "1");
	}

	@Test // DATAES-937
	public void shouldReturnEmptyListOnFindByIdWithEmptyInputList() {

		Iterable<Product> products = repository.findAllById(new ArrayList<>());

		assertThat(products).isEmpty();
	}

	@Test // DATAES-937
	public void shouldReturnEmptyListOnDerivedMethodWithEmptyInputList() {

		Iterable<Product> products = repository.findAllByNameIn(new ArrayList<>());

		assertThat(products).isEmpty();
	}

	/**
	 * @author Mohsin Husen
	 * @author Artur Konczak
	 */
	@Setter
	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	@Document(indexName = "test-index-product-query-keywords", type = "test-product-type", shards = 1, replicas = 0,
			refreshInterval = "-1")
	static class Product {

		@Id private String id;

		private List<String> title;

		private String name;

		private String description;

		@Field(type = FieldType.Keyword) private String text;

		private List<String> categories;

		private Float weight;

		@Field(type = FieldType.Float) private Float price;

		private Integer popularity;

		private boolean available;

		private String location;

		private Date lastModified;

		@Field(name = "sort-name", type = FieldType.Keyword) private String sortName;
	}

	/**
	 * Created by akonczak on 04/09/15.
	 */
	interface ProductRepository extends ElasticsearchRepository<Product, String> {

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

		List<Product> findAllByNameIn(List<String> names);
	}

}
