/*
 * Copyright 2018-2019 the original author or authors.
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
package org.springframework.data.elasticsearch.core.mapping;

import static org.assertj.core.api.Assertions.*;

import org.junit.Test;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.Score;
import org.springframework.data.mapping.MappingException;

/**
 * Unit tests for {@link SimpleElasticsearchPersistentProperty}.
 *
 * @author Oliver Gierke
 * @author Peter-Josef Meisch
 */
public class SimpleElasticsearchPersistentPropertyUnitTests {

	private final SimpleElasticsearchMappingContext context = new SimpleElasticsearchMappingContext();

	@Test // DATAES-462
	public void rejectsScorePropertyOfTypeOtherthanFloat() {

		assertThatExceptionOfType(MappingException.class) //
				.isThrownBy(() -> context.getRequiredPersistentEntity(InvalidScoreProperty.class)) //
				.withMessageContaining("scoreProperty");
	}

	@Test // DATAES-562
	public void fieldAnnotationWithNameSetsFieldname() {

		SimpleElasticsearchPersistentEntity<?> persistentEntity = context
				.getRequiredPersistentEntity(FieldNameProperty.class);
		ElasticsearchPersistentProperty persistentProperty = persistentEntity.getPersistentProperty("fieldProperty");

		assertThat(persistentProperty).isNotNull();
		assertThat(persistentProperty.getFieldName()).isEqualTo("by-name");
	}

	@Test // DATAES-562
	public void fieldAnnotationWithValueSetsFieldname() {

		SimpleElasticsearchPersistentEntity<?> persistentEntity = context
				.getRequiredPersistentEntity(FieldValueProperty.class);
		ElasticsearchPersistentProperty persistentProperty = persistentEntity.getPersistentProperty("fieldProperty");

		assertThat(persistentProperty).isNotNull();
		assertThat(persistentProperty.getFieldName()).isEqualTo("by-value");
	}

	static class InvalidScoreProperty {
		@Score String scoreProperty;
	}

	static class FieldNameProperty {
		@Field(name = "by-name") String fieldProperty;
	}

	static class FieldValueProperty {
		@Field(value = "by-value") String fieldProperty;
	}
}
