/*
 * Copyright 2020-2022 the original author or authors.
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

import org.reactivestreams.Publisher;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.mapping.callback.EntityCallback;

/**
 * Callback being invoked after a domain object is materialized from a {@link Document} when reading results.
 *
 * @author Roman Puchkovskiy
 * @since 4.0
 * @see org.springframework.data.mapping.callback.ReactiveEntityCallbacks
 */
@FunctionalInterface
public interface ReactiveAfterConvertCallback<T> extends EntityCallback<T> {

	/**
	 * Entity callback method invoked after a domain object is materialized from a {@link Document}. Can return either the
	 * same or a modified instance of the domain object.
	 *
	 * @param entity the domain object (the result of the conversion).
	 * @param document must not be {@literal null}.
	 * @param indexCoordinates must not be {@literal null}.
	 * @return a {@link Publisher} emitting the domain object that is the result of reading it from the {@link Document}.
	 */
	Publisher<T> onAfterConvert(T entity, Document document, IndexCoordinates indexCoordinates);
}
