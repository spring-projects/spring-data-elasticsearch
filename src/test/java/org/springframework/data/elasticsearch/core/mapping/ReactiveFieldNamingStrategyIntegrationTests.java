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
package org.springframework.data.elasticsearch.core.mapping;

import static org.springframework.data.elasticsearch.core.IndexOperationsAdapter.*;

import reactor.test.StepVerifier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.lang.Nullable;

/**
 * @author Peter-Josef Meisch
 */
@SpringIntegrationTest
public abstract class ReactiveFieldNamingStrategyIntegrationTests {

	@Autowired private ReactiveElasticsearchOperations operations;
	@Autowired private IndexNameProvider indexNameProvider;

	@BeforeEach
	void setUp() {
		indexNameProvider.increment();
		var indexOps = blocking(operations.indexOps(Entity.class));
		indexOps.delete();
		indexOps.createWithMapping();
	}

	@Test
	@Order(java.lang.Integer.MAX_VALUE)
	void cleanup() {
		blocking(operations.indexOps(IndexCoordinates.of(indexNameProvider.getPrefix() + '*'))).delete();
	}

	@Test // #1565
	@DisplayName("should use configured FieldNameStrategy")
	void shouldUseConfiguredFieldNameStrategy() {

		Entity entity = new Entity();
		entity.setId("42");
		entity.setSomeText("the text to be searched");
		operations.save(entity).block();

		// use a native query here to prevent automatic property name matching
		Query query = nativeMatchQuery("some_text", "searched");
		operations.search(query, Entity.class) //
				.as(StepVerifier::create) //
				.expectNextCount(1) //
				.verifyComplete();
	}

	abstract protected Query nativeMatchQuery(String fieldName, String value);

	@Document(indexName = "#{@indexNameProvider.indexName()}")
	static class Entity {
		@Nullable
		@Id private String id;
		@Nullable
		@Field(type = FieldType.Text) private String someText;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getSomeText() {
			return someText;
		}

		public void setSomeText(@Nullable String someText) {
			this.someText = someText;
		}
	}
}
