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
package org.springframework.data.elasticsearch.core.convert;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentProperty;
import org.springframework.data.elasticsearch.core.mapping.PropertyValueConverter;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchPersistentEntity;
import org.springframework.lang.Nullable;

/**
 * @author Peter-Josef Meisch
 */
public class PropertyValueConvertersUnitTests {

	@ParameterizedTest(name = "{0}") // #2018
	@MethodSource("propertyValueConverters")
	@DisplayName("should return original object on write if it cannot be converted")
	void shouldReturnOriginalObjectOnWriteIfItCannotBeConverted(PropertyValueConverter converter) {

		NoConverterForThisClass value = new NoConverterForThisClass();

		Object written = converter.write(value);

		assertThat(written).isEqualTo(value.toString());
	}

	static Stream<Arguments> propertyValueConverters() {

		SimpleElasticsearchMappingContext context = new SimpleElasticsearchMappingContext();
		SimpleElasticsearchPersistentEntity<?> persistentEntity = context
				.getRequiredPersistentEntity(NoConverterForThisClass.class);
		ElasticsearchPersistentProperty persistentProperty = persistentEntity.getRequiredPersistentProperty("property");

		List<PropertyValueConverter> converters = new ArrayList<>();

		converters.add(new DatePropertyValueConverter(persistentProperty,
				Collections.singletonList(ElasticsearchDateConverter.of(DateFormat.basic_date))));
		Class<?> genericType = Object.class;
		converters.add(new DateRangePropertyValueConverter(persistentProperty,
				genericType, Collections.singletonList(ElasticsearchDateConverter.of(DateFormat.basic_date))));
		converters.add(new NumberRangePropertyValueConverter(persistentProperty, genericType));
		converters.add(new TemporalPropertyValueConverter(persistentProperty,
				Collections.singletonList(ElasticsearchDateConverter.of(DateFormat.basic_date))));
		converters.add(new TemporalRangePropertyValueConverter(persistentProperty,
				genericType, Collections.singletonList(ElasticsearchDateConverter.of(DateFormat.basic_date))));

		return converters.stream().map(propertyValueConverter -> arguments(
				Named.of(propertyValueConverter.getClass().getSimpleName(), propertyValueConverter)));
	}

	static class NoConverterForThisClass {
		@SuppressWarnings("unused")
		@Nullable Long property;
	}
}
