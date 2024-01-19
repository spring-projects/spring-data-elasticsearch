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
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.data.elasticsearch.repositories.custommethod.QueryParameter;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.lang.Nullable;

/**
 * @author Christoph Strobl
 * @author Peter-Josef Meisch
 * @author Niklas Herder
 * @author Haibo Liu
 */
@ExtendWith(MockitoExtension.class)
public class ElasticsearchStringQueryUnitTests extends ElasticsearchStringQueryUnitTestBase {

	@Mock ElasticsearchOperations operations;

	@BeforeEach
	public void setUp() {
		when(operations.getElasticsearchConverter()).thenReturn(setupConverter());
	}

	@Test // DATAES-552
	public void shouldReplaceParametersCorrectly() throws Exception {

		org.springframework.data.elasticsearch.core.query.Query query = createQuery("findByName", "Luke");

		assertThat(query).isInstanceOf(StringQuery.class);
		assertThat(((StringQuery) query).getSource())
				.isEqualTo("{ 'bool' : { 'must' : { 'term' : { 'name' : 'Luke' } } } }");
	}

	@Test // DATAES-552
	public void shouldReplaceRepeatedParametersCorrectly() throws Exception {

		org.springframework.data.elasticsearch.core.query.Query query = createQuery("findWithRepeatedPlaceholder", "zero",
				"one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten", "eleven");

		assertThat(query).isInstanceOf(StringQuery.class);
		assertThat(((StringQuery) query).getSource())
				.isEqualTo("name:(zero, eleven, one, two, three, four, five, six, seven, eight, nine, ten, eleven, zero, one)");
	}

	@Test
	public void shouldReplaceParametersSpEL() throws Exception {

		org.springframework.data.elasticsearch.core.query.Query query = createQuery("findByNameSpEL", "Luke");
		String expected = """
				{
				  "bool":{
				    "must":{
				      "term":{
				        "name": "Luke"
				      }
				    }
				  }
				}
				""";

		assertThat(query).isInstanceOf(StringQuery.class);
		JSONAssert.assertEquals(((StringQuery) query).getSource(), expected, JSONCompareMode.NON_EXTENSIBLE);
	}

	@Test
	public void shouldReplaceParametersSpELWithQuotes() throws Exception {

		org.springframework.data.elasticsearch.core.query.Query query = createQuery("findByNameSpEL",
				"hello \"world\"");
		String expected = """
				{
				  "bool":{
				    "must":{
				      "term":{
				        "name": "hello \\"world\\""
				      }
				    }
				  }
				}
				""";

		assertThat(query).isInstanceOf(StringQuery.class);
		JSONAssert.assertEquals(((StringQuery) query).getSource(), expected, JSONCompareMode.NON_EXTENSIBLE);
	}

	@Test
	public void shouldUseParameterPropertySpEL() throws Exception {

		org.springframework.data.elasticsearch.core.query.Query query = createQuery("findByParameterPropertySpEL",
				new QueryParameter("Luke"));
		String expected = """
				{
				  "bool":{
				    "must":{
				      "term":{
				        "name": "Luke"
				      }
				    }
				  }
				}
				""";

		assertThat(query).isInstanceOf(StringQuery.class);
		JSONAssert.assertEquals(((StringQuery) query).getSource(), expected, JSONCompareMode.NON_EXTENSIBLE);
	}

	@Test
	public void shouldReplaceCollectionSpEL() throws Exception {

		final List<String> anotherString = List.of("hello \"Stranger\"", "Another string");
		List<String> params = new ArrayList<>(anotherString);
		org.springframework.data.elasticsearch.core.query.Query query = createQuery("findByNamesSpEL", params);
		String expected = """
				{
				  "bool":{
				    "must":{
				      "terms":{
				        "name": ["hello \\"Stranger\\"", "Another string"]
				      }
				    }
				  }
				}
				""";

		assertThat(query).isInstanceOf(StringQuery.class);
		JSONAssert.assertEquals(((StringQuery) query).getSource(), expected, JSONCompareMode.NON_EXTENSIBLE);
	}

	@Test
	public void shouldReplaceNonStringCollectionSpEL() throws Exception {

		final List<Integer> ages = List.of(1, 2, 3);
		List<Integer> params = new ArrayList<>(ages);
		org.springframework.data.elasticsearch.core.query.Query query = createQuery("findByAgesSpEL", params);
		String expected = """
				{
				  "bool":{
				    "must":{
				      "terms":{
				        "age": [1, 2, 3]
				      }
				    }
				  }
				}
				""";

		assertThat(query).isInstanceOf(StringQuery.class);
		JSONAssert.assertEquals(((StringQuery) query).getSource(), expected, JSONCompareMode.NON_EXTENSIBLE);
	}

	@Test
	public void shouldReplaceEmptyCollectionSpEL() throws Exception {

		final List<String> anotherString = List.of();
		List<String> params = new ArrayList<>(anotherString);
		org.springframework.data.elasticsearch.core.query.Query query = createQuery("findByNamesSpEL", params);
		String expected = """
				{
				  "bool":{
				    "must":{
				      "terms":{
				        "name": []
				      }
				    }
				  }
				}
				""";

		assertThat(query).isInstanceOf(StringQuery.class);
		JSONAssert.assertEquals(((StringQuery) query).getSource(), expected, JSONCompareMode.NON_EXTENSIBLE);
	}

	@Test
	public void shouldBeEmptyWithNullValuesInCollectionSpEL() throws Exception {

		final List<String> anotherString = List.of();
		List<String> params = new ArrayList<>(anotherString);
		// add a null value
		params.add(null);
		org.springframework.data.elasticsearch.core.query.Query query = createQuery("findByNamesSpEL", params);
		String expected = """
				{
				  "bool":{
				    "must":{
				      "terms":{
				        "name": []
				      }
				    }
				  }
				}
				""";

		assertThat(query).isInstanceOf(StringQuery.class);
		JSONAssert.assertEquals(((StringQuery) query).getSource(), expected, JSONCompareMode.NON_EXTENSIBLE);
	}

	@Test
	public void shouldIgnoreNullValuesInCollectionSpEL() throws Exception {

		final List<String> anotherString = List.of("abc");
		List<String> params = new ArrayList<>(anotherString);
		// add a null value
		params.add(null);
		org.springframework.data.elasticsearch.core.query.Query query = createQuery("findByNamesSpEL", params);
		String expected = """
				{
				  "bool":{
				    "must":{
				      "terms":{
				        "name": ["abc"]
				      }
				    }
				  }
				}
				""";

		assertThat(query).isInstanceOf(StringQuery.class);
		JSONAssert.assertEquals(((StringQuery) query).getSource(), expected, JSONCompareMode.NON_EXTENSIBLE);
	}

	@Test
	public void shouldReplaceCollectionParametersSpEL() throws Exception {

		final List<QueryParameter> anotherString = List.of(new QueryParameter("hello \"Stranger\""),
				new QueryParameter("Another string"));
		List<QueryParameter> params = new ArrayList<>(anotherString);
		org.springframework.data.elasticsearch.core.query.Query query = createQuery("findByNamesParameterSpEL", params);
		String expected = """
				{
				  "bool":{
				    "must":{
				      "terms":{
				        "name": ["hello \\"Stranger\\"", "Another string"]
				      }
				    }
				  }
				}
				""";

		assertThat(query).isInstanceOf(StringQuery.class);
		JSONAssert.assertEquals(((StringQuery) query).getSource(), expected, JSONCompareMode.NON_EXTENSIBLE);
	}

	@Test // #1790
	@DisplayName("should escape Strings in query parameters")
	void shouldEscapeStringsInQueryParameters() throws Exception {

		org.springframework.data.elasticsearch.core.query.Query query = createQuery("findByPrefix", "hello \"Stranger\"");

		assertThat(query).isInstanceOf(StringQuery.class);
		assertThat(((StringQuery) query).getSource())
				.isEqualTo("{\"bool\":{\"must\": [{\"match\": {\"prefix\": {\"name\" : \"hello \\\"Stranger\\\"\"}}]}}");
	}

	@Test // #1858
	@DisplayName("should only quote String query parameters")
	void shouldOnlyEscapeStringQueryParameters() throws Exception {
		org.springframework.data.elasticsearch.core.query.Query query = createQuery("findByAge", 30);

		assertThat(query).isInstanceOf(StringQuery.class);
		assertThat(((StringQuery) query).getSource()).isEqualTo("{ 'bool' : { 'must' : { 'term' : { 'age' : 30 } } } }");

	}

	@Test // #1858
	@DisplayName("should only quote String collection query parameters")
	void shouldOnlyEscapeStringCollectionQueryParameters() throws Exception {
		org.springframework.data.elasticsearch.core.query.Query query = createQuery("findByAgeIn",
				new ArrayList<>(Arrays.asList(30, 35, 40)));

		assertThat(query).isInstanceOf(StringQuery.class);
		assertThat(((StringQuery) query).getSource())
				.isEqualTo("{ 'bool' : { 'must' : { 'term' : { 'age' : [30,35,40] } } } }");

	}

	@Test // #1858
	@DisplayName("should escape Strings in collection query parameters")
	void shouldEscapeStringsInCollectionsQueryParameters() throws Exception {

		final List<String> another_string = Arrays.asList("hello \"Stranger\"", "Another string");
		List<String> params = new ArrayList<>(another_string);
		org.springframework.data.elasticsearch.core.query.Query query = createQuery("findByNameIn", params);

		assertThat(query).isInstanceOf(StringQuery.class);
		assertThat(((StringQuery) query).getSource()).isEqualTo(
				"{ 'bool' : { 'must' : { 'terms' : { 'name' : [\"hello \\\"Stranger\\\"\",\"Another string\"] } } } }");
	}

	@Test // #2326
	@DisplayName("should escape backslashes in collection query parameters")
	void shouldEscapeBackslashesInCollectionQueryParameters() throws NoSuchMethodException {

		final List<String> parameters = Arrays.asList("param\\1", "param\\2");
		List<String> params = new ArrayList<>(parameters);
		org.springframework.data.elasticsearch.core.query.Query query = createQuery("findByNameIn", params);

		assertThat(query).isInstanceOf(StringQuery.class);
		assertThat(((StringQuery) query).getSource()).isEqualTo(
				"{ 'bool' : { 'must' : { 'terms' : { 'name' : [\"param\\\\1\",\"param\\\\2\"] } } } }");
	}

	private org.springframework.data.elasticsearch.core.query.Query createQuery(String methodName, Object... args)
			throws NoSuchMethodException {

		Class<?>[] argTypes = Arrays.stream(args).map(Object::getClass).toArray(Class[]::new);
		ElasticsearchQueryMethod queryMethod = getQueryMethod(methodName, argTypes);
		ElasticsearchStringQuery elasticsearchStringQuery = queryForMethod(queryMethod);
		return elasticsearchStringQuery.createQuery(new ElasticsearchParametersParameterAccessor(queryMethod, args));
	}

	@Test // #1866
	@DisplayName("should use converter on parameters")
	void shouldUseConverterOnParameters() throws NoSuchMethodException {

		Car car = new Car();
		car.setName("Toyota");
		car.setModel("Prius");

		org.springframework.data.elasticsearch.core.query.Query query = createQuery("findByCar", car);

		assertThat(query).isInstanceOf(StringQuery.class);
		assertThat(((StringQuery) query).getSource())
				.isEqualTo("{ 'bool' : { 'must' : { 'term' : { 'car' : 'Toyota-Prius' } } } }");
	}

	private ElasticsearchStringQuery queryForMethod(ElasticsearchQueryMethod queryMethod) {
		return new ElasticsearchStringQuery(queryMethod, operations, queryMethod.getAnnotatedQuery(),
				QueryMethodEvaluationContextProvider.DEFAULT);
	}

	private ElasticsearchQueryMethod getQueryMethod(String name, Class<?>... parameters) throws NoSuchMethodException {

		Method method = SampleRepository.class.getMethod(name, parameters);
		return new ElasticsearchQueryMethod(method, new DefaultRepositoryMetadata(SampleRepository.class),
				new SpelAwareProxyProjectionFactory(), operations.getElasticsearchConverter().getMappingContext());
	}

	private interface SampleRepository extends Repository<Person, String> {

		@Query("{ 'bool' : { 'must' : { 'term' : { 'age' : ?0 } } } }")
		List<Person> findByAge(Integer age);

		@Query("{ 'bool' : { 'must' : { 'term' : { 'age' : ?0 } } } }")
		List<Person> findByAgeIn(ArrayList<Integer> age);

		@Query("{ 'bool' : { 'must' : { 'term' : { 'name' : '?0' } } } }")
		Person findByName(String name);

		@Query("""
				{
				  "bool":{
				    "must":{
				      "term":{
				        "name": "#{#name}"
				      }
				    }
				  }
				}
				""")
		Person findByNameSpEL(String name);

		@Query("""
				{
				  "bool":{
				    "must":{
				      "term":{
				        "name": "#{#param.value}"
				      }
				    }
				  }
				}
				""")
		Person findByParameterPropertySpEL(QueryParameter param);

		@Query("{ 'bool' : { 'must' : { 'terms' : { 'name' : ?0 } } } }")
		Person findByNameIn(ArrayList<String> names);

		@Query("""
				{
				  "bool":{
				    "must":{
				      "terms":{
				        "name": #{#names}
				      }
				    }
				  }
				}
				""")
		Person findByNamesSpEL(ArrayList<String> names);

		@Query("""
				{
				  "bool":{
				    "must":{
				      "terms":{
				        "age": #{#ages}
				      }
				    }
				  }
				}
				""")
		Person findByAgesSpEL(ArrayList<Integer> ages);

		@Query("""
				{
				  "bool":{
				    "must":{
				      "terms":{
				        "name": #{#names.![value]}
				      }
				    }
				  }
				}
				""")
		Person findByNamesParameterSpEL(ArrayList<QueryParameter> names);

		@Query(value = "name:(?0, ?11, ?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11, ?0, ?1)")
		Person findWithRepeatedPlaceholder(String arg0, String arg1, String arg2, String arg3, String arg4, String arg5,
				String arg6, String arg7, String arg8, String arg9, String arg10, String arg11);

		@Query("{\"bool\":{\"must\": [{\"match\": {\"prefix\": {\"name\" : \"?0\"}}]}}")
		SearchHits<Book> findByPrefix(String prefix);

		@Query("{ 'bool' : { 'must' : { 'term' : { 'car' : '?0' } } } }")
		Person findByCar(Car car);
	}

	/**
	 * @author Rizwan Idrees
	 * @author Mohsin Husen
	 * @author Artur Konczak
	 * @author Niklas Herder
	 */

	@Document(indexName = "test-index-person-query-unittest")
	static class Person {

		@Nullable public int age;
		@Nullable
		@Id private String id;
		@Nullable private String name;
		@Nullable
		@Field(type = FieldType.Nested) private List<Car> car;
		@Nullable
		@Field(type = FieldType.Nested, includeInParent = true) private List<Book> books;

		@Nullable
		public int getAge() {
			return age;
		}

		public void setAge(int age) {
			this.age = age;
		}

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

	@Document(indexName = "test-index-book-query-unittest")
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
