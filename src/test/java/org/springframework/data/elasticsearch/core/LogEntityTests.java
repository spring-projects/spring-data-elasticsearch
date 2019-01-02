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
package org.springframework.data.elasticsearch.core;

import static org.elasticsearch.index.query.QueryBuilders.*;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;

import org.elasticsearch.action.search.SearchPhaseExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.facet.LogEntity;
import org.springframework.data.elasticsearch.core.facet.LogEntityBuilder;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * LogEntityTests
 *
 * @author Artur Konczak
 * @author Mohsin Husen
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:elasticsearch-template-test.xml")
public class LogEntityTests {

	@Autowired
	private ElasticsearchTemplate template;

	@Before
	public void before() throws ParseException {
		template.deleteIndex(LogEntity.class);
		template.createIndex(LogEntity.class);
		template.putMapping(LogEntity.class);

		SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		IndexQuery indexQuery1 = new LogEntityBuilder("1").action("update").date(dateFormatter.parse("2013-10-18 18:01")).code(2)
				.ip("10.10.10.1").buildIndex();

		IndexQuery indexQuery2 = new LogEntityBuilder("2").action("update").date(dateFormatter.parse("2013-10-19 18:02")).code(2)
				.ip("10.10.10.2").buildIndex();

		IndexQuery indexQuery3 = new LogEntityBuilder("3").action("update").date(dateFormatter.parse("2013-10-19 18:03")).code(2)
				.ip("10.10.10.3").buildIndex();

		IndexQuery indexQuery4 = new LogEntityBuilder("4").action("update").date(dateFormatter.parse("2013-10-19 18:04")).code(2)
				.ip("10.10.10.4").buildIndex();

		template.bulkIndex(Arrays.asList(indexQuery1, indexQuery2, indexQuery3, indexQuery4));
		template.refresh(LogEntity.class);
	}

	/*
	DATAES-66
	*/
	@Test
	public void shouldIndexGivenLogEntityWithIPFieldType() throws ParseException {
		//when
		SearchQuery searchQuery = new NativeSearchQueryBuilder()
				.withQuery(termQuery("ip", "10.10.10.1")).build();

		List<LogEntity> entities = template.queryForList(searchQuery, LogEntity.class);
		//then
		assertThat(entities, is(notNullValue()));
		assertThat(entities.size(), is(1));
	}

	/*
	DATAES-66
	*/
	@Test(expected = SearchPhaseExecutionException.class)
	public void shouldThrowExceptionWhenInvalidIPGivenForSearchQuery() {
		//when
		SearchQuery searchQuery = new NativeSearchQueryBuilder()
				.withQuery(termQuery("ip", "10.10.10")).build();

		List<LogEntity> entities = template.queryForList(searchQuery, LogEntity.class);
		//then
		assertThat(entities, is(notNullValue()));
		assertThat(entities.size(), is(1));
	}

	/*
	DATAES-66
	*/
	@Test
	public void shouldReturnLogsForGivenIPRanges() {
		//when
		SearchQuery searchQuery = new NativeSearchQueryBuilder()
				.withQuery(rangeQuery("ip").from("10.10.10.1").to("10.10.10.3")).build();

		List<LogEntity> entities = template.queryForList(searchQuery, LogEntity.class);
		//then
		assertThat(entities, is(notNullValue()));
		assertThat(entities.size(), is(3));
	}
}
