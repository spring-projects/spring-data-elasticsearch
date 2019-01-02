/*
 * Copyright 2013-2019 the original author or authors.
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
package org.springframework.data.elasticsearch.repository.support;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Optional;

import org.apache.commons.lang.math.RandomUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.entities.IntegerIDEntity;
import org.springframework.data.elasticsearch.repositories.integer.IntegerIDRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Mark Paluch
 * @author Christoph Strobl
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:/integer-id-repository-test.xml")
public class IntegerIDRepositoryTests {

	@Autowired private IntegerIDRepository repository;

	@Autowired private ElasticsearchTemplate elasticsearchTemplate;

	@Before
	public void before() {
		elasticsearchTemplate.deleteIndex(IntegerIDEntity.class);
		elasticsearchTemplate.createIndex(IntegerIDEntity.class);
		elasticsearchTemplate.putMapping(IntegerIDEntity.class);
		elasticsearchTemplate.refresh(IntegerIDEntity.class);
	}

	@Test
	public void shouldDoBulkIndexDocument() {
		// given
		Integer documentId1 = RandomUtils.nextInt();
		IntegerIDEntity sampleEntity1 = new IntegerIDEntity();
		sampleEntity1.setId(documentId1);
		sampleEntity1.setMessage("some message");
		sampleEntity1.setVersion(System.currentTimeMillis());

		Integer documentId2 = RandomUtils.nextInt();
		IntegerIDEntity sampleEntity2 = new IntegerIDEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setMessage("some message");
		sampleEntity2.setVersion(System.currentTimeMillis());

		// when
		repository.saveAll(Arrays.asList(sampleEntity1, sampleEntity2));
		// then
		Optional<IntegerIDEntity> entity1FromElasticSearch = repository.findById(documentId1);
		assertThat(entity1FromElasticSearch.isPresent(), is(true));

		Optional<IntegerIDEntity> entity2FromElasticSearch = repository.findById(documentId2);
		assertThat(entity2FromElasticSearch.isPresent(), is(true));
	}

	@Test
	public void shouldSaveDocument() {
		// given
		Integer documentId = RandomUtils.nextInt();
		IntegerIDEntity sampleEntity = new IntegerIDEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("some message");
		sampleEntity.setVersion(System.currentTimeMillis());
		// when
		repository.save(sampleEntity);
		// then
		Optional<IntegerIDEntity> entityFromElasticSearch = repository.findById(documentId);
		assertThat(entityFromElasticSearch.isPresent(), is(true));
	}
}
