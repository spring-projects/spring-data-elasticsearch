/*
 * Copyright 2013-2014 the original author or authors.
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

/**
 * ElasticsearchPersistentEntity
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
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
}
