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
package org.springframework.data.elasticsearch;

import org.elasticsearch.index.query.QueryBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.data.elasticsearch.repositories.SampleElasticSearchBookRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.nestedQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Artur Konczak
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:/repository-test-nested-object.xml")
public class NestedObjectTests {

	@Resource
	private SampleElasticSearchBookRepository bookRepository;

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;


    @Before
    public void before() {
        elasticsearchTemplate.deleteIndex(Book.class);
        elasticsearchTemplate.createIndex(Book.class);
        elasticsearchTemplate.refresh(Book.class, true);
        elasticsearchTemplate.deleteIndex(Person.class);
        elasticsearchTemplate.createIndex(Person.class);
        elasticsearchTemplate.putMapping(Person.class);
        elasticsearchTemplate.refresh(Person.class, true);
    }

    @Test
    public void shouldIndexNestedObject(){

        List<Car> cars = new ArrayList<Car>();

        Car saturn = new Car();
        saturn.setName("Saturn");
        saturn.setModel("SL");

        Car subaru = new Car();
        subaru.setName("Subaru");
        subaru.setModel("Imprezza");

        Car ford = new Car();
        ford.setName("Ford");
        ford.setModel("Focus");

        cars.add(saturn);
        cars.add(subaru);
        cars.add(ford);

        Person foo = new Person();
        foo.setName("Foo");
        foo.setId("1");
        foo.setCar(cars);


        Car car  = new Car();
        car.setName("Saturn");
        car.setModel("Imprezza");

        Person bar = new Person();
        bar.setId("2");
        bar.setName("Bar");
        bar.setCar(Arrays.asList(car));

        List<IndexQuery> indexQueries = new ArrayList<IndexQuery>();
        IndexQuery indexQuery1 = new IndexQuery();
        indexQuery1.setId(foo.getId());
        indexQuery1.setObject(foo);

        IndexQuery indexQuery2 = new IndexQuery();
        indexQuery2.setId(bar.getId());
        indexQuery2.setObject(bar);

        indexQueries.add(indexQuery1);
        indexQueries.add(indexQuery2);

        elasticsearchTemplate.putMapping(Person.class);
        elasticsearchTemplate.bulkIndex(indexQueries);
        elasticsearchTemplate.refresh(Person.class, true);

        QueryBuilder builder = nestedQuery("car", boolQuery().must(termQuery("car.name", "saturn")).must(termQuery("car.model", "imprezza")));

        SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(builder).build();
        List<Person> persons = elasticsearchTemplate.queryForList(searchQuery, Person.class);

        assertThat(persons.size() , is(1));

    }

	@Test
	public void shouldIndexInnerObject() {
		// given
		String id = randomAlphanumeric(5);
		Book book = new Book();
		book.setId(id);
		book.setName("xyz");
		Author author = new Author();
		author.setId("1");
		author.setName("ABC");
		book.setAuthor(author);
		// when
		bookRepository.save(book);
		// then
		assertThat(bookRepository.findOne(id), is(notNullValue()));
	}
}
