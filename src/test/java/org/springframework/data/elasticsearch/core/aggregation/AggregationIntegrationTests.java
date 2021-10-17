/*
 * Copyright 2013-2021 the original author or authors.
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
import static org.elasticsearch.search.aggregations.PipelineAggregatorBuilders.*;
import static org.springframework.data.elasticsearch.annotations.FieldType.*;
import static org.springframework.data.elasticsearch.annotations.FieldType.Integer;

import java.lang.Integer;
import java.util.ArrayList;
import java.util.List;

import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.pipeline.InternalStatsBucket;
import org.elasticsearch.search.aggregations.pipeline.ParsedStatsBucket;
import org.elasticsearch.search.aggregations.pipeline.StatsBucket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;
import org.springframework.data.elasticsearch.backend.elasticsearch7.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.backend.elasticsearch7.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.backend.elasticsearch7.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.AggregationsContainer;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.data.elasticsearch.utils.IndexInitializer;
import org.springframework.lang.Nullable;

/**
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Jonathan Yan
 * @author Artur Konczak
 * @author Peter-Josef Meisch
 */
@SpringIntegrationTest
public abstract class AggregationIntegrationTests {

	static final String RIZWAN_IDREES = "Rizwan Idrees";
	static final String MOHSIN_HUSEN = "Mohsin Husen";
	static final String JONATHAN_YAN = "Jonathan Yan";
	static final String ARTUR_KONCZAK = "Artur Konczak";
	static final int YEAR_2002 = 2002;
	static final int YEAR_2001 = 2001;
	static final int YEAR_2000 = 2000;
	static final String INDEX_NAME = "test-index-articles-core-aggregation";

	@Autowired private ElasticsearchOperations operations;
	private IndexOperations indexOperations;

	@BeforeEach
	public void before() {
		indexOperations = operations.indexOps(ArticleEntity.class);
		IndexInitializer.init(indexOperations);

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

		IndexCoordinates index = IndexCoordinates.of(INDEX_NAME);
		operations.index(article1, index);
		operations.index(article2, index);
		operations.index(article3, index);
		operations.index(article4, index);
		indexOperations.refresh();
	}

	@AfterEach
	public void after() {
		indexOperations.delete();
	}

	@Test // DATAES-96
	public void shouldReturnAggregatedResponseForGivenSearchQuery() {

		// given
		NativeSearchQuery searchQuery = new NativeSearchQueryBuilder() //
				.withQuery(matchAllQuery()) //
				.withSearchType(SearchType.DEFAULT) //
				.addAggregation(terms("subjects").field("subject")) //
				.withMaxResults(0) //
				.build();
		// when
		SearchHits<ArticleEntity> searchHits = operations.search(searchQuery, ArticleEntity.class,
				IndexCoordinates.of(INDEX_NAME));
		AggregationsContainer<?> aggregationsContainer = searchHits.getAggregations();

		// then
		assertThat(aggregationsContainer).isNotNull();
		Aggregations aggregations = ((ElasticsearchAggregations) aggregationsContainer).aggregations();
		assertThat(aggregations.asMap().get("subjects")).isNotNull();
		assertThat(searchHits.hasSearchHits()).isFalse();
	}

	@Test // #1255
	@DisplayName("should work with pipeline aggregations")
	void shouldWorkWithPipelineAggregations() {

		IndexInitializer.init(operations.indexOps(PipelineAggsEntity.class));
		operations.save( //
				new PipelineAggsEntity("1-1", "one"), //
				new PipelineAggsEntity("2-1", "two"), //
				new PipelineAggsEntity("2-2", "two"), //
				new PipelineAggsEntity("3-1", "three"), //
				new PipelineAggsEntity("3-2", "three"), //
				new PipelineAggsEntity("3-3", "three") //
		); //

		NativeSearchQuery searchQuery = new NativeSearchQueryBuilder() //
				.withQuery(matchAllQuery()) //
				.withSearchType(SearchType.DEFAULT) //
				.addAggregation(terms("keyword_aggs").field("keyword")) //
				.addAggregation(statsBucket("keyword_bucket_stats", "keyword_aggs._count")) //
				.withMaxResults(0) //
				.build();

		SearchHits<PipelineAggsEntity> searchHits = operations.search(searchQuery, PipelineAggsEntity.class);

		AggregationsContainer<?> aggregationsContainer = searchHits.getAggregations();
		assertThat(aggregationsContainer).isNotNull();
		Aggregations aggregations = ((ElasticsearchAggregations) aggregationsContainer).aggregations();

		assertThat(aggregations.asMap().get("keyword_aggs")).isNotNull();
		Aggregation keyword_bucket_stats = aggregations.asMap().get("keyword_bucket_stats");
		assertThat(keyword_bucket_stats).isInstanceOf(StatsBucket.class);
		if (keyword_bucket_stats instanceof ParsedStatsBucket) {
			// Rest client
			ParsedStatsBucket statsBucket = (ParsedStatsBucket) keyword_bucket_stats;
			assertThat(statsBucket.getMin()).isEqualTo(1.0);
			assertThat(statsBucket.getMax()).isEqualTo(3.0);
			assertThat(statsBucket.getAvg()).isEqualTo(2.0);
			assertThat(statsBucket.getSum()).isEqualTo(6.0);
			assertThat(statsBucket.getCount()).isEqualTo(3L);
		}
		if (keyword_bucket_stats instanceof InternalStatsBucket) {
			// transport client
			InternalStatsBucket statsBucket = (InternalStatsBucket) keyword_bucket_stats;
			assertThat(statsBucket.getMin()).isEqualTo(1.0);
			assertThat(statsBucket.getMax()).isEqualTo(3.0);
			assertThat(statsBucket.getAvg()).isEqualTo(2.0);
			assertThat(statsBucket.getSum()).isEqualTo(6.0);
			assertThat(statsBucket.getCount()).isEqualTo(3L);
		}
	}

	// region entities
	@Document(indexName = "test-index-articles-core-aggregation")
	static class ArticleEntity {

		@Nullable @Id private String id;
		@Nullable private String title;
		@Nullable @Field(type = Text, fielddata = true) private String subject;

		@Nullable @MultiField(mainField = @Field(type = Text),
				otherFields = {
						@InnerField(suffix = "untouched", type = Text, store = true, fielddata = true, analyzer = "keyword"),
						@InnerField(suffix = "sort", type = Text, store = true,
								analyzer = "keyword") }) private List<String> authors = new ArrayList<>();

		@Nullable @Field(type = Integer, store = true) private List<Integer> publishedYears = new ArrayList<>();

		@Nullable private int score;

		public ArticleEntity(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getTitle() {
			return title;
		}

		public void setTitle(@Nullable String title) {
			this.title = title;
		}

		@Nullable
		public String getSubject() {
			return subject;
		}

		public void setSubject(@Nullable String subject) {
			this.subject = subject;
		}

		@Nullable
		public List<String> getAuthors() {
			return authors;
		}

		public void setAuthors(@Nullable List<String> authors) {
			this.authors = authors;
		}

		@Nullable
		public List<java.lang.Integer> getPublishedYears() {
			return publishedYears;
		}

		public void setPublishedYears(@Nullable List<java.lang.Integer> publishedYears) {
			this.publishedYears = publishedYears;
		}

		public int getScore() {
			return score;
		}

		public void setScore(int score) {
			this.score = score;
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

	@Document(indexName = "pipeline-aggs")
	static class PipelineAggsEntity {
		@Id private String id;
		@Field(type = Keyword) private String keyword;

		public PipelineAggsEntity() {}

		public PipelineAggsEntity(String id, String keyword) {
			this.id = id;
			this.keyword = keyword;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getKeyword() {
			return keyword;
		}

		public void setKeyword(String keyword) {
			this.keyword = keyword;
		}
	}
	// endregion

}
