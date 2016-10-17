/*
 * Copyright 2013 the original author or authors.
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
 *//*

package org.springframework.data.elasticsearch.core.facet;

import static org.elasticsearch.index.query.QueryBuilders.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.FacetedPage;
import org.springframework.data.elasticsearch.core.facet.request.HistogramFacetRequestBuilder;
import org.springframework.data.elasticsearch.core.facet.result.HistogramResult;
import org.springframework.data.elasticsearch.core.facet.result.IntervalUnit;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

*/
/**
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Jonathan Yan
 * @author Artur Konczak
 *//*

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:elasticsearch-template-test.xml")
public class ElasticsearchTemplateHistogramFacetTests {

	public static final long SEQUECE_CODE_INSERT = 1;
	public static final long SEQUECE_CODE_UPDATE = 2;
	public static final long SEQUECE_CODE_DELETE = 3;
	public static final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
	public static final String DATE_18 = "2013-10-18 18:01";
	public static final String DATE_17 = "2013-10-18 17:01";
	public static final String DATE_16 = "2013-10-18 16:01";


	@Autowired
	private ElasticsearchTemplate elasticsearchTemplate;

	@Before
	public void before() throws ParseException {
		elasticsearchTemplate.deleteIndex(LogEntity.class);
		elasticsearchTemplate.createIndex(LogEntity.class);
		elasticsearchTemplate.putMapping(LogEntity.class);
		elasticsearchTemplate.refresh(LogEntity.class, true);

		IndexQuery entry1 = new LogEntityBuilder("1").action("update").date(dateFormatter.parse(DATE_18)).code(SEQUECE_CODE_UPDATE).buildIndex();
		IndexQuery entry2 = new LogEntityBuilder("2").action("insert").date(dateFormatter.parse(DATE_17)).code(SEQUECE_CODE_INSERT).buildIndex();
		IndexQuery entry3 = new LogEntityBuilder("3").action("update").date(dateFormatter.parse(DATE_17)).code(SEQUECE_CODE_UPDATE).buildIndex();
		IndexQuery entry4 = new LogEntityBuilder("4").action("delete").date(dateFormatter.parse(DATE_16)).code(SEQUECE_CODE_DELETE).buildIndex();

		elasticsearchTemplate.index(entry1);
		elasticsearchTemplate.index(entry2);
		elasticsearchTemplate.index(entry3);
		elasticsearchTemplate.index(entry4);

		elasticsearchTemplate.refresh(LogEntity.class, true);
	}


	@Test
	public void shouldReturnSimpleHistogramFacetForGivenQuery() {
		// given
		String facetName = "sequenceCodeFacet";
		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery())
				.withFacet(new HistogramFacetRequestBuilder(facetName).field("sequenceCode").interval(1).build()
				).build();
		// when
		FacetedPage<LogEntity> result = elasticsearchTemplate.queryForPage(searchQuery, LogEntity.class);
		// then
		assertThat(result.getNumberOfElements(), is(equalTo(4)));

		HistogramResult facet = (HistogramResult) result.getFacet(facetName);
		assertThat(facet.getIntervalUnit().size(), is(equalTo(3)));

		IntervalUnit unit = facet.getIntervalUnit().get(0);
		assertThat(unit.getKey(), is(SEQUECE_CODE_INSERT));
		assertThat(unit.getCount(), is(1L));

		unit = facet.getIntervalUnit().get(1);
		assertThat(unit.getKey(), is(SEQUECE_CODE_UPDATE));
		assertThat(unit.getCount(), is(2L));

		unit = facet.getIntervalUnit().get(2);
		assertThat(unit.getKey(), is(SEQUECE_CODE_DELETE));
		assertThat(unit.getCount(), is(1L));
	}

	@Test
	public void shouldReturnDateHistogramFacetForGivenQuery() throws ParseException {
		// given
		String facetName = "sequenceCodeFacet";
		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery())
				.withFacet(new HistogramFacetRequestBuilder(facetName).field("date").interval(1).timeUnit(TimeUnit.HOURS).build()
				).build();
		// when
		FacetedPage<LogEntity> result = elasticsearchTemplate.queryForPage(searchQuery, LogEntity.class);
		// then
		assertThat(result.getNumberOfElements(), is(equalTo(4)));

		HistogramResult facet = (HistogramResult) result.getFacet(facetName);
		assertThat(facet.getIntervalUnit().size(), is(equalTo(3)));

		IntervalUnit unit = facet.getIntervalUnit().get(0);
		assertThat(unit.getKey(), is(dateFormatter.parse("2013-10-18 16:00").getTime()));
		assertThat(unit.getCount(), is(1L));

		unit = facet.getIntervalUnit().get(1);
		assertThat(unit.getKey(), is(dateFormatter.parse("2013-10-18 17:00").getTime()));
		assertThat(unit.getCount(), is(2L));

		unit = facet.getIntervalUnit().get(2);
		assertThat(unit.getKey(), is(dateFormatter.parse("2013-10-18 18:00").getTime()));
		assertThat(unit.getCount(), is(1L));
	}
}
*/
