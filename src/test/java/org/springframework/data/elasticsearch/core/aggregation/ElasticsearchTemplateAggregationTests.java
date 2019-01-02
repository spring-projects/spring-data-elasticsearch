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
package org.springframework.data.elasticsearch.core.aggregation;

import static org.elasticsearch.action.search.SearchType.*;
import static org.elasticsearch.index.query.QueryBuilders.*;
import static org.elasticsearch.search.aggregations.AggregationBuilders.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.search.aggregations.Aggregations;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.ResultsExtractor;
import org.springframework.data.elasticsearch.core.facet.ArticleEntity;
import org.springframework.data.elasticsearch.core.facet.ArticleEntityBuilder;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.search.aggregations.AggregationBuilders.terms;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Jonathan Yan
 * @author Artur Konczak
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:elasticsearch-template-test.xml")
public class ElasticsearchTemplateAggregationTests {

	public static final String RIZWAN_IDREES = "Rizwan Idrees";
	public static final String MOHSIN_HUSEN = "Mohsin Husen";
	public static final String JONATHAN_YAN = "Jonathan Yan";
	public static final String ARTUR_KONCZAK = "Artur Konczak";
	public static final int YEAR_2002 = 2002;
	public static final int YEAR_2001 = 2001;
	public static final int YEAR_2000 = 2000;
	@Autowired
	private ElasticsearchTemplate elasticsearchTemplate;

	@Before
	public void before() {
		elasticsearchTemplate.deleteIndex(ArticleEntity.class);
		elasticsearchTemplate.createIndex(ArticleEntity.class);
		elasticsearchTemplate.putMapping(ArticleEntity.class);
		elasticsearchTemplate.refresh(ArticleEntity.class);

		IndexQuery article1 = new ArticleEntityBuilder("1").title("article four").subject("computing").addAuthor(RIZWAN_IDREES).addAuthor(ARTUR_KONCZAK).addAuthor(MOHSIN_HUSEN).addAuthor(JONATHAN_YAN).score(10).buildIndex();
		IndexQuery article2 = new ArticleEntityBuilder("2").title("article three").subject("computing").addAuthor(RIZWAN_IDREES).addAuthor(ARTUR_KONCZAK).addAuthor(MOHSIN_HUSEN).addPublishedYear(YEAR_2000).score(20).buildIndex();
		IndexQuery article3 = new ArticleEntityBuilder("3").title("article two").subject("computing").addAuthor(RIZWAN_IDREES).addAuthor(ARTUR_KONCZAK).addPublishedYear(YEAR_2001).addPublishedYear(YEAR_2000).score(30).buildIndex();
		IndexQuery article4 = new ArticleEntityBuilder("4").title("article one").subject("accounting").addAuthor(RIZWAN_IDREES).addPublishedYear(YEAR_2002).addPublishedYear(YEAR_2001).addPublishedYear(YEAR_2000).score(40).buildIndex();

		elasticsearchTemplate.index(article1);
		elasticsearchTemplate.index(article2);
		elasticsearchTemplate.index(article3);
		elasticsearchTemplate.index(article4);
		elasticsearchTemplate.refresh(ArticleEntity.class);
	}

	@Test
	public void shouldReturnAggregatedResponseForGivenSearchQuery() {
		// given
		SearchQuery searchQuery = new NativeSearchQueryBuilder()
				.withQuery(matchAllQuery())
				.withSearchType(SearchType.DEFAULT)
				.withIndices("test-index-articles").withTypes("article")
				.addAggregation(terms("subjects").field("subject"))
				.build();
		// when
		Aggregations aggregations = elasticsearchTemplate.query(searchQuery, new ResultsExtractor<Aggregations>() {
			@Override
			public Aggregations extract(SearchResponse response) {
				return response.getAggregations();
			}
		});
		// then
		assertThat(aggregations, is(notNullValue()));
		assertThat(aggregations.asMap().get("subjects"), is(notNullValue()));
	}
}


