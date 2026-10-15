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

import static org.springframework.data.elasticsearch.core.IndexOperationsAdapter.*;

import reactor.test.StepVerifier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.data.elasticsearch.repositories.customidconversion.Product.ProductId;
import org.springframework.data.elasticsearch.repository.ReactiveElasticsearchRepository;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;

/**
 * Reactive version of {@link CustomIdConversionRepositoryIntegrationTests}. The reactive save operation must use the
 * same id conversion as the reactive read and delete operations.
 *
 * @author chuuminggg
 */
@SpringIntegrationTest
abstract class ReactiveCustomIdConversionRepositoryIntegrationTests {

	@Autowired ReactiveElasticsearchOperations operations;
	@Autowired ReactiveProductRepository repository;
	@Autowired IndexNameProvider indexNameProvider;

	@BeforeEach
	void before() {
		indexNameProvider.increment();
		blocking(operations.indexOps(Product.class)).createWithMapping();
	}

	@Test
	@Order(Integer.MAX_VALUE)
	void cleanup() {
		blocking(operations.indexOps(IndexCoordinates.of(indexNameProvider.getPrefix() + '*'))).delete();
	}

	@Test // #3359
	@DisplayName("should store the document with the converted id")
	void shouldStoreDocumentWithConvertedId() {

		repository.save(new Product(new ProductId("abc"), "name")).block();

		operations.exists("abc", Product.class) //
				.as(StepVerifier::create) //
				.expectNext(true) //
				.verifyComplete();
	}

	@Test // #3359
	@DisplayName("should find a saved entity by its id")
	void shouldFindSavedEntityById() {

		var id = new ProductId("abc");
		repository.save(new Product(id, "name")).block();

		repository.findById(id) //
				.as(StepVerifier::create) //
				.expectNextCount(1) //
				.verifyComplete();
	}

	interface ReactiveProductRepository extends ReactiveElasticsearchRepository<Product, ProductId> {}
}
