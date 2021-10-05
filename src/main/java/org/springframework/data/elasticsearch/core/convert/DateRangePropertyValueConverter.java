/*
 * Copyright 2021 the original author or authors.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mapping.PersistentProperty;

/**
 * @author Sascha Woo
 * @since 4.3
 */
public class DateRangePropertyValueConverter extends AbstractRangePropertyValueConverter<Date> {

	private static final Logger LOGGER = LoggerFactory.getLogger(DateRangePropertyValueConverter.class);

	private final List<ElasticsearchDateConverter> dateConverters;

	public DateRangePropertyValueConverter(PersistentProperty<?> property,
			List<ElasticsearchDateConverter> dateConverters) {

		super(property);
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
				LOGGER.trace(e.getMessage(), e);
			}
		}

		throw new ConversionException(String.format("Unable to convert value '%s' to %s for property '%s'", value,
				getGenericType().getTypeName(), getProperty().getName()));
	}

}
