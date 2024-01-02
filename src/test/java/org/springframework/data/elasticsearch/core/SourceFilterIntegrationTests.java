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
package org.springframework.data.elasticsearch.core;

import static org.assertj.core.api.Assertions.*;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilterBuilder;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.SourceFilter;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.lang.Nullable;

/**
 * @author Peter-Josef Meisch
 */
@SpringIntegrationTest
public abstract class SourceFilterIntegrationTests {

	@Autowired private ElasticsearchOperations operations;
	@Autowired private IndexNameProvider indexNameProvider;

	@BeforeEach
	void setUp() {
		indexNameProvider.increment();
		operations.indexOps(Entity.class).createWithMapping();

		Entity entity = new Entity();
		entity.setId("42");
		entity.setField1("one");
		entity.setField2("two");
		entity.setField3("three");
		operations.save(entity);
	}

	@Test
	@Order(java.lang.Integer.MAX_VALUE)
	void cleanup() {
		operations.indexOps(IndexCoordinates.of(indexNameProvider.getPrefix() + "*")).delete();
	}

	@Test // #1659, #1678
	@DisplayName("should only return requested fields on multiget")
	void shouldOnlyReturnRequestedFieldsOnGMultiGet() {

		// multiget has no fields, need sourcefilter here
		Query query = Query.multiGetQuery(Collections.singleton("42"));
		query.addSourceFilter(new FetchSourceFilterBuilder().withIncludes("field2").build()); //

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

		Query query = Query.multiGetQuery(Collections.singleton("42"));
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

		Query query = Query.multiGetQuery(Collections.singleton("42"));
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

	@Document(indexName = "#{@indexNameProvider.indexName()}")
	public static class Entity {
		@Nullable
		@Id private String id;
		@Nullable
		@Field(type = FieldType.Text) private String field1;
		@Nullable
		@Field(type = FieldType.Text) private String field2;
		@Nullable
		@Field(type = FieldType.Text) private String field3;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getField1() {
			return field1;
		}

		public void setField1(@Nullable String field1) {
			this.field1 = field1;
		}

		@Nullable
		public String getField2() {
			return field2;
		}

		public void setField2(@Nullable String field2) {
			this.field2 = field2;
		}

		@Nullable
		public String getField3() {
			return field3;
		}

		public void setField3(@Nullable String field3) {
			this.field3 = field3;
		}
	}
}
