/*
 * Copyright2020-2021 the original author or authors.
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
package org.springframework.data.elasticsearch.core.routing;

import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentProperty;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.lang.Nullable;

/**
 * Default implementation of the {@link RoutingResolver} interface. Returns {@literal null} for the non-bean method and
 * delegates to the corresponding persistent entity for the bean-method.
 *
 * @author Peter-Josef Meisch
 * @since 4.2
 */
public class DefaultRoutingResolver implements RoutingResolver {

	private final MappingContext<? extends ElasticsearchPersistentEntity<?>, ? extends ElasticsearchPersistentProperty> mappingContext;

	public DefaultRoutingResolver(
			MappingContext<? extends ElasticsearchPersistentEntity<?>, ? extends ElasticsearchPersistentProperty> mappingContext) {
		this.mappingContext = mappingContext;
	}

	@Override
	public String getRouting() {
		return null;
	}

	@Override
	@Nullable
	public <T> String getRouting(T bean) {

		ElasticsearchPersistentEntity<T> persistentEntity = (ElasticsearchPersistentEntity<T>) mappingContext
				.getPersistentEntity(bean.getClass());

		if (persistentEntity != null) {
			return persistentEntity.resolveRouting(bean);
		}

		return null;
	}
}
