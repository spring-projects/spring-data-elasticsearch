/*
 * Copyright 2026-present the original author or authors.
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
package org.springframework.data.elasticsearch.repositories.customidconversion;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.data.elasticsearch.repositories.customidconversion.Product.ProductId;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;

/**
 * Imperative counterpart of {@link ReactiveCustomIdConversionRepositoryIntegrationTests}: entities with an id that is
 * converted by a custom converter can be found after saving them.
 *
 * @author Minju Kim
 */
@SpringIntegrationTest
abstract class CustomIdConversionRepositoryIntegrationTests {

	@Autowired ElasticsearchOperations operations;
	@Autowired ProductRepository repository;
	@Autowired IndexNameProvider indexNameProvider;

	@BeforeEach
	void before() {
		indexNameProvider.increment();
		operations.indexOps(Product.class).createWithMapping();
	}

	@Test
	@Order(Integer.MAX_VALUE)
	void cleanup() {
		operations.indexOps(IndexCoordinates.of(indexNameProvider.getPrefix() + '*')).delete();
	}

	@Test // #3359
	@DisplayName("should store the document with the converted id")
	void shouldStoreDocumentWithConvertedId() {

		repository.save(new Product(new ProductId("abc"), "name"));

		assertThat(operations.exists("abc", Product.class)).isTrue();
	}

	@Test // #3359
	@DisplayName("should find a saved entity by its id")
	void shouldFindSavedEntityById() {

		var id = new ProductId("abc");
		repository.save(new Product(id, "name"));

		assertThat(repository.findById(id)).isPresent();
	}

	interface ProductRepository extends ElasticsearchRepository<Product, ProductId> {}
}
