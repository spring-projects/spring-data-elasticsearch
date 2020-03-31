/*
 * Copyright 2020 the original author or authors.
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

import static org.junit.jupiter.api.Assertions.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

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
		indexQueries.add(SearchAsYouTypeEntity.builder().id("1").name("test 1").suggest("test 1234").build().toIndex());
		indexQueries.add(SearchAsYouTypeEntity.builder().id("2").name("test 2").suggest("test 5678").build().toIndex());
		indexQueries.add(SearchAsYouTypeEntity.builder().id("3").name("test 3").suggest("asd 5678").build().toIndex());
		indexQueries.add(SearchAsYouTypeEntity.builder().id("4").name("test 4").suggest("not match").build().toIndex());
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
		assertEquals(2, result.size());
		assertTrue(result.contains(new SearchAsYouTypeEntity("1")));
		assertTrue(result.contains(new SearchAsYouTypeEntity("2")));
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
		assertEquals(2, result.size());
		assertTrue(result.contains(new SearchAsYouTypeEntity("2")));
		assertTrue(result.contains(new SearchAsYouTypeEntity("3")));
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
		assertEquals(1, result.size());
		assertTrue(result.contains(new SearchAsYouTypeEntity("4")));
	}

	/**
	 * @author Aleksei Arsenev
	 */
	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	@EqualsAndHashCode(onlyExplicitlyIncluded = true)
	@Document(indexName = "test-index-core-search-as-you-type", replicas = 0, refreshInterval = "-1")
	static class SearchAsYouTypeEntity {

		public SearchAsYouTypeEntity(@Nonnull String id) {
			this.id = id;
		}

		@NonNull @Id @EqualsAndHashCode.Include private String id;

		@Nullable private String name;

		@Nullable @Field(type = FieldType.Search_As_You_Type, maxShingleSize = 4) private String suggest;

		public IndexQuery toIndex() {
			IndexQuery indexQuery = new IndexQuery();
			indexQuery.setId(getId());
			indexQuery.setObject(this);
			return indexQuery;
		}
	}
}
