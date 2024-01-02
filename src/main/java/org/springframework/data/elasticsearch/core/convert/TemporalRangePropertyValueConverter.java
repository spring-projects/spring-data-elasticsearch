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

import java.time.temporal.TemporalAccessor;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.util.Assert;

/**
 * @author Sascha Woo
 * @since 4.3
 */
public class TemporalRangePropertyValueConverter extends AbstractRangePropertyValueConverter<TemporalAccessor> {

	private static final Log LOGGER = LogFactory.getLog(TemporalRangePropertyValueConverter.class);

	private final List<ElasticsearchDateConverter> dateConverters;

	public TemporalRangePropertyValueConverter(PersistentProperty<?> property,
			Class<?> genericType, List<ElasticsearchDateConverter> dateConverters) {

		super(property, genericType);

		Assert.notEmpty(dateConverters, "dateConverters must not be empty.");
		this.dateConverters = dateConverters;
	}

	@Override
	protected String format(TemporalAccessor temporal) {
		return dateConverters.get(0).format(temporal);
	}

	@Override
	protected TemporalAccessor parse(String value) {

		Class<?> type = getGenericType();
		for (ElasticsearchDateConverter converters : dateConverters) {
			try {
				return converters.parse(value, (Class<? extends TemporalAccessor>) type);
			} catch (Exception e) {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace(e.getMessage(), e);
				}
			}
		}

		throw new ConversionException(String.format("Unable to convert value '%s' to %s for property '%s'", value,
				type.getTypeName(), getProperty().getName()));
	}

}
