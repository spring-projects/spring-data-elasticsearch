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
package org.springframework.data.elasticsearch.repository.support;

import org.elasticsearch.index.VersionType;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentProperty;
import org.springframework.data.repository.core.support.PersistentEntityInformation;

/**
 * Elasticsearch specific implementation of
 * {@link org.springframework.data.repository.core.support.AbstractEntityInformation}
 *
 * @param <T>
 * @param <ID>
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Ryan Henszey
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Christoph Strobl
 * @author Ivan Greene
 * @author Sylvain Laurent
 */
public class MappingElasticsearchEntityInformation<T, ID> extends PersistentEntityInformation<T, ID>
		implements ElasticsearchEntityInformation<T, ID> {

	private final ElasticsearchPersistentEntity<T> entityMetadata;
	private final String indexName;
	private final String type;
	private final VersionType versionType;

	public MappingElasticsearchEntityInformation(ElasticsearchPersistentEntity<T> entity) {
		this(entity, entity.getIndexName(), entity.getIndexType(), entity.getVersionType());
	}

	public MappingElasticsearchEntityInformation(ElasticsearchPersistentEntity<T> entity, String indexName, String type,
			VersionType versionType) {
		super(entity);

		this.entityMetadata = entity;
		this.indexName = indexName;
		this.type = type;
		this.versionType = versionType;
	}

	@Override
	public String getIdAttribute() {
		return entityMetadata.getRequiredIdProperty().getFieldName();
	}

	@Override
	public String getIndexName() {
		return indexName != null ? indexName : entityMetadata.getIndexName();
	}

	@Override
	public String getType() {
		return type != null ? type : entityMetadata.getIndexType();
	}

	@Override
	public Long getVersion(T entity) {

		ElasticsearchPersistentProperty versionProperty = entityMetadata.getVersionProperty();
		try {
			return versionProperty != null ? (Long) entityMetadata.getPropertyAccessor(entity).getProperty(versionProperty)
					: null;
		} catch (Exception e) {
			throw new IllegalStateException("failed to load version field", e);
		}
	}

	@Override
	public VersionType getVersionType() {
		return versionType;
	}

	@Override
	public String getParentId(T entity) {

		ElasticsearchPersistentProperty parentProperty = entityMetadata.getParentIdProperty();
		try {
			return parentProperty != null ? (String) entityMetadata.getPropertyAccessor(entity).getProperty(parentProperty)
					: null;
		} catch (Exception e) {
			throw new IllegalStateException("failed to load parent ID: " + e, e);
		}
	}
}
