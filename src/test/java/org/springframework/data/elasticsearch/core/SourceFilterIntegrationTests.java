/*
 * Copyright 2021 the original author or authors.
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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.SourceFilter;
import org.springframework.data.elasticsearch.junit.jupiter.ElasticsearchRestTemplateConfiguration;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Peter-Josef Meisch
 */
@SpringIntegrationTest
@ContextConfiguration(classes = { ElasticsearchRestTemplateConfiguration.class })
public class SourceFilterIntegrationTests {

	@Autowired private ElasticsearchOperations operations;
	private IndexOperations indexOps;

	@BeforeEach
	void setUp() {
		indexOps = operations.indexOps(Entity.class);
		indexOps.create();
		indexOps.putMapping();

		operations.save(Entity.builder().id("42").field1("one").field2("two").field3("three").build());
	}

	@AfterEach
	void tearDown() {
		indexOps.delete();
	}

	@Test // #1659
	@DisplayName("should only return requested fields on search")
	void shouldOnlyReturnRequestedFieldsOnSearch() {

		Query query = Query.findAll();
		query.addFields("field2");

		SearchHits<Entity> searchHits = operations.search(query, Entity.class);

		assertThat(searchHits).hasSize(1);
		Entity entity = searchHits.getSearchHit(0).getContent();
		assertThat(entity.getField1()).isNull();
		assertThat(entity.getField2()).isEqualTo("two");
		assertThat(entity.getField3()).isNull();
	}

	@Test // #1659, #1678
	@DisplayName("should only return requested fields on multiget")
	void shouldOnlyReturnRequestedFieldsOnGMultiGet() {

		Query query = new NativeSearchQueryBuilder().withIds(Collections.singleton("42")).build();
		query.addFields("field2");

		List<MultiGetItem<Entity>> entities = operations.multiGet(query, Entity.class);

		assertThat(entities).hasSize(1);
		Entity entity = entities.get(0).getItem();
		assertThat(entity.getField1()).isNull();
		assertThat(entity.getField2()).isEqualTo("two");
		assertThat(entity.getField3()).isNull();
	}

	@Test // #1659
	@DisplayName("should not return excluded fields from SourceFilter on search")
	void shouldNotReturnExcludedFieldsFromSourceFilterOnSearch() {

		Query query = Query.findAll();
		query.addSourceFilter(new SourceFilter() {
			@Override
			public String[] getIncludes() {
				return new String[] {};
			}

			@Override
			public String[] getExcludes() {
				return new String[] { "field2" };
			}
		});

		SearchHits<Entity> entities = operations.search(query, Entity.class);

		assertThat(entities).hasSize(1);
		Entity entity = entities.getSearchHit(0).getContent();
		assertThat(entity.getField1()).isNotNull();
		assertThat(entity.getField2()).isNull();
		assertThat(entity.getField3()).isNotNull();
	}

	@Test // #1659, #1678
	@DisplayName("should not return excluded fields from SourceFilter on multiget")
	void shouldNotReturnExcludedFieldsFromSourceFilterOnMultiGet() {

		Query query = new NativeSearchQueryBuilder().withIds(Collections.singleton("42")).build();
		query.addSourceFilter(new SourceFilter() {
			@Override
			public String[] getIncludes() {
				return new String[] {};
			}

			@Override
			public String[] getExcludes() {
				return new String[] { "field2" };
			}
		});

		List<MultiGetItem<Entity>> entities = operations.multiGet(query, Entity.class);

		assertThat(entities).hasSize(1);
		Entity entity = entities.get(0).getItem();
		assertThat(entity.getField1()).isNotNull();
		assertThat(entity.getField2()).isNull();
		assertThat(entity.getField3()).isNotNull();
	}

	@Test // #1659
	@DisplayName("should only return included fields from SourceFilter on search")
	void shouldOnlyReturnIncludedFieldsFromSourceFilterOnSearch() {

		Query query = Query.findAll();
		query.addSourceFilter(new SourceFilter() {
			@Override
			public String[] getIncludes() {
				return new String[] { "field2" };
			}

			@Override
			public String[] getExcludes() {
				return new String[] {};
			}
		});

		SearchHits<Entity> entities = operations.search(query, Entity.class);

		assertThat(entities).hasSize(1);
		Entity entity = entities.getSearchHit(0).getContent();
		assertThat(entity.getField1()).isNull();
		assertThat(entity.getField2()).isNotNull();
		assertThat(entity.getField3()).isNull();
	}

	@Test // #1659, #1678
	@DisplayName("should only return included fields from SourceFilter on multiget")
	void shouldOnlyReturnIncludedFieldsFromSourceFilterOnMultiGet() {

		Query query = new NativeSearchQueryBuilder().withIds(Collections.singleton("42")).build();
		query.addSourceFilter(new SourceFilter() {
			@Override
			public String[] getIncludes() {
				return new String[] { "field2" };
			}

			@Override
			public String[] getExcludes() {
				return new String[] {};
			}
		});

		List<MultiGetItem<Entity>> entities = operations.multiGet(query, Entity.class);

		assertThat(entities).hasSize(1);
		Entity entity = entities.get(0).getItem();
		assertThat(entity.getField1()).isNull();
		assertThat(entity.getField2()).isNotNull();
		assertThat(entity.getField3()).isNull();
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	@Document(indexName = "sourcefilter-tests")
	public static class Entity {
		@Id private String id;
		@Field(type = FieldType.Text) private String field1;
		@Field(type = FieldType.Text) private String field2;
		@Field(type = FieldType.Text) private String field3;
	}
}
