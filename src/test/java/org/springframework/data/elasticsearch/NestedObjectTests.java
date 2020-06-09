/*
 * Copyright 2013-2020 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;
import static org.elasticsearch.index.query.QueryBuilders.*;
import static org.springframework.data.elasticsearch.utils.IdGenerator.*;

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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.junit.jupiter.ElasticsearchRestTemplateConfiguration;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.data.elasticsearch.utils.IndexInitializer;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Artur Konczak
 * @author Peter-Josef Meisch
 * @author Mark Paluch
 */
@SpringIntegrationTest
@ContextConfiguration(classes = { ElasticsearchRestTemplateConfiguration.class })
public class NestedObjectTests {

	@Autowired private ElasticsearchOperations operations;

	private final List<Class<?>> entityClasses = Arrays.asList(Book.class, Person.class, PersonMultipleLevelNested.class);

	@BeforeEach
	public void before() {
		entityClasses.stream().map(operations::indexOps).forEach(IndexInitializer::init);
	}

	@AfterEach
	void tearDown() {
		entityClasses.forEach(clazz -> operations.indexOps(clazz).delete());
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

		IndexCoordinates index = IndexCoordinates.of("test-index-person");
		operations.bulkIndex(indexQueries, index);
		operations.indexOps(Person.class).refresh();

		QueryBuilder builder = nestedQuery("car",
				boolQuery().must(termQuery("car.name", "saturn")).must(termQuery("car.model", "imprezza")), ScoreMode.None);

		NativeSearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(builder).build();
		SearchHits<Person> persons = operations.search(searchQuery, Person.class, index);

		assertThat(persons).hasSize(1);
	}

	@Test
	public void shouldIndexMultipleLevelNestedObject() {

		// given
		List<IndexQuery> indexQueries = createPerson();

		// when
		operations.bulkIndex(indexQueries, IndexCoordinates.of("test-index-person-multiple-level-nested"));
		operations.indexOps(PersonMultipleLevelNested.class).refresh();

		// then
		PersonMultipleLevelNested personIndexed = operations.get("1", PersonMultipleLevelNested.class,
				IndexCoordinates.of("test-index-person-multiple-level-nested"));
		assertThat(personIndexed).isNotNull();
	}

	@Test
	public void shouldIndexMultipleLevelNestedObjectWithIncludeInParent() {

		// given
		List<IndexQuery> indexQueries = createPerson();

		// when
		operations.bulkIndex(indexQueries, IndexCoordinates.of("test-index-person-multiple-level-nested"));

		// then
		Map<String, Object> mapping = operations.indexOps(PersonMultipleLevelNested.class).getMapping();

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
		IndexCoordinates index = IndexCoordinates.of("test-index-person-multiple-level-nested");
		operations.bulkIndex(indexQueries, index);
		operations.indexOps(PersonMultipleLevelNested.class).refresh();

		// then
		BoolQueryBuilder builder = boolQuery();
		builder.must(nestedQuery("girlFriends", termQuery("girlFriends.type", "temp"), ScoreMode.None)).must(
				nestedQuery("girlFriends.cars", termQuery("girlFriends.cars.name", "Ford".toLowerCase()), ScoreMode.None));

		NativeSearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(builder).build();

		SearchHits<PersonMultipleLevelNested> personIndexed = operations.search(searchQuery,
				PersonMultipleLevelNested.class, index);
		assertThat(personIndexed).isNotNull();
		assertThat(personIndexed.getTotalHits()).isEqualTo(1);
		assertThat(personIndexed.getSearchHit(0).getContent().getId()).isEqualTo("1");
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

		IndexCoordinates index = IndexCoordinates.of("test-index-person");
		operations.bulkIndex(indexQueries, index);
		operations.indexOps(Person.class).refresh();

		// when
		QueryBuilder builder = nestedQuery("books", boolQuery().must(termQuery("books.name", "java")), ScoreMode.None);

		NativeSearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(builder).build();
		SearchHits<Person> persons = operations.search(searchQuery, Person.class, index);

		// then
		assertThat(persons).hasSize(1);
	}

	@Test // DATAES-73
	public void shouldIndexAndSearchMapAsNestedType() {

		// given
		Book book1 = new Book();
		Book book2 = new Book();

		book1.setId(nextIdAsString());
		book1.setName("testBook1");

		book2.setId(nextIdAsString());
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
		IndexCoordinates index = IndexCoordinates.of("test-index-book-nested-objects");
		operations.bulkIndex(indexQueries, index);
		operations.indexOps(Book.class).refresh();

		// then
		NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
				.withQuery(nestedQuery("buckets", termQuery("buckets.1", "test3"), ScoreMode.None)).build();
		SearchHits<Book> books = operations.search(searchQuery, Book.class, index);

		assertThat(books.getSearchHits()).hasSize(1);
		assertThat(books.getSearchHit(0).getContent().getId()).isEqualTo(book2.getId());
	}

	@Setter
	@Getter
	@Document(indexName = "test-index-book-nested-objects", replicas = 0, refreshInterval = "-1")
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
	@Document(indexName = "test-index-person", replicas = 0, refreshInterval = "-1")
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
	@Document(indexName = "test-index-person-multiple-level-nested", replicas = 0, refreshInterval = "-1")
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
