/*
 * Copyright 2018-2020 the original author or authors.
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
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient;
import org.springframework.data.elasticsearch.core.event.ReactiveAfterSaveCallback;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.mapping.callback.ReactiveEntityCallbacks;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

/**
 * @author Roman Puchkovskiy
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

	@BeforeEach
	public void setUp() {
		template = new ReactiveElasticsearchTemplate(client);

		when(client.index(any(IndexRequest.class))).thenReturn(Mono.just(indexResponse));
		doReturn("response-id").when(indexResponse).getId();

		when(client.bulk(any(BulkRequest.class))).thenReturn(Mono.just(bulkResponse));
		doReturn(new BulkItemResponse[] { bulkItemResponse, bulkItemResponse }).when(bulkResponse).getItems();
		doReturn(docWriteResponse).when(bulkItemResponse).getResponse();
		doReturn("response-id").when(docWriteResponse).getId();
	}

	@Test // DATAES-771
	void saveOneShouldInvokeAfterSaveCallbacks() {

		ValueCapturingAfterSaveCallback afterSaveCallback = spy(new ValueCapturingAfterSaveCallback());

		template.setEntityCallbacks(ReactiveEntityCallbacks.create(afterSaveCallback));

		Person entity = new Person("init", "luke");

		Person saved = template.save(entity).block(Duration.ofSeconds(1));

		verify(afterSaveCallback).onAfterSave(eq(entity));
		assertThat(saved.id).isEqualTo("after-save");
	}

	@Test // DATAES-771
	void saveOneFromPublisherShouldInvokeAfterSaveCallbacks() {

		ValueCapturingAfterSaveCallback afterSaveCallback = spy(new ValueCapturingAfterSaveCallback());

		template.setEntityCallbacks(ReactiveEntityCallbacks.create(afterSaveCallback));

		Person entity = new Person("init", "luke");

		Person saved = template.save(Mono.just(entity)).block(Duration.ofSeconds(1));

		verify(afterSaveCallback).onAfterSave(eq(entity));
		assertThat(saved.id).isEqualTo("after-save");
	}

	@Test // DATAES-771
	void saveWithIndexCoordinatesShouldInvokeAfterSaveCallbacks() {

		ValueCapturingAfterSaveCallback afterSaveCallback = spy(new ValueCapturingAfterSaveCallback());

		template.setEntityCallbacks(ReactiveEntityCallbacks.create(afterSaveCallback));

		Person entity = new Person("init", "luke");

		Person saved = template.save(entity, IndexCoordinates.of("index")).block(Duration.ofSeconds(1));

		verify(afterSaveCallback).onAfterSave(eq(entity));
		assertThat(saved.id).isEqualTo("after-save");
	}

	@Test // DATAES-771
	void saveFromPublisherWithIndexCoordinatesShouldInvokeAfterSaveCallbacks() {

		ValueCapturingAfterSaveCallback afterSaveCallback = spy(new ValueCapturingAfterSaveCallback());

		template.setEntityCallbacks(ReactiveEntityCallbacks.create(afterSaveCallback));

		Person entity = new Person("init", "luke");

		Person saved = template.save(Mono.just(entity), IndexCoordinates.of("index")).block(Duration.ofSeconds(1));

		verify(afterSaveCallback).onAfterSave(eq(entity));
		assertThat(saved.id).isEqualTo("after-save");
	}

	@Test // DATAES-771
	void saveAllShouldInvokeAfterSaveCallbacks() {

		ValueCapturingAfterSaveCallback afterSaveCallback = spy(new ValueCapturingAfterSaveCallback());

		template.setEntityCallbacks(ReactiveEntityCallbacks.create(afterSaveCallback));

		Person entity1 = new Person("init1", "luke1");
		Person entity2 = new Person("init2", "luke2");

		List<Person> saved = template.saveAll(Arrays.asList(entity1, entity2), IndexCoordinates.of("index")).toStream()
				.collect(Collectors.toList());

		verify(afterSaveCallback, times(2)).onAfterSave(any());
		assertThat(saved.get(0).getId()).isEqualTo("after-save");
		assertThat(saved.get(1).getId()).isEqualTo("after-save");
	}

	@Test // DATAES-771
	void saveFromMonoAllShouldInvokeAfterSaveCallbacks() {

		ValueCapturingAfterSaveCallback afterSaveCallback = spy(new ValueCapturingAfterSaveCallback());

		template.setEntityCallbacks(ReactiveEntityCallbacks.create(afterSaveCallback));

		Person entity1 = new Person("init1", "luke1");
		Person entity2 = new Person("init2", "luke2");

		List<Person> saved = template.saveAll(Mono.just(Arrays.asList(entity1, entity2)), IndexCoordinates.of("index"))
				.toStream().collect(Collectors.toList());

		verify(afterSaveCallback, times(2)).onAfterSave(any());
		assertThat(saved.get(0).getId()).isEqualTo("after-save");
		assertThat(saved.get(1).getId()).isEqualTo("after-save");
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
			implements ReactiveAfterSaveCallback<Person> {

		@Override
		public Mono<Person> onAfterSave(Person entity) {

			return Mono.defer(() -> {
				capture(entity);
				Person newPerson = new Person() {
					{
						id = "after-save";
						firstname = entity.firstname;
					}
				};
				return Mono.just(newPerson);
			});
		}
	}
}
