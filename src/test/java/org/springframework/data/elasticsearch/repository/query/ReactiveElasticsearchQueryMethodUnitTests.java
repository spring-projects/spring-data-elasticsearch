/*
 * Copyright 2019-2024 the original author or authors.
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
package org.springframework.data.elasticsearch.repository.query;

import static org.assertj.core.api.Assertions.*;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.elasticsearch.annotations.CountQuery;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;
import org.springframework.lang.Nullable;

/**
 * @author Christoph Strobl
 */
public class ReactiveElasticsearchQueryMethodUnitTests {

	public static final String INDEX_NAME = "test-index-person-reactive-repository-query";
	SimpleElasticsearchMappingContext mappingContext;

	@BeforeEach
	public void setUp() {
		mappingContext = new SimpleElasticsearchMappingContext();
	}

	@Test // DATAES-519
	public void detectsCollectionFromRepoTypeIfReturnTypeNotAssignable() throws Exception {

		ReactiveElasticsearchQueryMethod queryMethod = queryMethod(NonReactiveRepository.class, "method");
		ElasticsearchEntityMetadata<?> metadata = queryMethod.getEntityInformation();

		assertThat(metadata.getJavaType()).isAssignableFrom(Person.class);
		assertThat(metadata.getIndexName()).isEqualTo(INDEX_NAME);
	}

	@Test // DATAES-519
	public void rejectsNullMappingContext() throws Exception {

		Method method = PersonRepository.class.getMethod("findByName", String.class);

		assertThatThrownBy(() -> new ReactiveElasticsearchQueryMethod(method,
				new DefaultRepositoryMetadata(PersonRepository.class), new SpelAwareProxyProjectionFactory(), null))
						.isInstanceOf(IllegalArgumentException.class);
	}

	@Test // DATAES-519
	public void rejectsMonoPageableResult() {
		assertThatThrownBy(() -> queryMethod(PersonRepository.class, "findMonoByName", String.class, Pageable.class))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test // DATAES-519
	public void throwsExceptionOnWrappedPage() {
		assertThatThrownBy(() -> queryMethod(PersonRepository.class, "findMonoPageByName", String.class, Pageable.class))
				.isInstanceOf(InvalidDataAccessApiUsageException.class);
	}

	@Test // DATAES-519
	public void throwsExceptionOnWrappedSlice() {
		assertThatThrownBy(() -> queryMethod(PersonRepository.class, "findMonoSliceByName", String.class, Pageable.class))
				.isInstanceOf(InvalidDataAccessApiUsageException.class);
	}

	@Test // DATAES-519
	public void allowsPageableOnFlux() throws Exception {
		queryMethod(PersonRepository.class, "findByName", String.class, Pageable.class);
	}

	@Test // DATAES-519
	public void fallsBackToRepositoryDomainTypeIfMethodDoesNotReturnADomainType() throws Exception {

		ReactiveElasticsearchQueryMethod method = queryMethod(PersonRepository.class, "deleteByName", String.class);

		assertThat(method.getEntityInformation().getJavaType()).isAssignableFrom(Person.class);
	}

	@Test // #1156
	@DisplayName("should reject count query method not returning a Mono of Long")
	void shouldRejectCountQueryMethodNotReturningAMonoOfLong() {

		assertThatThrownBy(() -> queryMethod(PersonRepository.class, "invalidCountQueryResult", String.class))
				.isInstanceOf(InvalidDataAccessApiUsageException.class);
	}

	@Test // #1156
	@DisplayName("should accept count query method returning a Mono of Long")
	void shouldAcceptCountQueryMethodReturningAMonoOfLong() throws Exception {
		queryMethod(PersonRepository.class, "validCountQueryResult", String.class);
	}

	private ReactiveElasticsearchQueryMethod queryMethod(Class<?> repository, String name, Class<?>... parameters)
			throws Exception {

		Method method = repository.getMethod(name, parameters);
		ProjectionFactory factory = new SpelAwareProxyProjectionFactory();
		return new ReactiveElasticsearchQueryMethod(method, new DefaultRepositoryMetadata(repository), factory,
				mappingContext);
	}

	interface PersonRepository extends Repository<Person, String> {

		Mono<Person> findMonoByName(String name, Pageable pageRequest);

		Mono<Page<Person>> findMonoPageByName(String name, Pageable pageRequest);

		Mono<Slice<Person>> findMonoSliceByName(String name, Pageable pageRequest);

		Flux<Person> findByName(String name);

		Flux<Person> findByName(String name, Pageable pageRequest);

		void deleteByName(String name);

		@CountQuery("{}")
		Flux<Person> invalidCountQueryResult(String name); // invalid return type here

		@CountQuery("{}")
		Mono<Long> validCountQueryResult(String name);
	}

	interface NonReactiveRepository extends Repository<Person, Long> {

		List<Person> method();
	}

	/**
	 * @author Rizwan Idrees
	 * @author Mohsin Husen
	 * @author Artur Konczak
	 */

	@Document(indexName = INDEX_NAME)
	static class Person {

		@Nullable
		@Id private String id;

		@Nullable private String name;
		@Nullable private String firstName;

		@Nullable
		@Field(type = FieldType.Nested) private List<Car> car;

		@Nullable
		@Field(type = FieldType.Nested, includeInParent = true) private List<Book> books;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		@Nullable
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Nullable
		public List<Car> getCar() {
			return car;
		}

		public void setCar(List<Car> car) {
			this.car = car;
		}

		@Nullable
		public List<Book> getBooks() {
			return books;
		}

		public void setBooks(List<Book> books) {
			this.books = books;
		}
	}

	@Document(indexName = "test-index-book-reactive-repository-query")
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

	static class Car {
		@Nullable private String name;
		@Nullable private String model;

		@Nullable
		public String getName() {
			return name;
		}

		public void setName(@Nullable String name) {
			this.name = name;
		}

		@Nullable
		public String getModel() {
			return model;
		}

		public void setModel(@Nullable String model) {
			this.model = model;
		}
	}

	static class Author {

		@Nullable private String id;
		@Nullable private String name;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		@Nullable
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

}
