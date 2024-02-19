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
package org.springframework.data.elasticsearch.repository.support;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.core.convert.ConversionService;
import org.springframework.data.elasticsearch.core.convert.ConversionException;
import org.springframework.data.elasticsearch.repository.support.value.ElasticsearchQueryValueConversionService;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.util.Assert;
import org.springframework.util.NumberUtils;

/**
 * To replace the placeholders like `?0`, `?1, `?2` of the query string.
 *
 * @author Peter-Josef Meisch
 * @author Niklas Herder
 * @author Haibo Liu
 */
final public class QueryStringPlaceholderReplacer {

	private static final Pattern PARAMETER_PLACEHOLDER = Pattern.compile("\\?(\\d+)");

	private final ConversionService conversionService;

	public QueryStringPlaceholderReplacer(ConversionService conversionService) {

		Assert.notNull(conversionService, "conversionService must not be null");

		this.conversionService = ElasticsearchQueryValueConversionService.getInstance(conversionService);
	}

	/**
	 * Replace the placeholders of the query string.
	 *
	 * @param input    raw query string
	 * @param accessor parameter info
	 * @return a plain string with placeholders replaced
	 */
	public String replacePlaceholders(String input, ParameterAccessor accessor) {

		Matcher matcher = PARAMETER_PLACEHOLDER.matcher(input);
		String result = input;

		while (matcher.find()) {
			String placeholder = Pattern.quote(matcher.group()) + "(?!\\d+)";
			int index = NumberUtils.parseNumber(matcher.group(1), Integer.class);
			String replacement = Matcher.quoteReplacement(getParameterWithIndex(accessor, index, input));
			result = result.replaceAll(placeholder, replacement);
			// need to escape backslashes that are not escapes for quotes so that they are sent as double-backslashes
			// to Elasticsearch
			result = result.replaceAll("\\\\([^\"'])", "\\\\\\\\$1");
		}
		return result;
	}

	private String getParameterWithIndex(ParameterAccessor accessor, int index, String input) {

		Object parameter = accessor.getBindableValue(index);
		String value = conversionService.convert(parameter, String.class);

		if (value == null) {
			throw new ConversionException(String.format(
					"Parameter value can't be null for placeholder at index '%s' in query '%s' when querying elasticsearch",
					index, input));
		}
		return value;
	}

}
