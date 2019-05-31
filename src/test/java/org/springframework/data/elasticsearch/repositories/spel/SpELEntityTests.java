/*
 * Copyright 2014-2019 the original author or authors.
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
package org.springframework.data.elasticsearch.repositories.spel;

import static org.assertj.core.api.Assertions.*;

import org.elasticsearch.index.query.QueryBuilders;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.elasticsearch.utils.IndexInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * SpELEntityTest
 *
 * @author Artur Konczak
 * @author Peter-Josef Meisch
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:/spel-repository-test.xml")
public class SpELEntityTests {

	@Autowired private SpELRepository repository;

	@Autowired private ElasticsearchTemplate template;

	@Before
	public void before() {
		IndexInitializer.init(template, SpELEntity.class);
	}

	@Test
	public void shouldDo() {

		// given
		repository.save(new SpELEntity());
		repository.save(new SpELEntity());

		// when

		// then
		NativeSearchQuery nativeSearchQuery = new NativeSearchQuery(QueryBuilders.matchAllQuery());
		nativeSearchQuery.addIndices("test-index-abz-entity");
		long count = template.count(nativeSearchQuery);
		assertThat(count).isEqualTo(2);
	}

	@Test
	public void shouldSupportSpelInType() {

		// given
		SpELEntity spELEntity = new SpELEntity();
		repository.save(spELEntity);

		// when

		// then
		NativeSearchQuery nativeSearchQuery = new NativeSearchQuery(QueryBuilders.matchAllQuery());
		nativeSearchQuery.addIndices("test-index-abz-entity");
		nativeSearchQuery.addTypes("myType");
		long count = template.count(nativeSearchQuery);
		assertThat(count).isEqualTo(1);
	}

	/**
	 * SpELEntity
	 *
	 * @author Artur Konczak
	 */
	@Document(indexName = "#{'test-index-abz'+'-'+'entity'}", type = "#{'my'+'Type'}", shards = 1, replicas = 0,
			refreshInterval = "-1")
	static class SpELEntity {

		@Id private String id;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}
	}

	interface SpELRepository extends ElasticsearchRepository<SpELEntity, String> {}
}
