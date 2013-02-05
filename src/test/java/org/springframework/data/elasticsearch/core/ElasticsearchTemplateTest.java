package org.springframework.data.elasticsearch.core;


import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.SampleEntity;
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

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:elasticsearch-template-test.xml")
public class ElasticsearchTemplateTest {

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    @Before
    public void before(){
        elasticsearchTemplate.createIndex(SampleEntity.class);
        DeleteQuery deleteQuery = new DeleteQuery();
        deleteQuery.setElasticsearchQuery(matchAllQuery());
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
        SearchQuery searchQuery = new SearchQuery();
        searchQuery.setElasticsearchQuery(matchAllQuery());
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

        SearchQuery searchQuery = new SearchQuery();
        searchQuery.setElasticsearchQuery(matchAllQuery());
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
        SearchQuery searchQuery = new SearchQuery();
        searchQuery.setElasticsearchQuery(matchAllQuery());
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
        SearchQuery searchQuery = new SearchQuery();
        searchQuery.setElasticsearchQuery(fieldQuery("id", documentId));
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
        SearchQuery searchQuery = new SearchQuery();
        searchQuery.setElasticsearchQuery(fieldQuery("id", documentId));
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
        deleteQuery.setElasticsearchQuery(fieldQuery("id", documentId));
        elasticsearchTemplate.delete(deleteQuery,SampleEntity.class);
        //then
        SearchQuery searchQuery = new SearchQuery();
        searchQuery.setElasticsearchQuery(fieldQuery("id",documentId));
        Page<SampleEntity> sampleEntities = elasticsearchTemplate.queryForPage(searchQuery,SampleEntity.class);
        assertThat(sampleEntities.getTotalElements(), equalTo(0L));
    }

    @Test
    public void shouldTestFilterBuilder(){
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

        SearchQuery searchQuery = new SearchQuery();
        searchQuery.setElasticsearchQuery(matchAllQuery());
        searchQuery.setElasticsearchFilter(boolFilter().must(termFilter("id", documentId)));
        //when
        Page<SampleEntity> sampleEntities = elasticsearchTemplate.queryForPage(searchQuery,SampleEntity.class);
        //then
        assertThat(sampleEntities.getTotalElements(), equalTo(1L));
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

}
