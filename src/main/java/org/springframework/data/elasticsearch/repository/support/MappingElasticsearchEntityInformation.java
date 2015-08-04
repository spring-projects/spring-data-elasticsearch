/*
 * Copyright 2013-2015 the original author or authors.
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
package org.springframework.data.elasticsearch.repository.support;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentProperty;
import org.springframework.data.repository.core.support.PersistentEntityInformation;
import org.springframework.util.Assert;

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
 */
public class MappingElasticsearchEntityInformation<T, ID extends Serializable> extends PersistentEntityInformation<T, ID>
		implements ElasticsearchEntityInformation<T, ID> {

	private static final Logger logger = LoggerFactory.getLogger(MappingElasticsearchEntityInformation.class);
	private final ElasticsearchPersistentEntity<T> entityMetadata;
	private final String indexName;
	private final String type;

	public MappingElasticsearchEntityInformation(ElasticsearchPersistentEntity<T> entity) {
		this(entity, null, null);
	}

	public MappingElasticsearchEntityInformation(ElasticsearchPersistentEntity<T> entity, String indexName, String type) {
		super(entity);
		this.entityMetadata = entity;
		this.indexName = indexName;
		this.type = type;
	}


	@Override
	public String getIdAttribute() {
		Assert.notNull(entityMetadata.getIdProperty(), "Unable to identify 'id' property in class "
				+ entityMetadata.getType().getSimpleName()
				+ ". Make sure the 'id' property is annotated with @Id or named as 'id' or 'documentId' ");
		return entityMetadata.getIdProperty().getFieldName();
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
			if (versionProperty != null) {
				return (Long) entityMetadata.getPropertyAccessor(entity).getProperty(versionProperty);
			}
		} catch (Exception e) {
			throw new IllegalStateException("failed to load version field", e);
		}
		return null;
	}

	@Override
	public String getParentId(T entity) {
		ElasticsearchPersistentProperty parentProperty = entityMetadata.getParentIdProperty();
		try {
			if (parentProperty != null) {
				return (String) entityMetadata.getPropertyAccessor(entity).getProperty(parentProperty);
			}
		} catch (Exception e) {
			throw new IllegalStateException("failed to load parent ID: " + e, e);
		}
		return null;
	}
}
