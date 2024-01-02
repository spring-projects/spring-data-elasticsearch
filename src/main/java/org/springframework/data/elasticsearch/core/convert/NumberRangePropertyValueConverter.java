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

import org.springframework.data.mapping.PersistentProperty;

/**
 * @author Sascha Woo
 * @since 4.3
 */
public class NumberRangePropertyValueConverter extends AbstractRangePropertyValueConverter<Number> {

	/**
	 * @param property the property this convertrer belongs to
	 * @param genericType the generic type of the Range
	 */
	public NumberRangePropertyValueConverter(PersistentProperty<?> property, Class<?> genericType) {
		super(property, genericType);
	}

	@Override
	protected String format(Number number) {
		return String.valueOf(number);
	}

	@Override
	protected Number parse(String value) {

		Class<?> type = getGenericType();
		if (Integer.class.isAssignableFrom(type)) {
			return Integer.valueOf(value);
		} else if (Float.class.isAssignableFrom(type)) {
			return Float.valueOf(value);
		} else if (Long.class.isAssignableFrom(type)) {
			return Long.valueOf(value);
		} else if (Double.class.isAssignableFrom(type)) {
			return Double.valueOf(value);
		}

		throw new ConversionException(String.format("Unable to convert value '%s' to %s for property '%s'", value,
				type.getTypeName(), getProperty().getName()));
	}

}
