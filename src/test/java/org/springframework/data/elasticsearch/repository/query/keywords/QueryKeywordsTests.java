/*
 * Copyright 2016-2019 the original author or authors.
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

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.utils.IndexInitializer;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Artur Konczak
 * @author Christoph Strobl
 * @author Peter-Josef Meisch
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:/repository-query-keywords.xml")
public class QueryKeywordsTests {

	@Autowired private ProductRepository repository;

	@Autowired private ElasticsearchTemplate elasticsearchTemplate;

	@Before
	public void before() {

		IndexInitializer.init(elasticsearchTemplate, Product.class);

		repository.saveAll(
				Arrays.asList(Product.builder().id("1").name("Sugar").text("Cane sugar").price(1.0f).available(false).build(),
						Product.builder().id("2").name("Sugar").text("Cane sugar").price(1.2f).available(true).build(),
						Product.builder().id("3").name("Sugar").text("Beet sugar").price(1.1f).available(true).build(),
						Product.builder().id("4").name("Salt").text("Rock salt").price(1.9f).available(true).build(),
						Product.builder().id("5").name("Salt").text("Sea salt").price(2.1f).available(false).build()));

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

		private String text;

		private List<String> categories;

		private Float weight;

		@Field(type = FieldType.Float) private Float price;

		private Integer popularity;

		private boolean available;

		private String location;

		private Date lastModified;
	}

	/**
	 * Created by akonczak on 04/09/15.
	 */
	interface ProductRepository extends PagingAndSortingRepository<Product, String> {

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
	}

}
