/*
 * Copyright 2013-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.elasticsearch.core.mapping;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mapping.PersistentProperty;

/**
 * ElasticsearchPersistentProperty
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Sascha Woo
 * @author Oliver Gierke
 */
public interface ElasticsearchPersistentProperty extends PersistentProperty<ElasticsearchPersistentProperty> {

	String getFieldName();

	/**
	 * Returns whether the current property is a <em>potential</em> score property of the owning
	 * {@link ElasticsearchPersistentEntity}. This method is mainly used by {@link ElasticsearchPersistentEntity}
	 * implementation to discover score property candidates on {@link ElasticsearchPersistentEntity} creation you should
	 * rather call {@link ElasticsearchPersistentEntity#isScoreProperty(PersistentProperty)} to determine whether the
	 * current property is the version property of that {@link ElasticsearchPersistentEntity} under consideration.
	 *
	 * @return
	 * @since 3.1
	 */
	boolean isScoreProperty();

	public enum PropertyToFieldNameConverter implements Converter<ElasticsearchPersistentProperty, String> {

		INSTANCE;

		public String convert(ElasticsearchPersistentProperty source) {
			return source.getFieldName();
		}
	}
}
