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
public class DateRangePropertyValueConverter extends AbstractRangePropertyValueConverter<Date> {

	private static final Log LOGGER = LogFactory.getLog(DateRangePropertyValueConverter.class);

	private final List<ElasticsearchDateConverter> dateConverters;

	public DateRangePropertyValueConverter(PersistentProperty<?> property,
			Class<?> genericType, List<ElasticsearchDateConverter> dateConverters) {

		super(property, genericType);
		this.dateConverters = dateConverters;
	}

	@Override
	protected String format(Date value) {
		return dateConverters.get(0).format(value);
	}

	@Override
	protected Date parse(String value) {

		for (ElasticsearchDateConverter converters : dateConverters) {
			try {
				return converters.parse(value);
			} catch (Exception e) {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace(e.getMessage(), e);
				}
			}
		}

		throw new ConversionException(String.format("Unable to convert value '%s' to %s for property '%s'", value,
				getGenericType().getTypeName(), getProperty().getName()));
	}

}
