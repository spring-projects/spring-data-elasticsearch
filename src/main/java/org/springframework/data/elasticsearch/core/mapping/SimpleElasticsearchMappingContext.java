/*
 * Copyright 2013-present the original author or authors.
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
package org.springframework.data.elasticsearch.core.mapping;

import org.jspecify.annotations.Nullable;

import org.springframework.data.core.TypeInformation;
import org.springframework.data.elasticsearch.config.ElasticsearchServerType;
import org.springframework.data.mapping.context.AbstractMappingContext;
import org.springframework.data.mapping.model.FieldNamingStrategy;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.PropertyNameFieldNamingStrategy;
import org.springframework.data.mapping.model.SimpleTypeHolder;

/**
 * SimpleElasticsearchMappingContext
 *
 * @author Steven Pearce
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Mark Paluch
 * @author Peter-Josef Meisch
 */
public class SimpleElasticsearchMappingContext
		extends AbstractMappingContext<SimpleElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> {

	private static final FieldNamingStrategy DEFAULT_NAMING_STRATEGY = PropertyNameFieldNamingStrategy.INSTANCE;

	private FieldNamingStrategy fieldNamingStrategy = DEFAULT_NAMING_STRATEGY;
	private boolean writeTypeHints = true;
	private ElasticsearchServerType serverType = ElasticsearchServerType.DEFAULT;

	/**
	 * Configures the {@link FieldNamingStrategy} to be used to determine the field name if no manual mapping is applied.
	 * Defaults to a strategy using the plain property name.
	 *
	 * @param fieldNamingStrategy the {@link FieldNamingStrategy} to be used to determine the field name if no manual
	 *          mapping is applied.
	 * @since 4.2
	 */
	public void setFieldNamingStrategy(@Nullable FieldNamingStrategy fieldNamingStrategy) {
		this.fieldNamingStrategy = fieldNamingStrategy == null ? DEFAULT_NAMING_STRATEGY : fieldNamingStrategy;
	}

	/**
	 * Sets the flag if type hints should be written in Entities created by this instance.
	 *
	 * @since 4.3
	 */
	public void setWriteTypeHints(boolean writeTypeHints) {
		this.writeTypeHints = writeTypeHints;
	}

	/**
	 * Sets the ElasticSearch Server Type
	 *
	 * @param serverType the {@link ElasticsearchServerType} to be used to set the server Type. The Default value will
	 *                   support the standard configurations, and SERVERLESS will support ElasticSearch Serverless
	 *
	 * @since 6.2
	 */
	public void setServerType(@Nullable ElasticsearchServerType serverType) {
		this.serverType = serverType == null ? ElasticsearchServerType.DEFAULT : serverType;
	}

	@Override
	protected boolean shouldCreatePersistentEntityFor(TypeInformation<?> type) {
		return !ElasticsearchSimpleTypes.HOLDER.isSimpleType(type.getType());
	}

	@Override
	protected <T> SimpleElasticsearchPersistentEntity<?> createPersistentEntity(TypeInformation<T> typeInformation) {
		return new SimpleElasticsearchPersistentEntity<>(typeInformation,
				new SimpleElasticsearchPersistentEntity.ContextConfiguration(fieldNamingStrategy, writeTypeHints, serverType));
	}

	@Override
	protected ElasticsearchPersistentProperty createPersistentProperty(Property property,
			SimpleElasticsearchPersistentEntity<?> owner, SimpleTypeHolder simpleTypeHolder) {
		return new SimpleElasticsearchPersistentProperty(property, owner, simpleTypeHolder);
	}
}
