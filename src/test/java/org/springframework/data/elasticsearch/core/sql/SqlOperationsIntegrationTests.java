/*
 * Copyright 2024-2025 the original author or authors.
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
package org.springframework.data.elasticsearch.core.sql;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.query.SqlQuery;
import org.springframework.data.elasticsearch.junit.jupiter.ElasticsearchTemplateConfiguration;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.lang.Nullable;
import org.springframework.test.context.ContextConfiguration;

/**
 * Testing the querying using SQL syntax.
 *
 * @author Youssef Aouichaoui
 */
@SpringIntegrationTest
@ContextConfiguration(classes = { SqlOperationsIntegrationTests.Config.class })
@DisplayName("Using Elasticsearch SQL Client")
class SqlOperationsIntegrationTests {
	@Autowired ElasticsearchOperations operations;
	@Nullable IndexOperations indexOps;

	@BeforeEach
	void setUp() {
		// create index
		indexOps = operations.indexOps(EntityForSQL.class);
		indexOps.createWithMapping();

		// add data
		operations.save(EntityForSQL.builder().withViews(3).build(), EntityForSQL.builder().withViews(0).build());
	}

	@AfterEach
	void tearDown() {
		// delete index
		if (indexOps != null) {
			indexOps.delete();
		}
	}

	// begin configuration region
	@Configuration
	@Import({ ElasticsearchTemplateConfiguration.class })
	static class Config {}
	// end region

	@Test // #2683
	void when_search_with_an_sql_query() {
		// Given
		SqlQuery query = SqlQuery.builder("SELECT * FROM entity_for_sql WHERE views = 0").build();

		// When

		// Then
		SqlResponse response = operations.search(query);
		assertNotNull(response);
		assertFalse(response.getRows().isEmpty());
		assertEquals(1, response.getRows().size());
	}

	@Test // #2683
	void when_search_with_an_sql_query_that_has_aggregated_column() {
		// Given
		SqlQuery query = SqlQuery.builder("SELECT SUM(views) AS TOTAL FROM entity_for_sql").build();

		// When

		// Then
		SqlResponse response = operations.search(query);
		assertThat(response.getColumns()).first().extracting(SqlResponse.Column::name).isEqualTo("TOTAL");
		assertThat(response.getRows()).hasSize(1).first().extracting(row -> row.get(response.getColumns().get(0)))
				.hasToString("3");
	}

	// begin region
	@Document(indexName = "entity_for_sql")
	static class EntityForSQL {
		@Id private String id;
		private final Integer views;

		public EntityForSQL(Builder builder) {
			this.views = builder.views;
		}

		@Nullable
		public String getId() {
			return id;
		}

		public Integer getViews() {
			return views;
		}

		public static Builder builder() {
			return new Builder();
		}

		static class Builder {
			private Integer views = 0;

			public Builder withViews(Integer views) {
				this.views = views;

				return this;
			}

			public EntityForSQL build() {
				return new EntityForSQL(this);
			}
		}
	}
	// end region

}
