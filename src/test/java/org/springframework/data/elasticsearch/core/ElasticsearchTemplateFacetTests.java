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
 */
package org.springframework.data.elasticsearch.core;

import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.search.facet.FacetBuilders;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.Article;
import org.springframework.data.elasticsearch.ArticleBuilder;
import org.springframework.data.elasticsearch.core.facet.Term;
import org.springframework.data.elasticsearch.core.facet.TermResult;
import org.springframework.data.elasticsearch.core.query.*;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.elasticsearch.index.query.FilterBuilders.termFilter;
import static org.elasticsearch.index.query.QueryBuilders.fieldQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Jonathan Yan
 * @author Artur Konczak
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:elasticsearch-template-test.xml")
public class ElasticsearchTemplateFacetTests {

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
        elasticsearchTemplate.deleteIndex(Article.class);
        elasticsearchTemplate.createIndex(Article.class);
        elasticsearchTemplate.putMapping(Article.class);
        elasticsearchTemplate.refresh(Article.class, true);

        IndexQuery article1 = new ArticleBuilder("1").title("article four").addAuthor(RIZWAN_IDREES).addAuthor(ARTUR_KONCZAK).addAuthor(MOHSIN_HUSEN).addAuthor(JONATHAN_YAN).buildIndex();
        IndexQuery article2 = new ArticleBuilder("2").title("article three").addAuthor(RIZWAN_IDREES).addAuthor(ARTUR_KONCZAK).addAuthor(MOHSIN_HUSEN).addPublishedYear(YEAR_2000).buildIndex();
        IndexQuery article3 = new ArticleBuilder("3").title("article two").addAuthor(RIZWAN_IDREES).addAuthor(ARTUR_KONCZAK).addPublishedYear(YEAR_2001).addPublishedYear(YEAR_2000).buildIndex();
        IndexQuery article4 = new ArticleBuilder("4").title("article one").addAuthor(RIZWAN_IDREES).addPublishedYear(YEAR_2002).addPublishedYear(YEAR_2001).addPublishedYear(YEAR_2000).buildIndex();

        elasticsearchTemplate.index(article1);
        elasticsearchTemplate.index(article2);
        elasticsearchTemplate.index(article3);
        elasticsearchTemplate.index(article4);
        elasticsearchTemplate.refresh(Article.class, true);
    }

    @Test
    public void shouldReturnFacetedAuthorsForGivenQueryWithDefaultOrder() {

        // given
        String facetName = "fauthors";
        SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery()).withFacet(new TermFacetRequestBuilder(facetName).withStringFields("authors").build()).build();
        // when
        FacetedPage<Article> result = elasticsearchTemplate.queryForPage(searchQuery, Article.class);
        // then
        assertThat(result.getNumberOfElements(), is(equalTo(4)));

        TermResult facet = (TermResult) result.getFacet(facetName);
        Term term = facet.getTerms().get(0);
        assertThat(term.getTerm(), is(RIZWAN_IDREES));
        assertThat(term.getCount(), is(4));

        term = facet.getTerms().get(1);
        assertThat(term.getTerm(), is(ARTUR_KONCZAK));
        assertThat(term.getCount(), is(3));

        term = facet.getTerms().get(2);
        assertThat(term.getTerm(), is(MOHSIN_HUSEN));
        assertThat(term.getCount(), is(2));

        term = facet.getTerms().get(3);
        assertThat(term.getTerm(), is(JONATHAN_YAN));
        assertThat(term.getCount(), is(1));
    }

    @Test
    public void shouldReturnFacetedAuthorsForGivenFilteredQuery() {

        // given
        String facetName = "fauthors";
        SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery())
                .withFilter(FilterBuilders.notFilter(FilterBuilders.termFilter("title","four")))
                .withFacet(new TermFacetRequestBuilder(facetName).applyQueryFilter().withStringFields("authors").build()).build();
        // when
        FacetedPage<Article> result = elasticsearchTemplate.queryForPage(searchQuery, Article.class);
        // then
        assertThat(result.getNumberOfElements(), is(equalTo(3)));

        TermResult facet = (TermResult) result.getFacet(facetName);
        Term term = facet.getTerms().get(0);
        assertThat(term.getTerm(), is(RIZWAN_IDREES));
        assertThat(term.getCount(), is(3));

        term = facet.getTerms().get(1);
        assertThat(term.getTerm(), is(ARTUR_KONCZAK));
        assertThat(term.getCount(), is(2));

        term = facet.getTerms().get(2);
        assertThat(term.getTerm(), is(MOHSIN_HUSEN));
        assertThat(term.getCount(), is(1));
    }

    @Test
    public void shouldReturnFacetedAuthorsForGivenQueryOrderedByTerm() {

        // given
        String facetName = "fauthors";
        SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery())
                .withFacet(new TermFacetRequestBuilder(facetName).withStringFields("authors").ascTerm().build()).build();
        // when
        FacetedPage<Article> result = elasticsearchTemplate.queryForPage(searchQuery, Article.class);
        // then
        assertThat(result.getNumberOfElements(), is(equalTo(4)));

        TermResult facet = (TermResult) result.getFacet(facetName);
        Term term = facet.getTerms().get(0);
        assertThat(term.getTerm(), is(ARTUR_KONCZAK));
        assertThat(term.getCount(), is(3));

        term = facet.getTerms().get(1);
        assertThat(term.getTerm(), is(JONATHAN_YAN));
        assertThat(term.getCount(), is(1));

        term = facet.getTerms().get(2);
        assertThat(term.getTerm(), is(MOHSIN_HUSEN));
        assertThat(term.getCount(), is(2));

        term = facet.getTerms().get(3);
        assertThat(term.getTerm(), is(RIZWAN_IDREES));
        assertThat(term.getCount(), is(4));

    }

    @Test
    public void shouldReturnFacetedAuthorsForGivenQueryOrderedByCountAsc() {

        // given
        String facetName = "fauthors";
        SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery())
                .withFacet(new TermFacetRequestBuilder(facetName).withStringFields("authors").ascCount().build()).build();
        // when
        FacetedPage<Article> result = elasticsearchTemplate.queryForPage(searchQuery, Article.class);
        // then
        assertThat(result.getNumberOfElements(), is(equalTo(4)));

        TermResult facet = (TermResult) result.getFacet(facetName);
        Term term = facet.getTerms().get(0);
        assertThat(term.getTerm(), is(JONATHAN_YAN));
        assertThat(term.getCount(), is(1));

        term = facet.getTerms().get(1);
        assertThat(term.getTerm(), is(MOHSIN_HUSEN));
        assertThat(term.getCount(), is(2));

        term = facet.getTerms().get(2);
        assertThat(term.getTerm(), is(ARTUR_KONCZAK));
        assertThat(term.getCount(), is(3));

        term = facet.getTerms().get(3);
        assertThat(term.getTerm(), is(RIZWAN_IDREES));
        assertThat(term.getCount(), is(4));
    }

    @Test
    public void shouldReturnFacetedYearsForGivenQuery() {

        // given
        String facetName = "fyears";
        SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery())
                .withFacet(new TermFacetRequestBuilder(facetName).withNumberFields("publishedYears").descCount().build()).build();
        // when
        FacetedPage<Article> result = elasticsearchTemplate.queryForPage(searchQuery, Article.class);
        // then
        assertThat(result.getNumberOfElements(), is(equalTo(4)));

        TermResult facet = (TermResult) result.getFacet(facetName);
        assertThat(facet.getTerms().size(), is(equalTo(3)));


        Term term = facet.getTerms().get(0);
        assertThat(term.getTerm(), is(Integer.toString(YEAR_2000)));
        assertThat(term.getCount(), is(3));

        term = facet.getTerms().get(1);
        assertThat(term.getTerm(), is(Integer.toString(YEAR_2001)));
        assertThat(term.getCount(), is(2));

        term = facet.getTerms().get(2);
        assertThat(term.getTerm(), is(Integer.toString(YEAR_2002)));
        assertThat(term.getCount(), is(1));
    }


    @Test
    public void shouldReturnSingleFacetOverYearsAndAuthorsForGivenQuery() {

        // given
        String facetName = "fyears";
        SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery())
                .withFacet(new TermFacetRequestBuilder(facetName).withNumberFields("publishedYears").withStringFields("authors").ascTerm().build()).build();
        // when
        FacetedPage<Article> result = elasticsearchTemplate.queryForPage(searchQuery, Article.class);
        // then
        assertThat(result.getNumberOfElements(), is(equalTo(4)));

        TermResult facet = (TermResult) result.getFacet(facetName);
        assertThat(facet.getTerms().size(), is(equalTo(7)));


        Term term = facet.getTerms().get(0);
        assertThat(term.getTerm(), is(Integer.toString(YEAR_2000)));
        assertThat(term.getCount(), is(3));

        term = facet.getTerms().get(1);
        assertThat(term.getTerm(), is(Integer.toString(YEAR_2001)));
        assertThat(term.getCount(), is(2));

        term = facet.getTerms().get(2);
        assertThat(term.getTerm(), is(Integer.toString(YEAR_2002)));
        assertThat(term.getCount(), is(1));

        term = facet.getTerms().get(3);
        assertThat(term.getTerm(), is(ARTUR_KONCZAK));
        assertThat(term.getCount(), is(3));

        term = facet.getTerms().get(4);
        assertThat(term.getTerm(), is(JONATHAN_YAN));
        assertThat(term.getCount(), is(1));

        term = facet.getTerms().get(5);
        assertThat(term.getTerm(), is(MOHSIN_HUSEN));
        assertThat(term.getCount(), is(2));

        term = facet.getTerms().get(6);
        assertThat(term.getTerm(), is(RIZWAN_IDREES));
        assertThat(term.getCount(), is(4));

    }

    @Test
    public void shouldReturnFacetedYearsAndFacetedAuthorsForGivenQuery() {

        // given
        String numberFacetName = "fAuthors";
        String stringFacetName = "fyears";
        SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery())
                .withFacet(new TermFacetRequestBuilder(numberFacetName).withNumberFields("publishedYears").ascTerm().build())
                .withFacet(new TermFacetRequestBuilder(stringFacetName).withStringFields("authors").ascTerm().build())
                .build();
        // when
        FacetedPage<Article> result = elasticsearchTemplate.queryForPage(searchQuery, Article.class);
        // then
        assertThat(result.getNumberOfElements(), is(equalTo(4)));

        TermResult numberFacet = (TermResult) result.getFacet(numberFacetName);
        assertThat(numberFacet.getTerms().size(), is(equalTo(3)));

        Term numberTerm = numberFacet.getTerms().get(0);
        assertThat(numberTerm.getTerm(), is(Integer.toString(YEAR_2000)));
        assertThat(numberTerm.getCount(), is(3));

        numberTerm = numberFacet.getTerms().get(1);
        assertThat(numberTerm.getTerm(), is(Integer.toString(YEAR_2001)));
        assertThat(numberTerm.getCount(), is(2));

        numberTerm = numberFacet.getTerms().get(2);
        assertThat(numberTerm.getTerm(), is(Integer.toString(YEAR_2002)));
        assertThat(numberTerm.getCount(), is(1));


        TermResult stringFacet = (TermResult) result.getFacet(stringFacetName);
        Term stringTerm = stringFacet.getTerms().get(0);
        assertThat(stringTerm.getTerm(), is(ARTUR_KONCZAK));
        assertThat(stringTerm.getCount(), is(3));

        stringTerm = stringFacet.getTerms().get(1);
        assertThat(stringTerm.getTerm(), is(JONATHAN_YAN));
        assertThat(stringTerm.getCount(), is(1));

        stringTerm = stringFacet.getTerms().get(2);
        assertThat(stringTerm.getTerm(), is(MOHSIN_HUSEN));
        assertThat(stringTerm.getCount(), is(2));

        stringTerm = stringFacet.getTerms().get(3);
        assertThat(stringTerm.getTerm(), is(RIZWAN_IDREES));
        assertThat(stringTerm.getCount(), is(4));
    }


    @Test
    public void shouldReturnFacetedYearsForNativeFacet() {

        // given
        String facetName = "fyears";
        SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery())
                .withFacet(new NativeFacetRequest(FacetBuilders.termsFacet(facetName).field("publishedYears"))).build();
        // when
        FacetedPage<Article> result = elasticsearchTemplate.queryForPage(searchQuery, Article.class);
        // then
        assertThat(result.getNumberOfElements(), is(equalTo(4)));

        TermResult facet = (TermResult) result.getFacet(facetName);
        assertThat(facet.getTerms().size(), is(equalTo(3)));


        Term term = facet.getTerms().get(0);
        assertThat(term.getTerm(), is(Integer.toString(YEAR_2000)));
        assertThat(term.getCount(), is(3));

        term = facet.getTerms().get(1);
        assertThat(term.getTerm(), is(Integer.toString(YEAR_2001)));
        assertThat(term.getCount(), is(2));

        term = facet.getTerms().get(2);
        assertThat(term.getTerm(), is(Integer.toString(YEAR_2002)));
        assertThat(term.getCount(), is(1));
    }


}
