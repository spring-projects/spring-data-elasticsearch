/*
 * Copyright 2021-2023 the original author or authors.
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

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.core.convert.ConversionService;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.util.NumberUtils;

/**
 * @author Peter-Josef Meisch
 * @author Niklas Herder
 */
final public class StringQueryUtil {

	private static final Pattern PARAMETER_PLACEHOLDER = Pattern.compile("\\?(\\d+)");

	private final ConversionService conversionService;

	public StringQueryUtil(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	public String replacePlaceholders(String input, ParameterAccessor accessor) {

		Matcher matcher = PARAMETER_PLACEHOLDER.matcher(input);
		String result = input;
		while (matcher.find()) {

			String placeholder = Pattern.quote(matcher.group()) + "(?!\\d+)";
			int index = NumberUtils.parseNumber(matcher.group(1), Integer.class);
			String replacement = Matcher.quoteReplacement(getParameterWithIndex(accessor, index));
			result = result.replaceAll(placeholder, replacement);
			// need to escape backslashes that are not escapes for quotes so that they are sent as double-backslashes
			// to Elasticsearch
			result = result.replaceAll("\\\\([^\"'])", "\\\\\\\\$1");
		}
		return result;
	}

	private String getParameterWithIndex(ParameterAccessor accessor, int index) {

		Object parameter = accessor.getBindableValue(index);
		String parameterValue = "null";

		if (parameter != null) {
			parameterValue = convert(parameter);
		}

		return parameterValue;

	}

	private String convert(Object parameter) {
		if (Collection.class.isAssignableFrom(parameter.getClass())) {
			Collection<?> collectionParam = (Collection<?>) parameter;
			StringBuilder sb = new StringBuilder("[");
			sb.append(collectionParam.stream().map(o -> {
				if (o instanceof String) {
					return "\"" + convert(o) + "\"";
				} else {
					return convert(o);
				}
			}).collect(Collectors.joining(",")));
			sb.append("]");
			return sb.toString();
		} else {
			String parameterValue = "null";
			if (conversionService.canConvert(parameter.getClass(), String.class)) {
				String converted = conversionService.convert(parameter, String.class);

				if (converted != null) {
					parameterValue = converted;
				}
			} else {
				parameterValue = parameter.toString();
			}

			parameterValue = parameterValue.replaceAll("\"", Matcher.quoteReplacement("\\\""));
			return parameterValue;
		}
	}

}
