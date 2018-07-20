/*
 * Copyright 2016-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.elasticsearch.immutable;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.Optional;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Young Gu
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Christoph Strobl
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

	/**
	 * @see DATAES-281
	 */
	@Test
	@Ignore("fix me - UnsupportedOperation")
	public void shouldSaveAndFindImmutableDocument() {

		// when
		ImmutableEntity entity = repository.save(new ImmutableEntity("test name"));
		assertThat(entity.getId(), is(notNullValue()));

		// then
		Optional<ImmutableEntity> entityFromElasticSearch = repository.findById(entity.getId());

		assertThat(entityFromElasticSearch.isPresent(), is(true));

		entityFromElasticSearch.ifPresent(immutableEntity -> {

			assertThat(immutableEntity.getName(), is("test name"));
			assertThat(immutableEntity.getId(), is(entity.getId()));
		});

	}
}
