/*
 * Copyright 2014 the original author or authors.
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

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;

import com.google.common.base.Function;
import com.querydsl.elasticsearch.ElasticsearchQuery;
import com.querydsl.core.types.Predicate;

import org.elasticsearch.search.SearchHit;
import org.springframework.data.elasticsearch.ElasticsearchException;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.EntityMapper;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentProperty;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.context.MappingContext;

/**
 * Spring Data specific {@link ElasticsearchQuery} implementations.
 *
 * @author Kevin Leturc
 */
public class SpringDataElasticsearchQuery<T, ID extends Serializable> extends ElasticsearchQuery<T> {

    private final String index;
    private final String type;

    /**
     * Creates a new {@link SpringDataElasticsearchQuery}.
     *
     * @param operations must not be {@literal null}.
     * @param information must not be {@literal null}.
     */
    public SpringDataElasticsearchQuery(final ElasticsearchOperations operations,
                                        final ElasticsearchEntityInformation<T, ID> information) {
        this(operations, information.getJavaType(), information.getIndexName(), information.getType());
    }

    /**
     * Creates a new {@link SpringDataElasticsearchQuery}.
     *
     * @param operations must not be {@literal null}.
     * @param entityType must not be {@literal null}.
     * @param index must not be {@literal null}.
     * @param type must not be {@literal null}.
     */
    public SpringDataElasticsearchQuery(final ElasticsearchOperations operations,
                                        final Class<T> entityType,
                                        final String index,
                                        final String type) {
        super(operations.getClient(),
                new SpringDataElasticsearchTransformer<T>(operations, entityType),
                new SpringDataElasticsearchSerializer(operations.getElasticsearchConverter()));
        this.index = index;
        this.type = type;
    }

    @Override
    public String getIndex() {
        return index;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public SpringDataElasticsearchQuery<T, ID> where(Predicate... o) {
        // Override this method to allow overriding list() to avoid to count twice the results on Elasticsearch
        super.where(o);
        return this;
    }

    private static class SpringDataElasticsearchTransformer<T> implements Function<SearchHit, T> {

        private final EntityMapper entityMapper;
        private final MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> mappingContext;
        private final Class<T> entityType;

        public SpringDataElasticsearchTransformer(ElasticsearchOperations operations, Class<T> entityType) {
            this.entityMapper = operations.getResultsMapper().getEntityMapper();
            this.mappingContext = operations.getElasticsearchConverter().getMappingContext();
            this.entityType = entityType;
        }
        public T apply(SearchHit input) {
            try {
                T entity = entityMapper.mapToObject(input.getSourceAsString(), entityType);
                setPersistentEntityId(entity, input.getId(), entityType);
                return entity;
            } catch (IOException e) {
                throw new ElasticsearchException("failed to map source [ " + input + "] to class " + entityType.getSimpleName(), e);
            }
        }

        private void setPersistentEntityId(T result, String id, Class<T> clazz) {
            if (mappingContext != null && clazz.isAnnotationPresent(Document.class)) {
                PersistentProperty<ElasticsearchPersistentProperty> idProperty = mappingContext.getPersistentEntity(clazz).getIdProperty();
                // Only deal with String because ES generated Ids are strings !
                if (idProperty != null && idProperty.getType().isAssignableFrom(String.class)) {
                    Method setter = idProperty.getSetter();
                    if (setter != null) {
                        try {
                            setter.invoke(result, id);
                        } catch (Throwable t) {
                            t.printStackTrace();
                        }
                    }
                }
            }
        }
    }
}
