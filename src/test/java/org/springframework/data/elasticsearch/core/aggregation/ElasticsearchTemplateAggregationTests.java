/*
 * Copyright 2013-2019 the original author or authors.
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
package org.springframework.data.elasticsearch.core.aggregation;

import static org.assertj.core.api.Assertions.*;
import static org.elasticsearch.index.query.QueryBuilders.*;
import static org.elasticsearch.search.aggregations.AggregationBuilders.*;
import static org.springframework.data.elasticsearch.annotations.FieldType.*;
import static org.springframework.data.elasticsearch.annotations.FieldType.Integer;

import lombok.Data;

import java.lang.Integer;
import java.util.ArrayList;
import java.util.List;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.search.aggregations.Aggregations;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.ResultsExtractor;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.data.elasticsearch.utils.IndexInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Jonathan Yan
 * @author Artur Konczak
 * @author Peter-Josef Meisch
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:elasticsearch-template-test.xml")
public class ElasticsearchTemplateAggregationTests {

	static final String RIZWAN_IDREES = "Rizwan Idrees";
	static final String MOHSIN_HUSEN = "Mohsin Husen";
	static final String JONATHAN_YAN = "Jonathan Yan";
	static final String ARTUR_KONCZAK = "Artur Konczak";
	static final int YEAR_2002 = 2002;
	static final int YEAR_2001 = 2001;
	static final int YEAR_2000 = 2000;
	static final String INDEX_NAME = "test-index-articles-core-aggregation";

	@Autowired private ElasticsearchTemplate elasticsearchTemplate;

	@Before
	public void before() {

		IndexInitializer.init(elasticsearchTemplate, ArticleEntity.class);

		IndexQuery article1 = new ArticleEntityBuilder("1").title("article four").subject("computing")
				.addAuthor(RIZWAN_IDREES).addAuthor(ARTUR_KONCZAK).addAuthor(MOHSIN_HUSEN).addAuthor(JONATHAN_YAN).score(10)
				.buildIndex();
		IndexQuery article2 = new ArticleEntityBuilder("2").title("article three").subject("computing")
				.addAuthor(RIZWAN_IDREES).addAuthor(ARTUR_KONCZAK).addAuthor(MOHSIN_HUSEN).addPublishedYear(YEAR_2000).score(20)
				.buildIndex();
		IndexQuery article3 = new ArticleEntityBuilder("3").title("article two").subject("computing")
				.addAuthor(RIZWAN_IDREES).addAuthor(ARTUR_KONCZAK).addPublishedYear(YEAR_2001).addPublishedYear(YEAR_2000)
				.score(30).buildIndex();
		IndexQuery article4 = new ArticleEntityBuilder("4").title("article one").subject("accounting")
				.addAuthor(RIZWAN_IDREES).addPublishedYear(YEAR_2002).addPublishedYear(YEAR_2001).addPublishedYear(YEAR_2000)
				.score(40).buildIndex();

		elasticsearchTemplate.index(article1);
		elasticsearchTemplate.index(article2);
		elasticsearchTemplate.index(article3);
		elasticsearchTemplate.index(article4);
		elasticsearchTemplate.refresh(ArticleEntity.class);
	}

	@After
	public void after() {

		elasticsearchTemplate.deleteIndex(ArticleEntity.class);
	}

	@Test
	public void shouldReturnAggregatedResponseForGivenSearchQuery() {

		// given
		SearchQuery searchQuery = new NativeSearchQueryBuilder() //
				.withQuery(matchAllQuery()) //
				.withSearchType(SearchType.DEFAULT) //
				.withIndices(INDEX_NAME).withTypes("article") //
				.addAggregation(terms("subjects").field("subject")) //
				.build();
		// when
		Aggregations aggregations = elasticsearchTemplate.query(searchQuery, new ResultsExtractor<Aggregations>() {
			@Override
			public Aggregations extract(SearchResponse response) {
				return response.getAggregations();
			}
		});
		// then
		assertThat(aggregations).isNotNull();
		assertThat(aggregations.asMap().get("subjects")).isNotNull();
	}

	/**
	 * Simple type to test facets
	 *
	 * @author Artur Konczak
	 * @author Mohsin Husen
	 */
	@Data
	@Document(indexName = "test-index-articles-core-aggregation", type = "article", shards = 1, replicas = 0,
			refreshInterval = "-1")
	static class ArticleEntity {

		@Id private String id;
		private String title;
		@Field(type = Text, fielddata = true) private String subject;

		@MultiField(mainField = @Field(type = Text),
				otherFields = {
						@InnerField(suffix = "untouched", type = Text, store = true, fielddata = true, analyzer = "keyword"),
						@InnerField(suffix = "sort", type = Text, store = true,
								analyzer = "keyword") }) private List<String> authors = new ArrayList<>();

		@Field(type = Integer, store = true) private List<Integer> publishedYears = new ArrayList<>();

		private int score;

		private ArticleEntity() {

		}

		public ArticleEntity(String id) {
			this.id = id;
		}
	}

	/**
	 * Simple type to test facets
	 *
	 * @author Artur Konczak
	 * @author Mohsin Husen
	 */
	static class ArticleEntityBuilder {

		private ArticleEntity result;

		public ArticleEntityBuilder(String id) {
			result = new ArticleEntity(id);
		}

		public ArticleEntityBuilder title(String title) {
			result.setTitle(title);
			return this;
		}

		public ArticleEntityBuilder subject(String subject) {
			result.setSubject(subject);
			return this;
		}

		public ArticleEntityBuilder addAuthor(String author) {
			result.getAuthors().add(author);
			return this;
		}

		public ArticleEntityBuilder addPublishedYear(Integer year) {
			result.getPublishedYears().add(year);
			return this;
		}

		public ArticleEntityBuilder score(int score) {
			result.setScore(score);
			return this;
		}

		public ArticleEntity build() {
			return result;
		}

		public IndexQuery buildIndex() {
			IndexQuery indexQuery = new IndexQuery();
			indexQuery.setId(result.getId());
			indexQuery.setObject(result);
			return indexQuery;
		}
	}

}
