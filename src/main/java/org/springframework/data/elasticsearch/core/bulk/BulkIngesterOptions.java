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

import java.time.Duration;

import org.jspecify.annotations.Nullable;
import org.springframework.data.elasticsearch.core.query.BulkOptions;
import org.springframework.util.Assert;

/**
 * Options for a {@link BulkIngester}. Use {@link #builder()} to create an instance; the defaults are the same as those
 * of the bulk ingester of the Elasticsearch Java client: at most 1000 operations or 5 MiB per request, one concurrent
 * request and no periodic flush.
 *
 * @author Ralph Ursprung
 * @since 6.2
 */
public final class BulkIngesterOptions {

	/**
	 * the default maximum number of operations per request.
	 */
	public static final int DEFAULT_MAX_OPERATIONS = 1000;

	/**
	 * the default maximum size in bytes of the operations of a request.
	 */
	public static final long DEFAULT_MAX_SIZE = 5L * 1024 * 1024;

	/**
	 * the default maximum number of concurrent requests.
	 */
	public static final int DEFAULT_MAX_CONCURRENT_REQUESTS = 1;

	private static final BulkIngesterOptions DEFAULT_OPTIONS = builder().build();

	private final int maxOperations;
	private final long maxSize;
	private final int maxConcurrentRequests;
	private final @Nullable Duration flushInterval;
	private final BulkOptions bulkOptions;

	private BulkIngesterOptions(final int maxOperations, final long maxSize, final int maxConcurrentRequests,
			final @Nullable Duration flushInterval, final BulkOptions bulkOptions) {
		this.maxOperations = maxOperations;
		this.maxSize = maxSize;
		this.maxConcurrentRequests = maxConcurrentRequests;
		this.flushInterval = flushInterval;
		this.bulkOptions = bulkOptions;
	}

	/**
	 * @return the maximum number of operations per request
	 */
	public int getMaxOperations() {
		return maxOperations;
	}

	/**
	 * @return the maximum size in bytes of the operations of a request. The request is sent after the operation that
	 *         reaches this size was added, so a request can be larger by up to one operation.
	 */
	public long getMaxSize() {
		return maxSize;
	}

	/**
	 * @return the maximum number of requests that may be in flight at the same time
	 */
	public int getMaxConcurrentRequests() {
		return maxConcurrentRequests;
	}

	/**
	 * @return the interval after which pending operations are sent, {@literal null} if there is no periodic flush
	 */
	@Nullable
	public Duration getFlushInterval() {
		return flushInterval;
	}

	/**
	 * @return the request-wide options that are applied to every request, like timeout, refresh policy or pipeline
	 */
	public BulkOptions getBulkOptions() {
		return bulkOptions;
	}

	/**
	 * @return a new {@link BulkIngesterOptionsBuilder}
	 */
	public static BulkIngesterOptionsBuilder builder() {
		return new BulkIngesterOptionsBuilder();
	}

	/**
	 * @return the default options
	 */
	public static BulkIngesterOptions defaultOptions() {
		return DEFAULT_OPTIONS;
	}

	@Override
	public String toString() {
		return "BulkIngesterOptions{" + //
				"maxOperations=" + maxOperations + //
				", maxSize=" + maxSize + //
				", maxConcurrentRequests=" + maxConcurrentRequests + //
				", flushInterval=" + flushInterval + //
				'}';
	}

	/**
	 * Builder for {@link BulkIngesterOptions}.
	 */
	public static final class BulkIngesterOptionsBuilder {

		private int maxOperations = DEFAULT_MAX_OPERATIONS;
		private long maxSize = DEFAULT_MAX_SIZE;
		private int maxConcurrentRequests = DEFAULT_MAX_CONCURRENT_REQUESTS;
		private @Nullable Duration flushInterval;
		private BulkOptions bulkOptions = BulkOptions.defaultOptions();

		private BulkIngesterOptionsBuilder() {}

		/**
		 * @param maxOperations the maximum number of operations per request, must be greater than 0
		 * @return this builder
		 */
		public BulkIngesterOptionsBuilder withMaxOperations(final int maxOperations) {

			Assert.isTrue(maxOperations > 0, "maxOperations must be greater than 0");

			this.maxOperations = maxOperations;
			return this;
		}

		/**
		 * @param maxSize the maximum size in bytes of the operations of a request, must be greater than 0. A request can
		 *          be larger by up to one operation.
		 * @return this builder
		 */
		public BulkIngesterOptionsBuilder withMaxSize(final long maxSize) {

			Assert.isTrue(maxSize > 0, "maxSize must be greater than 0");

			this.maxSize = maxSize;
			return this;
		}

		/**
		 * @param maxConcurrentRequests the maximum number of requests in flight at the same time, must be greater than 0
		 * @return this builder
		 */
		public BulkIngesterOptionsBuilder withMaxConcurrentRequests(final int maxConcurrentRequests) {

			Assert.isTrue(maxConcurrentRequests > 0, "maxConcurrentRequests must be greater than 0");

			this.maxConcurrentRequests = maxConcurrentRequests;
			return this;
		}

		/**
		 * @param flushInterval the interval after which pending operations are sent, must be at least one millisecond.
		 *          {@literal null} disables the periodic flush.
		 * @return this builder
		 */
		public BulkIngesterOptionsBuilder withFlushInterval(final @Nullable Duration flushInterval) {

			Assert.isTrue(flushInterval == null || flushInterval.toMillis() > 0,
					"flushInterval must be at least one millisecond");

			this.flushInterval = flushInterval;
			return this;
		}

		/**
		 * @param bulkOptions the request-wide options that are applied to every request, must not be {@literal null}
		 * @return this builder
		 */
		public BulkIngesterOptionsBuilder withBulkOptions(final BulkOptions bulkOptions) {

			Assert.notNull(bulkOptions, "bulkOptions must not be null");

			this.bulkOptions = bulkOptions;
			return this;
		}

		/**
		 * @return the {@link BulkIngesterOptions}
		 */
		public BulkIngesterOptions build() {
			return new BulkIngesterOptions(maxOperations, maxSize, maxConcurrentRequests, flushInterval, bulkOptions);
		}
	}
}
