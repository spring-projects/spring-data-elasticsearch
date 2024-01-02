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

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.data.domain.Range;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.util.Assert;

/**
 * @author Sascha Woo
 * @since 4.3
 */
public abstract class AbstractRangePropertyValueConverter<T> extends AbstractPropertyValueConverter {

	protected static final String LT_FIELD = "lt";
	protected static final String LTE_FIELD = "lte";
	protected static final String GT_FIELD = "gt";
	protected static final String GTE_FIELD = "gte";
	private final Class<?> genericType;

	/**
	 * @param property the property this convertrer belongs to
	 * @param genericType the generic type of the Range
	 */
	public AbstractRangePropertyValueConverter(PersistentProperty<?> property, Class<?> genericType) {
		super(property);
		this.genericType = genericType;
	}

	public Class<?> getGenericType() {
		return genericType;
	}

	@Override
	public Object read(Object value) {

		Assert.notNull(value, "value must not be null.");
		Assert.isInstanceOf(Map.class, value, "value must be instance of Map.");

		try {
			// noinspection unchecked
			Map<String, Object> source = (Map<String, Object>) value;
			Range.Bound<T> lowerBound;
			Range.Bound<T> upperBound;

			if (source.containsKey(GTE_FIELD)) {
				lowerBound = Range.Bound.inclusive(parse((String) source.get(GTE_FIELD)));
			} else if (source.containsKey(GT_FIELD)) {
				lowerBound = Range.Bound.exclusive(parse((String) source.get(GT_FIELD)));
			} else {
				lowerBound = Range.Bound.unbounded();
			}

			if (source.containsKey(LTE_FIELD)) {
				upperBound = Range.Bound.inclusive(parse((String) source.get(LTE_FIELD)));
			} else if (source.containsKey(LT_FIELD)) {
				upperBound = Range.Bound.exclusive(parse((String) source.get(LT_FIELD)));
			} else {
				upperBound = Range.Bound.unbounded();
			}

			return Range.of(lowerBound, upperBound);

		} catch (Exception e) {
			throw new ConversionException(
					String.format("Unable to convert value '%s' of property '%s'", value, getProperty().getName()), e);
		}
	}

	@Override
	public Object write(Object value) {

		Assert.notNull(value, "value must not be null.");

		if (!Range.class.isAssignableFrom(value.getClass())) {
			return value.toString();
		}

		try {
			// noinspection unchecked
			Range<T> range = (Range<T>) value;
			Range.Bound<T> lowerBound = range.getLowerBound();
			Range.Bound<T> upperBound = range.getUpperBound();
			Map<String, Object> target = new LinkedHashMap<>();

			if (lowerBound.getValue().isPresent()) {
				String lowerBoundValue = format(lowerBound.getValue().get());
				if (lowerBound.isInclusive()) {
					target.put(GTE_FIELD, lowerBoundValue);
				} else {
					target.put(GT_FIELD, lowerBoundValue);
				}
			}

			if (upperBound.getValue().isPresent()) {
				String upperBoundValue = format(upperBound.getValue().get());
				if (upperBound.isInclusive()) {
					target.put(LTE_FIELD, upperBoundValue);
				} else {
					target.put(LT_FIELD, upperBoundValue);
				}
			}

			return target;

		} catch (Exception e) {
			throw new ConversionException(
					String.format("Unable to convert value '%s' of property '%s'", value, getProperty().getName()), e);
		}
	}

	protected abstract String format(T value);

	protected abstract T parse(String value);

}
