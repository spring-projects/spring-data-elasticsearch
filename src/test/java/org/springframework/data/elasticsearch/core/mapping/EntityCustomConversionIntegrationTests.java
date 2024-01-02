/*
 * Copyright 2021-2024 the original author or authors.
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
package org.springframework.data.elasticsearch.core.mapping;

import static org.assertj.core.api.Assertions.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.geo.GeoJsonPoint;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.lang.Nullable;

/**
 * Test that a whole entity can be converted using custom conversions
 *
 * @author Peter-Josef Meisch
 */
@SpringIntegrationTest
public abstract class EntityCustomConversionIntegrationTests {

	@Configuration
	@EnableElasticsearchRepositories(basePackages = { "org.springframework.data.elasticsearch.core.mapping" },
			considerNestedRepositories = true)
	static class Config {}

	@Autowired private ElasticsearchOperations operations;
	@Autowired private IndexNameProvider indexNameProvider;

	@BeforeEach
	void setUp() {
		indexNameProvider.increment();
		IndexOperations indexOps = operations.indexOps(Entity.class);
		indexOps.createWithMapping();
	}

	@Test
	@Order(java.lang.Integer.MAX_VALUE)
	void cleanup() {
		operations.indexOps(IndexCoordinates.of(indexNameProvider.getPrefix() + "*")).delete();
	}

	@Test // #1667
	@DisplayName("should use CustomConversions on entity")
	void shouldUseCustomConversionsOnEntity() {

		Entity entity = new Entity();
		entity.setValue("hello"); //
		entity.setLocation(GeoJsonPoint.of(8.0, 42.7));

		org.springframework.data.elasticsearch.core.document.Document document = org.springframework.data.elasticsearch.core.document.Document
				.create();
		operations.getElasticsearchConverter().write(entity, document);

		assertThat(document.getString("the_value")).isEqualTo("hello");
		assertThat(document.getString("the_lon")).isEqualTo("8.0");
		assertThat(document.getString("the_lat")).isEqualTo("42.7");
	}

	@Test // #1667
	@DisplayName("should store and load entity from Elasticsearch")
	void shouldStoreAndLoadEntityFromElasticsearch() {

		Entity entity = new Entity();
		entity.setValue("hello"); //
		entity.setLocation(GeoJsonPoint.of(8.0, 42.7));

		Entity savedEntity = operations.save(entity);

		SearchHits<Entity> searchHits = operations.search(Query.findAll(), Entity.class);
		assertThat(searchHits.getTotalHits()).isEqualTo(1);
		Entity foundEntity = searchHits.getSearchHit(0).getContent();
		assertThat(foundEntity).isEqualTo(entity);
	}

	@Document(indexName = "#{@indexNameProvider.indexName()}")
	static class Entity {
		@Nullable private String value;
		@Nullable private GeoJsonPoint location;

		@Nullable
		public String getValue() {
			return value;
		}

		public void setValue(@Nullable String value) {
			this.value = value;
		}

		@Nullable
		public GeoJsonPoint getLocation() {
			return location;
		}

		public void setLocation(@Nullable GeoJsonPoint location) {
			this.location = location;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (!(o instanceof Entity entity))
				return false;

			if (!Objects.equals(value, entity.value))
				return false;
			return Objects.equals(location, entity.location);
		}

		@Override
		public int hashCode() {
			int result = value != null ? value.hashCode() : 0;
			result = 31 * result + (location != null ? location.hashCode() : 0);
			return result;
		}
	}

	@WritingConverter
	static class EntityToMapConverter implements Converter<Entity, Map<String, Object>> {
		@Override
		public Map<String, Object> convert(Entity source) {
			LinkedHashMap<String, Object> target = new LinkedHashMap<>();
			target.put("the_value", source.getValue());
			target.put("the_lat", "" + source.getLocation().getY());
			target.put("the_lon", "" + source.getLocation().getX());
			return target;
		}
	}

	@ReadingConverter
	static class MapToEntityConverter implements Converter<Map<String, Object>, Entity> {

		@Override
		public Entity convert(Map<String, Object> source) {
			Entity entity = new Entity();
			entity.setValue((String) source.get("the_value"));
			entity.setLocation(GeoJsonPoint.of( //
					Double.parseDouble((String) (source.get("the_lon"))), //
					Double.parseDouble((String) (source.get("the_lat"))) //
			));
			return entity;
		}
	}
}
