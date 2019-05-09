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
package org.springframework.data.elasticsearch.repository.support;

import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentProperty;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;

/**
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Mark Paluch
 */
@RunWith(MockitoJUnitRunner.class)
public class ElasticsearchRepositoryFactoryTests {

	@Mock private ElasticsearchOperations operations;
	private ElasticsearchConverter converter;
	private ElasticsearchRepositoryFactory factory;
	MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> mappingContext = new SimpleElasticsearchMappingContext();

	@Before
	public void before() {

		converter = new MappingElasticsearchConverter(mappingContext);
		when(operations.getElasticsearchConverter()).thenReturn(converter);
		factory = new ElasticsearchRepositoryFactory(operations);
	}

	@Test(expected = IllegalArgumentException.class)
	public void shouldThrowExceptionGivenQueryDslRepository() {

		// given
		RepositoryMetadata metadata = new DefaultRepositoryMetadata(QuerydslPredicateExecutor.class);
		// when
		factory.getRepositoryBaseClass(metadata);
	}
}
