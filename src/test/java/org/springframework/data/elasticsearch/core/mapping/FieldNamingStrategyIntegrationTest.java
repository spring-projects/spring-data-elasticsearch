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
package org.springframework.data.elasticsearch.core.mapping;

import static org.assertj.core.api.Assertions.*;
import static org.elasticsearch.index.query.QueryBuilders.*;

import org.elasticsearch.client.RestHighLevelClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.junit.jupiter.ElasticsearchRestTemplateConfiguration;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.data.mapping.model.FieldNamingStrategy;
import org.springframework.data.mapping.model.SnakeCaseFieldNamingStrategy;
import org.springframework.lang.Nullable;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Peter-Josef Meisch
 */
@SpringIntegrationTest
@ContextConfiguration(classes = { FieldNamingStrategyIntegrationTest.Config.class })
public class FieldNamingStrategyIntegrationTest {

	@Autowired private ElasticsearchOperations operations;

	@Configuration
	static class Config extends ElasticsearchRestTemplateConfiguration {

		@Override
		@Bean
		public ElasticsearchOperations elasticsearchOperations(ElasticsearchConverter elasticsearchConverter,
				RestHighLevelClient elasticsearchClient) {
			return super.elasticsearchOperations(elasticsearchConverter, elasticsearchClient);
		}

		@Override
		protected FieldNamingStrategy fieldNamingStrategy() {
			return new SnakeCaseFieldNamingStrategy();
		}
	}

	@BeforeEach
	void setUp() {
		IndexOperations indexOps = this.operations.indexOps(Entity.class);
		indexOps.delete();
		indexOps.create();
		indexOps.putMapping();
	}

	@Test // #1565
	@DisplayName("should use configured FieldNameStrategy")
	void shouldUseConfiguredFieldNameStrategy() {

		Entity entity = new Entity();
		entity.setId("42");
		entity.setSomeText("the text to be searched");
		operations.save(entity);

		// use a native query here to prevent automatic property name matching
		Query query = new NativeSearchQueryBuilder().withQuery(matchQuery("some_text", "searched")).build();
		SearchHits<Entity> searchHits = operations.search(query, Entity.class);

		assertThat(searchHits.getTotalHits()).isEqualTo(1);
	}

	@Document(indexName = "field-naming-strategy-test")
	static class Entity {
		@Nullable @Id private String id;
		@Nullable @Field(type = FieldType.Text) private String someText;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getSomeText() {
			return someText;
		}

		public void setSomeText(@Nullable String someText) {
			this.someText = someText;
		}
	}
}
