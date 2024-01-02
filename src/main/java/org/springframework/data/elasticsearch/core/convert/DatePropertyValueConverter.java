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

import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.data.mapping.PersistentProperty;

/**
 * @author Sascha Woo
 * @since 4.3
 */
public class DatePropertyValueConverter extends AbstractPropertyValueConverter {

	private static final Log LOGGER = LogFactory.getLog(DatePropertyValueConverter.class);

	private final List<ElasticsearchDateConverter> dateConverters;

	public DatePropertyValueConverter(PersistentProperty<?> property, List<ElasticsearchDateConverter> dateConverters) {

		super(property);
		this.dateConverters = dateConverters;
	}

	@Override
	public Object read(Object value) {

		String s = value.toString();

		for (ElasticsearchDateConverter dateConverter : dateConverters) {
			try {
				return dateConverter.parse(s);
			} catch (Exception e) {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace(e.getMessage(), e);
				}
			}
		}

		throw new ConversionException(String.format("Unable to convert value '%s' to %s for property '%s'", s,
				getProperty().getActualType().getTypeName(), getProperty().getName()));
	}

	@Override
	public Object write(Object value) {

		if (!Date.class.isAssignableFrom(value.getClass())) {
			return value.toString();
		}

		try {
			return dateConverters.get(0).format((Date) value);
		} catch (Exception e) {
			throw new ConversionException(
					String.format("Unable to convert value '%s' of property '%s'", value, getProperty().getName()), e);
		}
	}

}
