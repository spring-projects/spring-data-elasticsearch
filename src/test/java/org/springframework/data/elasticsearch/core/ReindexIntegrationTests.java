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
import static org.springframework.data.elasticsearch.utils.IdGenerator.*;

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
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.reindex.ReindexRequest;
import org.springframework.data.elasticsearch.core.reindex.ReindexResponse;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.lang.Nullable;

/**
 * @author Peter-Josef Meisch
 */
@SpringIntegrationTest
public abstract class ReindexIntegrationTests {

	@Autowired private ElasticsearchOperations operations;
	@Autowired private IndexNameProvider indexNameProvider;

	@BeforeEach
	public void setup() {

		indexNameProvider.increment();
		operations.indexOps(Entity.class).createWithMapping();
	}

	@Test
	@Order(java.lang.Integer.MAX_VALUE)
	void cleanup() {
		operations.indexOps(IndexCoordinates.of(indexNameProvider.getPrefix() + "*")).delete();
	}

	@Test // #1529
	void shouldReindex() {

		String sourceIndexName = indexNameProvider.indexName();

		String documentId1 = nextIdAsString();
		Entity entity1 = new Entity();
		entity1.setId(documentId1);
		entity1.setMessage("abc");
		String documentId2 = nextIdAsString();
		Entity entity2 = new Entity();
		entity2.setId(documentId2);
		entity2.setMessage("abc");
		operations.save(entity1, entity2);

		indexNameProvider.increment();
		IndexCoordinates destIndex = IndexCoordinates.of(indexNameProvider.indexName());
		IndexOperations indexOpsNew = operations.indexOps(destIndex);
		indexOpsNew.create();
		indexOpsNew.putMapping(Entity.class);

		ReindexRequest reindexRequest = ReindexRequest.builder( //
				IndexCoordinates.of(sourceIndexName), //
				destIndex) //
				.withSourceQuery(queryForId(documentId1)) //
				.withScript("ctx._source.newMessage = ctx._source.remove(\"message\")", "painless").withRefresh(true) //
				.build(); //
		ReindexResponse reindex = operations.reindex(reindexRequest);

		assertThat(reindex.getTotal()).isEqualTo(1);
		assertThat(reindex.getCreated()).isEqualTo(1);
		assertThat(operations.count(operations.matchAllQuery(), destIndex)).isEqualTo(1);

		Entity newEntity = operations.get(documentId1, Entity.class, destIndex);
		assertThat(newEntity).isNotNull();
		assertThat(newEntity.getNewMessage()).isEqualTo(entity1.getMessage());
		assertThat(newEntity.getMessage()).isNull();
	}

	protected abstract Query queryForId(String id);

	@Test // #1529
	void shouldSubmitReindexTask() {

		String sourceIndexName = indexNameProvider.indexName();
		indexNameProvider.increment();
		String destIndexName = indexNameProvider.indexName();
		IndexOperations indexOpsNew = operations.indexOps(IndexCoordinates.of(destIndexName));
		indexOpsNew.create();
		indexOpsNew.putMapping(Entity.class);

		ReindexRequest reindexRequest = ReindexRequest.builder( //
				IndexCoordinates.of(sourceIndexName), //
				IndexCoordinates.of(destIndexName)) //
				.build();

		String task = operations.submitReindex(reindexRequest);

		assertThat(task).matches(Pattern.compile("^.*:\\d+$")); // nodeid:tasknr
	}

	@Document(indexName = "#{@indexNameProvider.indexName()}")
	static class Entity {
		@Nullable
		@Id private String id;
		@Nullable
		@Field(type = FieldType.Text) private String message;
		@Nullable
		@Field(type = FieldType.Text) private String newMessage;

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

		@Nullable
		public String getNewMessage() {
			return newMessage;
		}

		public void setNewMessage(@Nullable String newMessage) {
			this.newMessage = newMessage;
		}
	}
}
