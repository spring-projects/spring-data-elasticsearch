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

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.data.elasticsearch.entities.Person;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * @author Christoph Strobl
 * @currentRead Fool's Fate - Robin Hobb
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
    public void shouldReplaceLotsOfParametersCorrectly() throws Exception{
        ReactiveElasticsearchStringQuery elasticsearchStringQuery = createQueryForMethod("findWithQuiteSomeParameters", String.class, String.class,
                String.class, String.class, String.class, String.class, String.class, String.class, String.class, String.class,
                String.class, String.class);
        StubParameterAccessor accesor = new StubParameterAccessor("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l");
        org.springframework.data.elasticsearch.core.query.Query query = elasticsearchStringQuery.createQuery(accesor);
        StringQuery reference = new StringQuery("name:(a, b, c, d, e, f, g, h, i, j, k, l)");
        assertThat(query).isInstanceOf(StringQuery.class);
        assertThat(((StringQuery) query).getSource()).isEqualTo(reference.getSource());
    }

	private ReactiveElasticsearchStringQuery createQueryForMethod(String name, Class<?>... parameters) throws Exception {

		Method method = SampleRepository.class.getMethod(name, parameters);
		ProjectionFactory factory = new SpelAwareProxyProjectionFactory();
		ReactiveElasticsearchQueryMethod queryMethod = new ReactiveElasticsearchQueryMethod(method,
				new DefaultRepositoryMetadata(SampleRepository.class), factory, converter.getMappingContext());
		return new ReactiveElasticsearchStringQuery(queryMethod, operations, PARSER,
				QueryMethodEvaluationContextProvider.DEFAULT);
	}

	private interface SampleRepository extends Repository<Person, String> {

		@Query("{ 'bool' : { 'must' : { 'term' : { 'name' : '?0' } } } }")
		Mono<Person> findByName(String name);

		@Query("{ 'bool' : { 'must' : { 'term' : { 'name' : '?#{[0]}' } } } }")
		Flux<Person> findByNameWithExpression(String param0);

		@Query(value = "name:(?0, ?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11)")
		Person findWithQuiteSomeParameters(String arg0, String arg1, String arg2, String arg3, String arg4,
												String arg5, String arg6, String arg7, String arg8, String arg9, String arg10, String arg11);
	}
}
