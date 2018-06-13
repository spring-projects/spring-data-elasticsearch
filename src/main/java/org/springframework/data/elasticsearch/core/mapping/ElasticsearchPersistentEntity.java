/*
 * Copyright 2013-2018 the original author or authors.
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
package org.springframework.data.elasticsearch.core.mapping;

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
 */
public interface ElasticsearchPersistentEntity<T> extends PersistentEntity<T, ElasticsearchPersistentProperty> {

	String getIndexName();

	String getIndexType();

	short getShards();

	short getReplicas();

	boolean isUseServerConfiguration();

	String getRefreshInterval();

	String getIndexStoreType();

	ElasticsearchPersistentProperty getVersionProperty();

	String getParentType();

	ElasticsearchPersistentProperty getParentIdProperty();

	String settingPath();

	boolean isCreateIndexAndMapping();

	/**
	 * Returns whether the {@link ElasticsearchPersistentEntity} has an score property. If this call returns
	 * {@literal true}, {@link #getScoreProperty()} will return a non-{@literal null} value.
	 *
	 * @return false when {@link ElasticsearchPersistentEntity} does not define a score property.
	 * @since 3.1
	 */
	boolean hasScoreProperty();

	/**
	 * Returns the score property of the {@link ElasticsearchPersistentEntity}. Can be {@literal null} in case no score
	 * property is available on the entity.
	 *
	 * @return the score {@link ElasticsearchPersistentProperty} of the {@link PersistentEntity} or {@literal null} if not
	 *         defined.
	 * @since 3.1
	 */
	@Nullable
	ElasticsearchPersistentProperty getScoreProperty();
}
