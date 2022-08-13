/*
 * Copyright 2018-2022 the original author or authors.
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
import static org.mockito.Mockito.*;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.lucene.search.TotalHits;
import org.elasticsearch.Version;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetItemResponse;
import org.elasticsearch.action.get.MultiGetRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.main.MainResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.index.get.GetResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.erhlc.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.client.erhlc.ReactiveElasticsearchClient;
import org.springframework.data.elasticsearch.client.erhlc.ReactiveElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.event.ReactiveAfterConvertCallback;
import org.springframework.data.elasticsearch.core.event.ReactiveAfterSaveCallback;
import org.springframework.data.elasticsearch.core.event.ReactiveBeforeConvertCallback;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.mapping.callback.ReactiveEntityCallbacks;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

/**
 * @author Roman Puchkovskiy
 * @author Peter-Josef Meisch
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ReactiveElasticsearchTemplateCallbackTests {

	private ReactiveElasticsearchTemplate template;

	@Mock private ReactiveElasticsearchClient client;

	@Mock private IndexResponse indexResponse;
	@Mock private BulkResponse bulkResponse;
	@Mock private BulkItemResponse bulkItemResponse;
	@Mock private DocWriteResponse docWriteResponse;
	@Mock private GetResult getResult;
	@Mock private GetResponse getResponse;
	@Mock private MultiGetItemResponse multiGetItemResponse;
	@Mock private org.elasticsearch.search.SearchHit searchHit;
	@Mock private org.elasticsearch.action.search.SearchResponse searchResponse;

	private final IndexCoordinates index = IndexCoordinates.of("index");

	@Spy private ValueCapturingAfterSaveCallback afterSaveCallback = new ValueCapturingAfterSaveCallback();
	@Spy private ValueCapturingAfterConvertCallback afterConvertCallback = new ValueCapturingAfterConvertCallback();
	@Spy private ValueCapturingBeforeConvertCallback beforeConvertCallback = new ValueCapturingBeforeConvertCallback();

	@BeforeEach
	public void setUp() {
		when(client.info()).thenReturn(
				Mono.just(new MainResponse("mockNodename", Version.CURRENT, new ClusterName("mockCluster"), "mockUuid", null)));

		template = new ReactiveElasticsearchTemplate(client);

		when(client.index(any(IndexRequest.class))).thenReturn(Mono.just(indexResponse));
		doReturn("response-id").when(indexResponse).getId();

		when(client.bulk(any(BulkRequest.class))).thenReturn(Mono.just(bulkResponse));
		doReturn(new BulkItemResponse[] { bulkItemResponse, bulkItemResponse }).when(bulkResponse).getItems();
		doReturn(docWriteResponse).when(bulkItemResponse).getResponse();
		doReturn("response-id").when(docWriteResponse).getId();

		doReturn(true).when(getResult).isExists();
		doReturn(false).when(getResult).isSourceEmpty();
		doReturn(new HashMap<String, Object>() {
			{
				put("id", "init");
				put("firstname", "luke");
			}
		}).when(getResult).getSource();

		doReturn(true).when(getResponse).isExists();
		doReturn(new HashMap<String, Object>() {
			{
				put("id", "init");
				put("firstname", "luke");
			}
		}).when(getResponse).getSourceAsMap();
		doReturn(getResponse).when(multiGetItemResponse).getResponse();
		when(client.multiGet(any(MultiGetRequest.class))).thenReturn(Flux.just(multiGetItemResponse, multiGetItemResponse));

		doReturn(Mono.just(getResult)).when(client).get(any(GetRequest.class));

		when(client.search(any(SearchRequest.class))).thenReturn(Flux.just(searchHit, searchHit));
		when(client.searchForResponse(any(SearchRequest.class))).thenReturn(Mono.just(searchResponse));

		when(searchResponse.getHits()).thenReturn(
				new org.elasticsearch.search.SearchHits(new org.elasticsearch.search.SearchHit[] { searchHit, searchHit },
						new TotalHits(2, TotalHits.Relation.EQUAL_TO), 1.0f));

		doReturn(new BytesArray(new byte[8])).when(searchHit).getSourceRef();
		doReturn(new HashMap<String, Object>() {
			{
				put("id", "init");
				put("firstname", "luke");
			}
		}).when(searchHit).getSourceAsMap();

		when(client.scroll(any(SearchRequest.class))).thenReturn(Flux.just(searchHit, searchHit));
	}

	@Test // DATAES-771
	void saveOneShouldInvokeAfterSaveCallbacks() {

		template.setEntityCallbacks(ReactiveEntityCallbacks.create(afterSaveCallback));

		Person entity = new Person("init", "luke");

		Person saved = template.save(entity).block(Duration.ofSeconds(1));

		verify(afterSaveCallback).onAfterSave(eq(entity), any());
		assertThat(saved.firstname).isEqualTo("after-save");
	}

	@Test // DATAES-771
	void saveOneFromPublisherShouldInvokeAfterSaveCallbacks() {

		template.setEntityCallbacks(ReactiveEntityCallbacks.create(afterSaveCallback));

		Person entity = new Person("init", "luke");

		Person saved = template.save(Mono.just(entity)).block(Duration.ofSeconds(1));

		verify(afterSaveCallback).onAfterSave(eq(entity), any());
		assertThat(saved.firstname).isEqualTo("after-save");
	}

	@Test // DATAES-771
	void saveWithIndexCoordinatesShouldInvokeAfterSaveCallbacks() {

		template.setEntityCallbacks(ReactiveEntityCallbacks.create(afterSaveCallback));

		Person entity = new Person("init", "luke");

		Person saved = template.save(entity, index).block(Duration.ofSeconds(1));

		verify(afterSaveCallback).onAfterSave(eq(entity), eq(index));
		assertThat(saved.firstname).isEqualTo("after-save");
	}

	@Test // DATAES-771
	void saveFromPublisherWithIndexCoordinatesShouldInvokeAfterSaveCallbacks() {

		template.setEntityCallbacks(ReactiveEntityCallbacks.create(afterSaveCallback));

		Person entity = new Person("init", "luke");

		Person saved = template.save(Mono.just(entity), index).block(Duration.ofSeconds(1));

		verify(afterSaveCallback).onAfterSave(eq(entity), eq(index));
		assertThat(saved.firstname).isEqualTo("after-save");
	}

	@Test // DATAES-771
	void saveAllShouldInvokeAfterSaveCallbacks() {

		template.setEntityCallbacks(ReactiveEntityCallbacks.create(afterSaveCallback));

		Person entity1 = new Person("init1", "luke1");
		Person entity2 = new Person("init2", "luke2");

		List<Person> saved = template.saveAll(Arrays.asList(entity1, entity2), index).toStream()
				.collect(Collectors.toList());

		verify(afterSaveCallback, times(2)).onAfterSave(any(), eq(index));
		assertThat(saved.get(0).firstname).isEqualTo("after-save");
		assertThat(saved.get(1).firstname).isEqualTo("after-save");
	}

	@Test // DATAES-771
	void saveFromMonoAllShouldInvokeAfterSaveCallbacks() {

		template.setEntityCallbacks(ReactiveEntityCallbacks.create(afterSaveCallback));

		Person entity1 = new Person("init1", "luke1");
		Person entity2 = new Person("init2", "luke2");

		List<Person> saved = template.saveAll(Mono.just(Arrays.asList(entity1, entity2)), index).toStream()
				.collect(Collectors.toList());

		verify(afterSaveCallback, times(2)).onAfterSave(any(), eq(index));
		assertThat(saved.get(0).firstname).isEqualTo("after-save");
		assertThat(saved.get(1).firstname).isEqualTo("after-save");
	}

	@Test // DATAES-772, #1678
	void multiGetShouldInvokeAfterConvertCallbacks() {

		template.setEntityCallbacks(ReactiveEntityCallbacks.create(afterConvertCallback));

		List<MultiGetItem<Person>> results = template.multiGet(pagedQueryForTwo(), Person.class, index)
				.timeout(Duration.ofSeconds(1)).toStream().collect(Collectors.toList());

		verify(afterConvertCallback, times(2)).onAfterConvert(eq(new Person("init", "luke")), eq(lukeDocument()),
				eq(index));
		assertThat(results.get(0).getItem().firstname).isEqualTo("after-convert");
		assertThat(results.get(1).getItem().firstname).isEqualTo("after-convert");
	}

	@Test // DATAES-772
	void getShouldInvokeAfterConvertCallbacks() {

		template.setEntityCallbacks(ReactiveEntityCallbacks.create(afterConvertCallback));

		Person result = template.get("init", Person.class).block(Duration.ofSeconds(1));

		verify(afterConvertCallback).onAfterConvert(eq(new Person("init", "luke")), eq(lukeDocument()), any());
		assertThat(result.firstname).isEqualTo("after-convert");
	}

	@Test // DATAES-772
	void getWithIndexCoordinatesShouldInvokeAfterConvertCallbacks() {

		template.setEntityCallbacks(ReactiveEntityCallbacks.create(afterConvertCallback));

		Person result = template.get("init", Person.class, index).block(Duration.ofSeconds(1));

		verify(afterConvertCallback).onAfterConvert(eq(new Person("init", "luke")), eq(lukeDocument()), eq(index));
		assertThat(result.firstname).isEqualTo("after-convert");
	}

	private Query pagedQueryForTwo() {
		return new NativeSearchQueryBuilder().withIds(Arrays.asList("init1", "init2")).withPageable(PageRequest.of(0, 10))
				.build();
	}

	private Document lukeDocument() {
		return Document.create().append("id", "init").append("firstname", "luke");
	}

	@Test // DATAES-772
	void searchShouldInvokeAfterConvertCallbacks() {

		template.setEntityCallbacks(ReactiveEntityCallbacks.create(afterConvertCallback));

		List<SearchHit<Person>> results = template.search(pagedQueryForTwo(), Person.class).timeout(Duration.ofSeconds(1))
				.toStream().collect(Collectors.toList());

		verify(afterConvertCallback, times(2)).onAfterConvert(eq(new Person("init", "luke")), eq(lukeDocument()), any());
		assertThat(results.get(0).getContent().firstname).isEqualTo("after-convert");
		assertThat(results.get(1).getContent().firstname).isEqualTo("after-convert");
	}

	@Test // DATAES-796
	void searchForPageShouldInvokeAfterConvertCallbacks() {

		template.setEntityCallbacks(ReactiveEntityCallbacks.create(afterConvertCallback));

		SearchPage<Person> searchPage = template.searchForPage(pagedQueryForTwo(), Person.class)
				.timeout(Duration.ofSeconds(1)).block();

		verify(afterConvertCallback, times(2)).onAfterConvert(eq(new Person("init", "luke")), eq(lukeDocument()), any());
		SearchHits<Person> searchHits = searchPage.getSearchHits();
		assertThat(searchHits.getSearchHit(0).getContent().firstname).isEqualTo("after-convert");
		assertThat(searchHits.getSearchHit(1).getContent().firstname).isEqualTo("after-convert");
	}

	@Test // DATAES-772
	void searchWithIndexCoordinatesShouldInvokeAfterConvertCallbacks() {

		template.setEntityCallbacks(ReactiveEntityCallbacks.create(afterConvertCallback));

		List<SearchHit<Person>> results = template.search(pagedQueryForTwo(), Person.class, index)
				.timeout(Duration.ofSeconds(1)).toStream().collect(Collectors.toList());

		verify(afterConvertCallback, times(2)).onAfterConvert(eq(new Person("init", "luke")), eq(lukeDocument()),
				eq(index));
		assertThat(results.get(0).getContent().firstname).isEqualTo("after-convert");
		assertThat(results.get(1).getContent().firstname).isEqualTo("after-convert");
	}

	@Test // DATAES-772
	void searchWithResultTypeShouldInvokeAfterConvertCallbacks() {

		template.setEntityCallbacks(ReactiveEntityCallbacks.create(afterConvertCallback));

		List<SearchHit<Person>> results = template.search(pagedQueryForTwo(), Person.class, Person.class)
				.timeout(Duration.ofSeconds(1)).toStream().collect(Collectors.toList());

		verify(afterConvertCallback, times(2)).onAfterConvert(eq(new Person("init", "luke")), eq(lukeDocument()), any());
		assertThat(results.get(0).getContent().firstname).isEqualTo("after-convert");
		assertThat(results.get(1).getContent().firstname).isEqualTo("after-convert");
	}

	@Test // DATAES-772
	void searchWithResultTypeAndIndexCoordinatesShouldInvokeAfterConvertCallbacks() {

		template.setEntityCallbacks(ReactiveEntityCallbacks.create(afterConvertCallback));

		List<SearchHit<Person>> results = template.search(pagedQueryForTwo(), Person.class, Person.class, index)
				.timeout(Duration.ofSeconds(1)).toStream().collect(Collectors.toList());

		verify(afterConvertCallback, times(2)).onAfterConvert(eq(new Person("init", "luke")), eq(lukeDocument()),
				eq(index));
		assertThat(results.get(0).getContent().firstname).isEqualTo("after-convert");
		assertThat(results.get(1).getContent().firstname).isEqualTo("after-convert");
	}

	@Test // DATAES-785
	void saveOneShouldInvokeBeforeConvertCallbacks() {

		template.setEntityCallbacks(ReactiveEntityCallbacks.create(beforeConvertCallback));

		Person entity = new Person("init1", "luke1");

		Person saved = template.save(entity, index).block(Duration.ofSeconds(1));

		verify(beforeConvertCallback).onBeforeConvert(any(), eq(index));
		assertThat(saved.firstname).isEqualTo("before-convert");
	}

	@Test // DATAES-785
	void saveAllShouldInvokeBeforeConvertCallbacks() {

		template.setEntityCallbacks(ReactiveEntityCallbacks.create(beforeConvertCallback));

		Person entity1 = new Person("init1", "luke1");
		Person entity2 = new Person("init2", "luke2");

		List<Person> saved = template.saveAll(Arrays.asList(entity1, entity2), index).toStream()
				.collect(Collectors.toList());

		verify(beforeConvertCallback, times(2)).onBeforeConvert(any(), eq(index));
		assertThat(saved.get(0).firstname).isEqualTo("before-convert");
		assertThat(saved.get(1).firstname).isEqualTo("before-convert");
	}

	static class Person {
		@Nullable
		@Id String id;
		@Nullable String firstname;

		public Person() {}

		public Person(@Nullable String id, @Nullable String firstname) {
			this.id = id;
			this.firstname = firstname;
		}

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getFirstname() {
			return firstname;
		}

		public void setFirstname(@Nullable String firstname) {
			this.firstname = firstname;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;

			Person person = (Person) o;

			if (!Objects.equals(id, person.id))
				return false;
			return Objects.equals(firstname, person.firstname);
		}

		@Override
		public int hashCode() {
			int result = id != null ? id.hashCode() : 0;
			result = 31 * result + (firstname != null ? firstname.hashCode() : 0);
			return result;
		}
	}

	static class ValueCapturingEntityCallback<T> {

		private final List<T> values = new ArrayList<>(1);

		protected void capture(T value) {
			values.add(value);
		}

		public List<T> getValues() {
			return values;
		}

		@Nullable
		public T getValue() {
			return CollectionUtils.lastElement(values);
		}

	}

	static class ValueCapturingAfterSaveCallback extends ValueCapturingEntityCallback<Person>
			implements ReactiveAfterSaveCallback<Person> {

		@Override
		public Mono<Person> onAfterSave(Person entity, IndexCoordinates index) {

			return Mono.defer(() -> {
				capture(entity);
				Person newPerson = new Person() {
					{
						id = entity.id;
						firstname = "after-save";
					}
				};
				return Mono.just(newPerson);
			});
		}
	}

	static class ValueCapturingAfterConvertCallback extends ValueCapturingEntityCallback<Person>
			implements ReactiveAfterConvertCallback<Person> {

		@Override
		public Mono<Person> onAfterConvert(Person entity, Document document, IndexCoordinates index) {

			return Mono.defer(() -> {
				capture(entity);
				Person newPerson = new Person() {
					{
						id = entity.id;
						firstname = "after-convert";
					}
				};
				return Mono.just(newPerson);
			});
		}
	}

	static class ValueCapturingBeforeConvertCallback extends ValueCapturingEntityCallback<Person>
			implements ReactiveBeforeConvertCallback<Person> {

		@Override
		public Mono<Person> onBeforeConvert(Person entity, IndexCoordinates index) {

			return Mono.defer(() -> {
				capture(entity);
				Person newPerson = new Person() {
					{
						id = entity.id;
						firstname = "before-convert";
					}
				};
				return Mono.just(newPerson);
			});
		}
	}
}
