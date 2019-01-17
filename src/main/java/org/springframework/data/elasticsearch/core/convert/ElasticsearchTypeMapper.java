/*
 * Copyright 2019 the original author or authors.
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
package org.springframework.data.elasticsearch.core.convert;

import java.util.Collections;
import java.util.Map;

import org.springframework.data.convert.SimpleTypeInformationMapper;
import org.springframework.data.convert.TypeAliasAccessor;
import org.springframework.data.convert.TypeMapper;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentProperty;
import org.springframework.data.mapping.Alias;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.lang.Nullable;

/**
 * Elasticsearch specific {@link TypeMapper} definition.
 *
 * @author Christoph Strobl
 * @since 3.2
 */
public interface ElasticsearchTypeMapper extends TypeMapper<Map<String, Object>> {

	String DEFAULT_TYPE_KEY = "_class";

	/**
	 * Returns whether the given key is the type key.
	 *
	 * @return {@literal true} if given {@literal key} is used as type hint key.
	 */
	boolean isTypeKey(String key);

	default boolean containsTypeInformation(Map<String, Object> source) {
		return readType(source) != null;
	}

	static ElasticsearchTypeMapper defaultTypeMapper(
			MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> mappingContext) {
		return new ElasticsearchDefaultTypeMapper(DEFAULT_TYPE_KEY, new MapTypeAliasAccessor(DEFAULT_TYPE_KEY),
				mappingContext, Collections.singletonList(new SimpleTypeInformationMapper()));
	}

	/**
	 * {@link TypeAliasAccessor} to store aliases in a {@link Map}.
	 *
	 * @author Christoph Strobl
	 */
	final class MapTypeAliasAccessor implements TypeAliasAccessor<Map<String, Object>> {

		private final @Nullable String typeKey;

		public MapTypeAliasAccessor(@Nullable String typeKey) {
			this.typeKey = typeKey;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.convert.TypeAliasAccessor#readAliasFrom(java.lang.Object)
		 */
		public Alias readAliasFrom(Map<String, Object> source) {
			return Alias.ofNullable(source.get(typeKey));
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.convert.TypeAliasAccessor#writeTypeTo(java.lang.Object, java.lang.Object)
		 */
		public void writeTypeTo(Map<String, Object> sink, Object alias) {

			if (typeKey == null) {
				return;
			}

			sink.put(typeKey, alias);
		}
	}

}
