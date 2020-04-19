/*
 * Copyright 2019-2020 the original author or authors.
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

import org.elasticsearch.common.time.DateFormatter;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.util.Assert;

/**
 * Provides Converter instances to convert to and from Dates in the different date and time formats that elasticsearch
 * understands.
 *
 * @author Peter-Josef Meisch
 * @since 4.0
 */
final public class ElasticsearchDateConverter {

	private static final ConcurrentHashMap<String, ElasticsearchDateConverter> converters = new ConcurrentHashMap<>();

	private final DateFormatter dateFormatter;

	/**
	 * Creates an ElasticsearchDateConverter for the given {@link DateFormat}.
	 * 
	 * @param dateFormat must not be @{literal null}
	 * @return converter
	 */
	public static ElasticsearchDateConverter of(DateFormat dateFormat) {

		Assert.notNull(dateFormat, "dateFormat must not be null");

		return of(dateFormat.name());
	}

	/**
	 * Creates an ElasticsearchDateConverter for the given pattern.
	 *
	 * @param pattern must not be {@literal null}
	 * @return converter
	 */
	public static ElasticsearchDateConverter of(String pattern) {
		Assert.notNull(pattern, "pattern must not be null");

		return converters.computeIfAbsent(pattern, p -> new ElasticsearchDateConverter(DateFormatter.forPattern(p)));
	}

	private ElasticsearchDateConverter(DateFormatter dateFormatter) {
		this.dateFormatter = dateFormatter;
	}

	/**
	 * Formats the given {@link TemporalAccessor} int a String
	 * 
	 * @param accessor must not be {@literal null}
	 * @return the formatted object
	 */
	public String format(TemporalAccessor accessor) {

		Assert.notNull(accessor, "accessor must not be null");

		return dateFormatter.format(accessor);
	}

	/**
	 * Formats the given {@link TemporalAccessor} int a String
	 *
	 * @param date must not be {@literal null}
	 * @return the formatted object
	 */
	public String format(Date date) {

		Assert.notNull(date, "accessor must not be null");

		return dateFormatter.format(Instant.ofEpochMilli(date.getTime()));
	}

	/**
	 * Parses a String into an object
	 * 
	 * @param input the String to parse, must not be {@literal null}.
	 * @param type the class to return
	 * @param <T> the class of type
	 * @return the new created object
	 */
	public <T extends TemporalAccessor> T parse(String input, Class<T> type) {
		TemporalAccessor accessor = dateFormatter.parse(input);
		try {
			Method method = type.getMethod("from", TemporalAccessor.class);
			Object o = method.invoke(null, accessor);
			return type.cast(o);
		} catch (NoSuchMethodException e) {
			throw new ConversionException("no 'from' factory method found in class " + type.getName());
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new ConversionException("could not create object of class " + type.getName(), e);
		}
	}

	/**
	 * Parses a String into a Date.
	 *
	 * @param input the String to parse, must not be {@literal null}.
	 * @return the new created object
	 */
	public Date parse(String input) {
		return new Date(Instant.from(dateFormatter.parse(input)).toEpochMilli());
	}
}
