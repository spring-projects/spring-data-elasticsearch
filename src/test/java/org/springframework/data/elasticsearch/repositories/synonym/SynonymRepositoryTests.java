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
package org.springframework.data.elasticsearch.repositories.synonym;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.List;

import org.elasticsearch.index.query.QueryBuilders;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.entities.SynonymEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * SynonymRepositoryTests
 *
 * @author Artur Konczak
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:synonym-test.xml")
public class SynonymRepositoryTests {

	@Autowired
	private SynonymRepository repository;

	@Autowired
	private ElasticsearchTemplate elasticsearchTemplate;

	@Before
	public void before() {
		elasticsearchTemplate.deleteIndex(SynonymEntity.class);
		elasticsearchTemplate.createIndex(SynonymEntity.class);
		elasticsearchTemplate.putMapping(SynonymEntity.class);
		elasticsearchTemplate.refresh(SynonymEntity.class);
	}


	@Test
	public void shouldDo() {
		//given
		SynonymEntity entry1 = new SynonymEntity();
		entry1.setText("Elizabeth is the English queen");
		SynonymEntity entry2 = new SynonymEntity();
		entry2.setText("Other text");

		repository.save(entry1);
		repository.save(entry2);
		//when

		//then
		assertThat(repository.count(),is(2L));

		List<SynonymEntity> synonymEntities = elasticsearchTemplate.queryForList(new NativeSearchQueryBuilder().withQuery(QueryBuilders.termQuery("text", "british")).build(), SynonymEntity.class);
		assertThat(synonymEntities.size(), is(1));
	}

}
