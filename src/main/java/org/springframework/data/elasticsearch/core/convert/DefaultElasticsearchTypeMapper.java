/*
 * Copyright 2019 the original author or authors.
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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.data.convert.DefaultTypeMapper;
import org.springframework.data.convert.SimpleTypeInformationMapper;
import org.springframework.data.convert.TypeAliasAccessor;
import org.springframework.data.convert.TypeInformationMapper;
import org.springframework.data.mapping.Alias;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.lang.Nullable;

/**
 * Elasticsearch specific {@link org.springframework.data.convert.TypeMapper} implementation.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 3.2
 */
public class DefaultElasticsearchTypeMapper extends DefaultTypeMapper<Map<String, Object>>
		implements ElasticsearchTypeMapper {

	private final @Nullable String typeKey;

	public DefaultElasticsearchTypeMapper(@Nullable String typeKey) {
		this(typeKey, Collections.singletonList(new SimpleTypeInformationMapper()));
	}

	public DefaultElasticsearchTypeMapper(@Nullable String typeKey,
			MappingContext<? extends PersistentEntity<?, ?>, ?> mappingContext) {
		this(typeKey, new MapTypeAliasAccessor(typeKey), mappingContext,
				Collections.singletonList(new SimpleTypeInformationMapper()));
	}

	public DefaultElasticsearchTypeMapper(@Nullable String typeKey, List<? extends TypeInformationMapper> mappers) {
		this(typeKey, new MapTypeAliasAccessor(typeKey), null, mappers);
	}

	public DefaultElasticsearchTypeMapper(@Nullable String typeKey, TypeAliasAccessor<Map<String, Object>> accessor,
			@Nullable MappingContext<? extends PersistentEntity<?, ?>, ?> mappingContext,
			List<? extends TypeInformationMapper> mappers) {

		super(accessor, mappingContext, mappers);
		this.typeKey = typeKey;
	}

	@Override
	public boolean isTypeKey(String key) {
		return typeKey != null && typeKey.equals(key);
	}

	/**
	 * {@link TypeAliasAccessor} to store aliases in a {@link Map}.
	 *
	 * @author Christoph Strobl
	 */
	public static class MapTypeAliasAccessor implements TypeAliasAccessor<Map<String, Object>> {

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
