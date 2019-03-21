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
package org.springframework.data.elasticsearch.repository.query;

import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.util.Assert;

/**
 * @author Christoph Strobl
 * @since 3.2
 */
public class SimpleElasticsearchEntityMetadata<T> implements ElasticsearchEntityMetadata<T> {

	private final Class<T> type;
	private final ElasticsearchPersistentEntity<?> entity;

	public SimpleElasticsearchEntityMetadata(Class<T> type, ElasticsearchPersistentEntity<?> entity) {

		Assert.notNull(type, "Type must not be null!");
		Assert.notNull(entity, "Entity must not be null!");

		this.type = type;
		this.entity = entity;
	}

	@Override
	public String getIndexName() {
		return entity.getIndexName();
	}

	@Override
	public String getIndexTypeName() {
		return entity.getIndexType();
	}

	@Override
	public Class<T> getJavaType() {
		return type;
	}
}
