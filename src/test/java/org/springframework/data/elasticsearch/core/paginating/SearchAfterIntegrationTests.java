/*
 * Copyright 2021-2022 the original author or authors.
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
package org.springframework.data.elasticsearch.core.paginating;

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.lang.Nullable;

/**
 * @author Peter-Josef Meisch
 */
@SpringIntegrationTest
public abstract class SearchAfterIntegrationTests {

	@Autowired private ElasticsearchOperations operations;
	@Autowired private IndexNameProvider indexNameProvider;

	@BeforeEach
	public void before() {

		indexNameProvider.increment();
		operations.indexOps(Entity.class).createWithMapping();
	}

	@Test
	@Order(java.lang.Integer.MAX_VALUE)
	void cleanup() {
		operations.indexOps(IndexCoordinates.of(indexNameProvider.getPrefix() + "*")).delete();
	}

	@Test // #1143
	@DisplayName("should read pages with search_after")
	void shouldReadPagesWithSearchAfter() {

		List<Entity> entities = IntStream.rangeClosed(1, 10).mapToObj(i -> new Entity((long) i, "message " + i))
				.collect(Collectors.toList());
		operations.save(entities);

		Query query = Query.findAll();
		query.setPageable(PageRequest.of(0, 3));
		query.addSort(Sort.by(Sort.Direction.ASC, "id"));

		List<Object> searchAfter = null;
		List<Entity> foundEntities = new ArrayList<>();

		int loop = 0;
		do {
			query.setSearchAfter(searchAfter);
			SearchHits<Entity> searchHits = operations.search(query, Entity.class);

			if (searchHits.getSearchHits().size() == 0) {
				break;
			}
			foundEntities.addAll(searchHits.stream().map(searchHit -> searchHit.getContent()).collect(Collectors.toList()));
			searchAfter = searchHits.getSearchHit((int) (searchHits.getSearchHits().size() - 1)).getSortValues();

			if (++loop > 10) {
				fail("loop not terminating");
			}
		} while (true);

		assertThat(foundEntities).containsExactlyElementsOf(entities);
	}

	@Document(indexName = "#{@indexNameProvider.indexName()}")
	private static class Entity {
		@Nullable
		@Id private Long id;
		@Nullable
		@Field(type = FieldType.Text) private String message;

		public Entity(@Nullable Long id, @Nullable String message) {
			this.id = id;
			this.message = message;
		}

		@Nullable
		public Long getId() {
			return id;
		}

		public void setId(@Nullable Long id) {
			this.id = id;
		}

		@Nullable
		public String getMessage() {
			return message;
		}

		public void setMessage(@Nullable String message) {
			this.message = message;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (!(o instanceof Entity))
				return false;

			Entity entity = (Entity) o;

			if (id != null ? !id.equals(entity.id) : entity.id != null)
				return false;
			return message != null ? message.equals(entity.message) : entity.message == null;
		}

		@Override
		public int hashCode() {
			int result = id != null ? id.hashCode() : 0;
			result = 31 * result + (message != null ? message.hashCode() : 0);
			return result;
		}
	}
}
