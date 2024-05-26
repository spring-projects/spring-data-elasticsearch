/*
 * Copyright 2013-2024 the original author or authors.
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

import java.util.Set;

import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Dynamic;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.core.index.Settings;
import org.springframework.data.elasticsearch.core.join.JoinField;
import org.springframework.data.elasticsearch.core.query.SeqNoPrimaryTerm;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.model.FieldNamingStrategy;
import org.springframework.lang.Nullable;

/**
 * ElasticsearchPersistentEntity
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Mark Paluch
 * @author Sascha Woo
 * @author Oliver Gierke
 * @author Ivan Greene
 * @author Peter-Josef Meisch
 * @author Roman Puchkovskiy
 * @author Subhobrata Dey
 */
public interface ElasticsearchPersistentEntity<T> extends PersistentEntity<T, ElasticsearchPersistentProperty> {

	IndexCoordinates getIndexCoordinates();

	/**
	 * Retrieves the aliases associated with the current entity.
	 *
	 * @return Returns a set of aliases of the {@link PersistentEntity}.
	 * @since 5.4
	 */
	Set<Alias> getAliases();

	short getShards();

	short getReplicas();

	boolean isUseServerConfiguration();

	@Nullable
	String getRefreshInterval();

	@Nullable
	String getIndexStoreType();

	@Override
	ElasticsearchPersistentProperty getVersionProperty();

	@Nullable
	String settingPath();

	@Nullable
	Document.VersionType getVersionType();

	boolean isCreateIndexAndMapping();

	/**
	 * returns the {@link ElasticsearchPersistentProperty} with the given fieldName (can be set by the {@link Field})
	 * annotation.
	 *
	 * @param fieldName to field name for the search, must not be {@literal null}
	 * @return the found property, otherwise null
	 * @since 4.0
	 */
	@Nullable
	ElasticsearchPersistentProperty getPersistentPropertyWithFieldName(String fieldName);

	/**
	 * Returns whether the {@link ElasticsearchPersistentEntity} has a {@link SeqNoPrimaryTerm} property. If this call
	 * returns {@literal true}, {@link #getSeqNoPrimaryTermProperty()} will return a non-{@literal null} value.
	 *
	 * @return false when {@link ElasticsearchPersistentEntity} does not define a SeqNoPrimaryTerm property.
	 * @since 4.0
	 */
	boolean hasSeqNoPrimaryTermProperty();

	/**
	 * Returns whether the {@link ElasticsearchPersistentEntity} has a {@link JoinField} property. If this call returns
	 * {@literal true}, {@link #getJoinFieldProperty()} will return a non-{@literal null} value.
	 *
	 * @return false when {@link ElasticsearchPersistentEntity} does not define a JoinField property.
	 * @since 4.1
	 */
	boolean hasJoinFieldProperty();

	/**
	 * Returns the {@link SeqNoPrimaryTerm} property of the {@link ElasticsearchPersistentEntity}. Can be {@literal null}
	 * in case no such property is available on the entity.
	 *
	 * @return the {@link SeqNoPrimaryTerm} {@link ElasticsearchPersistentProperty} of the {@link PersistentEntity} or
	 *         {@literal null} if not defined.
	 * @since 4.0
	 */
	@Nullable
	ElasticsearchPersistentProperty getSeqNoPrimaryTermProperty();

	/**
	 * Returns the {@link JoinField} property of the {@link ElasticsearchPersistentEntity}. Can be {@literal null} in case
	 * no such property is available on the entity.
	 *
	 * @return the {@link JoinField} {@link ElasticsearchPersistentProperty} of the {@link PersistentEntity} or
	 *         {@literal null} if not defined.
	 * @since 4.1
	 */
	@Nullable
	ElasticsearchPersistentProperty getJoinFieldProperty();

	/**
	 * Returns the {@link SeqNoPrimaryTerm} property of the {@link ElasticsearchPersistentEntity} or throws an
	 * IllegalStateException in case no such property is available on the entity.
	 *
	 * @return the {@link SeqNoPrimaryTerm} {@link ElasticsearchPersistentProperty} of the {@link PersistentEntity}.
	 * @since 4.0
	 */
	default ElasticsearchPersistentProperty getRequiredSeqNoPrimaryTermProperty() {
		ElasticsearchPersistentProperty property = this.getSeqNoPrimaryTermProperty();
		if (property != null) {
			return property;
		} else {
			throw new IllegalStateException(
					String.format("Required SeqNoPrimaryTerm property not found for %s!", this.getType()));
		}
	}

	/**
	 * @return the property annotated with {@link org.springframework.data.elasticsearch.annotations.IndexedIndexName} if
	 *         it exists, otherwise null
	 * @since 5.1
	 */
	@Nullable
	ElasticsearchPersistentProperty getIndexedIndexNameProperty();

	/**
	 * returns the default settings for an index.
	 *
	 * @return settings
	 * @since 4.1
	 */
	Settings getDefaultSettings();

	/**
	 * Resolves the routing for a bean.
	 *
	 * @param bean the bean to resolve the routing for
	 * @return routing value, may be {@literal null}
	 */
	@Nullable
	String resolveRouting(T bean);

	/**
	 * @return the {@link FieldNamingStrategy} for the entity
	 * @since 4.3
	 */
	FieldNamingStrategy getFieldNamingStrategy();

	/**
	 * @return true if type hints on this entity should be written.
	 * @since 4.3
	 */
	boolean writeTypeHints();

	/**
	 * @return the {@code dynamic} mapping parameter value.
	 * @since 4.3
	 */
	Dynamic dynamic();

	/**
	 * @return the storeIdInSource value from the document annotation
	 * @since 5.1
	 */
	boolean storeIdInSource();

	/**
	 * @return the storeVersionInSource value from the document annotation.
	 * @since 5.1
	 */
	boolean storeVersionInSource();

	/**
	 * @return if the mapping should be written to the index on repository bootstrap even if the index already exists.
	 * @since 5.2
	 */
	boolean isAlwaysWriteMapping();
}
