/*
 * Copyright 2014-2019 the original author or authors.
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
package org.springframework.data.elasticsearch;

import static org.hamcrest.core.Is.*;
import static org.junit.Assert.*;

import org.elasticsearch.index.query.QueryBuilders;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.entities.SpELEntity;
import org.springframework.data.elasticsearch.repositories.spel.SpELRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * SpELEntityTest
 *
 * @author Artur Konczak
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:/spel-repository-test.xml")
public class SpELEntityTests {

	@Autowired private SpELRepository repository;

	@Autowired private ElasticsearchTemplate template;

	@Before
	public void init() {
		repository.deleteAll();
	}

	@Ignore("DATAES-211")
	@Test
	public void shouldDo() {
		// Given
		repository.save(new SpELEntity());
		repository.save(new SpELEntity());
		// When

		// Then
		NativeSearchQuery nativeSearchQuery = new NativeSearchQuery(QueryBuilders.matchAllQuery());
		nativeSearchQuery.addIndices("abz-entity");
		long count = template.count(nativeSearchQuery);
		assertThat(count, is(2L));
	}

	@Ignore("DATAES-211")
	@Test
	public void shouldSupportSpelInType() {
		// Given
		SpELEntity spELEntity = new SpELEntity();
		repository.save(spELEntity);

		// When

		// Then
		NativeSearchQuery nativeSearchQuery = new NativeSearchQuery(QueryBuilders.matchAllQuery());
		nativeSearchQuery.addIndices("abz-entity");
		nativeSearchQuery.addTypes("myType");
		long count = template.count(nativeSearchQuery);
		assertThat(count, is(1L));
	}
}
