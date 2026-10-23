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
package org.springframework.data.elasticsearch.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;
import org.springframework.data.elasticsearch.BulkFailureException;
import org.springframework.data.elasticsearch.core.bulk.BulkIngester;
import org.springframework.data.elasticsearch.core.bulk.BulkIngesterListener;
import org.springframework.data.elasticsearch.core.bulk.BulkOperationResult;
import org.springframework.data.elasticsearch.core.bulk.BulkOperationType;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.util.Assert;

/**
 * Base class for {@link BulkIngester} implementations. It contains everything that does not depend on a specific
 * client:
 * <ul>
 * <li>argument checks and the closed state,</li>
 * <li>running auditing and the {@link org.springframework.data.elasticsearch.core.event.BeforeConvertCallback} for
 * index operations on the calling thread,</li>
 * <li>id conversion and default routing for delete operations,</li>
 * <li>mapping the item responses to {@link BulkOperationResult}s,</li>
 * <li>writing id, version, sequence number and primary term back to the entities and running the
 * {@link org.springframework.data.elasticsearch.core.event.AfterSaveCallback} for successful index operations,</li>
 * <li>unwrapping and translating request failures,</li>
 * <li>calling the listener, logging exceptions thrown by it and preventing it from calling the ingester.</li>
 * </ul>
 * A client implementation converts the queries to its own operations, sends them with the bulk helper of its client and
 * calls {@link #onResponse(long, List, List)} or {@link #onFailure(long, List, Throwable)} when a request has finished.
 * <p>
 * <strong>Note:</strong> Although this class is public, it is not considered to be part of the official Spring Data
 * Elasticsearch API and so might change at any time.
 *
 * @param <C> the type of the context that can be passed when adding an operation
 * @author Ralph Ursprung
 * @since 6.2
 */
public abstract class AbstractBulkIngester<C> implements BulkIngester<C> {

	private static final Log LOGGER = LogFactory.getLog(AbstractBulkIngester.class);

	private final AbstractElasticsearchTemplate template;
	private final IndexCoordinates index;
	private final BulkIngesterListener<C> listener;
	private final ThreadLocal<Boolean> inCallback = new ThreadLocal<>();
	private final Object closeLock = new Object();
	private volatile boolean closed;

	/**
	 * @param template the template that created the ingester, provides converter, callbacks and routing
	 * @param index the index for operations that don't define one
	 * @param listener the listener that is informed about the outcome of each request
	 */
	protected AbstractBulkIngester(final AbstractElasticsearchTemplate template, final IndexCoordinates index,
			final BulkIngesterListener<C> listener) {

		Assert.notNull(template, "template must not be null");
		Assert.notNull(index, "index must not be null");
		Assert.notNull(listener, "listener must not be null");

		this.template = template;
		this.index = index;
		this.listener = listener;
	}

	/**
	 * @return the index for operations that don't define one
	 */
	protected IndexCoordinates getIndex() {
		return index;
	}

	// region BulkIngester
	@Override
	public final void add(final IndexQuery query, final @Nullable C context) {

		Assert.notNull(query, "query must not be null");
		assertOpen();

		template.maybeCallbackBeforeConvertWithQuery(query, index);
		final var operationType = query.getOpType() == IndexQuery.OpType.CREATE ? BulkOperationType.CREATE
				: BulkOperationType.INDEX;
		doAdd(query, new OperationContext<>(operationType, context, query));
	}

	@Override
	public final void add(final UpdateQuery query, final @Nullable C context) {

		Assert.notNull(query, "query must not be null");
		assertOpen();

		doAdd(query, new OperationContext<>(BulkOperationType.UPDATE, context, null));
	}

	@Override
	public final void addDelete(final String id, final @Nullable String routing, final @Nullable C context) {

		Assert.notNull(id, "id must not be null");
		assertOpen();

		final var convertedId = template.getElasticsearchConverter().convertId(id);
		final var effectiveRouting = routing != null ? routing : template.routingResolver.getRouting();
		doAddDelete(convertedId, effectiveRouting, new OperationContext<>(BulkOperationType.DELETE, context, null));
	}

	@Override
	public final void flush() {

		assertNotInCallback();
		doFlush();
	}

	@Override
	public final void close() {

		assertNotInCallback();

		synchronized (closeLock) {
			if (closed) {
				return;
			}
			closed = true;
			doClose();
		}
	}
	// endregion

	// region client specific
	/**
	 * Adds an index or create operation to the client's bulk helper. Auditing and the before-convert callback have
	 * already been applied.
	 *
	 * @param query the query to convert and add
	 * @param context the context to pass to the bulk helper and back to {@link #onResponse(long, List, List)}
	 */
	protected abstract void doAdd(IndexQuery query, OperationContext<C> context);

	/**
	 * Adds an update operation to the client's bulk helper.
	 *
	 * @param query the query to convert and add
	 * @param context the context to pass to the bulk helper and back to {@link #onResponse(long, List, List)}
	 */
	protected abstract void doAdd(UpdateQuery query, OperationContext<C> context);

	/**
	 * Adds a delete operation to the client's bulk helper.
	 *
	 * @param id the converted id of the document to delete
	 * @param routing the routing, may be {@literal null}
	 * @param context the context to pass to the bulk helper and back to {@link #onResponse(long, List, List)}
	 */
	protected abstract void doAddDelete(String id, @Nullable String routing, OperationContext<C> context);

	/**
	 * Flushes the client's bulk helper.
	 */
	protected abstract void doFlush();

	/**
	 * Closes the client's bulk helper and waits until all requests and listener calls have completed. Called at most
	 * once. Must not close the client.
	 */
	protected abstract void doClose();

	/**
	 * Translates a failure of a bulk request with the client's exception translator.
	 *
	 * @param throwable the failure, never a {@link CompletionException} or {@link ExecutionException} with a cause
	 * @return the translated exception
	 */
	protected abstract RuntimeException translateException(Throwable throwable);
	// endregion

	// region request completion
	/**
	 * To be called by the client implementation when a bulk request returned a response. Writes the indexing information
	 * back to the entities of successful index operations, runs the after-save callback for them and calls the listener.
	 * This method does not throw exceptions.
	 *
	 * @param executionId the id of the request
	 * @param contexts the contexts of the operations of the request in the order they were added
	 * @param items the item responses in the same order as the contexts
	 */
	protected final void onResponse(final long executionId, final List<OperationContext<C>> contexts,
			final List<ItemResponse> items) {

		inCallback.set(Boolean.TRUE);
		try {
			if (contexts.size() != items.size()) {
				notifyFailure(executionId, contexts, new IllegalStateException("Bulk response contains " + items.size()
						+ " items, but the request contained " + contexts.size() + " operations"));
				return;
			}

			final var results = new ArrayList<BulkOperationResult<C>>(contexts.size());
			for (var i = 0; i < contexts.size(); i++) {
				results.add(toResult(contexts.get(i), items.get(i)));
			}

			try {
				listener.afterBulk(executionId, Collections.unmodifiableList(results));
			} catch (RuntimeException e) {
				LOGGER.error("Bulk ingester listener threw an exception for request " + executionId, e);
			}
		} catch (RuntimeException e) {
			LOGGER.error("Could not process the response of bulk request " + executionId, e);
		} finally {
			inCallback.remove();
		}
	}

	/**
	 * To be called by the client implementation when a bulk request failed as a whole. Translates the failure and calls
	 * the listener. This method does not throw exceptions.
	 *
	 * @param executionId the id of the request
	 * @param contexts the contexts of the operations of the request in the order they were added
	 * @param failure the failure
	 * @return the translated failure that was passed to the listener
	 */
	protected final RuntimeException onFailure(final long executionId, final List<OperationContext<C>> contexts,
			final Throwable failure) {

		inCallback.set(Boolean.TRUE);
		try {
			final var translated = translate(failure);
			notifyFailure(executionId, contexts, translated);
			return translated;
		} finally {
			inCallback.remove();
		}
	}

	private RuntimeException translate(final Throwable failure) {

		var cause = failure;
		while ((cause instanceof CompletionException || cause instanceof ExecutionException) && cause.getCause() != null) {
			cause = cause.getCause();
		}

		try {
			return translateException(cause);
		} catch (RuntimeException e) {
			LOGGER.error("Could not translate the failure of a bulk request", e);
			return cause instanceof RuntimeException runtimeException ? runtimeException : new RuntimeException(cause);
		}
	}

	private void notifyFailure(final long executionId, final List<OperationContext<C>> contexts,
			final RuntimeException failure) {

		final var userContexts = new ArrayList<@Nullable C>(contexts.size());
		contexts.forEach(context -> userContexts.add(context.context));

		try {
			listener.afterBulk(executionId, Collections.unmodifiableList(userContexts), failure);
		} catch (RuntimeException e) {
			LOGGER.error("Bulk ingester listener threw an exception for failed request " + executionId, e);
		}
	}

	private BulkOperationResult<C> toResult(final OperationContext<C> context, final ItemResponse item) {

		if (item.error() != null) {
			return new BulkOperationResult<>(context.operationType, context.context, item.id(), item.index(), item.status(),
					null, new BulkFailureException.FailureDetails(item.status(), item.error()));
		}

		final var indexedObjectInformation = new IndexedObjectInformation(item.id(), item.index(), item.seqNo(),
				item.primaryTerm(), item.version());

		final var indexQuery = context.indexQuery;
		if (indexQuery != null) {
			try {
				template.updateIndexedObjectsWithQueries(List.of(indexQuery), List.of(indexedObjectInformation));
				template.maybeCallbackAfterSaveWithQuery(indexQuery, index);
			} catch (RuntimeException e) {
				LOGGER.error("Could not update the entity or run the after-save callback for document " + item.id(), e);
			}
		}

		return new BulkOperationResult<>(context.operationType, context.context, item.id(), item.index(), item.status(),
				indexedObjectInformation, null);
	}
	// endregion

	private void assertOpen() {

		assertNotInCallback();
		Assert.state(!closed, "The bulk ingester is closed");
	}

	private void assertNotInCallback() {
		Assert.state(inCallback.get() == null,
				"The bulk ingester must not be used from within a bulk ingester listener or entity callback");
	}

	/**
	 * Information about an operation that the client implementation passes to its bulk helper and back to
	 * {@link #onResponse(long, List, List)} or {@link #onFailure(long, List, Throwable)}.
	 *
	 * @param <C> the type of the context
	 */
	public static final class OperationContext<C> {

		private final BulkOperationType operationType;
		private final @Nullable C context;
		private final @Nullable IndexQuery indexQuery;

		OperationContext(final BulkOperationType operationType, final @Nullable C context,
				final @Nullable IndexQuery indexQuery) {
			this.operationType = operationType;
			this.context = context;
			this.indexQuery = indexQuery;
		}

		/**
		 * @return the type of the operation
		 */
		public BulkOperationType getOperationType() {
			return operationType;
		}

		/**
		 * @return the context that was passed when the operation was added
		 */
		@Nullable
		public C getContext() {
			return context;
		}
	}

	/**
	 * Client-independent representation of an item of a bulk response.
	 *
	 * @param id the id of the document
	 * @param index the name of the index
	 * @param status the HTTP status of the operation
	 * @param seqNo the sequence number
	 * @param primaryTerm the primary term
	 * @param version the version
	 * @param error the reason of the failure, {@literal null} if and only if the operation was successful
	 */
	public record ItemResponse( //
			@Nullable String id, //
			@Nullable String index, //
			int status, //
			@Nullable Long seqNo, //
			@Nullable Long primaryTerm, //
			@Nullable Long version, //
			@Nullable String error //
	) {
	}
}
