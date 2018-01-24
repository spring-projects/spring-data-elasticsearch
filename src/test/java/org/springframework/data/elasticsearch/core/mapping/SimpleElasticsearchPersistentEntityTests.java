/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.data.elasticsearch.core.mapping;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;

import org.junit.Test;
import org.springframework.data.annotation.Version;
import org.springframework.data.mapping.model.MappingException;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;

/**
 * @author Rizwan Idrees
 * @author Mohsin Husen
 */
public class SimpleElasticsearchPersistentEntityTests {

	@Test(expected = IllegalArgumentException.class)
	public void shouldThrowExceptionGivenVersionPropertyIsNotLong() throws NoSuchFieldException, IntrospectionException {
		// given
		TypeInformation typeInformation = ClassTypeInformation.from(EntityWithWrongVersionType.class);
		SimpleElasticsearchPersistentProperty persistentProperty = new SimpleElasticsearchPersistentProperty(
				EntityWithWrongVersionType.class.getDeclaredField("version"), new PropertyDescriptor("version",
				EntityWithWrongVersionType.class), new SimpleElasticsearchPersistentEntity<EntityWithWrongVersionType>(
				typeInformation), new SimpleTypeHolder()
		);

		// when
		new SimpleElasticsearchPersistentEntity(typeInformation).addPersistentProperty(persistentProperty);
	}

	@Test(expected = MappingException.class)
	public void shouldThrowExceptionGivenMultipleVersionPropertiesArePresent() throws NoSuchFieldException,
			IntrospectionException {
		// given
		TypeInformation typeInformation = ClassTypeInformation.from(EntityWithMultipleVersionField.class);
		SimpleElasticsearchPersistentProperty persistentProperty1 = new SimpleElasticsearchPersistentProperty(
				EntityWithMultipleVersionField.class.getDeclaredField("version1"), new PropertyDescriptor("version1",
				EntityWithMultipleVersionField.class),
				new SimpleElasticsearchPersistentEntity<EntityWithMultipleVersionField>(typeInformation),
				new SimpleTypeHolder()
		);

		SimpleElasticsearchPersistentProperty persistentProperty2 = new SimpleElasticsearchPersistentProperty(
				EntityWithMultipleVersionField.class.getDeclaredField("version2"), new PropertyDescriptor("version2",
				EntityWithMultipleVersionField.class),
				new SimpleElasticsearchPersistentEntity<EntityWithMultipleVersionField>(typeInformation),
				new SimpleTypeHolder()
		);

		SimpleElasticsearchPersistentEntity simpleElasticsearchPersistentEntity = new SimpleElasticsearchPersistentEntity(
				typeInformation);
		simpleElasticsearchPersistentEntity.addPersistentProperty(persistentProperty1);
		// when
		simpleElasticsearchPersistentEntity.addPersistentProperty(persistentProperty2);
	}

	private class EntityWithWrongVersionType {

		@Version
		private String version;

		public String getVersion() {
			return version;
		}

		public void setVersion(String version) {
			this.version = version;
		}
	}

	private class EntityWithMultipleVersionField {

		@Version
		private Long version1;
		@Version
		private Long version2;

		public Long getVersion1() {
			return version1;
		}

		public void setVersion1(Long version1) {
			this.version1 = version1;
		}

		public Long getVersion2() {
			return version2;
		}

		public void setVersion2(Long version2) {
			this.version2 = version2;
		}
	}
}
