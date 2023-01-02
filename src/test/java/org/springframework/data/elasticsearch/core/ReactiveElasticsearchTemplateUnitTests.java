/*
 * Copyright 2018-2023 the original author or authors.
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
import static org.elasticsearch.action.search.SearchRequest.*;
import static org.mockito.Mockito.*;
import static org.springframework.data.elasticsearch.annotations.FieldType.*;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.lang.Double;
import java.lang.Long;
import java.lang.Object;
import java.util.Collections;

import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.ScriptedField;
import org.springframework.data.elasticsearch.client.erhlc.ReactiveElasticsearchClient;
import org.springframework.data.elasticsearch.client.erhlc.ReactiveElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.lang.Nullable;

/**
 * @author Christoph Strobl
 * @author Peter-Josef Meisch
 */
@ExtendWith(MockitoExtension.class)
public class ReactiveElasticsearchTemplateUnitTests {

	@Mock ReactiveElasticsearchClient client;
	ReactiveElasticsearchTemplate template;

	private IndexCoordinates index = IndexCoordinates.of("index");

	@BeforeEach
	public void setUp() {
		template = new ReactiveElasticsearchTemplate(client);
	}

	@Test // DATAES-504
	public void insertShouldUseDefaultRefreshPolicy() {

		ArgumentCaptor<IndexRequest> captor = ArgumentCaptor.forClass(IndexRequest.class);
		when(client.index(captor.capture())).thenReturn(Mono.empty());

		template.save(Collections.singletonMap("key", "value"), index) //
				.as(StepVerifier::create) //
				.verifyComplete();

		assertThat(captor.getValue().getRefreshPolicy()).isEqualTo(WriteRequest.RefreshPolicy.NONE);
	}

	@Test // DATAES-504
	public void insertShouldApplyRefreshPolicy() {

		ArgumentCaptor<IndexRequest> captor = ArgumentCaptor.forClass(IndexRequest.class);
		when(client.index(captor.capture())).thenReturn(Mono.empty());

		template.setRefreshPolicy(RefreshPolicy.WAIT_UNTIL);

		template.save(Collections.singletonMap("key", "value"), index) //
				.as(StepVerifier::create) //
				.verifyComplete();

		assertThat(captor.getValue().getRefreshPolicy()).isEqualTo(WriteRequest.RefreshPolicy.WAIT_UNTIL);
	}

	@Test // DATAES-504, DATAES-518
	public void searchShouldFallBackToDefaultIndexOptionsIfNotSet() {

		ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
		when(client.search(captor.capture())).thenReturn(Flux.empty());

		template.search(new CriteriaQuery(new Criteria("*")).setPageable(PageRequest.of(0, 10)), SampleEntity.class) //
				.as(StepVerifier::create) //
				.verifyComplete();

		assertThat(captor.getValue().indicesOptions()).isEqualTo(DEFAULT_INDICES_OPTIONS);
	}

	@Test // DATAES-504, DATAES-518
	public void searchShouldApplyIndexOptionsIfSet() {

		ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
		when(client.search(captor.capture())).thenReturn(Flux.empty());

		template.setIndicesOptions(IndicesOptions.LENIENT_EXPAND_OPEN);

		Query query = new CriteriaQuery(new Criteria("*")).setPageable(PageRequest.of(0, 10));
		template.search(query, SampleEntity.class, index) //
				.as(StepVerifier::create) //
				.verifyComplete();

		assertThat(captor.getValue().indicesOptions()).isEqualTo(IndicesOptions.LENIENT_EXPAND_OPEN);
	}

	@Test // DATAES-504
	public void searchShouldApplyPaginationIfSet() {

		ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
		when(client.search(captor.capture())).thenReturn(Flux.empty());

		Query query = new CriteriaQuery(new Criteria("*")).setPageable(PageRequest.of(2, 50));
		template.search(query, SampleEntity.class, index) //
				.as(StepVerifier::create) //
				.verifyComplete();

		assertThat(captor.getValue().source().from()).isEqualTo(100);
		assertThat(captor.getValue().source().size()).isEqualTo(50);
	}

	@Test // DATAES-504, DATAES-518
	public void searchShouldUseScrollIfPaginationNotSet() {

		ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
		when(client.scroll(captor.capture())).thenReturn(Flux.empty());

		template.search(new CriteriaQuery(new Criteria("*")).setPageable(Pageable.unpaged()), SampleEntity.class) //
				.as(StepVerifier::create) //
				.verifyComplete();

		verify(client).scroll(any());
	}

	@Test // DATAES-504
	public void deleteShouldUseDefaultRefreshPolicy() {

		ArgumentCaptor<DeleteRequest> captor = ArgumentCaptor.forClass(DeleteRequest.class);
		when(client.delete(captor.capture())).thenReturn(Mono.empty());

		template.delete("id", index) //
				.as(StepVerifier::create) //
				.verifyComplete();

		assertThat(captor.getValue().getRefreshPolicy()).isEqualTo(WriteRequest.RefreshPolicy.NONE);
	}

	@Test // DATAES-504
	public void deleteShouldApplyRefreshPolicy() {

		ArgumentCaptor<DeleteRequest> captor = ArgumentCaptor.forClass(DeleteRequest.class);
		when(client.delete(captor.capture())).thenReturn(Mono.empty());

		template.setRefreshPolicy(RefreshPolicy.WAIT_UNTIL);

		template.delete("id", index) //
				.as(StepVerifier::create) //
				.verifyComplete();

		assertThat(captor.getValue().getRefreshPolicy()).isEqualTo(WriteRequest.RefreshPolicy.WAIT_UNTIL);
	}

	@Test // DATAES-504
	public void deleteByShouldUseDefaultRefreshPolicy() {

		ArgumentCaptor<DeleteByQueryRequest> captor = ArgumentCaptor.forClass(DeleteByQueryRequest.class);
		when(client.deleteBy(captor.capture())).thenReturn(Mono.empty());

		template.delete(new StringQuery(QueryBuilders.matchAllQuery().toString()), Object.class, index) //
				.as(StepVerifier::create) //
				.verifyComplete();

		assertThat(captor.getValue().isRefresh()).isFalse();
	}

	@Test // DATAES-504
	public void deleteByShouldApplyRefreshPolicy() {

		ArgumentCaptor<DeleteByQueryRequest> captor = ArgumentCaptor.forClass(DeleteByQueryRequest.class);
		when(client.deleteBy(captor.capture())).thenReturn(Mono.empty());

		template.setRefreshPolicy(RefreshPolicy.IMMEDIATE);

		template.delete(new StringQuery(QueryBuilders.matchAllQuery().toString()), Object.class, index) //
				.as(StepVerifier::create) //
				.verifyComplete();

		assertThat(captor.getValue().isRefresh()).isTrue();
	}

	@Test // DATAES-504
	public void deleteByShouldApplyIndicesOptions() {

		ArgumentCaptor<DeleteByQueryRequest> captor = ArgumentCaptor.forClass(DeleteByQueryRequest.class);
		when(client.deleteBy(captor.capture())).thenReturn(Mono.empty());

		template.delete(new StringQuery(QueryBuilders.matchAllQuery().toString()), Object.class, index) //
				.as(StepVerifier::create) //
				.verifyComplete();

		assertThat(captor.getValue().indicesOptions()).isEqualTo(DEFAULT_INDICES_OPTIONS);
	}

	@Test // DATAES-504
	public void deleteByShouldApplyIndicesOptionsIfSet() {

		ArgumentCaptor<DeleteByQueryRequest> captor = ArgumentCaptor.forClass(DeleteByQueryRequest.class);
		when(client.deleteBy(captor.capture())).thenReturn(Mono.empty());

		template.setIndicesOptions(IndicesOptions.LENIENT_EXPAND_OPEN);

		template.delete(new StringQuery(QueryBuilders.matchAllQuery().toString()), Object.class, index) //
				.as(StepVerifier::create) //
				.verifyComplete();

		assertThat(captor.getValue().indicesOptions()).isEqualTo(IndicesOptions.LENIENT_EXPAND_OPEN);
	}

	@Document(indexName = "test-index-sample-core-reactive-template-Unit")
	static class SampleEntity {

		@Nullable
		@Id private String id;
		@Nullable
		@Field(type = Text, store = true, fielddata = true) private String type;
		@Nullable
		@Field(type = Text, store = true, fielddata = true) private String message;
		@Nullable private int rate;
		@Nullable
		@ScriptedField private Double scriptedRate;
		@Nullable private boolean available;
		@Nullable private String highlightedMessage;
		@Nullable private GeoPoint location;
		@Nullable
		@Version private Long version;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}

		public int getRate() {
			return rate;
		}

		public void setRate(int rate) {
			this.rate = rate;
		}

		public java.lang.Double getScriptedRate() {
			return scriptedRate;
		}

		public void setScriptedRate(java.lang.Double scriptedRate) {
			this.scriptedRate = scriptedRate;
		}

		public boolean isAvailable() {
			return available;
		}

		public void setAvailable(boolean available) {
			this.available = available;
		}

		public String getHighlightedMessage() {
			return highlightedMessage;
		}

		public void setHighlightedMessage(String highlightedMessage) {
			this.highlightedMessage = highlightedMessage;
		}

		public GeoPoint getLocation() {
			return location;
		}

		public void setLocation(GeoPoint location) {
			this.location = location;
		}

		public java.lang.Long getVersion() {
			return version;
		}

		public void setVersion(java.lang.Long version) {
			this.version = version;
		}
	}
}
