/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.elasticsearch.core;


import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.SampleEntity;
import org.springframework.data.elasticsearch.SampleMappingEntity;
import org.springframework.data.elasticsearch.core.query.*;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang.RandomStringUtils.randomNumeric;
import static org.elasticsearch.index.query.FilterBuilders.boolFilter;
import static org.elasticsearch.index.query.FilterBuilders.termFilter;
import static org.elasticsearch.index.query.QueryBuilders.fieldQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
/**
 * @author Rizwan Idrees
 * @author Mohsin Husen
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:elasticsearch-template-test.xml")
public class ElasticsearchTemplateTest {

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    @Before
    public void before(){
        elasticsearchTemplate.createIndex(SampleEntity.class);
        DeleteQuery deleteQuery = new DeleteQuery();
        deleteQuery.setQuery(matchAllQuery());
        elasticsearchTemplate.delete(deleteQuery,SampleEntity.class);
        elasticsearchTemplate.refresh(SampleEntity.class, true);
    }

    @Test
    public void shouldReturnCountForGivenSearchQuery(){
        //given
        String documentId = randomNumeric(5);
        SampleEntity sampleEntity = new SampleEntity();
        sampleEntity.setId(documentId);
        sampleEntity.setMessage("some message");
        sampleEntity.setVersion(System.currentTimeMillis());
        IndexQuery indexQuery = new IndexQuery();
        indexQuery.setId(documentId);
        indexQuery.setObject(sampleEntity);
        elasticsearchTemplate.index(indexQuery);
        elasticsearchTemplate.refresh(SampleEntity.class, true);
        SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery()).build();
        //when
        long count = elasticsearchTemplate.count(searchQuery, SampleEntity.class);
        //then
        assertThat(count, is(equalTo(1L)));
    }

    @Test
    public void shouldReturnObjectForGivenId(){
        //given
        String documentId = randomNumeric(5);
        SampleEntity sampleEntity = new SampleEntity();
        sampleEntity.setId(documentId);
        sampleEntity.setMessage("some message");
        sampleEntity.setVersion(System.currentTimeMillis());

        IndexQuery indexQuery = new IndexQuery();
        indexQuery.setId(documentId);
        indexQuery.setObject(sampleEntity);

        elasticsearchTemplate.index(indexQuery);
        //when
        GetQuery getQuery = new GetQuery();
        getQuery.setId(documentId);
        SampleEntity sampleEntity1 = elasticsearchTemplate.queryForObject(getQuery, SampleEntity.class);
        //then
        assertNotNull("not null....", sampleEntity1);
        assertEquals(sampleEntity, sampleEntity1);
    }

    @Test
    public void shouldReturnPageForGivenSearchQuery(){
        //given
        String documentId = randomNumeric(5);
        SampleEntity sampleEntity = new SampleEntity();
        sampleEntity.setId(documentId);
        sampleEntity.setMessage("some message");
        sampleEntity.setVersion(System.currentTimeMillis());

        IndexQuery indexQuery = new IndexQuery();
        indexQuery.setId(documentId);
        indexQuery.setObject(sampleEntity);

        elasticsearchTemplate.index(indexQuery);
        elasticsearchTemplate.refresh(SampleEntity.class, true);

        SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery()).build();
        //when
        Page<SampleEntity> sampleEntities = elasticsearchTemplate.queryForPage(searchQuery, SampleEntity.class);
        //then
        assertThat(sampleEntities, is(notNullValue()));
        assertThat(sampleEntities.getTotalElements(), greaterThanOrEqualTo(1L));
    }

    @Test
    public void shouldDoBulkIndex(){
        //given
        List<IndexQuery> indexQueries = new ArrayList<IndexQuery>();
        //first document
        String documentId = randomNumeric(5);
        SampleEntity sampleEntity1 = new SampleEntity();
        sampleEntity1.setId(documentId);
        sampleEntity1.setMessage("some message");
        sampleEntity1.setVersion(System.currentTimeMillis());

        IndexQuery indexQuery1 = new IndexQuery();
        indexQuery1.setId(documentId);
        indexQuery1.setObject(sampleEntity1);
        indexQueries.add(indexQuery1);

        //second document
        String documentId2 = randomNumeric(5);
        SampleEntity sampleEntity2 = new SampleEntity();
        sampleEntity2.setId(documentId2);
        sampleEntity2.setMessage("some message");
        sampleEntity2.setVersion(System.currentTimeMillis());

        IndexQuery indexQuery2 = new IndexQuery();
        indexQuery2.setId(documentId2);
        indexQuery2.setObject(sampleEntity2);

        indexQueries.add(indexQuery2);
        //when
        elasticsearchTemplate.bulkIndex(indexQueries);
        elasticsearchTemplate.refresh(SampleEntity.class,true);
        //then
        SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery()).build();
        Page<SampleEntity> sampleEntities = elasticsearchTemplate.queryForPage(searchQuery,SampleEntity.class);
        assertThat(sampleEntities.getTotalElements(), is(equalTo(2L)));
    }

    @Test
    public void shouldDeleteDocumentForGivenId(){
        //given
        String documentId = randomNumeric(5);
        SampleEntity sampleEntity = new SampleEntity();
        sampleEntity.setId(documentId);
        sampleEntity.setMessage("some message");
        sampleEntity.setVersion(System.currentTimeMillis());

        IndexQuery indexQuery = new IndexQuery();
        indexQuery.setId(documentId);
        indexQuery.setObject(sampleEntity);

        elasticsearchTemplate.index(indexQuery);
        //when
        elasticsearchTemplate.delete("test-index","test-type",documentId);
        elasticsearchTemplate.refresh(SampleEntity.class, true);
        //then
        SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(fieldQuery("id", documentId)).build();
        Page<SampleEntity> sampleEntities = elasticsearchTemplate.queryForPage(searchQuery,SampleEntity.class);
        assertThat(sampleEntities.getTotalElements(), equalTo(0L));
    }

    @Test
    public void shouldDeleteEntityForGivenId(){
        //given
        String documentId = randomNumeric(5);
        SampleEntity sampleEntity = new SampleEntity();
        sampleEntity.setId(documentId);
        sampleEntity.setMessage("some message");
        sampleEntity.setVersion(System.currentTimeMillis());

        IndexQuery indexQuery = new IndexQuery();
        indexQuery.setId(documentId);
        indexQuery.setObject(sampleEntity);

        elasticsearchTemplate.index(indexQuery);
        //when
        elasticsearchTemplate.delete(SampleEntity.class,documentId);
        elasticsearchTemplate.refresh(SampleEntity.class, true);
        //then
        SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(fieldQuery("id", documentId)).build();
        Page<SampleEntity> sampleEntities = elasticsearchTemplate.queryForPage(searchQuery,SampleEntity.class);
        assertThat(sampleEntities.getTotalElements(), equalTo(0L));
    }

    @Test
    public void shouldDeleteDocumentForGivenQuery(){
        //given
        String documentId = randomNumeric(5);
        SampleEntity sampleEntity = new SampleEntity();
        sampleEntity.setId(documentId);
        sampleEntity.setMessage("some message");
        sampleEntity.setVersion(System.currentTimeMillis());

        IndexQuery indexQuery = new IndexQuery();
        indexQuery.setId(documentId);
        indexQuery.setObject(sampleEntity);

        elasticsearchTemplate.index(indexQuery);
        //when
        DeleteQuery deleteQuery = new DeleteQuery();
        deleteQuery.setQuery(fieldQuery("id", documentId));
        elasticsearchTemplate.delete(deleteQuery,SampleEntity.class);
        //then
        SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(fieldQuery("id",documentId)).build();
        Page<SampleEntity> sampleEntities = elasticsearchTemplate.queryForPage(searchQuery,SampleEntity.class);
        assertThat(sampleEntities.getTotalElements(), equalTo(0L));
    }

    @Test
    public void shouldFilterSearchResultsForGivenFilter(){
        //given
        String documentId = randomNumeric(5);
        SampleEntity sampleEntity = new SampleEntity();
        sampleEntity.setId(documentId);
        sampleEntity.setMessage("some message");
        sampleEntity.setVersion(System.currentTimeMillis());

        IndexQuery indexQuery = new IndexQuery();
        indexQuery.setId(documentId);
        indexQuery.setObject(sampleEntity);
        elasticsearchTemplate.index(indexQuery);
        elasticsearchTemplate.refresh(SampleEntity.class, true);

        SearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(matchAllQuery())
                .withFilter(boolFilter().must(termFilter("id", documentId)))
                .build();
        //when
        Page<SampleEntity> sampleEntities = elasticsearchTemplate.queryForPage(searchQuery,SampleEntity.class);
        //then
        assertThat(sampleEntities.getTotalElements(), equalTo(1L));
    }

    @Test
    public void shouldSortResultsGivenSortCriteria(){
        //given
        List<IndexQuery> indexQueries = new ArrayList<IndexQuery>();
        //first document
        String documentId = randomNumeric(5);
        SampleEntity sampleEntity1 = new SampleEntity();
        sampleEntity1.setId(documentId);
        sampleEntity1.setMessage("abc");
        sampleEntity1.setRate(10);
        sampleEntity1.setVersion(System.currentTimeMillis());

        IndexQuery indexQuery1 = new IndexQuery();
        indexQuery1.setId(documentId);
        indexQuery1.setObject(sampleEntity1);

        //second document
        String documentId2 = randomNumeric(5);
        SampleEntity sampleEntity2 = new SampleEntity();
        sampleEntity2.setId(documentId2);
        sampleEntity2.setMessage("xyz");
        sampleEntity2.setRate(5);
        sampleEntity2.setVersion(System.currentTimeMillis());

        IndexQuery indexQuery2 = new IndexQuery();
        indexQuery2.setId(documentId2);
        indexQuery2.setObject(sampleEntity2);

        //third document
        String documentId3 = randomNumeric(5);
        SampleEntity sampleEntity3 = new SampleEntity();
        sampleEntity3.setId(documentId3);
        sampleEntity3.setMessage("xyz");
        sampleEntity3.setRate(15);
        sampleEntity3.setVersion(System.currentTimeMillis());

        IndexQuery indexQuery3 = new IndexQuery();
        indexQuery3.setId(documentId3);
        indexQuery3.setObject(sampleEntity3);

        indexQueries.add(indexQuery1);
        indexQueries.add(indexQuery2);
        indexQueries.add(indexQuery3);

        elasticsearchTemplate.bulkIndex(indexQueries);
        elasticsearchTemplate.refresh(SampleEntity.class, true);

        SearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(matchAllQuery())
                .withSort(new FieldSortBuilder("rate").ignoreUnmapped(true).order(SortOrder.ASC))
                .build();
        //when
        Page<SampleEntity> sampleEntities = elasticsearchTemplate.queryForPage(searchQuery,SampleEntity.class);
        //then
        assertThat(sampleEntities.getTotalElements(), equalTo(3L));
        assertThat(sampleEntities.getContent().get(0).getRate(),is(sampleEntity2.getRate()));
    }

    @Test
    public void shouldExecuteStringQuery(){
        //given
        String documentId = randomNumeric(5);
        SampleEntity sampleEntity = new SampleEntity();
        sampleEntity.setId(documentId);
        sampleEntity.setMessage("some message");
        sampleEntity.setVersion(System.currentTimeMillis());

        IndexQuery indexQuery = new IndexQuery();
        indexQuery.setId(documentId);
        indexQuery.setObject(sampleEntity);

        elasticsearchTemplate.index(indexQuery);
        elasticsearchTemplate.refresh(SampleEntity.class, true);

        StringQuery stringQuery = new StringQuery(matchAllQuery().toString());
        //when
        Page<SampleEntity> sampleEntities = elasticsearchTemplate.queryForPage(stringQuery,SampleEntity.class);
        //then
        assertThat(sampleEntities.getTotalElements(), equalTo(1L));
    }

    @Test
    public void shouldReturnPageableResultsGivenStringQuery(){
        //given
        String documentId = randomNumeric(5);
        SampleEntity sampleEntity = new SampleEntity();
        sampleEntity.setId(documentId);
        sampleEntity.setMessage("some message");
        sampleEntity.setVersion(System.currentTimeMillis());

        IndexQuery indexQuery = new IndexQuery();
        indexQuery.setId(documentId);
        indexQuery.setObject(sampleEntity);

        elasticsearchTemplate.index(indexQuery);
        elasticsearchTemplate.refresh(SampleEntity.class, true);

        StringQuery stringQuery = new StringQuery(matchAllQuery().toString(),new PageRequest(0,10));
        //when
        Page<SampleEntity> sampleEntities = elasticsearchTemplate.queryForPage(stringQuery,SampleEntity.class);

        //then
        assertThat(sampleEntities.getTotalElements(), is(greaterThanOrEqualTo(1L)));
    }

    @Test
    @Ignore("By default, the search request will fail if there is no mapping associated with a field. The ignore_unmapped option allows to ignore fields that have no mapping and not sort by them")
    public void shouldReturnSortedPageableResultsGivenStringQuery(){
        //todo
        //given
        String documentId = randomNumeric(5);
        SampleEntity sampleEntity = new SampleEntity();
        sampleEntity.setId(documentId);
        sampleEntity.setMessage("some message");
        sampleEntity.setVersion(System.currentTimeMillis());

        IndexQuery indexQuery = new IndexQuery();
        indexQuery.setId(documentId);
        indexQuery.setObject(sampleEntity);

        elasticsearchTemplate.index(indexQuery);
        elasticsearchTemplate.refresh(SampleEntity.class, true);

        StringQuery stringQuery = new StringQuery(matchAllQuery().toString(),new PageRequest(0,10), new Sort(new Sort.Order(Sort.Direction.ASC,"messsage")));
        //when
        Page<SampleEntity> sampleEntities = elasticsearchTemplate.queryForPage(stringQuery,SampleEntity.class);
        //then
        assertThat(sampleEntities.getTotalElements(), is(greaterThanOrEqualTo(1L)));
    }

    @Test
    public void shouldReturnObjectMatchingGivenStringQuery(){
        //given
        String documentId = randomNumeric(5);
        SampleEntity sampleEntity = new SampleEntity();
        sampleEntity.setId(documentId);
        sampleEntity.setMessage("some message");
        sampleEntity.setVersion(System.currentTimeMillis());

        IndexQuery indexQuery = new IndexQuery();
        indexQuery.setId(documentId);
        indexQuery.setObject(sampleEntity);

        elasticsearchTemplate.index(indexQuery);
        elasticsearchTemplate.refresh(SampleEntity.class, true);

        StringQuery stringQuery = new StringQuery(fieldQuery("id",documentId).toString());
        //when
        SampleEntity sampleEntity1 = elasticsearchTemplate.queryForObject(stringQuery, SampleEntity.class);
        //then
        assertThat(sampleEntity1, is(notNullValue()));
        assertThat(sampleEntity1.getId(), is(equalTo(documentId)));
    }

    @Test
    public void shouldCreateIndexGivenEntityClass(){
        //when
        boolean created = elasticsearchTemplate.createIndex(SampleEntity.class);
        //then
        assertThat(created, is(true));
    }


    @Test
    public void shouldExecuteGivenCriteriaQuery(){
        //given
        String documentId = randomNumeric(5);
        SampleEntity sampleEntity = new SampleEntity();
        sampleEntity.setId(documentId);
        sampleEntity.setMessage("some test message");
        sampleEntity.setVersion(System.currentTimeMillis());

        IndexQuery indexQuery = new IndexQuery();
        indexQuery.setId(documentId);
        indexQuery.setObject(sampleEntity);

        elasticsearchTemplate.index(indexQuery);
        elasticsearchTemplate.refresh(SampleEntity.class, true);
        CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("message").contains("test"));

        //when
        SampleEntity sampleEntity1 = elasticsearchTemplate.queryForObject(criteriaQuery,SampleEntity.class);
        //then
        assertThat(sampleEntity1, is(notNullValue()));
    }

    @Test
    public void shouldReturnSpecifiedFields(){
        //given
        String documentId = randomNumeric(5);
        String message = "some test message";
        SampleEntity sampleEntity = new SampleEntity();
        sampleEntity.setId(documentId);
        sampleEntity.setMessage(message);
        sampleEntity.setVersion(System.currentTimeMillis());

        IndexQuery indexQuery = new IndexQuery();
        indexQuery.setId(documentId);
        indexQuery.setObject(sampleEntity);

        elasticsearchTemplate.index(indexQuery);
        elasticsearchTemplate.refresh(SampleEntity.class, true);
        SearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(matchAllQuery())
                .withIndices("test-index")
                .withTypes("test-type")
                .withFields("message")
                .build();
        //when
        Page<String> page = elasticsearchTemplate.queryForPage(searchQuery, new ResultsMapper<String>() {
            @Override
            public Page<String> mapResults(SearchResponse response) {
                List<String> values = new ArrayList<String>();
                for(SearchHit searchHit : response.getHits()){
                    values.add((String) searchHit.field("message").value());
                }
                return new PageImpl<String>(values);
            }
        });
        //then
        assertThat(page, is(notNullValue()));
        assertThat(page.getTotalElements(), is(equalTo(1L)));
        assertThat(page.getContent().get(0), is(message));
    }

    @Test
    public void shouldReturnSimilarResultsGivenMoreLikeThisQuery(){
        //given
        String sampleMessage = "So we build a web site or an application and want to add search to it, " +
                "and then it hits us: getting search working is hard. We want our search solution to be fast," +
                " we want a painless setup and a completely free search schema, we want to be able to index data simply using JSON over HTTP, " +
                "we want our search server to be always available, we want to be able to start with one machine and scale to hundreds, " +
                "we want real-time search, we want simple multi-tenancy, and we want a solution that is built for the cloud.";

        String documentId1 = randomNumeric(5);
        SampleEntity sampleEntity1 = new SampleEntity();
        sampleEntity1.setId(documentId1);
        sampleEntity1.setMessage(sampleMessage);
        sampleEntity1.setVersion(System.currentTimeMillis());

        IndexQuery indexQuery1 = new IndexQuery();
        indexQuery1.setId(documentId1);
        indexQuery1.setObject(sampleEntity1);

        elasticsearchTemplate.index(indexQuery1);

        String documentId2 = randomNumeric(5);
        SampleEntity sampleEntity2 = new SampleEntity();
        sampleEntity2.setId(documentId2);
        sampleEntity2.setMessage(sampleMessage);
        sampleEntity2.setVersion(System.currentTimeMillis());

        IndexQuery indexQuery2 = new IndexQuery();
        indexQuery2.setId(documentId2);
        indexQuery2.setObject(sampleEntity2);

        elasticsearchTemplate.index(indexQuery2);
        elasticsearchTemplate.refresh(SampleEntity.class, true);


        MoreLikeThisQuery moreLikeThisQuery = new MoreLikeThisQuery();
        moreLikeThisQuery.setId(documentId2);
        moreLikeThisQuery.addFields("message");
        moreLikeThisQuery.setMinDocFreq(1);
        //when
        Page<SampleEntity> sampleEntities = elasticsearchTemplate.moreLikeThis(moreLikeThisQuery, SampleEntity.class);

        //then
        assertThat(sampleEntities.getTotalElements(), is(equalTo(1L)));
        assertThat(sampleEntities.getContent(), hasItem(sampleEntity1));
    }

    @Test
    public void shouldReturnResultsWithScanAndScroll(){
        //given
        List<IndexQuery> indexQueries = new ArrayList<IndexQuery>();
        //first document
        String documentId = randomNumeric(5);
        SampleEntity sampleEntity1 = new SampleEntity();
        sampleEntity1.setId(documentId);
        sampleEntity1.setMessage("some message");
        sampleEntity1.setVersion(System.currentTimeMillis());

        IndexQuery indexQuery1 = new IndexQuery();
        indexQuery1.setId(documentId);
        indexQuery1.setObject(sampleEntity1);
        indexQueries.add(indexQuery1);

        //second document
        String documentId2 = randomNumeric(5);
        SampleEntity sampleEntity2 = new SampleEntity();
        sampleEntity2.setId(documentId2);
        sampleEntity2.setMessage("some message");
        sampleEntity2.setVersion(System.currentTimeMillis());

        IndexQuery indexQuery2 = new IndexQuery();
        indexQuery2.setId(documentId2);
        indexQuery2.setObject(sampleEntity2);

        indexQueries.add(indexQuery2);
        //when
        elasticsearchTemplate.bulkIndex(indexQueries);
        elasticsearchTemplate.refresh(SampleEntity.class,true);
        //then

        SearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(matchAllQuery())
                .withIndices("test-index")
                .withTypes("test-type")
                .withPageable(new PageRequest(0,1))
                .build();

        String scrollId = elasticsearchTemplate.scan(searchQuery,1000,false);
        List<SampleEntity> sampleEntities = new ArrayList<SampleEntity>();
        boolean hasRecords = true;
        while (hasRecords){
            Page<SampleEntity> page = elasticsearchTemplate.scroll(scrollId, 5000L , new ResultsMapper<SampleEntity>() {
                @Override
                public Page<SampleEntity> mapResults(SearchResponse response) {
                    List<SampleEntity> chunk = new ArrayList<SampleEntity>();
                    for(SearchHit searchHit : response.getHits()){
                        if(response.getHits().getHits().length <= 0) {
                            return null;
                        }
                        SampleEntity user = new SampleEntity();
                        user.setId(searchHit.getId());
                        user.setMessage((String)searchHit.getSource().get("message"));
                        chunk.add(user);
                    }
                    return new PageImpl<SampleEntity>(chunk);
                }

            });
            if(page != null) {
                sampleEntities.addAll(page.getContent());
                hasRecords = page.hasNextPage();
            }
            else{
                hasRecords = false;
            }

        }
        assertThat(sampleEntities.size(), is(equalTo(2)));
    }

    @Test
    public void shouldReturnListForGivenCriteria(){
        //given
        List<IndexQuery> indexQueries = new ArrayList<IndexQuery>();
        //first document
        String documentId = randomNumeric(5);
        SampleEntity sampleEntity1 = new SampleEntity();
        sampleEntity1.setId(documentId);
        sampleEntity1.setMessage("test message");
        sampleEntity1.setVersion(System.currentTimeMillis());

        IndexQuery indexQuery1 = new IndexQuery();
        indexQuery1.setId(documentId);
        indexQuery1.setObject(sampleEntity1);
        indexQueries.add(indexQuery1);

        //second document
        String documentId2 = randomNumeric(5);
        SampleEntity sampleEntity2 = new SampleEntity();
        sampleEntity2.setId(documentId2);
        sampleEntity2.setMessage("test test");
        sampleEntity2.setVersion(System.currentTimeMillis());

        IndexQuery indexQuery2 = new IndexQuery();
        indexQuery2.setId(documentId2);
        indexQuery2.setObject(sampleEntity2);

        indexQueries.add(indexQuery2);

        //second document
        String documentId3 = randomNumeric(5);
        SampleEntity sampleEntity3 = new SampleEntity();
        sampleEntity3.setId(documentId3);
        sampleEntity3.setMessage("some message");
        sampleEntity3.setVersion(System.currentTimeMillis());

        IndexQuery indexQuery3 = new IndexQuery();
        indexQuery3.setId(documentId3);
        indexQuery3.setObject(sampleEntity3);

        indexQueries.add(indexQuery3);
        //when
        elasticsearchTemplate.bulkIndex(indexQueries);
        elasticsearchTemplate.refresh(SampleEntity.class,true);
        //when
        CriteriaQuery singleCriteriaQuery = new CriteriaQuery(new Criteria("message").contains("test"));
        CriteriaQuery multipleCriteriaQuery = new CriteriaQuery(new Criteria("message").contains("some").and("message").contains("message"));
        List<SampleEntity> sampleEntitiesForSingleCriteria = elasticsearchTemplate.queryForList(singleCriteriaQuery,SampleEntity.class);
        List<SampleEntity> sampleEntitiesForAndCriteria = elasticsearchTemplate.queryForList(multipleCriteriaQuery,SampleEntity.class);
        //then
        assertThat(sampleEntitiesForSingleCriteria.size(),is(2));
        assertThat(sampleEntitiesForAndCriteria.size(),is(1));
    }

    @Test
    public void shouldReturnListForGivenStringQuery(){
        //given
        List<IndexQuery> indexQueries = new ArrayList<IndexQuery>();
        //first document
        String documentId = randomNumeric(5);
        SampleEntity sampleEntity1 = new SampleEntity();
        sampleEntity1.setId(documentId);
        sampleEntity1.setMessage("test message");
        sampleEntity1.setVersion(System.currentTimeMillis());

        IndexQuery indexQuery1 = new IndexQuery();
        indexQuery1.setId(documentId);
        indexQuery1.setObject(sampleEntity1);
        indexQueries.add(indexQuery1);

        //second document
        String documentId2 = randomNumeric(5);
        SampleEntity sampleEntity2 = new SampleEntity();
        sampleEntity2.setId(documentId2);
        sampleEntity2.setMessage("test test");
        sampleEntity2.setVersion(System.currentTimeMillis());

        IndexQuery indexQuery2 = new IndexQuery();
        indexQuery2.setId(documentId2);
        indexQuery2.setObject(sampleEntity2);

        indexQueries.add(indexQuery2);

        //second document
        String documentId3 = randomNumeric(5);
        SampleEntity sampleEntity3 = new SampleEntity();
        sampleEntity3.setId(documentId3);
        sampleEntity3.setMessage("some message");
        sampleEntity3.setVersion(System.currentTimeMillis());

        IndexQuery indexQuery3 = new IndexQuery();
        indexQuery3.setId(documentId3);
        indexQuery3.setObject(sampleEntity3);

        indexQueries.add(indexQuery3);
        //when
        elasticsearchTemplate.bulkIndex(indexQueries);
        elasticsearchTemplate.refresh(SampleEntity.class,true);
        //when
        StringQuery stringQuery = new StringQuery(matchAllQuery().toString());
        List<SampleEntity> sampleEntities = elasticsearchTemplate.queryForList(stringQuery,SampleEntity.class);
        //then
        assertThat(sampleEntities.size(),is(3));
    }

    @Test
    public void shouldPutMappingForGivenEntity()throws Exception{
        //given
        Class entity = SampleMappingEntity.class;
        elasticsearchTemplate.createIndex(entity);
        //when
        assertThat(elasticsearchTemplate.putMapping(entity) , is(true)) ;
    }

    @Test
    public void shouldDeleteIndexForGivenEntity(){
        //given
        Class clazz = SampleEntity.class;
        //when
        elasticsearchTemplate.deleteIndex(clazz);
        //then
        assertThat(elasticsearchTemplate.indexExists(clazz),is(false));
    }



}
