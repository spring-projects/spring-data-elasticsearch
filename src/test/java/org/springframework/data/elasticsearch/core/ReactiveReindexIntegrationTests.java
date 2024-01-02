/*
 * Copyright 2022-2024 the original author or authors.
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
import static org.springframework.data.elasticsearch.core.IndexOperationsAdapter.*;
import static org.springframework.data.elasticsearch.utils.IdGenerator.*;

import reactor.test.StepVerifier;

import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.reindex.ReindexRequest;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.lang.Nullable;

/**
 * x * Note: the imperative version of these tests have more details and test methods, but they test that the request is
 * built correctly. The same method from the {@link org.springframework.data.elasticsearch.client.elc.RequestConverter}
 * are used here, so there is no need to test this more than once.
 *
 * @author Peter-Josef Meisch
 * @since 4.4
 */
@SpringIntegrationTest
public abstract class ReactiveReindexIntegrationTests {

	@Autowired private ReactiveElasticsearchOperations operations;
	@Autowired private IndexNameProvider indexNameProvider;

	@BeforeEach
	public void beforeEach() {

		indexNameProvider.increment();
		blocking(operations.indexOps(Entity.class)).createWithMapping();
	}

	@Test
	@Order(java.lang.Integer.MAX_VALUE)
	void cleanup() {
		blocking(operations.indexOps(IndexCoordinates.of(indexNameProvider.getPrefix() + '*'))).delete();
	}

	@Test
	// #1529
	void shouldReindex() {

		String sourceIndexName = indexNameProvider.indexName();
		String documentId = nextIdAsString();

		Entity entity = new Entity();
		entity.setId(documentId);
		entity.setMessage("abc");
		operations.save(entity) //
				.as(StepVerifier::create) //
				.expectNextCount(1) //
				.verifyComplete();

		indexNameProvider.increment();
		String destIndexName = indexNameProvider.indexName();
		ReactiveIndexOperations indexOpsNew = operations.indexOps(IndexCoordinates.of(destIndexName));
		indexOpsNew.create() //
				.then(indexOpsNew.putMapping(Entity.class)) //
				.as(StepVerifier::create) //
				.expectNext(true) //
				.verifyComplete();

		ReindexRequest reindexRequest = ReindexRequest.builder( //
				IndexCoordinates.of(sourceIndexName), //
				IndexCoordinates.of(destIndexName)) //
				.withRefresh(true) //
				.build(); //

		operations.reindex(reindexRequest) //
				.as(StepVerifier::create) //
				.consumeNextWith(reindexResponse -> {
					assertThat(reindexResponse.getTotal()).isEqualTo(1L);
					assertThat(reindexResponse.getCreated()).isEqualTo(1L);
				}) //
				.verifyComplete();

		operations.count(operations.matchAllQuery(), Entity.class, IndexCoordinates.of(destIndexName)) //
				.as(StepVerifier::create) //
				.expectNext(1L) //
				.verifyComplete();
	}

	@Test
	// #1529
	void shouldSubmitReindexTask() {

		String sourceIndexName = indexNameProvider.indexName();
		indexNameProvider.increment();
		String destIndexName = indexNameProvider.indexName();
		ReactiveIndexOperations indexOpsNew = operations.indexOps(IndexCoordinates.of(destIndexName));
		indexOpsNew.create() //
				.then(indexOpsNew.putMapping(Entity.class)).as(StepVerifier::create) //
				.expectNext(true) //
				.verifyComplete();

		ReindexRequest reindexRequest = ReindexRequest.builder( //
				IndexCoordinates.of(sourceIndexName), //
				IndexCoordinates.of(destIndexName)) //
				.build();

		operations.submitReindex(reindexRequest) //
				.as(StepVerifier::create) //
				.consumeNextWith(task -> assertThat(task).matches(Pattern.compile("^.*:\\d+$"))) //
				.verifyComplete();
	}

	@Document(indexName = "#{@indexNameProvider.indexName()}")
	static class Entity {
		@Nullable
		@Id private String id;
		@Nullable
		@Field(type = FieldType.Text) private String message;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getMessage() {
			return message;
		}

		public void setMessage(@Nullable String message) {
			this.message = message;
		}
	}
}
