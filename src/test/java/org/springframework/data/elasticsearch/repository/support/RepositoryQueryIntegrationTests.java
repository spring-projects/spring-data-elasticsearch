/*
 * Copyright 2025 the original author or authors.
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
package org.springframework.data.elasticsearch.repository.support;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.SearchTemplateQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.script.Script;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.lang.Nullable;

@SpringIntegrationTest
abstract class RepositoryQueryIntegrationTests {
	@Autowired private SampleElasticsearchRepository repository;
	@Autowired private ElasticsearchOperations operations;
	@Autowired private IndexNameProvider indexNameProvider;

	@BeforeEach
	void before() {
		indexNameProvider.increment();
		operations.indexOps(LOTRCharacter.class).createWithMapping();
	}

	@Test
	@org.junit.jupiter.api.Order(Integer.MAX_VALUE)
	public void cleanup() {
		operations.indexOps(IndexCoordinates.of(indexNameProvider.getPrefix() + "*")).delete();
	}

	@Test // #2997
	@DisplayName("should use searchtemplate query")
	void shouldUseSearchtemplateQuery() {
		// store some data
		repository.saveAll(List.of(
				new LOTRCharacter("1", "Frodo is a hobbit"),
				new LOTRCharacter("2", "Legolas is an elf"),
				new LOTRCharacter("3", "Gandalf  is a wizard"),
				new LOTRCharacter("4", "Bilbo is a hobbit"),
				new LOTRCharacter("5", "Gimli is a dwarf")));

		// store a searchtemplate
		String searchInCharacter = """
				{
					"query": {
						"bool": {
							"must": [
								{
									"match": {
										"lotrCharacter": "{{word}}"
									}
								}
							]
						}
					},
					"from": 0,
					"size": 100,
					"sort": {
						"id": {
							"order": "desc"
						}
					}
				  }
				""";

		Script scriptSearchInCharacter = Script.builder() //
				.withId("searchInCharacter") //
				.withLanguage("mustache") //
				.withSource(searchInCharacter) //
				.build();

		var success = operations.putScript(scriptSearchInCharacter);
		assertThat(success).isTrue();

		// search with repository for hobbits order by id descending
		var searchHits = repository.searchInCharacter("hobbit");

		// check result (bilbo, frodo)
		assertThat(searchHits).isNotNull();
		assertThat(searchHits.getTotalHits()).isEqualTo(2);
		assertThat(searchHits.getSearchHit(0).getId()).isEqualTo("4");
		assertThat(searchHits.getSearchHit(1).getId()).isEqualTo("1");
	}

	@Document(indexName = "#{@indexNameProvider.indexName()}")
	static class LOTRCharacter {
		@Nullable
		@Id
		@Field(fielddata = true) // needed for the sort to work
		private String id;

		@Field(type = FieldType.Text)
		@Nullable private String lotrCharacter;

		public LOTRCharacter(@Nullable String id, @Nullable String lotrCharacter) {
			this.id = id;
			this.lotrCharacter = lotrCharacter;
		}

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getLotrCharacter() {
			return lotrCharacter;
		}

		public void setLotrCharacter(@Nullable String lotrCharacter) {
			this.lotrCharacter = lotrCharacter;
		}
	}

	interface SampleElasticsearchRepository
			extends ElasticsearchRepository<LOTRCharacter, String> {
		@SearchTemplateQuery(id = "searchInCharacter")
		SearchHits<LOTRCharacter> searchInCharacter(String word);
	}
}
