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
package org.springframework.data.elasticsearch.repository.support;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.elasticsearch.core.convert.DateTimeConverters;
import org.springframework.data.elasticsearch.repository.query.ElasticsearchStringQuery;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.util.ClassUtils;
import org.springframework.util.NumberUtils;

/**
 * @author Peter-Josef Meisch
 */
final public class StringQueryUtil {

	private static final Pattern PARAMETER_PLACEHOLDER = Pattern.compile("\\?(\\d+)");
	private static final GenericConversionService conversionService = new GenericConversionService();

	{
		if (!conversionService.canConvert(java.util.Date.class, String.class)) {
			conversionService.addConverter(DateTimeConverters.JavaDateConverter.INSTANCE);
		}
		if (ClassUtils.isPresent("org.joda.time.DateTimeZone", ElasticsearchStringQuery.class.getClassLoader())) {
			if (!conversionService.canConvert(org.joda.time.ReadableInstant.class, String.class)) {
				conversionService.addConverter(DateTimeConverters.JodaDateTimeConverter.INSTANCE);
			}
			if (!conversionService.canConvert(org.joda.time.LocalDateTime.class, String.class)) {
				conversionService.addConverter(DateTimeConverters.JodaLocalDateTimeConverter.INSTANCE);
			}
		}
	}

	private StringQueryUtil() {}

	public static String replacePlaceholders(String input, ParameterAccessor accessor) {

		Matcher matcher = PARAMETER_PLACEHOLDER.matcher(input);
		String result = input;
		while (matcher.find()) {

			String placeholder = Pattern.quote(matcher.group()) + "(?!\\d+)";
			int index = NumberUtils.parseNumber(matcher.group(1), Integer.class);
			result = result.replaceAll(placeholder, Matcher.quoteReplacement(getParameterWithIndex(accessor, index)));
		}
		return result;
	}

	private static String getParameterWithIndex(ParameterAccessor accessor, int index) {

		Object parameter = accessor.getBindableValue(index);
		String parameterValue = "null";

		// noinspection ConstantConditions
		if (parameter != null) {

			if (conversionService.canConvert(parameter.getClass(), String.class)) {
				String converted = conversionService.convert(parameter, String.class);

				if (converted != null) {
					parameterValue = converted;
				}
			} else {
				parameterValue = parameter.toString();
			}
		}

		parameterValue = parameterValue.replaceAll("\"", Matcher.quoteReplacement("\\\""));
		return parameterValue;

	}

}
