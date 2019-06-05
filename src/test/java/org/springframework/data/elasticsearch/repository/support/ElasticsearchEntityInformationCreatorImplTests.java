/*
 * Copyright 2014-2019 the original author or authors.
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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentProperty;
import org.springframework.data.mapping.context.MappingContext;

/**
 * @author Florian Hopf
 * @author Mark Paluch
 */
@RunWith(MockitoJUnitRunner.class)
public class ElasticsearchEntityInformationCreatorImplTests {

	@Mock MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> mappingContext;
	@Mock ElasticsearchPersistentEntity<String> persistentEntity;

	ElasticsearchEntityInformationCreatorImpl entityInfoCreator;

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
		entityInfoCreator.getEntityInformation(String.class);
	}
}
