/*
 * Copyright 2018-2019 the original author or authors.
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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
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
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.Score;
import org.springframework.data.elasticsearch.annotations.ScriptedField;
import org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.StringQuery;

/**
 * @author Christoph Strobl
 * @currentRead Fool's Fate - Robin Hobb
 * @author Peter-Josef Meisch
 */
public class ReactiveElasticsearchTemplateUnitTests {

	@Rule //
	public MockitoRule rule = MockitoJUnit.rule();

	@Mock ReactiveElasticsearchClient client;
	ReactiveElasticsearchTemplate template;

	@Before
	public void setUp() {

		template = new ReactiveElasticsearchTemplate(client);
	}

	@Test // DATAES-504
	public void insertShouldUseDefaultRefreshPolicy() {

		ArgumentCaptor<IndexRequest> captor = ArgumentCaptor.forClass(IndexRequest.class);
		when(client.index(captor.capture())).thenReturn(Mono.empty());

		template.save(Collections.singletonMap("key", "value"), "index", "type") //
				.as(StepVerifier::create) //
				.verifyComplete();

		assertThat(captor.getValue().getRefreshPolicy()).isEqualTo(RefreshPolicy.IMMEDIATE);
	}

	@Test // DATAES-504
	public void insertShouldApplyRefreshPolicy() {

		ArgumentCaptor<IndexRequest> captor = ArgumentCaptor.forClass(IndexRequest.class);
		when(client.index(captor.capture())).thenReturn(Mono.empty());

		template.setRefreshPolicy(RefreshPolicy.WAIT_UNTIL);

		template.save(Collections.singletonMap("key", "value"), "index", "type") //
				.as(StepVerifier::create) //
				.verifyComplete();

		assertThat(captor.getValue().getRefreshPolicy()).isEqualTo(RefreshPolicy.WAIT_UNTIL);
	}

	@Test // DATAES-504, DATAES-518
	public void findShouldFallBackToDefaultIndexOptionsIfNotSet() {

		ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
		when(client.search(captor.capture())).thenReturn(Flux.empty());

		template.find(new CriteriaQuery(new Criteria("*")).setPageable(PageRequest.of(0, 10)), SampleEntity.class) //
				.as(StepVerifier::create) //
				.verifyComplete();

		assertThat(captor.getValue().indicesOptions()).isEqualTo(DEFAULT_INDICES_OPTIONS);
	}

	@Test // DATAES-504, DATAES-518
	public void findShouldApplyIndexOptionsIfSet() {

		ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
		when(client.search(captor.capture())).thenReturn(Flux.empty());

		template.setIndicesOptions(IndicesOptions.LENIENT_EXPAND_OPEN);

		template.find(new CriteriaQuery(new Criteria("*")).setPageable(PageRequest.of(0, 10)), SampleEntity.class) //
				.as(StepVerifier::create) //
				.verifyComplete();

		assertThat(captor.getValue().indicesOptions()).isEqualTo(IndicesOptions.LENIENT_EXPAND_OPEN);
	}

	@Test // DATAES-504
	public void findShouldApplyPaginationIfSet() {

		ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
		when(client.search(captor.capture())).thenReturn(Flux.empty());

		template.find(new CriteriaQuery(new Criteria("*")).setPageable(PageRequest.of(2, 50)), SampleEntity.class) //
				.as(StepVerifier::create) //
				.verifyComplete();

		assertThat(captor.getValue().source().from()).isEqualTo(100);
		assertThat(captor.getValue().source().size()).isEqualTo(50);
	}

	@Test // DATAES-504, DATAES-518
	public void findShouldUseScrollIfPaginationNotSet() {

		ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
		when(client.scroll(captor.capture())).thenReturn(Flux.empty());

		template.find(new CriteriaQuery(new Criteria("*")).setPageable(Pageable.unpaged()), SampleEntity.class) //
				.as(StepVerifier::create) //
				.verifyComplete();

		verify(client).scroll(any());
	}

	@Test // DATAES-504
	public void deleteShouldUseDefaultRefreshPolicy() {

		ArgumentCaptor<DeleteRequest> captor = ArgumentCaptor.forClass(DeleteRequest.class);
		when(client.delete(captor.capture())).thenReturn(Mono.empty());

		template.deleteById("id", "index", "type") //
				.as(StepVerifier::create) //
				.verifyComplete();

		assertThat(captor.getValue().getRefreshPolicy()).isEqualTo(RefreshPolicy.IMMEDIATE);
	}

	@Test // DATAES-504
	public void deleteShouldApplyRefreshPolicy() {

		ArgumentCaptor<DeleteRequest> captor = ArgumentCaptor.forClass(DeleteRequest.class);
		when(client.delete(captor.capture())).thenReturn(Mono.empty());

		template.setRefreshPolicy(RefreshPolicy.WAIT_UNTIL);

		template.deleteById("id", "index", "type") //
				.as(StepVerifier::create) //
				.verifyComplete();

		assertThat(captor.getValue().getRefreshPolicy()).isEqualTo(RefreshPolicy.WAIT_UNTIL);
	}

	@Test // DATAES-504
	public void deleteByShouldUseDefaultRefreshPolicy() {

		ArgumentCaptor<DeleteByQueryRequest> captor = ArgumentCaptor.forClass(DeleteByQueryRequest.class);
		when(client.deleteBy(captor.capture())).thenReturn(Mono.empty());

		template.deleteBy(new StringQuery(QueryBuilders.matchAllQuery().toString()), Object.class, "index", "type") //
				.as(StepVerifier::create) //
				.verifyComplete();

		assertThat(captor.getValue().isRefresh()).isTrue();
	}

	@Test // DATAES-504
	public void deleteByShouldApplyRefreshPolicy() {

		ArgumentCaptor<DeleteByQueryRequest> captor = ArgumentCaptor.forClass(DeleteByQueryRequest.class);
		when(client.deleteBy(captor.capture())).thenReturn(Mono.empty());

		template.setRefreshPolicy(RefreshPolicy.NONE);

		template.deleteBy(new StringQuery(QueryBuilders.matchAllQuery().toString()), Object.class, "index", "type") //
				.as(StepVerifier::create) //
				.verifyComplete();

		assertThat(captor.getValue().isRefresh()).isFalse();
	}

	@Test // DATAES-504
	public void deleteByShouldApplyIndicesOptions() {

		ArgumentCaptor<DeleteByQueryRequest> captor = ArgumentCaptor.forClass(DeleteByQueryRequest.class);
		when(client.deleteBy(captor.capture())).thenReturn(Mono.empty());

		template.deleteBy(new StringQuery(QueryBuilders.matchAllQuery().toString()), Object.class, "index", "type") //
				.as(StepVerifier::create) //
				.verifyComplete();

		assertThat(captor.getValue().indicesOptions()).isEqualTo(DEFAULT_INDICES_OPTIONS);
	}

	@Test // DATAES-504
	public void deleteByShouldApplyIndicesOptionsIfSet() {

		ArgumentCaptor<DeleteByQueryRequest> captor = ArgumentCaptor.forClass(DeleteByQueryRequest.class);
		when(client.deleteBy(captor.capture())).thenReturn(Mono.empty());

		template.setIndicesOptions(IndicesOptions.LENIENT_EXPAND_OPEN);

		template.deleteBy(new StringQuery(QueryBuilders.matchAllQuery().toString()), Object.class, "index", "type") //
				.as(StepVerifier::create) //
				.verifyComplete();

		assertThat(captor.getValue().indicesOptions()).isEqualTo(IndicesOptions.LENIENT_EXPAND_OPEN);
	}

	/**
	 * @author Rizwan Idrees
	 * @author Mohsin Husen
	 * @author Chris White
	 * @author Sascha Woo
	 */
	@Setter
	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	@ToString
	@Builder
	@Document(indexName = "test-index-sample-core-reactive-template-Unit", type = "test-type", shards = 1, replicas = 0, refreshInterval = "-1")
	static class SampleEntity {

		@Id private String id;
		@Field(type = Text, store = true, fielddata = true) private String type;
		@Field(type = Text, store = true, fielddata = true) private String message;
		private int rate;
		@ScriptedField private Double scriptedRate;
		private boolean available;
		private String highlightedMessage;
		private GeoPoint location;
		@Version private Long version;
		@Score private float score;

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;

			SampleEntity that = (SampleEntity) o;

			if (available != that.available)
				return false;
			if (rate != that.rate)
				return false;
			if (highlightedMessage != null ? !highlightedMessage.equals(that.highlightedMessage)
					: that.highlightedMessage != null)
				return false;
			if (id != null ? !id.equals(that.id) : that.id != null)
				return false;
			if (location != null ? !location.equals(that.location) : that.location != null)
				return false;
			if (message != null ? !message.equals(that.message) : that.message != null)
				return false;
			if (type != null ? !type.equals(that.type) : that.type != null)
				return false;
			if (version != null ? !version.equals(that.version) : that.version != null)
				return false;

			return true;
		}

		@Override
		public int hashCode() {
			int result = id != null ? id.hashCode() : 0;
			result = 31 * result + (type != null ? type.hashCode() : 0);
			result = 31 * result + (message != null ? message.hashCode() : 0);
			result = 31 * result + rate;
			result = 31 * result + (available ? 1 : 0);
			result = 31 * result + (highlightedMessage != null ? highlightedMessage.hashCode() : 0);
			result = 31 * result + (location != null ? location.hashCode() : 0);
			result = 31 * result + (version != null ? version.hashCode() : 0);
			return result;
		}
	}
}
