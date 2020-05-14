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

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.junit.jupiter.ElasticsearchRestTemplateConfiguration;
import org.springframework.data.elasticsearch.junit.jupiter.ReactiveElasticsearchRestTemplateConfiguration;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Peter-Josef Meisch
 * @author Roman Puchkovskiy
 */
@SpringIntegrationTest
@ContextConfiguration(classes = { ReactiveElasticsearchOperationsCallbackTest.Config.class })
public class ReactiveElasticsearchOperationsCallbackTest {

	@Configuration
	@Import({ ReactiveElasticsearchRestTemplateConfiguration.class, ElasticsearchRestTemplateConfiguration.class })
	static class Config {
		@Component
		static class SampleEntityBeforeConvertCallback implements ReactiveBeforeConvertCallback<SampleEntity> {
			@Override
			public Mono<SampleEntity> onBeforeConvert(SampleEntity entity, IndexCoordinates index) {
				entity.setText("reactive-converted");
				return Mono.just(entity);
			}
		}

	}

	@Autowired private ReactiveElasticsearchOperations operations;
	@Autowired private ElasticsearchOperations nonreactiveOperations;

	@BeforeEach
	void setUp() {
		IndexOperations indexOps = nonreactiveOperations.indexOps(SampleEntity.class);
		indexOps.create();
		indexOps.putMapping(SampleEntity.class);
	}

	@AfterEach
	void tearDown() {
		IndexOperations indexOps = nonreactiveOperations.indexOps(SampleEntity.class);
		indexOps.delete();
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

	@Document(indexName = "test-operations-reactive-callback")
	static class SampleEntity {
		@Id private String id;
		private String text;

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
