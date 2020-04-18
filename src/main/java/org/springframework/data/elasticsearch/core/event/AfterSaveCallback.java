/*
 * Copyright 2020 the original author or authors.
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
package org.springframework.data.elasticsearch.core.event;

import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.mapping.callback.EntityCallback;
import org.springframework.data.mapping.callback.EntityCallbacks;

/**
 * Entity callback triggered after save of an entity.
 *
 * @author Roman Puchkovskiy
 * @since 4.0
 * @see EntityCallbacks
 */
@FunctionalInterface
public interface AfterSaveCallback<T> extends EntityCallback<T> {

	/**
	 * Entity callback method invoked after a domain object is saved. Can return either the same or a modified instance of
	 * the domain object.
	 *
	 * @param entity the domain object that was saved.
	 * @param index must not be {@literal null}.
	 * @return the domain object that was persisted.
	 */
	T onAfterSave(T entity, IndexCoordinates index);
}
