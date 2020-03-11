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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Component;

/**
 * @author Peter-Josef Meisch
 */
abstract class ElasticsearchOperationsCallbackTest {

	@Autowired private ElasticsearchOperations operations;

	@Configuration
	static class Config {

		@Component
		static class SampleEntityBeforeConvertCallback implements BeforeConvertCallback<SampleEntity> {
			@Override
			public SampleEntity onBeforeConvert(SampleEntity entity) {
				entity.setText("converted");
				return entity;
			}
		}
	}

	@BeforeEach
	void setUp() {
		IndexOperations indexOps = operations.indexOps(SampleEntity.class);
		indexOps.delete();
		indexOps.create();
		indexOps.putMapping(indexOps.createMapping(SampleEntity.class));
	}

	@AfterEach
	void tearDown() {
		IndexOperations indexOps = operations.indexOps(SampleEntity.class);
		indexOps.delete();
	}

	@Test
	void shouldCallBeforeConvertCallback() {
		SampleEntity entity = new SampleEntity("1", "test");

		SampleEntity saved = operations.save(entity);

		assertThat(saved.getText()).isEqualTo("converted");
	}

	@Document(indexName = "test-operations-callback")
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
