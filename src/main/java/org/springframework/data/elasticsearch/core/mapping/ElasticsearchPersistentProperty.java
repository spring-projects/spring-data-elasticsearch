/*
 * Copyright 2013-2023 the original author or authors.
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
package org.springframework.data.elasticsearch.core.mapping;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.elasticsearch.core.query.SeqNoPrimaryTerm;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.lang.Nullable;

/**
 * ElasticsearchPersistentProperty
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Sascha Woo
 * @author Oliver Gierke
 * @author Peter-Josef Meisch
 * @author Roman Puchkovskiy
 */
public interface ElasticsearchPersistentProperty extends PersistentProperty<ElasticsearchPersistentProperty> {

	/**
	 * Returns the name to be used to store the property in the document.
	 *
	 * @return
	 */
	String getFieldName();

	/**
	 * Returns whether the current property is a {@link SeqNoPrimaryTerm} property.
	 *
	 * @return true if the type is {@link SeqNoPrimaryTerm}
	 * @since 4.0
	 */
	boolean isSeqNoPrimaryTermProperty();

	/**
	 * @return true if an {@link PropertyValueConverter} is available for this instance.
	 * @since 4.0
	 */
	boolean hasPropertyValueConverter();

	/**
	 * @return the {@link PropertyValueConverter} for this instance.
	 * @since 4.0
	 */
	@Nullable
	PropertyValueConverter getPropertyValueConverter();

	/**
	 * Returns true if the property may be read from the store into the entity.
	 *
	 * @return true if readable, false otherwise
	 * @since 4.0
	 */
	boolean isReadable();

	/**
	 * @return {@literal true} if null values should be stored in Elasticsearch
	 * @since 4.1
	 */
	boolean storeNullValue();

	/**
	 * @return {@literal true} if this is a GeoPoint property
	 * @since 4.1
	 */
	boolean isGeoPointProperty();

	/**
	 * @return {@literal true} if this is a GeoShape property
	 * @since 4.1
	 */
	boolean isGeoShapeProperty();

	/**
	 * @return {@literal true} if this is a JoinField property
	 * @since 4.1
	 */
	boolean isJoinFieldProperty();

	/**
	 * @return {@literal true} if this is a Completion property
	 * @since 4.1
	 */
	boolean isCompletionProperty();

	/**
	 * calls {@link #getActualType()} but returns null when an exception is thrown
	 *
	 * @since 4.1
	 */
	@Nullable
	default Class<?> getActualTypeOrNull() {
		try {
			return getActualType();
		} catch (Exception e) {
			return null;
		}
	}

	enum PropertyToFieldNameConverter implements Converter<ElasticsearchPersistentProperty, String> {

		INSTANCE;

		@Override
		public String convert(ElasticsearchPersistentProperty source) {
			return source.getFieldName();
		}
	}

	/**
	 * when building CriteriaQueries use the name; the fieldname is set later with
	 * {@link org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter}.
	 */
	enum QueryPropertyToFieldNameConverter implements Converter<ElasticsearchPersistentProperty, String> {

		INSTANCE;

		@Override
		public String convert(ElasticsearchPersistentProperty source) {
			return source.getName();
		}
	}
}
