/*
 * Copyright 2026-present the original author or authors.
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
package org.springframework.data.elasticsearch.core.bulk;

import org.jspecify.annotations.Nullable;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;

/**
 * Collects index, update and delete operations and sends them to Elasticsearch in bulk requests. A request is sent when
 * one of the limits defined in the {@link BulkIngesterOptions} is reached, when {@link #flush()} is called, when the
 * optional flush interval has passed or when the ingester is closed. The outcome of each request is reported to the
 * {@link BulkIngesterListener}.
 * <p>
 * An ingester is thread-safe: operations may be added from several threads. Adding an operation blocks the calling
 * thread when the maximum number of concurrent requests is in flight, so an ingester must not be used on threads that
 * must not block, like reactive event loops.
 * <p>
 * Each operation may be associated with a context object of type {@code C}. It is not sent to Elasticsearch but passed
 * back to the listener together with the result of the operation.
 * <p>
 * An ingester must be closed after use to send the remaining operations and release its threads. Closing it does not
 * close the underlying client.
 *
 * @param <C> the type of the context that can be passed when adding an operation
 * @author Ralph Ursprung
 * @since 6.2
 */
public interface BulkIngester<C> extends AutoCloseable {

	/**
	 * Adds an index or create operation. Auditing and the
	 * {@link org.springframework.data.elasticsearch.core.event.BeforeConvertCallback} are applied on the calling thread.
	 *
	 * @param query the query, must not be {@literal null}
	 * @throws IllegalStateException if the ingester is closed
	 */
	default void add(IndexQuery query) {
		add(query, null);
	}

	/**
	 * Adds an index or create operation with a context. Auditing and the
	 * {@link org.springframework.data.elasticsearch.core.event.BeforeConvertCallback} are applied on the calling thread.
	 *
	 * @param query the query, must not be {@literal null}
	 * @param context the context passed back to the listener, may be {@literal null}
	 * @throws IllegalStateException if the ingester is closed
	 */
	void add(IndexQuery query, @Nullable C context);

	/**
	 * Adds an update operation.
	 *
	 * @param query the query, must not be {@literal null}
	 * @throws IllegalStateException if the ingester is closed
	 */
	default void add(UpdateQuery query) {
		add(query, null);
	}

	/**
	 * Adds an update operation with a context.
	 *
	 * @param query the query, must not be {@literal null}
	 * @param context the context passed back to the listener, may be {@literal null}
	 * @throws IllegalStateException if the ingester is closed
	 */
	void add(UpdateQuery query, @Nullable C context);

	/**
	 * Adds a delete operation for a document id.
	 *
	 * @param id the id of the document, must not be {@literal null}
	 * @throws IllegalStateException if the ingester is closed
	 */
	default void addDelete(String id) {
		addDelete(id, null, null);
	}

	/**
	 * Adds a delete operation for a document id with a routing and a context.
	 *
	 * @param id the id of the document, must not be {@literal null}
	 * @param routing the routing, if {@literal null} the routing of the operations instance that created this ingester
	 *          is used
	 * @param context the context passed back to the listener, may be {@literal null}
	 * @throws IllegalStateException if the ingester is closed
	 */
	void addDelete(String id, @Nullable String routing, @Nullable C context);

	/**
	 * Sends the pending operations without waiting for the response. This may block when the maximum number of
	 * concurrent requests is in flight.
	 */
	void flush();

	/**
	 * Sends the pending operations and waits until all requests have completed and the listener has been called for each
	 * of them. Calling this method more than once has no effect.
	 */
	@Override
	void close();

	/**
	 * @return the number of operations that are not yet sent
	 */
	int getPendingOperations();

	/**
	 * @return the estimated size in bytes of the operations that are not yet sent
	 */
	long getPendingOperationsSize();

	/**
	 * @return the number of requests that are in flight
	 */
	int getPendingRequests();

	/**
	 * @return the number of operations that were added since the ingester was created
	 */
	long getOperationsCount();

	/**
	 * @return how often adding an operation had to wait because the maximum number of concurrent requests was in flight
	 */
	long getOperationContentionsCount();

	/**
	 * @return the number of requests that were sent since the ingester was created
	 */
	long getRequestCount();

	/**
	 * @return how often sending a request had to wait because the maximum number of concurrent requests was in flight
	 */
	long getRequestContentionsCount();
}
