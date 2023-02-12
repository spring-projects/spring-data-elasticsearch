/*
 * Copyright 2023 the original author or authors.
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
package org.springframework.data.elasticsearch.core.query;

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
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.lang.Nullable;

/**
 * @author Peter-Josef Meisch
 */
@SpringIntegrationTest
public abstract class NativeQueryIntegrationTests {
	@Autowired private ElasticsearchOperations operations;
	@Autowired private IndexNameProvider indexNameProvider;

	@BeforeEach
	public void before() {
		indexNameProvider.increment();
		operations.indexOps(SampleEntity.class).createWithMapping();
	}

	@Test
	@Order(java.lang.Integer.MAX_VALUE)
	void cleanup() {
		operations.indexOps(IndexCoordinates.of(indexNameProvider.getPrefix() + "*")).delete();
	}

	@Test // #2391
	@DisplayName("should be able to use CriteriaQuery in a NativeQuery")
	void shouldBeAbleToUseCriteriaQueryInANativeQuery() {

		var entity = new SampleEntity();
		entity.setId("7");
		entity.setText("seven");
		operations.save(entity);
		entity = new SampleEntity();
		entity.setId("42");
		entity.setText("criteria");
		operations.save(entity);

		var criteriaQuery = CriteriaQuery.builder(Criteria.where("text").is("criteria")).build();
		var nativeQuery = NativeQuery.builder().withQuery(criteriaQuery).build();

		var searchHits = operations.search(nativeQuery, SampleEntity.class);

		assertThat(searchHits.getTotalHits()).isEqualTo(1);
		assertThat(searchHits.getSearchHit(0).getId()).isEqualTo(entity.getId());
	}

	@Test // #2391
	@DisplayName("should be able to use StringQuery in a NativeQuery")
	void shouldBeAbleToUseStringQueryInANativeQuery() {

		var entity = new SampleEntity();
		entity.setId("7");
		entity.setText("seven");
		operations.save(entity);
		entity = new SampleEntity();
		entity.setId("42");
		entity.setText("string");
		operations.save(entity);

		var stringQuery = StringQuery.builder("""
				{
				    "bool": {
				      "must": [
				        {
				          "match": {
				            "text": "string"
				          }
				        }
				      ]
				    }
				  }
								""").build();
		var nativeQuery = NativeQuery.builder().withQuery(stringQuery).build();

		var searchHits = operations.search(nativeQuery, SampleEntity.class);

		assertThat(searchHits.getTotalHits()).isEqualTo(1);
		assertThat(searchHits.getSearchHit(0).getId()).isEqualTo(entity.getId());
	}

	@Document(indexName = "#{@indexNameProvider.indexName()}")
	static class SampleEntity {

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}

		@Nullable
		@Id private String id;

		@Field(type = FieldType.Text) private String text;
	}
}
