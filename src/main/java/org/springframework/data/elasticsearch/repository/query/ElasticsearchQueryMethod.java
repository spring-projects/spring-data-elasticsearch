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
package org.springframework.data.elasticsearch.repository.query;

import java.lang.reflect.Method;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentProperty;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * ElasticsearchQueryMethod
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Christoph Strobl
 */
public class ElasticsearchQueryMethod extends QueryMethod {

	private final Query queryAnnotation;
	private final MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> mappingContext;
	private @Nullable ElasticsearchEntityMetadata<?> metadata;

	public ElasticsearchQueryMethod(Method method, RepositoryMetadata metadata, ProjectionFactory factory,
			MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> mappingContext) {

		super(method, metadata, factory);

		Assert.notNull(mappingContext, "MappingContext must not be null!");

		this.queryAnnotation = method.getAnnotation(Query.class);
		this.mappingContext = mappingContext;
	}

	public boolean hasAnnotatedQuery() {
		return this.queryAnnotation != null;
	}

	public String getAnnotatedQuery() {
		return (String) AnnotationUtils.getValue(queryAnnotation, "value");
	}

	/**
	 * @return the {@link ElasticsearchEntityMetadata} for the query methods {@link #getReturnedObjectType() return type}.
	 * @since 3.2
	 */
	public ElasticsearchEntityMetadata<?> getEntityInformation() {

		if (metadata == null) {

			Class<?> returnedObjectType = getReturnedObjectType();
			Class<?> domainClass = getDomainClass();

			if (ClassUtils.isPrimitiveOrWrapper(returnedObjectType)) {

				this.metadata = new SimpleElasticsearchEntityMetadata<>((Class<Object>) domainClass,
						mappingContext.getRequiredPersistentEntity(domainClass));

			} else {

				ElasticsearchPersistentEntity<?> returnedEntity = mappingContext.getPersistentEntity(returnedObjectType);
				ElasticsearchPersistentEntity<?> managedEntity = mappingContext.getRequiredPersistentEntity(domainClass);
				returnedEntity = returnedEntity == null || returnedEntity.getType().isInterface() ? managedEntity
						: returnedEntity;
				ElasticsearchPersistentEntity<?> collectionEntity = domainClass.isAssignableFrom(returnedObjectType)
						? returnedEntity
						: managedEntity;

				this.metadata = new SimpleElasticsearchEntityMetadata<>((Class<Object>) returnedEntity.getType(),
						collectionEntity);
			}
		}

		return this.metadata;
	}

	protected MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> getMappingContext() {
		return mappingContext;
	}
}
