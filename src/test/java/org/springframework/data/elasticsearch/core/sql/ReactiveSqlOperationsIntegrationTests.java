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

import static org.springframework.data.elasticsearch.core.IndexOperationsAdapter.*;

import reactor.test.StepVerifier;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.SqlQuery;
import org.springframework.data.elasticsearch.junit.jupiter.ReactiveElasticsearchTemplateConfiguration;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.lang.Nullable;
import org.springframework.test.context.ContextConfiguration;

/**
 * Testing the reactive querying using SQL syntax.
 *
 * @author Youssef Aouichaoui
 */
@SpringIntegrationTest
@ContextConfiguration(classes = { ReactiveSqlOperationsIntegrationTests.Config.class })
@DisplayName("Using Elasticsearch SQL Reactive Client")
public class ReactiveSqlOperationsIntegrationTests {
	@Autowired ReactiveElasticsearchOperations operations;

	@BeforeEach
	void setUp() {
		// create index
		blocking(operations.indexOps(EntityForSQL.class)).createWithMapping();

		// add data
		operations
				.saveAll(List.of(EntityForSQL.builder().withViews(3).build(), EntityForSQL.builder().withViews(0).build()),
						EntityForSQL.class)
				.blockLast();
	}

	@AfterEach
	void tearDown() {
		// delete index
		blocking(operations.indexOps(EntityForSQL.class)).delete();
	}

	// begin configuration region
	@Configuration
	@Import({ ReactiveElasticsearchTemplateConfiguration.class })
	static class Config {}
	// end region

	@Test // #2683
	void when_search_with_an_sql_query() {
		// Given
		SqlQuery query = SqlQuery.builder("SELECT * FROM entity_for_sql WHERE views = 0").build();

		// When

		// Then
		operations.search(query).as(StepVerifier::create).expectNextCount(1).verifyComplete();
	}

	// begin region
	@Document(indexName = "entity_for_sql")
	static class EntityForSQL {
		@Id private String id;
		private final Integer views;

		public EntityForSQL(EntityForSQL.Builder builder) {
			this.views = builder.views;
		}

		@Nullable
		public String getId() {
			return id;
		}

		public Integer getViews() {
			return views;
		}

		public static EntityForSQL.Builder builder() {
			return new EntityForSQL.Builder();
		}

		static class Builder {
			private Integer views = 0;

			public EntityForSQL.Builder withViews(Integer views) {
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
