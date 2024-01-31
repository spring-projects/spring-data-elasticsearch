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
package org.springframework.data.elasticsearch.repository.support.value;

import java.util.Collections;
import java.util.Set;
import java.util.regex.Matcher;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.lang.Nullable;

/**
 * Values in elasticsearch query may contain quotations and should be escaped when converting. Note that the converter
 * should only be used in this situation, rather than common string to string conversions.
 *
 * @since 5.3
 * @author Haibo Liu
 */
public class ElasticsearchStringValueToStringConverter implements GenericConverter {

	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(String.class, String.class));
	}

	@Override
	@Nullable
	public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		return source != null ? escape(source) : null;
	}

	private String escape(@Nullable Object source) {
		// escape the quotes in the string, because the string should already be quoted manually
		return String.valueOf(source).replaceAll("\"", Matcher.quoteReplacement("\\\""));
	}
}
