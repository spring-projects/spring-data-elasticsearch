/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.data.elasticsearch.core.partition;

import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.entities.PartitionedEntity;
import org.springframework.data.elasticsearch.entities.SpELEntity;
import org.springframework.data.elasticsearch.repositories.partition.PartitionedEntityRepository;
import org.springframework.data.elasticsearch.repositories.spel.SpELRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * SpELEntityTest
 *
 * @author Artur Konczak
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:/partitioned-repository-test.xml")
public class PartitionedEntityTests {

	@Autowired private PartitionedEntityRepository repository;

	@Autowired private ElasticsearchPartitioner partitioner;

	@Autowired private ElasticsearchTemplate template;


	@Before
	public void init(){
		template.deleteIndex("index_2000");
	}

	@Test
	public void testSimpleInsert() {
		PartitionedEntity e = new PartitionedEntity();
		e.setId("2023");
		repository.save(e);
		Assert.assertEquals("2000_2023", e.getId());
		Assert.assertTrue(template.indexExists("index_2000"));

		e.setLabel("test");
		repository.save(e);

		e = repository.findOne("2000_2023");
		Assert.assertNotNull(e);
		Assert.assertEquals("test", e.getLabel());
	}



}
