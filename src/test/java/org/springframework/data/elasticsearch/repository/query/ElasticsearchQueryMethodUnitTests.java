/*
 * Copyright 2021-2024 the original author or authors.
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
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.CountQuery;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;
import org.springframework.lang.Nullable;

/**
 * @author Peter-Josef Meisch
 */
public class ElasticsearchQueryMethodUnitTests {

	private SimpleElasticsearchMappingContext mappingContext;

	@BeforeEach
	public void setUp() {
		mappingContext = new SimpleElasticsearchMappingContext();
	}

	@Test // #1156
	@DisplayName("should reject count query method not returning a Long")
	void shouldRejectCountQueryMethodNotReturningLong() {

		assertThatThrownBy(() -> queryMethod(PersonRepository.class, "invalidCountQueryResult", String.class))
				.isInstanceOf(InvalidDataAccessApiUsageException.class);
	}

	@Test // #1156
	@DisplayName("should accept count query method returning a Long")
	void shouldAcceptCountQueryMethodReturningALong() throws Exception {
		queryMethod(PersonRepository.class, "validCountQueryResult", String.class);
	}

	private ElasticsearchQueryMethod queryMethod(Class<?> repository, String name, Class<?>... parameters)
			throws Exception {

		Method method = repository.getMethod(name, parameters);
		ProjectionFactory factory = new SpelAwareProxyProjectionFactory();
		return new ElasticsearchQueryMethod(method, new DefaultRepositoryMetadata(repository), factory, mappingContext);
	}

	interface PersonRepository extends Repository<ReactiveElasticsearchQueryMethodUnitTests.Person, String> {
		@CountQuery("{}")
		List<Person> invalidCountQueryResult(String name); // invalid return type here

		@CountQuery("{}")
		Long validCountQueryResult(String name);
	}

	@Document(indexName = "query-method-unit-tests")
	private static class Person {
		@Nullable
		@Id private String id;
		@Nullable private String name;
		@Nullable private String firstName;

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
		public String getFirstName() {
			return firstName;
		}

		public void setFirstName(@Nullable String firstName) {
			this.firstName = firstName;
		}
	}
}
