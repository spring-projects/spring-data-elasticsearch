package org.springframework.data.elasticsearch.repositories;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.SampleEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;

import static org.apache.commons.lang.RandomStringUtils.randomNumeric;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:custom-method-repository-test.xml")
public class CustomMethodRepositoryTest {

    @Resource
    private SampleCustomMethodRepository repository;

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

}
