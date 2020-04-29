/*
 * Copyright 2013-2020 the original author or authors.
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

import org.elasticsearch.index.VersionType;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.core.query.SeqNoPrimaryTerm;
import org.springframework.data.mapping.PersistentEntity;
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
 */
public interface ElasticsearchPersistentEntity<T> extends PersistentEntity<T, ElasticsearchPersistentProperty> {

	IndexCoordinates getIndexCoordinates();

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
	String getParentType();

	@Nullable
	ElasticsearchPersistentProperty getParentIdProperty();

	String settingPath();

	@Nullable
	VersionType getVersionType();

	boolean isCreateIndexAndMapping();

	/**
	 * Returns whether the {@link ElasticsearchPersistentEntity} has a score property. If this call returns
	 * {@literal true}, {@link #getScoreProperty()} will return a non-{@literal null} value.
	 *
	 * @return false when {@link ElasticsearchPersistentEntity} does not define a score property.
	 * @since 3.1
	 * @deprecated since 4.0
	 */
	@Deprecated
	boolean hasScoreProperty();

	/**
	 * Returns the score property of the {@link ElasticsearchPersistentEntity}. Can be {@literal null} in case no score
	 * property is available on the entity.
	 *
	 * @return the score {@link ElasticsearchPersistentProperty} of the {@link PersistentEntity} or {@literal null} if not
	 *         defined.
	 * @since 3.1
	 * @deprecated since 4.0
	 */
	@Nullable
	@Deprecated
	ElasticsearchPersistentProperty getScoreProperty();

	/**
	 * returns the {@link ElasticsearchPersistentProperty} with the given fieldName (may be set by the {@link Field}
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
}
