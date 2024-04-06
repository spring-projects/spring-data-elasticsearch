/*
 * Copyright 2013-2024 the original author or authors.
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

import org.springframework.data.convert.EntityConverter;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentProperty;
import org.springframework.data.elasticsearch.core.mapping.PropertyValueConverter;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Christoph Strobl
 * @author Peter-Josef Meisch
 * @author Sasch Woo
 * @author Roman Puchkovskiy
 */
public interface ElasticsearchConverter
		extends EntityConverter<ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty, Object, Document> {

	/**
	 * Get the configured {@link ProjectionFactory}. <br />
	 * <strong>NOTE</strong> Should be overwritten in implementation to make use of the type cache.
	 *
	 * @since 3.2
	 */
	default ProjectionFactory getProjectionFactory() {
		return new SpelAwareProxyProjectionFactory();
	}

	// region write
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

		String converted = getConversionService().convert(idValue, String.class);

		if (converted == null) {
			return idValue.toString();
		}

		return converted;
	}

	/**
	 * Map an object to a {@link Document}.
	 *
	 * @param source the object to map
	 * @return will not be {@literal null}.
	 */
	default Document mapObject(@Nullable Object source) {

		Document target = Document.create();

		if (source != null) {
			write(source, target);
		}
		return target;
	}
	// endregion

	// region query
	/**
	 * Updates a {@link Query} by renaming the property names in the query to the correct mapped field names and the
	 * values to the converted values if the {@link ElasticsearchPersistentProperty} for a property has a
	 * {@link PropertyValueConverter}. If domainClass is null it's a noop.
	 *
	 * @param query the query that is internally updated, must not be {@literal null}
	 * @param domainClass the class of the object that is searched with the query
	 */
	void updateQuery(Query query, @Nullable Class<?> domainClass);

	/**
	 * Replaces the parts in a dot separated property path with the field names of the respective properties. If no
	 * matching property is found, the original parts are rteturned.
	 *
	 * @param propertyPath the property path
	 * @param persistentEntity the replaced values.
	 * @return a String wihere the property names are replaced with field names
	 * @since 5.2
	 */
	String updateFieldNames(String propertyPath, ElasticsearchPersistentEntity<?> persistentEntity);
	// endregion
}
