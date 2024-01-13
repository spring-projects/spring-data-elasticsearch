/*
 * Copyright 2024 the original author or authors.
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

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.lang.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Matcher;

/**
 * Convert a collection into string for value part of the elasticsearch query.
 * <p>
 * The string should be wrapped with square brackets, with each element quoted therefore escaped if quotes exist in the
 * original element.
 * <p>
 * eg: The value part of an elasticsearch terms query should looks like {@code ["hello \"Stranger\"","Another string"]},
 * and the whole query string may be
 * {@code { 'bool' : { 'must' : { 'terms' : { 'name' : ["hello \"Stranger\"","Another string"] } } } }}
 *
 * @author Haibo Liu
 */
public class ElasticsearchCollectionToStringConverter implements GenericConverter {

	private static final String DELIMITER = ",";

	private final ConversionService conversionService;

	public ElasticsearchCollectionToStringConverter(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(Collection.class, String.class));
	}

	@Override
	@Nullable
	public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (source == null) {
			return null;
		}
		Collection<?> sourceCollection = (Collection<?>) source;
		if (sourceCollection.isEmpty()) {
			return "[]";
		}
		StringJoiner sb = new StringJoiner(DELIMITER, "[", "]");
		for (Object sourceElement : sourceCollection) {
			Object targetElement = this.conversionService.convert(
					sourceElement, sourceType.elementTypeDescriptor(sourceElement), targetType);
			sb.add("\"" + escape(targetElement) + "\"");
		}
		return sb.toString();
	}

	private String escape(@Nullable Object target) {
		// escape the quotes in the string, because the string should already be quoted manually
		return String.valueOf(target).replaceAll("\"", Matcher.quoteReplacement("\\\""));
	}
}
