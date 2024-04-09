/*
 * Copyright 2013-2024 the original author or authors.
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
import static org.springframework.data.elasticsearch.utils.IdGenerator.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.lang.Nullable;

/**
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Artur Konczak
 * @author Peter-Josef Meisch
 * @author Mark Paluch
 */
@SpringIntegrationTest
public abstract class NestedObjectIntegrationTests {

	@Autowired private IndexNameProvider indexNameProvider;
	@Autowired private ElasticsearchOperations operations;

	private final List<Class<?>> entityClasses = Arrays.asList(Book.class, Person.class, PersonMultipleLevelNested.class);

	@BeforeEach
	public void before() {
		indexNameProvider.increment();
		entityClasses.stream().map(operations::indexOps).forEach(IndexOperations::createWithMapping);
	}

	@Test
	@Order(java.lang.Integer.MAX_VALUE)
	void cleanup() {
		operations.indexOps(IndexCoordinates.of(indexNameProvider.getPrefix() + "*")).delete();
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

		operations.bulkIndex(indexQueries, Person.class);
		operations.indexOps(Person.class).refresh();

		Query searchQuery = getNestedQuery1();
		SearchHits<Person> persons = operations.search(searchQuery, Person.class);

		assertThat(persons).hasSize(1);
	}

	@NotNull
	abstract protected Query getNestedQuery1();

	@Test
	public void shouldIndexMultipleLevelNestedObject() {

		// given
		List<IndexQuery> indexQueries = createPerson();

		// when
		operations.bulkIndex(indexQueries, PersonMultipleLevelNested.class);

		// then
		PersonMultipleLevelNested personIndexed = operations.get("1", PersonMultipleLevelNested.class);
		assertThat(personIndexed).isNotNull();
	}

	@Test
	public void shouldIndexMultipleLevelNestedObjectWithIncludeInParent() {

		// given
		List<IndexQuery> indexQueries = createPerson();

		// when
		operations.bulkIndex(indexQueries, PersonMultipleLevelNested.class);

		// then
		Map<String, Object> mapping = operations.indexOps(PersonMultipleLevelNested.class).getMapping();

		assertThat(mapping).isNotNull();
		Map<String, Object> propertyMap = (Map<String, Object>) mapping.get("properties");
		assertThat(propertyMap).isNotNull();
		Map<String, Object> bestCarsAttributes = (Map<String, Object>) propertyMap.get("bestCars");
		assertThat(bestCarsAttributes.get("include_in_parent")).isNotNull();
	}

	@Test
	public void shouldSearchUsingNestedQueryOnMultipleLevelNestedObject() {

		// given
		List<IndexQuery> indexQueries = createPerson();

		// when
		operations.bulkIndex(indexQueries, PersonMultipleLevelNested.class);

		// then
		Query searchQuery = getNestedQuery2();

		SearchHits<PersonMultipleLevelNested> personIndexed = operations.search(searchQuery,
				PersonMultipleLevelNested.class);
		assertThat(personIndexed).isNotNull();
		assertThat(personIndexed.getTotalHits()).isEqualTo(1);
		assertThat(personIndexed.getSearchHit(0).getContent().getId()).isEqualTo("1");
	}

	@NotNull
	abstract protected Query getNestedQuery2();

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
		var foo = getPerson();

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

		operations.bulkIndex(indexQueries, Person.class);

		// when
		Query searchQuery = getNestedQuery3();
		SearchHits<Person> persons = operations.search(searchQuery, Person.class);

		// then
		assertThat(persons).hasSize(1);
	}

	private static Person getPerson() {
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
		return foo;
	}

	@NotNull
	abstract protected Query getNestedQuery3();

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
		operations.bulkIndex(indexQueries, Book.class);

		// then
		Query searchQuery = getNestedQuery4();
		SearchHits<Book> books = operations.search(searchQuery, Book.class);

		assertThat(books.getSearchHits()).hasSize(1);
		assertThat(books.getSearchHit(0).getContent().getId()).isEqualTo(book2.getId());
	}

	@NotNull
	abstract protected Query getNestedQuery4();

	@Document(indexName = "#{@indexNameProvider.indexName()}-book")
	static class Book {

		@Nullable
		@Id private String id;
		@Nullable private String name;
		@Nullable
		@Field(type = FieldType.Object) private Author author;
		@Nullable
		@Field(type = FieldType.Nested) private Map<Integer, Collection<String>> buckets = new HashMap<>();
		@Nullable
		@MultiField(mainField = @Field(type = FieldType.Text, analyzer = "whitespace"),
				otherFields = { @InnerField(suffix = "prefix", type = FieldType.Text, analyzer = "stop",
						searchAnalyzer = "standard") }) private String description;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getName() {
			return name;
		}

		public void setName(@Nullable String name) {
			this.name = name;
		}

		@Nullable
		public Author getAuthor() {
			return author;
		}

		public void setAuthor(@Nullable Author author) {
			this.author = author;
		}

		@Nullable
		public Map<Integer, Collection<String>> getBuckets() {
			return buckets;
		}

		public void setBuckets(@Nullable Map<Integer, Collection<String>> buckets) {
			this.buckets = buckets;
		}

		@Nullable
		public String getDescription() {
			return description;
		}

		public void setDescription(@Nullable String description) {
			this.description = description;
		}
	}

	@Document(indexName = "#{@indexNameProvider.indexName()}-person")
	static class Person {
		@Nullable
		@Id private String id;
		@Nullable private String name;
		@Nullable
		@Field(type = FieldType.Nested) private List<Car> car;
		@Nullable
		@Field(type = FieldType.Nested, includeInParent = true) private List<Book> books;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getName() {
			return name;
		}

		public void setName(@Nullable String name) {
			this.name = name;
		}

		@Nullable
		public List<Car> getCar() {
			return car;
		}

		public void setCar(@Nullable List<Car> car) {
			this.car = car;
		}

		@Nullable
		public List<Book> getBooks() {
			return books;
		}

		public void setBooks(@Nullable List<Book> books) {
			this.books = books;
		}
	}

	static class Car {
		@Nullable private String name;
		@Nullable private String model;

		@Nullable
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Nullable
		public String getModel() {
			return model;
		}

		public void setModel(String model) {
			this.model = model;
		}
	}

	@Document(indexName = "#{@indexNameProvider.indexName()}-person-multiple-nested")
	static class PersonMultipleLevelNested {
		@Nullable
		@Id private String id;
		@Nullable private String name;
		@Nullable
		@Field(type = FieldType.Nested) private List<GirlFriend> girlFriends;
		@Nullable
		@Field(type = FieldType.Nested) private List<Car> cars;
		@Nullable
		@Field(type = FieldType.Nested, includeInParent = true) private List<Car> bestCars;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getName() {
			return name;
		}

		public void setName(@Nullable String name) {
			this.name = name;
		}

		@Nullable
		public List<GirlFriend> getGirlFriends() {
			return girlFriends;
		}

		public void setGirlFriends(@Nullable List<GirlFriend> girlFriends) {
			this.girlFriends = girlFriends;
		}

		@Nullable
		public List<Car> getCars() {
			return cars;
		}

		public void setCars(@Nullable List<Car> cars) {
			this.cars = cars;
		}

		@Nullable
		public List<Car> getBestCars() {
			return bestCars;
		}

		public void setBestCars(@Nullable List<Car> bestCars) {
			this.bestCars = bestCars;
		}
	}

	static class GirlFriend {
		@Nullable private String name;
		@Nullable private String type;
		@Nullable
		@Field(type = FieldType.Nested) private List<Car> cars;

		@Nullable
		public String getName() {
			return name;
		}

		public void setName(@Nullable String name) {
			this.name = name;
		}

		@Nullable
		public String getType() {
			return type;
		}

		public void setType(@Nullable String type) {
			this.type = type;
		}

		@Nullable
		public List<Car> getCars() {
			return cars;
		}

		public void setCars(@Nullable List<Car> cars) {
			this.cars = cars;
		}
	}

	static class Author {
		@Nullable private String id;
		@Nullable private String name;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getName() {
			return name;
		}

		public void setName(@Nullable String name) {
			this.name = name;
		}
	}

}
