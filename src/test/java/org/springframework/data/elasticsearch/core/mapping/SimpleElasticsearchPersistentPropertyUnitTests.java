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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
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

	@Test
	// DATAES-716
	void shouldSetPropertyConverters() {
		SimpleElasticsearchPersistentEntity<?> persistentEntity = context.getRequiredPersistentEntity(DatesProperty.class);

		ElasticsearchPersistentProperty persistentProperty = persistentEntity.getRequiredPersistentProperty("date");
		assertThat(persistentProperty.hasPropertyConverter()).isFalse();

		persistentProperty = persistentEntity.getRequiredPersistentProperty("localDate");
		assertThat(persistentProperty.hasPropertyConverter()).isTrue();
		assertThat(persistentProperty.getPropertyConverter()).isNotNull();

		persistentProperty = persistentEntity.getRequiredPersistentProperty("localDateTime");
		assertThat(persistentProperty.hasPropertyConverter()).isTrue();
		assertThat(persistentProperty.getPropertyConverter()).isNotNull();
	}

	@Test
	// DATAES-716
	void shouldConvertFromLocalDate() {
		SimpleElasticsearchPersistentEntity<?> persistentEntity = context.getRequiredPersistentEntity(DatesProperty.class);
		ElasticsearchPersistentProperty persistentProperty = persistentEntity.getRequiredPersistentProperty("localDate");
		LocalDate localDate = LocalDate.of(2019, 12, 27);

		String converted = persistentProperty.getPropertyConverter().write(localDate);

		assertThat(converted).isEqualTo("27.12.2019");
	}

	@Test
	// DATAES-716
	void shouldConvertToLocalDate() {
		SimpleElasticsearchPersistentEntity<?> persistentEntity = context.getRequiredPersistentEntity(DatesProperty.class);
		ElasticsearchPersistentProperty persistentProperty = persistentEntity.getRequiredPersistentProperty("localDate");

		Object converted = persistentProperty.getPropertyConverter().read("27.12.2019");

		assertThat(converted).isInstanceOf(LocalDate.class);
		assertThat(converted).isEqualTo(LocalDate.of(2019, 12, 27));
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

	static class DatesProperty {
		@Field(type = FieldType.Date, format = DateFormat.basic_date) Date date;
		@Field(type = FieldType.Date, format = DateFormat.custom, pattern = "dd.MM.uuuu") LocalDate localDate;
		@Field(type = FieldType.Date, format = DateFormat.basic_date_time) LocalDateTime localDateTime;
	}
}
