package org.springframework.data.elasticsearch;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.data.elasticsearch.repositories.SampleElasticSearchBookRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;


import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:/repository-test-nested-object.xml")
public class NestedObjectTest{

    @Resource
    private SampleElasticSearchBookRepository repository;


    @Test
    public void shouldIndexNestedObject(){
        //given
        String id = randomAlphanumeric(5);
        Book book = new Book();
        book.setId(id);
        book.setName("xyz");
        Author author = new Author();
        author.setId("1");
        author.setName("ABC");
        book.setAuthor(author);
         //when
        repository.save(book);
        //then
        assertThat(repository.findOne(id), is(notNullValue()));
    }
}

