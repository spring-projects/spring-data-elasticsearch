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
/**
 * @author Rizwan Idrees
 * @author Mohsin Husen
 */
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

