package org.springframework.data.elasticsearch.repositories.existing.index;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:/existing-index-repository-test.xml")
public class CreateIndexFalseRepositoryTests {

	@Autowired
	private CreateIndexFalseRepository repository;

	@Autowired
	private ElasticsearchTemplate elasticsearchTemplate;


	/*
		DATAES-143
	*/
	@Test
	public void shouldNotCreateIndex() {
		//given

		//when
		//then
		assertFalse(elasticsearchTemplate.indexExists(CreateIndexFalseEntity.class));
	}
}