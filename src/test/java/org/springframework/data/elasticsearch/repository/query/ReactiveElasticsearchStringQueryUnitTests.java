/*
 * Copyright 2019-2023 the original author or authors.
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
import static org.mockito.Mockito.*;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.lang.Nullable;

/**
 * @author Christoph Strobl
 * @author Peter-Josef Meisch
 */
@ExtendWith(MockitoExtension.class)
public class ReactiveElasticsearchStringQueryUnitTests extends ElasticsearchStringQueryUnitTestBase {

	SpelExpressionParser PARSER = new SpelExpressionParser();

	@Mock ReactiveElasticsearchOperations operations;

	@BeforeEach
	public void setUp() {
		when(operations.getElasticsearchConverter()).thenReturn(setupConverter());
	}

	@Test // DATAES-519
	public void bindsSimplePropertyCorrectly() throws Exception {

		ReactiveElasticsearchStringQuery elasticsearchStringQuery = createQueryForMethod("findByName", String.class);
		StubParameterAccessor accessor = new StubParameterAccessor("Luke");

		org.springframework.data.elasticsearch.core.query.Query query = elasticsearchStringQuery.createQuery(accessor);
		StringQuery reference = new StringQuery("{ 'bool' : { 'must' : { 'term' : { 'name' : 'Luke' } } } }");

		assertThat(query).isInstanceOf(StringQuery.class);
		assertThat(((StringQuery) query).getSource()).isEqualTo(reference.getSource());
	}

	@Test // DATAES-519
	@Disabled("TODO: fix spel query integration")
	public void bindsExpressionPropertyCorrectly() throws Exception {

		ReactiveElasticsearchStringQuery elasticsearchStringQuery = createQueryForMethod("findByNameWithExpression",
				String.class);
		StubParameterAccessor accessor = new StubParameterAccessor("Luke");

		org.springframework.data.elasticsearch.core.query.Query query = elasticsearchStringQuery.createQuery(accessor);
		StringQuery reference = new StringQuery("{ 'bool' : { 'must' : { 'term' : { 'name' : 'Luke' } } } }");

		assertThat(query).isInstanceOf(StringQuery.class);
		assertThat(((StringQuery) query).getSource()).isEqualTo(reference.getSource());
	}

	@Test // DATAES-552
	public void shouldReplaceLotsOfParametersCorrectly() throws Exception {

		org.springframework.data.elasticsearch.core.query.Query query = createQuery("findWithQuiteSomeParameters", "zero",
				"one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten", "eleven");

		assertThat(query).isInstanceOf(StringQuery.class);
		assertThat(((StringQuery) query).getSource())
				.isEqualTo("name:(zero, one, two, three, four, five, six, seven, eight, nine, ten, eleven)");
	}

	@Test // DATAES-552
	public void shouldReplaceRepeatedParametersCorrectly() throws Exception {

		org.springframework.data.elasticsearch.core.query.Query query = createQuery("findWithRepeatedPlaceholder", "zero",
				"one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten", "eleven");

		assertThat(query).isInstanceOf(StringQuery.class);
		assertThat(((StringQuery) query).getSource())
				.isEqualTo("name:(zero, eleven, one, two, three, four, five, six, seven, eight, nine, ten, eleven, zero, one)");
	}

	@Test // #1790
	@DisplayName("should escape Strings in query parameters")
	void shouldEscapeStringsInQueryParameters() throws Exception {

		org.springframework.data.elasticsearch.core.query.Query query = createQuery("findByPrefix", "hello \"Stranger\"");

		assertThat(query).isInstanceOf(StringQuery.class);
		assertThat(((StringQuery) query).getSource())
				.isEqualTo("{\"bool\":{\"must\": [{\"match\": {\"prefix\": {\"name\" : \"hello \\\"Stranger\\\"\"}}]}}");
	}

	@Test // #1866
	@DisplayName("should use converter on parameters")
	void shouldUseConverterOnParameters() throws Exception {

		Car car = new Car();
		car.setName("Toyota");
		car.setModel("Prius");

		org.springframework.data.elasticsearch.core.query.Query query = createQuery("findByCar", car);

		assertThat(query).isInstanceOf(StringQuery.class);
		assertThat(((StringQuery) query).getSource())
				.isEqualTo("{ 'bool' : { 'must' : { 'term' : { 'car' : 'Toyota-Prius' } } } }");
	}

	@Test // #2135
	@DisplayName("should handle array-of-strings parameters correctly")
	void shouldHandleArrayOfStringsParametersCorrectly() throws Exception {

		List<String> otherNames = List.of("Wesley", "Emmett");

		org.springframework.data.elasticsearch.core.query.Query query = createQuery("findByOtherNames", otherNames);

		assertThat(query).isInstanceOf(StringQuery.class);
		assertThat(((StringQuery) query).getSource())
				.isEqualTo("{ 'bool' : { 'must' : { 'terms' : { 'otherNames' : [\"Wesley\",\"Emmett\"] } } } }");
	}

	@Test // #2135
	@DisplayName("should handle array-of-Integers parameters correctly")
	void shouldHandleArrayOfIntegerParametersCorrectly() throws Exception {

		List<Integer> ages = List.of(42, 57);

		org.springframework.data.elasticsearch.core.query.Query query = createQuery("findByAges", ages);

		assertThat(query).isInstanceOf(StringQuery.class);
		assertThat(((StringQuery) query).getSource())
				.isEqualTo("{ 'bool' : { 'must' : { 'terms' : { 'ages' : [42,57] } } } }");
	}

	private org.springframework.data.elasticsearch.core.query.Query createQuery(String methodName, Object... args)
			throws NoSuchMethodException {

		Class<?>[] argTypes = Arrays.stream(args).map(Object::getClass)
				.map(clazz -> Collection.class.isAssignableFrom(clazz) ? List.class : clazz).toArray(Class[]::new);
		ReactiveElasticsearchQueryMethod queryMethod = getQueryMethod(methodName, argTypes);
		ReactiveElasticsearchStringQuery elasticsearchStringQuery = queryForMethod(queryMethod);

		return elasticsearchStringQuery.createQuery(new ElasticsearchParametersParameterAccessor(queryMethod, args));
	}

	private ReactiveElasticsearchStringQuery queryForMethod(ReactiveElasticsearchQueryMethod queryMethod) {
		return new ReactiveElasticsearchStringQuery(queryMethod, operations, PARSER,
				QueryMethodEvaluationContextProvider.DEFAULT);
	}

	private ReactiveElasticsearchQueryMethod getQueryMethod(String name, Class<?>... parameters)
			throws NoSuchMethodException {

		Method method = SampleRepository.class.getMethod(name, parameters);
		return new ReactiveElasticsearchQueryMethod(method, new DefaultRepositoryMetadata(SampleRepository.class),
				new SpelAwareProxyProjectionFactory(), operations.getElasticsearchConverter().getMappingContext());
	}

	private ReactiveElasticsearchStringQuery createQueryForMethod(String name, Class<?>... parameters) throws Exception {

		ReactiveElasticsearchQueryMethod queryMethod = getQueryMethod(name, parameters);
		return queryForMethod(queryMethod);
	}

	private interface SampleRepository extends Repository<Person, String> {

		@Query("{ 'bool' : { 'must' : { 'term' : { 'name' : '?0' } } } }")
		Mono<Person> findByName(String name);

		@Query("{ 'bool' : { 'must' : { 'term' : { 'name' : '?#{[0]}' } } } }")
		Flux<Person> findByNameWithExpression(String param0);

		@Query(value = "name:(?0, ?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11)")
		Person findWithQuiteSomeParameters(String arg0, String arg1, String arg2, String arg3, String arg4, String arg5,
				String arg6, String arg7, String arg8, String arg9, String arg10, String arg11);

		@Query(value = "name:(?0, ?11, ?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11, ?0, ?1)")
		Person findWithRepeatedPlaceholder(String arg0, String arg1, String arg2, String arg3, String arg4, String arg5,
				String arg6, String arg7, String arg8, String arg9, String arg10, String arg11);

		@Query("{\"bool\":{\"must\": [{\"match\": {\"prefix\": {\"name\" : \"?0\"}}]}}")
		Flux<SearchHit<Book>> findByPrefix(String prefix);

		@Query("{ 'bool' : { 'must' : { 'term' : { 'car' : '?0' } } } }")
		Mono<Person> findByCar(Car car);

		@Query("{ 'bool' : { 'must' : { 'terms' : { 'otherNames' : ?0 } } } }")
		Flux<Person> findByOtherNames(List<String> otherNames);

		@Query("{ 'bool' : { 'must' : { 'terms' : { 'ages' : ?0 } } } }")
		Flux<Person> findByAges(List<Integer> ages);

	}

	/**
	 * @author Rizwan Idrees
	 * @author Mohsin Husen
	 * @author Artur Konczak
	 */

	@Document(indexName = "test-index-person-reactive-repository-string-query")
	public class Person {

		@Nullable
		@Id private String id;

		@Nullable private String name;

		@Nullable private List<String> otherNames;

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
		public List<String> getOtherNames() {
			return otherNames;
		}

		public void setOtherNames(List<String> otherNames) {
			this.otherNames = otherNames;
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

	@Document(indexName = "test-index-book-reactive-repository-string-query")
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
