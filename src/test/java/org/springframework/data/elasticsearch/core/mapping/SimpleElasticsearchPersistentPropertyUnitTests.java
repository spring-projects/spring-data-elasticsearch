/*
 * Copyright 2018-2020 the original author or authors.
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
import java.util.GregorianCalendar;

import org.junit.jupiter.api.Test;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Score;
import org.springframework.data.mapping.MappingException;
import org.springframework.lang.Nullable;

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

	@Test // DATAES-716, DATAES-792
	void shouldSetPropertyConverters() {
		SimpleElasticsearchPersistentEntity<?> persistentEntity = context.getRequiredPersistentEntity(DatesProperty.class);

		ElasticsearchPersistentProperty persistentProperty = persistentEntity.getRequiredPersistentProperty("localDate");
		assertThat(persistentProperty.hasPropertyConverter()).isTrue();
		assertThat(persistentProperty.getPropertyConverter()).isNotNull();

		persistentProperty = persistentEntity.getRequiredPersistentProperty("localDateTime");
		assertThat(persistentProperty.hasPropertyConverter()).isTrue();
		assertThat(persistentProperty.getPropertyConverter()).isNotNull();

		persistentProperty = persistentEntity.getRequiredPersistentProperty("legacyDate");
		assertThat(persistentProperty.hasPropertyConverter()).isTrue();
		assertThat(persistentProperty.getPropertyConverter()).isNotNull();

	}

	@Test // DATAES-716
	void shouldConvertFromLocalDate() {
		SimpleElasticsearchPersistentEntity<?> persistentEntity = context.getRequiredPersistentEntity(DatesProperty.class);
		ElasticsearchPersistentProperty persistentProperty = persistentEntity.getRequiredPersistentProperty("localDate");
		LocalDate localDate = LocalDate.of(2019, 12, 27);

		String converted = persistentProperty.getPropertyConverter().write(localDate);

		assertThat(converted).isEqualTo("27.12.2019");
	}

	@Test // DATAES-716
	void shouldConvertToLocalDate() {
		SimpleElasticsearchPersistentEntity<?> persistentEntity = context.getRequiredPersistentEntity(DatesProperty.class);
		ElasticsearchPersistentProperty persistentProperty = persistentEntity.getRequiredPersistentProperty("localDate");

		Object converted = persistentProperty.getPropertyConverter().read("27.12.2019");

		assertThat(converted).isInstanceOf(LocalDate.class);
		assertThat(converted).isEqualTo(LocalDate.of(2019, 12, 27));
	}

	@Test // DATAES_792
	void shouldConvertFromLegacyDate() {
		SimpleElasticsearchPersistentEntity<?> persistentEntity = context.getRequiredPersistentEntity(DatesProperty.class);
		ElasticsearchPersistentProperty persistentProperty = persistentEntity.getRequiredPersistentProperty("legacyDate");
		Date legacyDate = new GregorianCalendar(2020, 3, 19, 21, 44).getTime();

		String converted = persistentProperty.getPropertyConverter().write(legacyDate);

		assertThat(converted).isEqualTo("20200419T194400.000Z");
	}

	@Test // DATES-792
	void shouldConvertToLegacyDate() {
		SimpleElasticsearchPersistentEntity<?> persistentEntity = context.getRequiredPersistentEntity(DatesProperty.class);
		ElasticsearchPersistentProperty persistentProperty = persistentEntity.getRequiredPersistentProperty("legacyDate");

		Object converted = persistentProperty.getPropertyConverter().read("20200419T194400.000Z");

		assertThat(converted).isInstanceOf(Date.class);
		assertThat(converted).isEqualTo(new GregorianCalendar(2020, 3, 19, 21, 44).getTime());
	}

	static class InvalidScoreProperty {
		@Nullable @Score String scoreProperty;
	}

	static class FieldNameProperty {
		@Nullable @Field(name = "by-name") String fieldProperty;
	}

	static class FieldValueProperty {
		@Nullable @Field(value = "by-value") String fieldProperty;
	}

	static class DatesProperty {
		@Nullable @Field(type = FieldType.Date, format = DateFormat.custom, pattern = "dd.MM.uuuu") LocalDate localDate;
		@Nullable @Field(type = FieldType.Date, format = DateFormat.basic_date_time) LocalDateTime localDateTime;
		@Nullable @Field(type = FieldType.Date, format = DateFormat.basic_date_time) Date legacyDate;
	}
}
