/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import org.springframework.data.mapping.model.BeanWrapper;
import org.springframework.data.repository.core.support.AbstractEntityInformation;
import org.springframework.util.Assert;

/**
 * Elasticsearch specific implementation of {@link org.springframework.data.repository.core.support.AbstractEntityInformation}
 *
 * @param <T>
 * @param <ID>
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Ryan Henszey
 */
public class MappingElasticsearchEntityInformation<T, ID extends Serializable> extends AbstractEntityInformation<T, ID>
        implements ElasticsearchEntityInformation<T, ID> {

    private static final Logger logger = LoggerFactory.getLogger(MappingElasticsearchEntityInformation.class);
    private final ElasticsearchPersistentEntity<T> entityMetadata;
    private final String indexName;
    private final String type;
    private Class<?> idClass;

    public MappingElasticsearchEntityInformation(ElasticsearchPersistentEntity<T> entity) {
        this(entity, null, null);
    }

    public MappingElasticsearchEntityInformation(ElasticsearchPersistentEntity<T> entity, String indexName, String type) {
        super(entity.getType());
        this.entityMetadata = entity;
        this.indexName = indexName;
        this.type = type;
        this.idClass = entity.getIdProperty().getType();
    }

    @SuppressWarnings("unchecked")
    @Override
    public ID getId(T entity) {
        ElasticsearchPersistentProperty id = entityMetadata.getIdProperty();
        try {
            return (ID) BeanWrapper.create(entity, null).getProperty(id);
        } catch (Exception e) {
            throw new IllegalStateException("ID could not be resolved", e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<ID> getIdType() {
        return (Class<ID>)idClass;
    }

    @Override
    public String getIdAttribute() {
        Assert.notNull(entityMetadata.getIdProperty(),"Unable to identify 'id' property in class " + entityMetadata.getType().getSimpleName() +". Make sure the 'id' property is annotated with @Id or named as 'id' or 'documentId' ");
        return entityMetadata.getIdProperty().getFieldName();
    }

    @Override
    public String getIndexName() {
        return indexName != null?  indexName : entityMetadata.getIndexName();
    }

    @Override
    public String getType() {
        return type != null? type : entityMetadata.getIndexType();
    }

    @Override
    public Long getVersion(T entity) {
        ElasticsearchPersistentProperty versionProperty = entityMetadata.getVersionProperty();
        try {
            if(versionProperty != null){
                return (Long) BeanWrapper.create(entity, null).getProperty(versionProperty);
            }
        } catch (Exception e) {
            throw new IllegalStateException("failed to load version field", e);
        }
        return null;
    }
}


