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

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.lang.Nullable;

/**
 * Convert a collection into string for value part of the elasticsearch query.
 * <p>
 * If the value is type {@link String}, it should be wrapped with square brackets, with each element quoted therefore
 * escaped(by {@link ElasticsearchStringValueToStringConverter}) if quotations exist in the original element.
 * <p>
 * eg: The value part of an elasticsearch terms query should looks like {@code ["hello \"Stranger\"","Another string"]}
 * for query
 *
 * <pre>
 * {@code
 *  {
 *    "bool":{
 *      "must":{
 *        "terms":{
 *          "name": ["hello \"Stranger\"", "Another string"]
 *        }
 *      }
 *    }
 *  }
 * }
 * </pre>
 *
 * @since 5.3
 * @author Haibo Liu
 */
public class ElasticsearchCollectionValueToStringConverter implements GenericConverter {

	private static final String DELIMITER = ",";

	private final ConversionService conversionService;

	public ElasticsearchCollectionValueToStringConverter(ConversionService conversionService) {
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
			return "[]";
		}
		Collection<?> sourceCollection = (Collection<?>) source;

		if (sourceCollection.isEmpty()) {
			return "[]";
		}

		StringJoiner sb = new StringJoiner(DELIMITER, "[", "]");

		for (Object sourceElement : sourceCollection) {
			// ignore the null value in collection
			if (Objects.isNull(sourceElement)) {
				continue;
			}

			Object targetElement = this.conversionService.convert(
					sourceElement, sourceType.elementTypeDescriptor(sourceElement), targetType);

			if (sourceElement instanceof String) {
				sb.add("\"" + targetElement + '"');
			} else {
				sb.add(String.valueOf(targetElement));
			}
		}
		return sb.toString();
	}
}
