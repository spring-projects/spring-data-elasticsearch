/*
 * Copyright 2020-2024 the original author or authors.
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
import static org.springframework.data.elasticsearch.core.IndexOperationsAdapter.*;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

/**
 * @author Peter-Josef Meisch
 * @author Roman Puchkovskiy
 */
@SpringIntegrationTest
public abstract class ReactiveCallbackIntegrationTests {

	@Configuration
	static class Config {

		@Component
		static class SampleEntityBeforeConvertCallback implements ReactiveBeforeConvertCallback<SampleEntity> {
			@Override
			public Mono<SampleEntity> onBeforeConvert(SampleEntity entity, IndexCoordinates index) {
				entity.setText("reactive-converted");
				return Mono.just(entity);
			}
		}

		@Component
		static class SampleEntityAfterLoadCallback implements ReactiveAfterLoadCallback<SampleEntity> {

			@Override
			public Mono<org.springframework.data.elasticsearch.core.document.Document> onAfterLoad(
					org.springframework.data.elasticsearch.core.document.Document document, Class<SampleEntity> type,
					IndexCoordinates indexCoordinates) {

				document.put("className", document.get("_class"));
				return Mono.just(document);
			}
		}

	}

	@Autowired private ReactiveElasticsearchOperations operations;
	@Autowired private IndexNameProvider indexNameProvider;

	@BeforeEach
	void setUp() {
		indexNameProvider.increment();
		blocking(operations.indexOps(SampleEntity.class)).createWithMapping();
	}

	@Test
	@Order(java.lang.Integer.MAX_VALUE)
	void cleanup() {
		blocking(operations.indexOps(IndexCoordinates.of(indexNameProvider.getPrefix() + '*'))).delete();
	}

	@Test // DATES-68
	void shouldCallCallbackOnSave() {
		SampleEntity sample = new SampleEntity("42", "initial");

		operations.save(sample) //
				.as(StepVerifier::create) //
				.consumeNextWith(it -> { //
					assertThat(it.text).isEqualTo("reactive-converted"); //
				}) //
				.verifyComplete();
	}

	@Test // #2009
	@DisplayName("should invoke after load callback")
	void shouldInvokeAfterLoadCallback() {

		SampleEntity entity = new SampleEntity("1", "test");

		operations.save(entity) //
				.then(operations.get(entity.getId(), SampleEntity.class)) //
				.as(StepVerifier::create) //
				.consumeNextWith(loaded -> { //
					assertThat(loaded).isNotNull(); //
					assertThat(loaded.className).isEqualTo(SampleEntity.class.getName()); //
				}).verifyComplete(); //
	}

	@Document(indexName = "#{@indexNameProvider.indexName()}")
	static class SampleEntity {
		@Id private String id;
		private String text;

		@Nullable private String className;

		public SampleEntity(String id, String text) {
			this.id = id;
			this.text = text;
		}

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
