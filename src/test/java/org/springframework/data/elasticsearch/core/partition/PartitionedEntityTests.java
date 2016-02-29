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

import org.elasticsearch.index.query.QueryBuilders;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.ElasticsearchException;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.FacetedPage;
import org.springframework.data.elasticsearch.core.partition.keys.DatePartition;
import org.springframework.data.elasticsearch.core.partition.keys.LongPartition;
import org.springframework.data.elasticsearch.core.partition.keys.Partition;
import org.springframework.data.elasticsearch.core.partition.keys.StringPartition;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.entities.partition.*;
import org.springframework.data.elasticsearch.repositories.partition.*;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

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

	SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

	@Autowired
	private LongPartitionedEntityRepository longPartitionedEntityRepository;

	@Autowired
	private StringPartitionedEntityRepository stringPartitionedEntityRepository;

	@Autowired
	private DatePartitionedEntityRepository datePartitionedEntityRepository;

	@Autowired
	private CompositePartitionedEntityRepository compositePartitionedEntityRepository;

	@Autowired
	private SpelPartitionedEntityRepository spelPartitionedEntityRepository;

	@Autowired
	private ElasticsearchPartitioner partitioner;

	@Autowired
	private ElasticsearchPartitionsCache cache;

	@Autowired
	private ElasticsearchTemplate template;


	@Before
	public void init() {
		List<String> indices = cache.listIndicesForPrefix("index_");
		for (String indice : indices) {
			template.deleteIndex(indice);
		}
	}

	@Test
	public void testLongPartition() {
		LongPartitionedEntity e = new LongPartitionedEntity();
		e.setId("2023");
		longPartitionedEntityRepository.save(e);
		Assert.assertEquals("2000_2023", e.getId());
		Assert.assertTrue(template.indexExists("index_2000"));

		e.setLabel("test");
		longPartitionedEntityRepository.save(e);

		e = longPartitionedEntityRepository.findOne("2000_2023");
		Assert.assertNotNull(e);
		Assert.assertEquals("test", e.getLabel());
	}

	@Test
	public void testDatePartition() throws ParseException {
		DatePartitionedEntity e = new DatePartitionedEntity();
		e.setId("2023");
		e.setCreationDate(sdf.parse("02/03/2015"));
		datePartitionedEntityRepository.save(e);
		Assert.assertEquals("201503_2023", e.getId());
		Assert.assertTrue(template.indexExists("index_201503"));

		e.setLabel("test");
		datePartitionedEntityRepository.save(e);

		e = datePartitionedEntityRepository.findOne("201503_2023");
		Assert.assertNotNull(e);
		Assert.assertEquals("test", e.getLabel());
	}

	@Test
	public void testStringPartition() throws ParseException {
		StringPartitionedEntity e = new StringPartitionedEntity();
		e.setId("2023");
		e.setCustomerId("johndoe");
		stringPartitionedEntityRepository.save(e);
		Assert.assertEquals("johndoe_2023", e.getId());
		Assert.assertTrue(template.indexExists("index_johndoe"));

		e.setLabel("test");
		stringPartitionedEntityRepository.save(e);

		e = stringPartitionedEntityRepository.findOne("johndoe_2023");
		Assert.assertNotNull(e);
		Assert.assertEquals("test", e.getLabel());
	}

	@Test(expected = ElasticsearchException.class)
	public void testMissingKey() throws ParseException {
		StringPartitionedEntity e = new StringPartitionedEntity();
		e.setId("2023");
		stringPartitionedEntityRepository.save(e);
	}

	@Test
	public void testSearchQueries() throws ParseException {
		LongPartitionedEntity e1 = new LongPartitionedEntity();
		e1.setId("1500");
		longPartitionedEntityRepository.save(e1);
		LongPartitionedEntity e2 = new LongPartitionedEntity();
		e2.setId("3500");
		longPartitionedEntityRepository.save(e2);
		Partition boundary = new LongPartition("id", 1000, 5000);
		NativeSearchQueryBuilder builder = new NativeSearchQueryBuilder();
		builder.withPartition(boundary);
		builder.withQuery(QueryBuilders.matchAllQuery());
		FacetedPage<LongPartitionedEntity> page = longPartitionedEntityRepository.search(builder.build());
		Assert.assertEquals(2, page.getContent().size());

		builder = new NativeSearchQueryBuilder();
		builder.withPartition(new LongPartition("id", 1000, 2000));
		builder.withQuery(QueryBuilders.matchAllQuery());
		page = longPartitionedEntityRepository.search(builder.build());
		Assert.assertEquals(1, page.getContent().size());

		DatePartitionedEntity d1 = new DatePartitionedEntity();
		d1.setId("1500");
		d1.setCreationDate(sdf.parse("02/03/2015"));
		datePartitionedEntityRepository.save(d1);

		DatePartitionedEntity d2 = new DatePartitionedEntity();
		d2.setId("2500");
		d2.setCreationDate(sdf.parse("15/06/2015"));
		datePartitionedEntityRepository.save(d2);

		builder = new NativeSearchQueryBuilder();
		builder.withPartition(new DatePartition("creationDate", sdf.parse("01/02/2015"), sdf.parse("01/07/2015")));
		builder.withQuery(QueryBuilders.matchAllQuery());
		FacetedPage<DatePartitionedEntity> dateFacetedPage = datePartitionedEntityRepository.search(builder.build());
		Assert.assertEquals(2, dateFacetedPage.getContent().size());


		//Full Scan
		Page<DatePartitionedEntity> datePage = datePartitionedEntityRepository.findAll(new PageRequest(0,10));
		Assert.assertEquals(2, datePage.getContent().size());

	}

	@Test
	public void testCriteriaQueries() throws ParseException {
		LongPartitionedEntity e1 = new LongPartitionedEntity();
		e1.setId("1500");
		e1.setLabel("test1");
		longPartitionedEntityRepository.save(e1);
		LongPartitionedEntity e2 = new LongPartitionedEntity();
		e2.setId("3500");
		e2.setLabel("test2");
		longPartitionedEntityRepository.save(e2);
		Criteria criteria = new Criteria("label").contains("test1").or("label").contains("test2");
		CriteriaQuery query = new CriteriaQuery(criteria);
		query.withPartition(new LongPartition("id", 1000, 5000));

		Page<LongPartitionedEntity> page = template.queryForPage(query, LongPartitionedEntity.class);
		Assert.assertEquals(2, page.getContent().size());
	}

	@Test
	public void testRepositorieQueries() throws ParseException {
		StringPartitionedEntity e1 = new StringPartitionedEntity();
		e1.setCustomerId("johndoe");
		stringPartitionedEntityRepository.save(e1);

		List<StringPartitionedEntity> entities = stringPartitionedEntityRepository.findByCustomerId("johndoe", new StringPartition("customerId", new String[]{"johndoe"}), new PageRequest(0,10));
	}

	@Test
	public void testCompositePartition() throws ParseException {
		CompositePartitionedEntity e = new CompositePartitionedEntity();
		e.setId("2023");
		e.setCustomerId("johndoe");
		e.setCreationDate(sdf.parse("15/06/2015"));
		compositePartitionedEntityRepository.save(e);
		Assert.assertEquals("johndoe_201506_2023", e.getId());
		Assert.assertTrue(template.indexExists("index_johndoe_201506"));

		e.setLabel("test");
		compositePartitionedEntityRepository.save(e);

		e = compositePartitionedEntityRepository.findOne("johndoe_201506_2023");
		Assert.assertNotNull(e);
		Assert.assertEquals("test", e.getLabel());

		Iterable<CompositePartitionedEntity> iterable = compositePartitionedEntityRepository.findAll();
		Assert.assertTrue(iterable.iterator().hasNext());

		NativeSearchQueryBuilder builder = new NativeSearchQueryBuilder();
		builder.withPartition(new StringPartition("customerId", new String[]{"johndoe"}));
		builder.withPartition(new DatePartition("creationDate", sdf.parse("01/02/2015"), sdf.parse("01/07/2015")));
		builder.withQuery(QueryBuilders.matchAllQuery());
		FacetedPage<CompositePartitionedEntity> page = compositePartitionedEntityRepository.search(builder.build());
		Assert.assertEquals(1, page.getContent().size());

	}

	@Test
	public void testSpelEntitie() throws ParseException  {
		SpelPartitionedEntity e = new SpelPartitionedEntity();
		e.setId("2023005");
		e.setCustomerId("johndoe");
		e.setCreationDate(sdf.parse("15/06/2015"));
		spelPartitionedEntityRepository.save(e);
		Assert.assertEquals("johndoe-2015-2000000-2023005", e.getId());
		Assert.assertTrue(template.indexExists("index-johndoe-2015-2000000"));

		e.setLabel("test");
		spelPartitionedEntityRepository.save(e);

		e = spelPartitionedEntityRepository.findOne("johndoe-2015-2000000-2023005");
		Assert.assertNotNull(e);
		Assert.assertEquals("test", e.getLabel());
/*
		List<SpelPartitionedEntity> l = spelPartitionedEntityRepository.findByLabel("test");
		Assert.assertEquals(1, l.size());

		Partition[] partitions = new Partition[]{
				new StringPartition("customerId", new String[]{"johndoe"}),
				new DatePartition("creationDate", sdf.parse("01/02/2015"), sdf.parse("01/07/2015")),
				new LongPartition("id", 1000000, 3000000)
		};

		List<SpelPartitionedEntity> l2 = spelPartitionedEntityRepository.findByLabel("test", partitions);
		Assert.assertEquals(1, l2.size());
*/
		Partition[] partitions2 = new Partition[]{
				new StringPartition("customerId", new String[]{"johndoe"}),
				new DatePartition("creationDate", sdf.parse("01/02/2014"), sdf.parse("01/07/2014")),
				new LongPartition("id", 1000000, 3000000)
		};

		List<SpelPartitionedEntity> l3 = spelPartitionedEntityRepository.findByLabel("test", partitions2);
		Assert.assertEquals(0, l3.size());
	}
}