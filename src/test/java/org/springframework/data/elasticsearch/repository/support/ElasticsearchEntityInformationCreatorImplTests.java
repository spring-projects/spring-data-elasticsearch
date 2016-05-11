/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.elasticsearch.repository.support;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentProperty;
import org.springframework.data.mapping.context.MappingContext;

import static org.mockito.Mockito.doReturn;

/**
 * @author Florian Hopf
 */
@RunWith(MockitoJUnitRunner.class)
public class ElasticsearchEntityInformationCreatorImplTests {

	@Mock
	private MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> mappingContext;
	@Mock
	private ElasticsearchPersistentEntity<String> persistentEntity;
	private ElasticsearchEntityInformationCreatorImpl entityInfoCreator;

	@Before
	public void before() {
		entityInfoCreator = new ElasticsearchEntityInformationCreatorImpl(mappingContext);
	}

	@Test(expected = IllegalArgumentException.class)
	public void shouldThrowIllegalArgumentExceptionOnMissingEntity() {
		entityInfoCreator.getEntityInformation(String.class);
	}

	@Test(expected = IllegalArgumentException.class)
	public void shouldThrowIllegalArgumentExceptionOnMissingIdAnnotation() {
		doReturn(persistentEntity).when(mappingContext).getPersistentEntity(String.class);
		doReturn(String.class).when(persistentEntity).getType();
		doReturn(null).when(persistentEntity).getIdProperty();
		entityInfoCreator.getEntityInformation(String.class);
	}
}
