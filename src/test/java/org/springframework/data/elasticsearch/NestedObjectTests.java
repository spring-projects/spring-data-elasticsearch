/*
 * Copyright 2013-2019 the original author or authors.
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
package org.springframework.data.elasticsearch;

import static org.apache.commons.lang.RandomStringUtils.*;
import static org.assertj.core.api.Assertions.*;
import static org.elasticsearch.index.query.QueryBuilders.*;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Page;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.GetQuery;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.data.elasticsearch.utils.IndexInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Artur Konczak
 * @author Peter-Josef Meisch
 * @author Mark Paluch
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:/elasticsearch-template-test.xml")
public class NestedObjectTests {

	@Autowired private ElasticsearchTemplate elasticsearchTemplate;

	@Before
	public void before() {

		IndexInitializer.init(elasticsearchTemplate, Book.class);
		IndexInitializer.init(elasticsearchTemplate, Person.class);
		IndexInitializer.init(elasticsearchTemplate, PersonMultipleLevelNested.class);
	}

	@Test
	public void shouldIndexInitialLevelNestedObject() {

		List<Car> cars = new ArrayList<>();

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

		Car car = new Car();
		car.setName("Saturn");
		car.setModel("Imprezza");

		Person bar = new Person();
		bar.setId("2");
		bar.setName("Bar");
		bar.setCar(Collections.singletonList(car));

		List<IndexQuery> indexQueries = new ArrayList<>();
		IndexQuery indexQuery1 = new IndexQuery();
		indexQuery1.setId(foo.getId());
		indexQuery1.setObject(foo);

		IndexQuery indexQuery2 = new IndexQuery();
		indexQuery2.setId(bar.getId());
		indexQuery2.setObject(bar);

		indexQueries.add(indexQuery1);
		indexQueries.add(indexQuery2);

		elasticsearchTemplate.bulkIndex(indexQueries);
		elasticsearchTemplate.refresh(Person.class);

		QueryBuilder builder = nestedQuery("car",
				boolQuery().must(termQuery("car.name", "saturn")).must(termQuery("car.model", "imprezza")), ScoreMode.None);

		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(builder).build();
		List<Person> persons = elasticsearchTemplate.queryForList(searchQuery, Person.class);

		assertThat(persons).hasSize(1);
	}

	@Test
	public void shouldIndexMultipleLevelNestedObject() {

		// given
		List<IndexQuery> indexQueries = createPerson();

		// when
		elasticsearchTemplate.bulkIndex(indexQueries);
		elasticsearchTemplate.refresh(PersonMultipleLevelNested.class);

		// then
		GetQuery getQuery = new GetQuery();
		getQuery.setId("1");
		PersonMultipleLevelNested personIndexed = elasticsearchTemplate.queryForObject(getQuery,
				PersonMultipleLevelNested.class);
		assertThat(personIndexed).isNotNull();
	}

	@Test
	public void shouldIndexMultipleLevelNestedObjectWithIncludeInParent() {

		// given
		List<IndexQuery> indexQueries = createPerson();

		// when
		elasticsearchTemplate.bulkIndex(indexQueries);

		// then
		Map<String, Object> mapping = elasticsearchTemplate.getMapping(PersonMultipleLevelNested.class);

		assertThat(mapping).isNotNull();
		Map<String, Object> propertyMap = (Map<String, Object>) mapping.get("properties");
		assertThat(propertyMap).isNotNull();
		Map bestCarsAttributes = (Map) propertyMap.get("bestCars");
		assertThat(bestCarsAttributes.get("include_in_parent")).isNotNull();
	}

	@Test
	public void shouldSearchUsingNestedQueryOnMultipleLevelNestedObject() {

		// given
		List<IndexQuery> indexQueries = createPerson();

		// when
		elasticsearchTemplate.bulkIndex(indexQueries);
		elasticsearchTemplate.refresh(PersonMultipleLevelNested.class);

		// then
		BoolQueryBuilder builder = boolQuery();
		builder.must(nestedQuery("girlFriends", termQuery("girlFriends.type", "temp"), ScoreMode.None)).must(
				nestedQuery("girlFriends.cars", termQuery("girlFriends.cars.name", "Ford".toLowerCase()), ScoreMode.None));

		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(builder).build();

		Page<PersonMultipleLevelNested> personIndexed = elasticsearchTemplate.queryForPage(searchQuery,
				PersonMultipleLevelNested.class);
		assertThat(personIndexed).isNotNull();
		assertThat(personIndexed.getTotalElements()).isEqualTo(1);
		assertThat(personIndexed.getContent().get(0).getId()).isEqualTo("1");
	}

	private List<IndexQuery> createPerson() {

		PersonMultipleLevelNested person1 = new PersonMultipleLevelNested();

		person1.setId("1");
		person1.setName("name");

		Car saturn = new Car();
		saturn.setName("Saturn");
		saturn.setModel("SL");

		Car subaru = new Car();
		subaru.setName("Subaru");
		subaru.setModel("Imprezza");

		Car car = new Car();
		car.setName("Saturn");
		car.setModel("Imprezza");

		Car ford = new Car();
		ford.setName("Ford");
		ford.setModel("Focus");

		GirlFriend permanent = new GirlFriend();
		permanent.setName("permanent");
		permanent.setType("permanent");
		permanent.setCars(Arrays.asList(saturn, subaru));

		GirlFriend temp = new GirlFriend();
		temp.setName("temp");
		temp.setType("temp");
		temp.setCars(Arrays.asList(car, ford));

		person1.setGirlFriends(Arrays.asList(permanent, temp));

		IndexQuery indexQuery1 = new IndexQuery();
		indexQuery1.setId(person1.getId());
		indexQuery1.setObject(person1);

		PersonMultipleLevelNested person2 = new PersonMultipleLevelNested();

		person2.setId("2");
		person2.setName("name");

		person2.setGirlFriends(Collections.singletonList(permanent));

		IndexQuery indexQuery2 = new IndexQuery();
		indexQuery2.setId(person2.getId());
		indexQuery2.setObject(person2);

		List<IndexQuery> indexQueries = new ArrayList<>();
		indexQueries.add(indexQuery1);
		indexQueries.add(indexQuery2);

		return indexQueries;
	}

	@Test
	public void shouldSearchBooksForPersonInitialLevelNestedType() {

		// given
		List<Car> cars = new ArrayList<>();

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

		Book java = new Book();
		java.setId("1");
		java.setName("java");
		Author javaAuthor = new Author();
		javaAuthor.setId("1");
		javaAuthor.setName("javaAuthor");
		java.setAuthor(javaAuthor);

		Book spring = new Book();
		spring.setId("2");
		spring.setName("spring");
		Author springAuthor = new Author();
		springAuthor.setId("2");
		springAuthor.setName("springAuthor");
		spring.setAuthor(springAuthor);

		Person foo = new Person();
		foo.setName("Foo");
		foo.setId("1");
		foo.setCar(cars);
		foo.setBooks(Arrays.asList(java, spring));

		Car car = new Car();
		car.setName("Saturn");
		car.setModel("Imprezza");

		Person bar = new Person();
		bar.setId("2");
		bar.setName("Bar");
		bar.setCar(Collections.singletonList(car));

		List<IndexQuery> indexQueries = new ArrayList<>();
		IndexQuery indexQuery1 = new IndexQuery();
		indexQuery1.setId(foo.getId());
		indexQuery1.setObject(foo);

		IndexQuery indexQuery2 = new IndexQuery();
		indexQuery2.setId(bar.getId());
		indexQuery2.setObject(bar);

		indexQueries.add(indexQuery1);
		indexQueries.add(indexQuery2);

		elasticsearchTemplate.bulkIndex(indexQueries);
		elasticsearchTemplate.refresh(Person.class);

		// when
		QueryBuilder builder = nestedQuery("books", boolQuery().must(termQuery("books.name", "java")), ScoreMode.None);

		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(builder).build();
		List<Person> persons = elasticsearchTemplate.queryForList(searchQuery, Person.class);

		// then
		assertThat(persons).hasSize(1);
	}

	@Test // DATAES-73
	public void shouldIndexAndSearchMapAsNestedType() {

		// given
		Book book1 = new Book();
		Book book2 = new Book();

		book1.setId(randomNumeric(5));
		book1.setName("testBook1");

		book2.setId(randomNumeric(5));
		book2.setName("testBook2");

		Map<Integer, Collection<String>> map1 = new HashMap<>();
		map1.put(1, Arrays.asList("test1", "test2"));

		Map<Integer, Collection<String>> map2 = new HashMap<>();
		map2.put(1, Arrays.asList("test3", "test4"));

		book1.setBuckets(map1);
		book2.setBuckets(map2);

		List<IndexQuery> indexQueries = new ArrayList<>();
		IndexQuery indexQuery1 = new IndexQuery();
		indexQuery1.setId(book1.getId());
		indexQuery1.setObject(book1);

		IndexQuery indexQuery2 = new IndexQuery();
		indexQuery2.setId(book2.getId());
		indexQuery2.setObject(book2);

		indexQueries.add(indexQuery1);
		indexQueries.add(indexQuery2);

		// when
		elasticsearchTemplate.bulkIndex(indexQueries);
		elasticsearchTemplate.refresh(Book.class);

		// then
		SearchQuery searchQuery = new NativeSearchQueryBuilder()
				.withQuery(nestedQuery("buckets", termQuery("buckets.1", "test3"), ScoreMode.None)).build();
		Page<Book> books = elasticsearchTemplate.queryForPage(searchQuery, Book.class);

		assertThat(books.getContent()).hasSize(1);
		assertThat(books.getContent().get(0).getId()).isEqualTo(book2.getId());
	}

	@Setter
	@Getter
	@Document(indexName = "test-index-book-nested-objects", type = "book", shards = 1, replicas = 0,
			refreshInterval = "-1")
	static class Book {

		@Id private String id;
		private String name;
		@Field(type = FieldType.Object) private Author author;
		@Field(type = FieldType.Nested) private Map<Integer, Collection<String>> buckets = new HashMap<>();
		@MultiField(mainField = @Field(type = FieldType.Text, analyzer = "whitespace"),
				otherFields = { @InnerField(suffix = "prefix", type = FieldType.Text, analyzer = "stop",
						searchAnalyzer = "standard") }) private String description;
	}

	@Data
	@Document(indexName = "test-index-person", type = "user", shards = 1, replicas = 0, refreshInterval = "-1")
	static class Person {

		@Id private String id;

		private String name;

		@Field(type = FieldType.Nested) private List<Car> car;

		@Field(type = FieldType.Nested, includeInParent = true) private List<Book> books;
	}

	@Data
	static class Car {

		private String name;
		private String model;
	}

	/**
	 * @author Rizwan Idrees
	 * @author Mohsin Husen
	 * @author Artur Konczak
	 */
	@Data
	@Document(indexName = "test-index-person-multiple-level-nested", type = "user", shards = 1, replicas = 0,
			refreshInterval = "-1")
	static class PersonMultipleLevelNested {

		@Id private String id;

		private String name;

		@Field(type = FieldType.Nested) private List<GirlFriend> girlFriends;

		@Field(type = FieldType.Nested) private List<Car> cars;

		@Field(type = FieldType.Nested, includeInParent = true) private List<Car> bestCars;
	}

	/**
	 * @author Mohsin Husen
	 */
	@Data
	static class GirlFriend {

		private String name;

		private String type;

		@Field(type = FieldType.Nested) private List<Car> cars;
	}

	/**
	 * @author Rizwan Idrees
	 * @author Mohsin Husen
	 */
	@Data
	static class Author {

		private String id;
		private String name;
	}

}
