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

import com.querydsl.elasticsearch.ElasticsearchSerializer;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.PathType;

import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentProperty;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.util.Assert;

/**
 * Custom {@link ElasticsearchSerializer} to take mapping information into account when building keys for constraints.
 *
 * @author Kevin Leturc
 */
public class SpringDataElasticsearchSerializer extends ElasticsearchSerializer {

    private final MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> mappingContext;

    /**
     * Creates a new {@link SpringDataElasticsearchSerializer} for the given {@link ElasticsearchConverter}.
     *
     * @param converter must not be {@literal null}.
     */
    public SpringDataElasticsearchSerializer(ElasticsearchConverter converter) {

        Assert.notNull(converter, "ElasticsearchConverter must not be null!");

        this.mappingContext = converter.getMappingContext();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getKeyForPath(Path<?> expr, PathMetadata metadata) {

        if (!metadata.getPathType().equals(PathType.PROPERTY)) {
            return super.getKeyForPath(expr, metadata);
        }

        Path<?> parent = metadata.getParent();
        ElasticsearchPersistentEntity<?> entity = mappingContext.getPersistentEntity(parent.getType());
        ElasticsearchPersistentProperty property = entity.getPersistentProperty(metadata.getName());

        return property == null ? super.getKeyForPath(expr, metadata) : property.getFieldName();
    }

}
