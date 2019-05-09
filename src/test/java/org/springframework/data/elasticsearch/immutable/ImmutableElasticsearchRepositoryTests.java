/*
 * Copyright 2016-2019 the original author or authors.
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

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.repository.CrudRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Young Gu
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Christoph Strobl
 * @author Peter-Josef Meisch
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:immutable-repository-test.xml")
public class ImmutableElasticsearchRepositoryTests {

	@Autowired ImmutableElasticsearchRepository repository;
	@Autowired ElasticsearchOperations operations;

	@Before
	public void before() {

		operations.deleteIndex(ImmutableEntity.class);
		operations.createIndex(ImmutableEntity.class);
		operations.refresh(ImmutableEntity.class);
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

	/**
	 * @author Young Gu
	 * @author Oliver Gierke
	 */
	@Document(indexName = "test-index-immutable")
	@NoArgsConstructor(force = true)
	@Getter
	static class ImmutableEntity {
		private final String id, name;

		public ImmutableEntity(String name) {

			this.id = null;
			this.name = name;
		}
	}

	/**
	 * @author Young Gu
	 * @author Oliver Gierke
	 */
	public interface ImmutableElasticsearchRepository extends CrudRepository<ImmutableEntity, String> {}

}
