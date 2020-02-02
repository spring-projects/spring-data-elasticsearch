/*
 * Copyright 2020 the original author or authors.
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
package org.springframework.data.elasticsearch.core.event;

import static org.assertj.core.api.Assertions.*;
import static org.elasticsearch.index.query.QueryBuilders.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.elasticsearch.action.index.IndexRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.core.AbstractElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.data.elasticsearch.core.query.UpdateQueryBuilder;
import org.springframework.data.mapping.callback.EntityCallbacks;

/**
 * base class for testing the events. Implemented for the different operation implementations.
 *
 * @author Peter-Josef Meisch
 */
abstract class ElasticsearchOperationsEventTest {

	private static final String INDEX_NAME = "index-event-test";

	protected final IndexCoordinates index = IndexCoordinates.of(INDEX_NAME);

	@Autowired protected ElasticsearchOperations operations;
	@Autowired private IndexOperations indexOperations;

	@BeforeEach
	void setUp() {
		indexOperations.deleteIndex(index.getIndexName());
		indexOperations.createIndex(index.getIndexName());
		indexOperations.putMapping(index, SampleEntity.class);
	}

	@AfterEach
	void tearDown() {
		indexOperations.deleteIndex(index.getIndexName());
	}

	@Test
	void shouldTriggerBeforeIndexCallbackOnSingleInsert() {
		AtomicInteger callCount = new AtomicInteger();
		BeforeIndexCallback<SampleEntity> beforeIndexCallback = (entity, index) -> {
			callCount.getAndIncrement();
			return entity;
		};
		EntityCallbacks callbacks = EntityCallbacks.create(beforeIndexCallback);
		((AbstractElasticsearchTemplate) operations).setEntityCallbacks(callbacks);

		IndexQuery indexQuery = getIndexQuery(new SampleEntity("1"));

		operations.index(indexQuery, index);

		assertThat(callCount).hasValue(1);
	}

	@Test
	void shouldTriggerBeforeIndexCallbackOnBulkInsert() {
		AtomicInteger callCount = new AtomicInteger();
		BeforeIndexCallback<SampleEntity> beforeIndexCallback = (entity, index) -> {
			callCount.getAndIncrement();
			return entity;
		};
		EntityCallbacks callbacks = EntityCallbacks.create(beforeIndexCallback);
		((AbstractElasticsearchTemplate) operations).setEntityCallbacks(callbacks);

		List<IndexQuery> indexQueries = new ArrayList<>();
		indexQueries.add(getIndexQuery(new SampleEntity("1")));
		indexQueries.add(getIndexQuery(new SampleEntity("1")));
		operations.bulkIndex(indexQueries, index);

		assertThat(callCount).hasValue(2);
	}

	@Test
	void shouldTriggerBeforeUpdateCallbackOnSingleUpdate() {
		AtomicInteger callCount = new AtomicInteger();
		BeforeUpdateCallback beforeUpdateCallback = (updateInfo, id, index) -> {
			callCount.getAndIncrement();
			return updateInfo;
		};
		EntityCallbacks callbacks = EntityCallbacks.create(beforeUpdateCallback);
		((AbstractElasticsearchTemplate) operations).setEntityCallbacks(callbacks);

		IndexQuery indexQuery = getIndexQuery(new SampleEntity("1"));
		operations.index(indexQuery, index);
		indexOperations.refresh(index);

		IndexRequest indexRequest = new IndexRequest();
		indexRequest.source("text", "some text");
		UpdateQuery updateQuery = new UpdateQueryBuilder().withId("1").withIndexRequest(indexRequest).build();

		operations.update(updateQuery, index);

		assertThat(callCount).hasValue(1);
	}

	@Test
	void shouldTriggerBeforeUpdateCallbackOnBulkUpdate() {
		AtomicInteger callCount = new AtomicInteger();
		BeforeUpdateCallback beforeUpdateCallback = (updates, id, index) -> {
			callCount.getAndIncrement();
			return updates;
		};
		EntityCallbacks callbacks = EntityCallbacks.create(beforeUpdateCallback);
		((AbstractElasticsearchTemplate) operations).setEntityCallbacks(callbacks);

		List<IndexQuery> indexQueries = new ArrayList<>();
		indexQueries.add(getIndexQuery(new SampleEntity("1")));
		indexQueries.add(getIndexQuery(new SampleEntity("2")));
		operations.bulkIndex(indexQueries, index);
		indexOperations.refresh(index);

		List<UpdateQuery> updateQueries = new ArrayList<>();
		IndexRequest indexRequest1 = new IndexRequest();
		indexRequest1.source("text", "some text");
		updateQueries.add(new UpdateQueryBuilder().withId("1").withIndexRequest(indexRequest1).build());
		IndexRequest indexRequest2 = new IndexRequest();
		indexRequest2.source("text", "some text");
		updateQueries.add(new UpdateQueryBuilder().withId("2").withIndexRequest(indexRequest2).build());

		operations.bulkUpdate(updateQueries, index);

		assertThat(callCount).hasValue(2);
	}

	@Test
	void shouldTriggerBeforeDeleteCallbackOnSingleDelete() {
		AtomicInteger callCount = new AtomicInteger();
		BeforeDeleteCallback beforeDeleteCallback = (id, index) -> {
			callCount.getAndIncrement();
			return id;
		};
		EntityCallbacks callbacks = EntityCallbacks.create(beforeDeleteCallback);
		((AbstractElasticsearchTemplate) operations).setEntityCallbacks(callbacks);

		IndexQuery indexQuery = getIndexQuery(new SampleEntity("1"));
		operations.index(indexQuery, index);
		indexOperations.refresh(index);

		operations.delete("1", index);

		assertThat(callCount).hasValue(1);
	}

	@Test
	void shouldTriggerBeforeDeleteCallbackOnQueryDelete() {
		AtomicInteger callCount = new AtomicInteger();
		BeforeDeleteQueryCallback beforeDeleteQueryCallback = (query, index) -> {
			callCount.getAndIncrement();
			return query;
		};
		EntityCallbacks callbacks = EntityCallbacks.create(beforeDeleteQueryCallback);
		((AbstractElasticsearchTemplate) operations).setEntityCallbacks(callbacks);

		IndexQuery indexQuery = getIndexQuery(new SampleEntity("1"));
		operations.index(indexQuery, index);
		indexOperations.refresh(index);

		NativeSearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery()).build();
		operations.delete(searchQuery, SampleEntity.class, index);

		assertThat(callCount).hasValue(1);
	}

	private IndexQuery getIndexQuery(SampleEntity sampleEntity) {
		return new IndexQueryBuilder().withId(sampleEntity.getId()).withObject(sampleEntity).build();
	}

	@Document(indexName = INDEX_NAME)
	private static class SampleEntity {
		@Id private String id;

		public SampleEntity() {}

		public SampleEntity(String id) {
			this(id, null);
		}

		public SampleEntity(String id, String text) {
			this.id = id;
			this.text = text;
		}

		private String text;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}
	}
}
