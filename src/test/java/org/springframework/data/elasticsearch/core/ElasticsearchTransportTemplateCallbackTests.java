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
package org.springframework.data.elasticsearch.core;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.core.event.AfterSaveCallback;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.BulkOptions;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.mapping.callback.EntityCallbacks;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

/**
 * @author Roman Puchkovskiy
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ElasticsearchTransportTemplateCallbackTests {

	private ElasticsearchTemplate template;

	@Mock private Client client;

	@Mock private IndexRequestBuilder indexRequestBuilder;
	@Mock private ActionFuture<IndexResponse> indexResponseActionFuture;
	@Mock private IndexResponse indexResponse;
	@Mock private BulkRequestBuilder bulkRequestBuilder;
	@Mock private ActionFuture<BulkResponse> bulkResponseActionFuture;
	@Mock private BulkResponse bulkResponse;
	@Mock private BulkItemResponse bulkItemResponse;

	@BeforeEach
	public void setUp() {
		template = new ElasticsearchTemplate(client);

		when(client.prepareIndex(anyString(), anyString(), anyString())).thenReturn(indexRequestBuilder);
		doReturn(indexResponseActionFuture).when(indexRequestBuilder).execute();
		when(indexResponseActionFuture.actionGet()).thenReturn(indexResponse);
		doReturn("response-id").when(indexResponse).getId();

		when(client.prepareBulk()).thenReturn(bulkRequestBuilder);
		doReturn(bulkResponseActionFuture).when(bulkRequestBuilder).execute();
		when(bulkResponseActionFuture.actionGet()).thenReturn(bulkResponse);
		doReturn(new BulkItemResponse[] { bulkItemResponse, bulkItemResponse }).when(bulkResponse).getItems();
		doReturn("response-id").when(bulkItemResponse).getId();
	}

	@Test // DATAES-771
	void saveOneShouldInvokeAfterSaveCallbacks() {

		ValueCapturingAfterSaveCallback afterSaveCallback = spy(new ValueCapturingAfterSaveCallback());

		template.setEntityCallbacks(EntityCallbacks.create(afterSaveCallback));

		Person entity = new Person("init", "luke");

		Person saved = template.save(entity);

		verify(afterSaveCallback).onAfterSave(eq(entity));
		assertThat(saved.id).isEqualTo("after-save");
	}

	@Test // DATAES-771
	void saveWithIndexCoordinatesShouldInvokeAfterSaveCallbacks() {

		ValueCapturingAfterSaveCallback afterSaveCallback = spy(new ValueCapturingAfterSaveCallback());

		template.setEntityCallbacks(EntityCallbacks.create(afterSaveCallback));

		Person entity = new Person("init", "luke");

		Person saved = template.save(entity, IndexCoordinates.of("index"));

		verify(afterSaveCallback).onAfterSave(eq(entity));
		assertThat(saved.id).isEqualTo("after-save");
	}

	@Test // DATAES-771
	void saveArrayShouldInvokeAfterSaveCallbacks() {

		ValueCapturingAfterSaveCallback afterSaveCallback = spy(new ValueCapturingAfterSaveCallback());

		template.setEntityCallbacks(EntityCallbacks.create(afterSaveCallback));

		Person entity1 = new Person("init1", "luke1");
		Person entity2 = new Person("init2", "luke2");

		Iterable<Person> saved = template.save(entity1, entity2);

		verify(afterSaveCallback, times(2)).onAfterSave(any());
		Iterator<Person> savedIterator = saved.iterator();
		assertThat(savedIterator.next().getId()).isEqualTo("after-save");
		assertThat(savedIterator.next().getId()).isEqualTo("after-save");
	}

	@Test // DATAES-771
	void saveIterableShouldInvokeAfterSaveCallbacks() {

		ValueCapturingAfterSaveCallback afterSaveCallback = spy(new ValueCapturingAfterSaveCallback());

		template.setEntityCallbacks(EntityCallbacks.create(afterSaveCallback));

		Person entity1 = new Person("init1", "luke1");
		Person entity2 = new Person("init2", "luke2");

		Iterable<Person> saved = template.save(Arrays.asList(entity1, entity2));

		verify(afterSaveCallback, times(2)).onAfterSave(any());
		Iterator<Person> savedIterator = saved.iterator();
		assertThat(savedIterator.next().getId()).isEqualTo("after-save");
		assertThat(savedIterator.next().getId()).isEqualTo("after-save");
	}

	@Test // DATAES-771
	void saveIterableWithIndexCoordinatesShouldInvokeAfterSaveCallbacks() {

		ValueCapturingAfterSaveCallback afterSaveCallback = spy(new ValueCapturingAfterSaveCallback());

		template.setEntityCallbacks(EntityCallbacks.create(afterSaveCallback));

		Person entity1 = new Person("init1", "luke1");
		Person entity2 = new Person("init2", "luke2");

		Iterable<Person> saved = template.save(Arrays.asList(entity1, entity2), IndexCoordinates.of("index"));

		verify(afterSaveCallback, times(2)).onAfterSave(any());
		Iterator<Person> savedIterator = saved.iterator();
		assertThat(savedIterator.next().getId()).isEqualTo("after-save");
		assertThat(savedIterator.next().getId()).isEqualTo("after-save");
	}

	@Test // DATAES-771
	void indexShouldInvokeAfterSaveCallbacks() {

		ValueCapturingAfterSaveCallback afterSaveCallback = spy(new ValueCapturingAfterSaveCallback());

		template.setEntityCallbacks(EntityCallbacks.create(afterSaveCallback));

		Person entity = new Person("init", "luke");

		IndexQuery indexQuery = indexQueryForEntity(entity);
		template.index(indexQuery, IndexCoordinates.of("index"));

		verify(afterSaveCallback).onAfterSave(eq(entity));
		Person savedPerson = (Person) indexQuery.getObject();
		assertThat(savedPerson.id).isEqualTo("after-save");
	}

	private IndexQuery indexQueryForEntity(Person entity) {
		IndexQuery indexQuery = new IndexQuery();
		indexQuery.setObject(entity);
		return indexQuery;
	}

	@Test // DATAES-771
	void bulkIndexShouldInvokeAfterSaveCallbacks() {

		ValueCapturingAfterSaveCallback afterSaveCallback = spy(new ValueCapturingAfterSaveCallback());

		template.setEntityCallbacks(EntityCallbacks.create(afterSaveCallback));

		Person entity1 = new Person("init1", "luke1");
		Person entity2 = new Person("init2", "luke2");

		IndexQuery query1 = indexQueryForEntity(entity1);
		IndexQuery query2 = indexQueryForEntity(entity2);
		template.bulkIndex(Arrays.asList(query1, query2), IndexCoordinates.of("index"));

		verify(afterSaveCallback, times(2)).onAfterSave(any());
		Person savedPerson1 = (Person) query1.getObject();
		Person savedPerson2 = (Person) query2.getObject();
		assertThat(savedPerson1.getId()).isEqualTo("after-save");
		assertThat(savedPerson2.getId()).isEqualTo("after-save");
	}

	@Test // DATAES-771
	void bulkIndexWithOptionsShouldInvokeAfterSaveCallbacks() {

		ValueCapturingAfterSaveCallback afterSaveCallback = spy(new ValueCapturingAfterSaveCallback());

		template.setEntityCallbacks(EntityCallbacks.create(afterSaveCallback));

		Person entity1 = new Person("init1", "luke1");
		Person entity2 = new Person("init2", "luke2");

		IndexQuery query1 = indexQueryForEntity(entity1);
		IndexQuery query2 = indexQueryForEntity(entity2);
		template.bulkIndex(Arrays.asList(query1, query2), BulkOptions.defaultOptions(), IndexCoordinates.of("index"));

		verify(afterSaveCallback, times(2)).onAfterSave(any());
		Person savedPerson1 = (Person) query1.getObject();
		Person savedPerson2 = (Person) query2.getObject();
		assertThat(savedPerson1.getId()).isEqualTo("after-save");
		assertThat(savedPerson2.getId()).isEqualTo("after-save");
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	static class Person {

		@Id String id;
		String firstname;
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
			implements AfterSaveCallback<Person> {

		@Override
		public Person onAfterSave(Person entity) {

			capture(entity);
			return new Person() {
				{
					id = "after-save";
					firstname = entity.firstname;
				}
			};
		}
	}
}
