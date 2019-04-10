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

import java.lang.reflect.Method;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.data.elasticsearch.entities.Person;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;

/**
 * @author Christoph Strobl
 */
@RunWith(MockitoJUnitRunner.class)
public class ElasticsearchStringQueryUnitTests {

	@Mock ElasticsearchOperations operations;
	ElasticsearchConverter converter;

	@Before
	public void setUp() {
		converter = new MappingElasticsearchConverter(new SimpleElasticsearchMappingContext());
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

	private org.springframework.data.elasticsearch.core.query.Query createQuery(String methodName, String... args)
			throws NoSuchMethodException {

		Class<?>[] argTypes = Arrays.stream(args).map(Object::getClass).toArray(size -> new Class[size]);
		ElasticsearchQueryMethod queryMethod = getQueryMethod(methodName, argTypes);
		ElasticsearchStringQuery elasticsearchStringQuery = queryForMethod(queryMethod);
		return elasticsearchStringQuery.createQuery(new ElasticsearchParametersParameterAccessor(queryMethod, args));
	}

	private ElasticsearchStringQuery queryForMethod(ElasticsearchQueryMethod queryMethod) {
		return new ElasticsearchStringQuery(queryMethod, operations, queryMethod.getAnnotatedQuery());
	}

	private ElasticsearchQueryMethod getQueryMethod(String name, Class<?>... parameters) throws NoSuchMethodException {

		Method method = SampleRepository.class.getMethod(name, parameters);
		return new ElasticsearchQueryMethod(method, new DefaultRepositoryMetadata(SampleRepository.class),
				new SpelAwareProxyProjectionFactory(), converter.getMappingContext());
	}

	private interface SampleRepository extends Repository<Person, String> {

		@Query("{ 'bool' : { 'must' : { 'term' : { 'name' : '?0' } } } }")
		Person findByName(String name);

		@Query(value = "name:(?0, ?11, ?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11, ?0, ?1)")
		Person findWithRepeatedPlaceholder(String arg0, String arg1, String arg2, String arg3, String arg4, String arg5,
				String arg6, String arg7, String arg8, String arg9, String arg10, String arg11);
	}
}
