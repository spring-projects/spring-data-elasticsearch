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
package org.springframework.data.elasticsearch.repository.query.valueconverter;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.annotations.ValueConverter;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.mapping.PropertyValueConverter;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.lang.Nullable;

/**
 * Integration tests to check that {@link org.springframework.data.elasticsearch.annotations.ValueConverter} annotated
 * properties are handled correctly (method name derived queries, for
 *
 * @{@link org.springframework.data.elasticsearch.core.query.Query} methods we don't know which parameters map to which
 *         property.
 * @author Peter-Josef Meisch
 */
@SpringIntegrationTest
abstract class ValueConverterIntegrationTests {

	@Autowired private EntityRepository repository;
	@Autowired ElasticsearchOperations operations;
	@Autowired IndexNameProvider indexNameProvider;

	@BeforeEach
	public void before() {
		indexNameProvider.increment();
		operations.indexOps(Entity.class).createWithMapping();

	}

	@Test
	@Order(Integer.MAX_VALUE)
	void cleanup() {
		operations.indexOps(IndexCoordinates.of(indexNameProvider.getPrefix() + '*')).delete();
	}

	@Test // #2338
	@DisplayName("should apply ValueConverter")
	void shouldApplyValueConverter() {

		Entity entity = new Entity();
		entity.setId("42");
		entity.setText("answer");
		operations.save(entity);

		SearchHits<Entity> searchHits = repository.queryByText("text-answer");
		assertThat(searchHits.getTotalHits()).isEqualTo(1);

		searchHits = repository.findByText("answer");
		assertThat(searchHits.getTotalHits()).isEqualTo(1);
	}

	interface EntityRepository extends ElasticsearchRepository<Entity, String> {
		SearchHits<Entity> findByText(String text);

		@Query("{ \"term\": { \"text\": \"?0\" } }")
		SearchHits<Entity> queryByText(String text);
	}

	@Document(indexName = "#{@indexNameProvider.indexName()}")
	static class Entity {
		@Id
		@Nullable private String id;

		@Field(type = FieldType.Keyword)
		@ValueConverter(TextConverter.class)
		@Nullable private String text;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getText() {
			return text;
		}

		public void setText(@Nullable String text) {
			this.text = text;
		}
	}

	static class TextConverter implements PropertyValueConverter {

		public static final String PREFIX = "text-";

		@Override
		public Object write(Object value) {
			return PREFIX + value.toString();
		}

		@Override
		public Object read(Object value) {

			String valueString = value.toString();

			if (valueString.startsWith(PREFIX)) {
				return valueString.substring(PREFIX.length());
			} else {
				return value;
			}
		}
	}
}
