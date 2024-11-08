/*
 * Copyright 2025 the original author or authors.
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

import static org.mockito.Mockito.*;

import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;

@ExtendWith(MockitoExtension.class)
public class ReactiveRepositoryQueryUnitTestsBase {

	@Mock ReactiveElasticsearchOperations operations;

	/**
	 * set up the {operations} mock to return the {@link ElasticsearchConverter} from setupConverter().
	 */
	@BeforeEach
	public void setUp() {
		when(operations.getElasticsearchConverter()).thenReturn(setupConverter());
	}

	/**
	 * @return a simple {@link MappingElasticsearchConverter} with no special setup.
	 */
	protected MappingElasticsearchConverter setupConverter() {
		return new MappingElasticsearchConverter(
				new SimpleElasticsearchMappingContext());
	}

	/**
	 * Creates a {@link ReactiveElasticsearchQueryMethod} for the given method
	 *
	 * @param repositoryClass
	 * @param name
	 * @param parameters
	 * @return
	 * @throws NoSuchMethodException
	 */

	protected ReactiveElasticsearchQueryMethod getQueryMethod(Class<?> repositoryClass, String name,
			Class<?>... parameters)
			throws NoSuchMethodException {

		Method method = repositoryClass.getMethod(name, parameters);
		return new ReactiveElasticsearchQueryMethod(method,
				new DefaultRepositoryMetadata(repositoryClass),
				new SpelAwareProxyProjectionFactory(), operations.getElasticsearchConverter().getMappingContext());
	}
}
