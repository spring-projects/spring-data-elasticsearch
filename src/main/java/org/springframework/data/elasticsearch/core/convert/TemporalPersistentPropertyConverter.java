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

import java.time.temporal.TemporalAccessor;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mapping.PersistentProperty;

/**
 * @author Sascha Woo
 * @since 4.3
 */
public class TemporalPersistentPropertyConverter extends AbstractPersistentPropertyConverter {

	private static final Logger LOGGER = LoggerFactory.getLogger(TemporalPersistentPropertyConverter.class);

	private final List<ElasticsearchDateConverter> dateConverters;

	public TemporalPersistentPropertyConverter(PersistentProperty<?> property,
			List<ElasticsearchDateConverter> dateConverters) {

		super(property);
		this.dateConverters = dateConverters;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object read(String value) {

		Class<?> actualType = getProperty().getActualType();

		for (ElasticsearchDateConverter dateConverter : dateConverters) {
			try {
				return dateConverter.parse(value, (Class<? extends TemporalAccessor>) actualType);
			} catch (Exception e) {
				LOGGER.trace(e.getMessage(), e);
			}
		}

		throw new ConversionException(String.format("Unable to convert value '%s' to %s for property '%s'", value,
				getProperty().getActualType().getTypeName(), getProperty().getName()));
	}

	@Override
	public String write(Object value) {

		try {
			return dateConverters.get(0).format((TemporalAccessor) value);
		} catch (Exception e) {
			throw new ConversionException(
					String.format("Unable to convert value '%s' of property '%s'", value, getProperty().getName()), e);
		}
	}

}
