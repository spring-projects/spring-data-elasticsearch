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

import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Listener that is informed about the outcome of the bulk requests sent by a {@link BulkIngester}. A listener is
 * mandatory, as the requests are sent asynchronously and failures would otherwise be lost.
 * <p>
 * The methods are called on threads of the bulk ingester and not on the thread that added the operations. When more
 * than one concurrent request is allowed, they may be called concurrently, so implementations must be thread-safe.
 * Implementations must not call {@code add}, {@code addDelete}, {@link BulkIngester#flush() flush} or
 * {@link BulkIngester#close() close} on the ingester; doing so throws an {@link IllegalStateException}. Long-running
 * work should be handed off to another thread, as it blocks the ingester. Exceptions thrown by the listener are logged
 * and otherwise ignored.
 *
 * @param <C> the type of the context that can be passed when adding an operation
 * @author Ralph Ursprung
 * @since 6.2
 */
public interface BulkIngesterListener<C> {

	/**
	 * Called after Elasticsearch returned a response for a bulk request. The response may contain both successful and
	 * failed operations, use {@link BulkOperationResult#isSuccessful()} to check each of them.
	 *
	 * @param executionId the id of the request, unique for the ingester that sent it
	 * @param results the results in the order in which the operations were added
	 */
	void afterBulk(long executionId, List<BulkOperationResult<C>> results);

	/**
	 * Called when a bulk request failed as a whole, for example because Elasticsearch could not be reached. It is
	 * unknown whether any of the operations of this request were applied.
	 *
	 * @param executionId the id of the request, unique for the ingester that sent it
	 * @param contexts the contexts of the operations of the request in the order in which they were added, an element is
	 *          {@literal null} if no context was passed for that operation
	 * @param failure the translated failure
	 */
	void afterBulk(long executionId, List<@Nullable C> contexts, RuntimeException failure);
}
