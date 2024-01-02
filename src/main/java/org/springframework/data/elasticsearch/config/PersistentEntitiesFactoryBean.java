/*
 * Copyright 2020-2024 the original author or authors.
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
package org.springframework.data.elasticsearch.config;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.mapping.context.PersistentEntities;

/**
 * Simple helper to be able to wire the {@link PersistentEntities} from a {@link MappingElasticsearchConverter} bean
 * available in the application context.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Christoph Strobl
 * @author Peter-Josef Meisch
 * @since 4.1
 */
public class PersistentEntitiesFactoryBean implements FactoryBean<PersistentEntities> {

	private final MappingElasticsearchConverter converter;

	/**
	 * Creates a new {@link PersistentEntitiesFactoryBean} for the given {@link MappingElasticsearchConverter}.
	 *
	 * @param converter must not be {@literal null}.
	 */
	public PersistentEntitiesFactoryBean(MappingElasticsearchConverter converter) {
		this.converter = converter;
	}

	@Override
	public PersistentEntities getObject() {
		return PersistentEntities.of(converter.getMappingContext());
	}

	@Override
	public Class<?> getObjectType() {
		return PersistentEntities.class;
	}
}
