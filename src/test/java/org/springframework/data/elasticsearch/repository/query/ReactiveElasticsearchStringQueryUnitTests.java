/*
 * Copyright 2019 the original author or authors.
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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * @author Christoph Strobl
 * @currentRead Fool's Fate - Robin Hobb
 * @author Peter-Josef Meisch
 */
@RunWith(MockitoJUnitRunner.class)
public class ReactiveElasticsearchStringQueryUnitTests {

	SpelExpressionParser PARSER = new SpelExpressionParser();
	ElasticsearchConverter converter;

	@Mock ReactiveElasticsearchOperations operations;

	@Before
	public void setUp() {
		converter = new MappingElasticsearchConverter(new SimpleElasticsearchMappingContext());
	}

	@Test // DATAES-519
	public void bindsSimplePropertyCorrectly() throws Exception {

		ReactiveElasticsearchStringQuery elasticsearchStringQuery = createQueryForMethod("findByName", String.class);
		StubParameterAccessor accesor = new StubParameterAccessor("Luke");

		org.springframework.data.elasticsearch.core.query.Query query = elasticsearchStringQuery.createQuery(accesor);
		StringQuery reference = new StringQuery("{ 'bool' : { 'must' : { 'term' : { 'name' : 'Luke' } } } }");

		assertThat(query).isInstanceOf(StringQuery.class);
		assertThat(((StringQuery) query).getSource()).isEqualTo(reference.getSource());
	}

	@Test // DATAES-519
	@Ignore("TODO: fix spel query integration")
	public void bindsExpressionPropertyCorrectly() throws Exception {

		ReactiveElasticsearchStringQuery elasticsearchStringQuery = createQueryForMethod("findByNameWithExpression",
				String.class);
		StubParameterAccessor accesor = new StubParameterAccessor("Luke");

		org.springframework.data.elasticsearch.core.query.Query query = elasticsearchStringQuery.createQuery(accesor);
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

	private org.springframework.data.elasticsearch.core.query.Query createQuery(String methodName, String... args)
			throws NoSuchMethodException {

		Class<?>[] argTypes = Arrays.stream(args).map(Object::getClass).toArray(size -> new Class[size]);
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
				new SpelAwareProxyProjectionFactory(), converter.getMappingContext());
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
	}

	/**
	 * @author Rizwan Idrees
	 * @author Mohsin Husen
	 * @author Artur Konczak
	 */

	@Document(indexName = "test-index-person-reactive-repository-string-query", type = "user", shards = 1, replicas = 0,
			refreshInterval = "-1")
	public class Person {

		@Id private String id;

		private String name;

		@Field(type = FieldType.Nested) private List<Car> car;

		@Field(type = FieldType.Nested, includeInParent = true) private List<Book> books;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public List<Car> getCar() {
			return car;
		}

		public void setCar(List<Car> car) {
			this.car = car;
		}

		public List<Book> getBooks() {
			return books;
		}

		public void setBooks(List<Book> books) {
			this.books = books;
		}
	}

	/**
	 * @author Rizwan Idrees
	 * @author Mohsin Husen
	 * @author Nordine Bittich
	 */
	@Setter
	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	@Document(indexName = "test-index-book-reactive-repository-string-query", type = "book", shards = 1, replicas = 0,
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

	/**
	 * @author Rizwan Idrees
	 * @author Mohsin Husen
	 * @author Artur Konczak
	 */
	@Setter
	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	static class Car {

		private String name;
		private String model;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getModel() {
			return model;
		}

		public void setModel(String model) {
			this.model = model;
		}
	}

	/**
	 * @author Rizwan Idrees
	 * @author Mohsin Husen
	 */
	static class Author {

		private String id;
		private String name;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
