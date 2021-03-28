/*
 * Copyright 2020-2021 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.junit.jupiter.ElasticsearchRestTemplateConfiguration;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.data.elasticsearch.utils.IndexInitializer;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Aleksei Arsenev
 */
@SpringIntegrationTest
@ContextConfiguration(classes = { SearchAsYouTypeTests.Config.class })
public class SearchAsYouTypeTests {
	@Configuration
	@Import({ ElasticsearchRestTemplateConfiguration.class })
	static class Config {}

	@Autowired private ElasticsearchOperations operations;

	@BeforeEach
	private void setup() {
		IndexInitializer.init(operations.indexOps(SearchAsYouTypeEntity.class));
	}

	@AfterEach
	void after() {
		operations.indexOps(SearchAsYouTypeEntity.class).delete();
	}

	private void loadEntities() {
		List<IndexQuery> indexQueries = new ArrayList<>();
		indexQueries.add(new SearchAsYouTypeEntity("1", "test 1", "test 1234").toIndex());
		indexQueries.add(new SearchAsYouTypeEntity("2", "test 2", "test 5678").toIndex());
		indexQueries.add(new SearchAsYouTypeEntity("3", "test 3", "asd 5678").toIndex());
		indexQueries.add(new SearchAsYouTypeEntity("4", "test 4", "not match").toIndex());
		IndexCoordinates index = IndexCoordinates.of("test-index-core-search-as-you-type");
		operations.bulkIndex(indexQueries, index);
		operations.indexOps(SearchAsYouTypeEntity.class).refresh();
	}

	@Test // DATAES-773
	void shouldRetrieveEntityById() {
		loadEntities();
		IndexCoordinates index = IndexCoordinates.of("test-index-core-search-as-you-type");
		operations.get("1", SearchAsYouTypeEntity.class, index);
	}

	@Test // DATAES-773
	void shouldReturnCorrectResultsForTextString() {

		// given
		loadEntities();

		// when
		Query query = new NativeSearchQuery(QueryBuilders.multiMatchQuery("test ", //
				"suggest", "suggest._2gram", "suggest._3gram", "suggest._4gram").type(MultiMatchQueryBuilder.Type.BOOL_PREFIX));
		IndexCoordinates index = IndexCoordinates.of("test-index-core-search-as-you-type");
		List<SearchAsYouTypeEntity> result = operations.search(query, SearchAsYouTypeEntity.class, index) //
				.getSearchHits() //
				.stream() //
				.map(SearchHit::getContent) //
				.collect(Collectors.toList());

		// then
		List<String> ids = result.stream().map(SearchAsYouTypeEntity::getId).collect(Collectors.toList());
		assertThat(ids).containsExactlyInAnyOrder("1", "2");
	}

	@Test // DATAES-773
	void shouldReturnCorrectResultsForNumQuery() {

		// given
		loadEntities();

		// when
		Query query = new NativeSearchQuery(QueryBuilders.multiMatchQuery("5678 ", //
				"suggest", "suggest._2gram", "suggest._3gram", "suggest._4gram").type(MultiMatchQueryBuilder.Type.BOOL_PREFIX));
		IndexCoordinates index = IndexCoordinates.of("test-index-core-search-as-you-type");
		List<SearchAsYouTypeEntity> result = operations.search(query, SearchAsYouTypeEntity.class, index) //
				.getSearchHits() //
				.stream() //
				.map(SearchHit::getContent) //
				.collect(Collectors.toList());

		// then
		List<String> ids = result.stream().map(SearchAsYouTypeEntity::getId).collect(Collectors.toList());
		assertThat(ids).containsExactlyInAnyOrder("2", "3");
	}

	@Test // DATAES-773
	void shouldReturnCorrectResultsForNotMatchQuery() {

		// given
		loadEntities();

		// when
		Query query = new NativeSearchQuery(QueryBuilders.multiMatchQuery("n mat", //
				"suggest", "suggest._2gram", "suggest._3gram", "suggest._4gram").type(MultiMatchQueryBuilder.Type.BOOL_PREFIX));
		IndexCoordinates index = IndexCoordinates.of("test-index-core-search-as-you-type");
		List<SearchAsYouTypeEntity> result = operations.search(query, SearchAsYouTypeEntity.class, index) //
				.getSearchHits() //
				.stream() //
				.map(SearchHit::getContent) //
				.collect(Collectors.toList());

		// then
		assertThat(result.get(0).getId()).isEqualTo("4");
	}

	/**
	 * @author Aleksei Arsenev
	 */
	@Document(indexName = "test-index-core-search-as-you-type")
	static class SearchAsYouTypeEntity {

		public SearchAsYouTypeEntity(@Nonnull String id) {
			this.id = id;
		}

		@Nullable @Id private String id;
		@Nullable private String name;
		@Nullable @Field(type = FieldType.Search_As_You_Type, maxShingleSize = 4) private String suggest;

		public SearchAsYouTypeEntity() {
		}

		public SearchAsYouTypeEntity(String id, @Nullable String name, @Nullable String suggest) {
			this.id = id;
			this.name = name;
			this.suggest = suggest;
		}

		public IndexQuery toIndex() {
			IndexQuery indexQuery = new IndexQuery();
			indexQuery.setId(getId());
			indexQuery.setObject(this);
			return indexQuery;
		}

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getName() {
			return name;
		}

		public void setName(@Nullable String name) {
			this.name = name;
		}

		@Nullable
		public String getSuggest() {
			return suggest;
		}

		public void setSuggest(@Nullable String suggest) {
			this.suggest = suggest;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			SearchAsYouTypeEntity that = (SearchAsYouTypeEntity) o;

			if (id != null ? !id.equals(that.id) : that.id != null) return false;
			if (name != null ? !name.equals(that.name) : that.name != null) return false;
			return suggest != null ? suggest.equals(that.suggest) : that.suggest == null;
		}

		@Override
		public int hashCode() {
			int result = id != null ? id.hashCode() : 0;
			result = 31 * result + (name != null ? name.hashCode() : 0);
			result = 31 * result + (suggest != null ? suggest.hashCode() : 0);
			return result;
		}
	}
}
