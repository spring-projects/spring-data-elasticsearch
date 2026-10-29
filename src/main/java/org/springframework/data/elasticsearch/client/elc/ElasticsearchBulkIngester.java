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
package org.springframework.data.elasticsearch.client.elc;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._helpers.bulk.BulkListener;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import io.micrometer.observation.Observation;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.Nullable;
import org.springframework.data.elasticsearch.core.AbstractBulkIngester;
import org.springframework.data.elasticsearch.core.bulk.BulkIngesterListener;
import org.springframework.data.elasticsearch.core.bulk.BulkIngesterOptions;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.data.elasticsearch.core.bulk.BulkIngester} implementation that uses the
 * {@link co.elastic.clients.elasticsearch._helpers.bulk.BulkIngester} of the Elasticsearch Java client. Each bulk
 * request is recorded as one {@link ElasticsearchOperationName#BULK} observation.
 *
 * @param <C> the type of the context that can be passed when adding an operation
 * @author Ralph Ursprung
 * @since 6.2
 */
class ElasticsearchBulkIngester<C> extends AbstractBulkIngester<C> {

	private final ElasticsearchTemplate template;
	private final RequestConverter requestConverter;
	private final ElasticsearchExceptionTranslator exceptionTranslator;
	private final Map<Long, Observation> observations = new ConcurrentHashMap<>();
	private final co.elastic.clients.elasticsearch._helpers.bulk.BulkIngester<OperationContext<C>> delegate;

	ElasticsearchBulkIngester(final ElasticsearchTemplate template, final ElasticsearchClient client,
			final RequestConverter requestConverter, final ElasticsearchExceptionTranslator exceptionTranslator,
			final BulkIngesterOptions options, final IndexCoordinates index, final BulkIngesterListener<C> listener) {

		super(template, index, listener);

		Assert.notNull(client, "client must not be null");
		Assert.notNull(requestConverter, "requestConverter must not be null");
		Assert.notNull(exceptionTranslator, "exceptionTranslator must not be null");
		Assert.notNull(options, "options must not be null");

		this.template = template;
		this.requestConverter = requestConverter;
		this.exceptionTranslator = exceptionTranslator;

		final var builder = new co.elastic.clients.elasticsearch._helpers.bulk.BulkIngester.Builder<OperationContext<C>>() //
				.client(client) //
				.maxOperations(options.getMaxOperations()) //
				.maxSize(options.getMaxSize()) //
				.maxConcurrentRequests(options.getMaxConcurrentRequests()) //
				.globalSettings(requestConverter.bulkRequestBuilder(options.getBulkOptions(), template.getRefreshPolicy())) //
				.listener(new Listener());

		final var flushInterval = options.getFlushInterval();
		if (flushInterval != null) {
			builder.flushInterval(flushInterval.toMillis(), TimeUnit.MILLISECONDS);
		}

		this.delegate = builder.build();
	}

	// region AbstractBulkIngester
	@Override
	protected void doAdd(final IndexQuery query, final OperationContext<C> context) {
		delegate.add(requestConverter.bulkOperation(query, getIndex()), context);
	}

	@Override
	protected void doAdd(final UpdateQuery query, final OperationContext<C> context) {
		delegate.add(requestConverter.bulkOperation(query, getIndex()), context);
	}

	@Override
	protected void doAddDelete(final String id, final @Nullable String routing, final OperationContext<C> context) {
		delegate.add(requestConverter.bulkDeleteOperation(id, routing, getIndex()), context);
	}

	@Override
	protected void doFlush() {
		delegate.flush();
	}

	@Override
	protected void doClose() {
		delegate.close();
	}

	@Override
	protected RuntimeException translateException(final Throwable throwable) {
		return exceptionTranslator.translateException(throwable);
	}
	// endregion

	// region statistics
	@Override
	public int getPendingOperations() {
		return delegate.pendingOperations();
	}

	@Override
	public long getPendingOperationsSize() {
		return delegate.pendingOperationsSize();
	}

	@Override
	public int getPendingRequests() {
		return delegate.pendingRequests();
	}

	@Override
	public long getOperationsCount() {
		return delegate.operationsCount();
	}

	@Override
	public long getOperationContentionsCount() {
		return delegate.operationContentionsCount();
	}

	@Override
	public long getRequestCount() {
		return delegate.requestCount();
	}

	@Override
	public long getRequestContentionsCount() {
		return delegate.requestContentionsCount();
	}
	// endregion

	private static ItemResponse toItemResponse(final BulkResponseItem item) {

		final var error = item.error();
		final var errorReason = error != null ? Objects.requireNonNullElse(error.reason(), error.type()) : null;

		return new ItemResponse(item.id(), item.index(), item.status(), item.seqNo(), item.primaryTerm(), item.version(),
				errorReason);
	}

	private class Listener implements BulkListener<OperationContext<C>> {

		@Override
		public void beforeBulk(final long executionId, final BulkRequest request,
				final List<OperationContext<C>> contexts) {

			final var observation = template.createObservation(ElasticsearchOperationName.BULK, getIndex(),
					contexts.size());
			observation.start();
			observations.put(executionId, observation);
		}

		@Override
		public void afterBulk(final long executionId, final BulkRequest request,
				final List<OperationContext<C>> contexts, final BulkResponse response) {

			final var observation = observations.remove(executionId);
			try {
				onResponse(executionId, contexts,
						response.items().stream().map(ElasticsearchBulkIngester::toItemResponse).toList());
			} finally {
				if (observation != null) {
					observation.stop();
				}
			}
		}

		@Override
		public void afterBulk(final long executionId, final BulkRequest request,
				final List<OperationContext<C>> contexts, final Throwable failure) {

			final var observation = observations.remove(executionId);
			final var translated = onFailure(executionId, contexts, failure);
			if (observation != null) {
				observation.error(translated);
				observation.stop();
			}
		}
	}
}
