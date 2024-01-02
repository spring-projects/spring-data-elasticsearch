/*
 * Copyright 2021-2024 the original author or authors.
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
 * Callback being invoked after a {@link Document} is read from Elasticsearch and before it is converted into a domain
 * object.
 *
 * @author Peter-Josef Meisch
 * @since 4.4
 * @see org.springframework.data.mapping.callback.EntityCallbacks
 */
@FunctionalInterface
public interface ReactiveAfterLoadCallback<T> extends EntityCallback<Document> {

	/**
	 * Entity callback method invoked after a domain object is materialized from a {@link Document}. Can return either the
	 * same or a modified instance of the {@link Document} object.
	 *
	 * @param document the document.
	 * @param indexCoordinates of the index the document was read from.
	 * @return a possible modified or new {@link Document}.
	 */
	Publisher<Document> onAfterLoad(Document document, Class<T> type, IndexCoordinates indexCoordinates);
}
