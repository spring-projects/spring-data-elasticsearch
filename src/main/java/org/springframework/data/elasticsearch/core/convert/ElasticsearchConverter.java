/*
 * Copyright 2013-2019 the original author or authors.
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

import org.springframework.core.convert.ConversionService;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentProperty;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.util.Assert;

/**
 * ElasticsearchConverter
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Christoph Strobl
 */
public interface ElasticsearchConverter {

	/**
	 * Returns the underlying {@link org.springframework.data.mapping.context.MappingContext} used by the converter.
	 *
	 * @return never {@literal null}
	 */
	MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> getMappingContext();

	/**
	 * Returns the underlying {@link org.springframework.core.convert.ConversionService} used by the converter.
	 *
	 * @return never {@literal null}.
	 */
	ConversionService getConversionService();

	/**
	 * Convert a given {@literal idValue} to its {@link String} representation taking potentially registered
	 * {@link org.springframework.core.convert.converter.Converter Converters} into account.
	 *
	 * @param idValue must not be {@literal null}.
	 * @return never {@literal null}.
	 * @since 3.2
	 */
	default String convertId(Object idValue) {

		Assert.notNull(idValue, "idValue must not be null!");
		if (!getConversionService().canConvert(idValue.getClass(), String.class)) {
			return idValue.toString();
		}

		return getConversionService().convert(idValue, String.class);
	}
}
