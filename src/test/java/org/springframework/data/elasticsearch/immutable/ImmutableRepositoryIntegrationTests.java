/*
 * Copyright 2016-2024 the original author or authors.
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
package org.springframework.data.elasticsearch.immutable;

import static org.assertj.core.api.Assertions.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.data.repository.CrudRepository;
import org.springframework.lang.Nullable;

/**
 * @author Young Gu
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Christoph Strobl
 * @author Peter-Josef Meisch
 */
@SpringIntegrationTest
public abstract class ImmutableRepositoryIntegrationTests {

	@Autowired ImmutableElasticsearchRepository repository;
	@Autowired ElasticsearchOperations operations;
	@Autowired private IndexNameProvider indexNameProvider;

	@BeforeEach
	public void before() {
		indexNameProvider.increment();
		operations.indexOps(ImmutableEntity.class).createWithMapping();
	}

	@Test
	@Order(java.lang.Integer.MAX_VALUE)
	void cleanup() {
		operations.indexOps(IndexCoordinates.of(indexNameProvider.getPrefix() + "*")).delete();
	}

	@Test // DATAES-281
	public void shouldSaveAndFindImmutableDocument() {

		// when
		ImmutableEntity entity = repository.save(new ImmutableEntity("test name"));
		assertThat(entity.getId()).isNotNull();

		// then
		Optional<ImmutableEntity> entityFromElasticSearch = repository.findById(entity.getId());

		assertThat(entityFromElasticSearch).isPresent();

		entityFromElasticSearch.ifPresent(immutableEntity -> {

			assertThat(immutableEntity.getName()).isEqualTo("test name");
			assertThat(immutableEntity.getId()).isEqualTo(entity.getId());
		});
	}

	@Document(indexName = "#{@indexNameProvider.indexName()}")
	static class ImmutableEntity {
		private final String id, name;

		@PersistenceCreator
		public ImmutableEntity(String id, String name) {
			this.id = id;
			this.name = name;
		}

		public ImmutableEntity(String name) {
			this(null, name);
		}

		public ImmutableEntity withId(@Nullable String id) {
			return new ImmutableEntity(id, this.name);
		}

		public String getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}

	public interface ImmutableElasticsearchRepository extends CrudRepository<ImmutableEntity, String> {}

}
