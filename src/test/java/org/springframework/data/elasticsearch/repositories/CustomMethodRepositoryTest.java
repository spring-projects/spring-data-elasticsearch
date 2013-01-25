package org.springframework.data.elasticsearch.repositories;


import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.SampleEntity;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.DeleteQuery;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;

import static org.apache.commons.lang.RandomStringUtils.randomNumeric;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:custom-method-repository-test.xml")
public class CustomMethodRepositoryTest {

    @Resource
    private SampleCustomMethodRepository repository;

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
    public void shouldExecuteCustomMethod(){
        //given
        String documentId = randomNumeric(5);
        SampleEntity sampleEntity = new SampleEntity();
        sampleEntity.setId(documentId);
        sampleEntity.setType("test");
        sampleEntity.setMessage("some message");
        repository.save(sampleEntity);
        //when
        Page<SampleEntity> page = repository.findByType("test", new PageRequest(1, 10));
        //then
        assertThat(page, is(notNullValue()));
        assertThat(page.getTotalElements(), is(greaterThanOrEqualTo(1L)));
    }

    @Test
    public void shouldExecuteCustomMethodForNext(){
        //given
        String documentId = randomNumeric(5);
        SampleEntity sampleEntity = new SampleEntity();
        sampleEntity.setId(documentId);
        sampleEntity.setType("some");
        sampleEntity.setMessage("some message");
        repository.save(sampleEntity);
        //when
        Page<SampleEntity> page = repository.findByTypeNot("test", new PageRequest(1, 10));
        //then
        assertThat(page, is(notNullValue()));
        assertThat(page.getTotalElements(), is(greaterThanOrEqualTo(1L)));
    }

    @Test
    public void shouldExecuteCustomMethodWithQuery(){
        //given
        String documentId = randomNumeric(5);
        SampleEntity sampleEntity = new SampleEntity();
        sampleEntity.setId(documentId);
        sampleEntity.setType("test");
        sampleEntity.setMessage("customQuery");
        repository.save(sampleEntity);
        //when
        Page<SampleEntity> page  = repository.findByMessage("customQuery", new PageRequest(1, 10));
        //then
        assertThat(page, is(notNullValue()));
        assertThat(page.getTotalElements(), is(greaterThanOrEqualTo(1L)));
    }


    @Test
    public void shouldExecuteCustomMethodWithLessThan(){
        //given
        String documentId = randomNumeric(5);
        SampleEntity sampleEntity = new SampleEntity();
        sampleEntity.setId(documentId);
        sampleEntity.setType("test");
        sampleEntity.setRate(10);
        sampleEntity.setMessage("some message");
        repository.save(sampleEntity);

        String documentId2 = randomNumeric(5);
        SampleEntity sampleEntity2 = new SampleEntity();
        sampleEntity2.setId(documentId2);
        sampleEntity2.setType("test");
        sampleEntity2.setRate(20);
        sampleEntity2.setMessage("some message");
        repository.save(sampleEntity2);

        //when
        Page<SampleEntity> page = repository.findByRateLessThan(10, new PageRequest(1, 10));
        //then
        assertThat(page, is(notNullValue()));
        assertThat(page.getTotalElements(), is(equalTo(1L)));
    }

}
