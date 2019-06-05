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
package org.springframework.data.elasticsearch.core.convert;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchSimpleTypes;
import org.springframework.util.NumberUtils;

/**
 * Elasticsearch specific {@link CustomConversions}.
 *
 * @author Christoph Strobl
 * @since 3.2
 */
public class ElasticsearchCustomConversions extends CustomConversions {

	private static final StoreConversions STORE_CONVERSIONS;
	private static final List<Object> STORE_CONVERTERS;

	static {

		List<Object> converters = new ArrayList<>();

		converters.addAll(GeoConverters.getConvertersToRegister());
		converters.add(StringToUUIDConverter.INSTANCE);
		converters.add(UUIDToStringConverter.INSTANCE);
		converters.add(BigDecimalToDoubleConverter.INSTANCE);
		converters.add(DoubleToBigDecimalConverter.INSTANCE);

		STORE_CONVERTERS = Collections.unmodifiableList(converters);
		STORE_CONVERSIONS = StoreConversions.of(ElasticsearchSimpleTypes.HOLDER, STORE_CONVERTERS);
	}

	/**
	 * Creates a new {@link CustomConversions} instance registering the given converters.
	 *
	 * @param converters must not be {@literal null}.
	 */
	public ElasticsearchCustomConversions(Collection<?> converters) {
		super(STORE_CONVERSIONS, converters);
	}

	/**
	 * {@link Converter} to read a {@link UUID} from its {@link String} representation.
	 */
	@ReadingConverter
	enum StringToUUIDConverter implements Converter<String, UUID> {

		INSTANCE;

		@Override
		public UUID convert(String source) {
			return UUID.fromString(source);
		}
	}

	/**
	 * {@link Converter} to write a {@link UUID} to its {@link String} representation.
	 */
	@WritingConverter
	enum UUIDToStringConverter implements Converter<UUID, String> {

		INSTANCE;

		@Override
		public String convert(UUID source) {
			return source.toString();
		}
	}

	/**
	 * {@link Converter} to read a {@link BigDecimal} from a {@link Double} value.
	 */
	@ReadingConverter
	enum DoubleToBigDecimalConverter implements Converter<Double, BigDecimal> {

		INSTANCE;

		@Override
		public BigDecimal convert(Double source) {
			return NumberUtils.convertNumberToTargetClass(source, BigDecimal.class);
		}
	}

	/**
	 * {@link Converter} to write a {@link BigDecimal} to a {@link Double} value.
	 */
	@WritingConverter
	enum BigDecimalToDoubleConverter implements Converter<BigDecimal, Double> {

		INSTANCE;

		@Override
		public Double convert(BigDecimal source) {
			return NumberUtils.convertNumberToTargetClass(source, Double.class);
		}
	}
}
