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

import java.util.List;
import java.util.Map;

import org.springframework.data.convert.DefaultTypeMapper;
import org.springframework.data.convert.TypeAliasAccessor;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.lang.Nullable;

/**
 * Elasticsearch specific {@link org.springframework.data.convert.TypeMapper} implementation.
 *
 * @author Christoph Strobl
 * @since 3.2
 */
class ElasticsearchDefaultTypeMapper extends DefaultTypeMapper<Map<String, Object>> implements ElasticsearchTypeMapper {

	private final @Nullable String typeKey;

	ElasticsearchDefaultTypeMapper(@Nullable String typeKey, TypeAliasAccessor accessor,
			@Nullable MappingContext mappingContext, List additionalMappers) {

		super(accessor, mappingContext, additionalMappers);
		this.typeKey = typeKey;
	}

	@Override
	public boolean isTypeKey(String key) {
		return typeKey != null && typeKey.equals(key);
	}
}
