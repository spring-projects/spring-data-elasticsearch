package org.springframework.data.elasticsearch.repositories;


import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.SampleEntity;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.util.Arrays;

import static org.apache.commons.lang.RandomStringUtils.randomNumeric;
import static org.elasticsearch.index.query.QueryBuilders.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:/repository-test.xml")
public class RepositoryTest {

    @Resource
    private SampleElasticsearchRepository repository;


    @Test
    public void shouldDoBulkIndexDocument(){
        //given
        String documentId = randomNumeric(5);
        SampleEntity sampleEntity1 = new SampleEntity();
        sampleEntity1.setId(documentId);
        sampleEntity1.setMessage("some message");

        SampleEntity sampleEntity2 = new SampleEntity();
        sampleEntity2.setId(documentId);
        sampleEntity2.setMessage("some message");

        //when
        repository.save(Arrays.asList(sampleEntity1, sampleEntity2));
        //then
        SampleEntity entityFromElasticSearch =  repository.findOne(documentId);
        assertThat(entityFromElasticSearch, is(notNullValue()));
    }

    @Test
    public void shouldSaveDocument(){
        //given
        String documentId = randomNumeric(5);
        SampleEntity sampleEntity = new SampleEntity();
        sampleEntity.setId(documentId);
        sampleEntity.setMessage("some message");
        //when
        repository.save(sampleEntity);
        //then
        SampleEntity entityFromElasticSearch =  repository.findOne(documentId);
        assertThat(entityFromElasticSearch, is(notNullValue()));
    }

    @Test
    public void shouldFindDocumentById(){
        //given
        String documentId = randomNumeric(5);
        SampleEntity sampleEntity = new SampleEntity();
        sampleEntity.setId(documentId);
        sampleEntity.setMessage("some message");
        repository.save(sampleEntity);
        //when
        SampleEntity entityFromElasticSearch = repository.findOne(documentId);
        //then
        assertThat(entityFromElasticSearch, is(notNullValue()));
        assertThat(sampleEntity, is((equalTo(sampleEntity))));
    }

    @Test
    public void shouldReturnCountOfDocuments(){
        //given
        String documentId = randomNumeric(5);
        SampleEntity sampleEntity = new SampleEntity();
        sampleEntity.setId(documentId);
        sampleEntity.setMessage("some message");
        repository.save(sampleEntity);
        //when
        Long count = repository.count();
        //then
        assertThat(count, is(greaterThanOrEqualTo(1L)));
    }

    @Test
    public void shouldFindAllDocuments(){
        //when
        Iterable<SampleEntity> results = repository.findAll();
        //then
        assertThat(results, is(notNullValue()));
    }

    @Test
    public void shouldDeleteDocument(){
        //given
        String documentId = randomNumeric(5);
        SampleEntity sampleEntity = new SampleEntity();
        sampleEntity.setId(documentId);
        sampleEntity.setMessage("some message");
        repository.save(sampleEntity);
        //when
        repository.delete(documentId);
        //then
        SampleEntity entityFromElasticSearch = repository.findOne(documentId);
        assertThat(entityFromElasticSearch, is(nullValue()));
    }

    @Test
    public void shouldSearchDocumentsGivenSearchQuery(){
        //given
        String documentId = randomNumeric(5);
        SampleEntity sampleEntity = new SampleEntity();
        sampleEntity.setId(documentId);
        sampleEntity.setMessage("some test message");
        repository.save(sampleEntity);

        SearchQuery query = new SearchQuery();
        query.setElasticsearchQuery(termQuery("message", "test"));
        //when
        Page<SampleEntity> page = repository.search(query);
        //then
        assertThat(page, is(notNullValue()));
        assertThat(page.getNumberOfElements(), is(greaterThanOrEqualTo(1)));
    }

    @Test
    public void shouldSearchDocumentsGivenElasticsearchQuery(){
        //given
        String documentId = randomNumeric(5);
        SampleEntity sampleEntity = new SampleEntity();
        sampleEntity.setId(documentId);
        sampleEntity.setMessage("hello world.");
        repository.save(sampleEntity);
        //when
        Page<SampleEntity> page = repository.search(termQuery("message", "world"), new PageRequest(0,50));
        //then
        assertThat(page, is(notNullValue()));
        assertThat(page.getNumberOfElements(), is(greaterThanOrEqualTo(1)));
    }

    @Test
    @Ignore
    public  void testFindAllByIdQuery(){
        //todo : find solution for findAll(Iterable<Ids> ids)
        //given
        String documentId = randomNumeric(5);
        SampleEntity sampleEntity = new SampleEntity();
        sampleEntity.setId(documentId);
        sampleEntity.setMessage("hello world.");
        repository.save(sampleEntity);

        String documentId2 = randomNumeric(5);
        SampleEntity sampleEntity2 = new SampleEntity();
        sampleEntity2.setId(documentId2);
        sampleEntity2.setMessage("hello world.");
        repository.save(sampleEntity2);

        //when
        Iterable<SampleEntity> sampleEntities=repository.findAll(Arrays.asList(documentId,documentId2));

        //then
        assertNotNull("sample entities cant be null..", sampleEntities);
    }

    @Test
    public void testSaveIterableEntities(){
           //given

        String documentId = randomNumeric(5);
        SampleEntity sampleEntity = new SampleEntity();
        sampleEntity.setId(documentId);
        sampleEntity.setMessage("hello world.");

        String documentId2 = randomNumeric(5);
        SampleEntity sampleEntity2 = new SampleEntity();
        sampleEntity2.setId(documentId2);
        sampleEntity2.setMessage("hello world.");

        Iterable<SampleEntity> sampleEntities = Arrays.asList(sampleEntity,sampleEntity2);

        //when
        repository.save(sampleEntities);

        //then
        Page<SampleEntity> entities = repository.search(fieldQuery("id", documentId), new PageRequest(0, 50));
        assertNotNull(entities);
    }

    @Test
    public void testDocumentExistById(){
        //given
        String documentId = randomNumeric(5);
        SampleEntity sampleEntity = new SampleEntity();
        sampleEntity.setId(documentId);
        sampleEntity.setMessage("hello world.");
        repository.save(sampleEntity);

        //when
        boolean exist = repository.exists(documentId);

        //then
        assertEquals(exist, true);
    }

    @Test
    public void testSearchForGivenSearchQuery(){
        //given
        String documentId = randomNumeric(5);
        SampleEntity sampleEntity = new SampleEntity();
        sampleEntity.setId(documentId);
        sampleEntity.setMessage("hello world.");
        repository.save(sampleEntity);
        //when
        SearchQuery searchQuery = new SearchQuery();
        searchQuery.setElasticsearchQuery(fieldQuery("id",documentId));
        Page<SampleEntity> sampleEntities= repository.search(searchQuery);
        //then
        assertThat(sampleEntities.getTotalElements(), equalTo(1L));
    }

    @Test
    public void testDeleteAll(){
        //when
        repository.deleteAll();
        //then
        SearchQuery searchQuery = new SearchQuery();
        searchQuery.setElasticsearchQuery(matchAllQuery());
        Page<SampleEntity> sampleEntities= repository.search(searchQuery);
        assertThat(sampleEntities.getTotalElements(), equalTo(0L));
    }

    @Test
    public void testDeleteByEntity(){
        //given
        String documentId = randomNumeric(5);
        SampleEntity sampleEntity = new SampleEntity();
        sampleEntity.setId(documentId);
        sampleEntity.setMessage("hello world.");
        repository.save(sampleEntity);
        //when
        repository.delete(sampleEntity);
        //then
        SearchQuery searchQuery = new SearchQuery();
        searchQuery.setElasticsearchQuery(fieldQuery("id", documentId));
        Page<SampleEntity> sampleEntities= repository.search(searchQuery);
        assertThat(sampleEntities.getTotalElements(),equalTo(0L));
    }

    @Test
    public void testSearchForReturnIterableEntities(){
        //given
        String documentId = randomNumeric(5);
        SampleEntity sampleEntity = new SampleEntity();
        sampleEntity.setId(documentId);
        sampleEntity.setMessage("hello world.");
        repository.save(sampleEntity);

        String documentId2 = randomNumeric(5);
        SampleEntity sampleEntity2 = new SampleEntity();
        sampleEntity2.setId(documentId2);
        sampleEntity2.setMessage("hello world.");
        repository.save(sampleEntity2);

        //when
        Iterable<SampleEntity> sampleEntities=repository.search(fieldQuery("id",documentId));
       //then
        assertNotNull("sample entities cant be null..", sampleEntities);
    }

    @Test
    public void testDeleteIterableEntities(){
        //given
        String documentId = randomNumeric(5);
        SampleEntity sampleEntity = new SampleEntity();
        sampleEntity.setId(documentId);
        sampleEntity.setMessage("hello world.");
        repository.save(sampleEntity);

        String documentId2 = randomNumeric(5);
        SampleEntity sampleEntity2 = new SampleEntity();
        sampleEntity2.setId(documentId2);
        sampleEntity2.setMessage("hello world.");
        repository.save(sampleEntity);

        Iterable<SampleEntity> sampleEntities = Arrays.asList(sampleEntity,sampleEntity2);

        //when
        repository.delete(sampleEntities);

        //then
        Page<SampleEntity> entities = repository.search(fieldQuery("id", documentId), new PageRequest(0,50));
        assertThat(entities.getTotalElements(),equalTo(0L));
        entities = repository.search(fieldQuery("id", documentId2), new PageRequest(0,50));
        assertThat(entities.getTotalElements(), equalTo(0L));
    }

    @Test
    public void testIndexEntity(){
        //given
        String documentId = randomNumeric(5);
        SampleEntity sampleEntity = new SampleEntity();
        sampleEntity.setId(documentId);
        sampleEntity.setMessage("some message");
        //when
        repository.index(sampleEntity);
        //then
        Page<SampleEntity> entities = repository.search(fieldQuery("id", documentId), new PageRequest(0,50));
        assertThat(entities.getTotalElements(),equalTo(1L));
    }

    @Test
    @Ignore("By default, the search request will fail if there is no mapping associated with a field. The ignore_unmapped option allows to ignore fields that have no mapping and not sort by them")
    public void testFindBySort(){
        //todo
        //given
        String documentId = randomNumeric(5);
        SampleEntity sampleEntity = new SampleEntity();
        sampleEntity.setId(documentId);
        sampleEntity.setMessage("A. hello world.");
        repository.save(sampleEntity);

        String documentId2 = randomNumeric(5);
        SampleEntity sampleEntity2 = new SampleEntity();
        sampleEntity2.setId(documentId2);
        sampleEntity2.setMessage("B.hello world.");
        repository.save(sampleEntity2);

        //when
        Iterable<SampleEntity> sampleEntities=repository.findAll(new Sort(new Sort.Order(Sort.Direction.ASC,"message")));

        //then
        assertNotNull("sample entities cant be null..", sampleEntities);
    }

}
