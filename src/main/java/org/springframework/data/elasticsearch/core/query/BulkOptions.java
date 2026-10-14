/*
 * Copyright 2019-present the original author or authors.
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
package org.springframework.data.elasticsearch.core.query;

import java.time.Duration;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.data.elasticsearch.core.ActiveShardCount;
import org.springframework.data.elasticsearch.core.RefreshPolicy;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.util.Assert;

/**
 * Options that may be passed to an
 * {@link org.springframework.data.elasticsearch.core.DocumentOperations#bulkIndex(List, BulkOptions, IndexCoordinates)}
 * or
 * {@link org.springframework.data.elasticsearch.core.DocumentOperations#bulkUpdate(List, BulkOptions, IndexCoordinates)}
 * call. <br/>
 * Use {@link BulkOptions#builder()} to obtain a builder, then set the desired properties and call
 * {@link BulkOptionsBuilder#build()} to get the BulkOptions object. <br/>
 * If {@link #getMaxOperations()} or {@link #getMaxSize()} are set, a client implementation that supports it (like the
 * one based on the Elasticsearch Java client) splits a bulk operation into several bulk requests that are sent one
 * after the other. Item failures of all requests are collected and reported in one
 * {@link org.springframework.data.elasticsearch.BulkFailureException} at the end. Requests that were sent successfully
 * before a failure are not rolled back. Implementations that don't support it send one bulk request as before.
 *
 * @author Peter-Josef Meisch
 * @author Mark Paluch
 * @author Ralph Ursprung
 * @since 3.2
 */
public class BulkOptions {

	private static final BulkOptions defaultOptions = builder().build();

	private final @Nullable Duration timeout;
	private final @Nullable RefreshPolicy refreshPolicy;
	private final @Nullable ActiveShardCount waitForActiveShards;
	private final @Nullable String pipeline;
	private final @Nullable String routingId;
	private final @Nullable Integer maxOperations;
	private final @Nullable Long maxSize;

	private BulkOptions(@Nullable Duration timeout, @Nullable RefreshPolicy refreshPolicy,
			@Nullable ActiveShardCount waitForActiveShards, @Nullable String pipeline, @Nullable String routingId,
			@Nullable Integer maxOperations, @Nullable Long maxSize) {
		this.timeout = timeout;
		this.refreshPolicy = refreshPolicy;
		this.waitForActiveShards = waitForActiveShards;
		this.pipeline = pipeline;
		this.routingId = routingId;
		this.maxOperations = maxOperations;
		this.maxSize = maxSize;
	}

	@Nullable
	public Duration getTimeout() {
		return timeout;
	}

	@Nullable
	public RefreshPolicy getRefreshPolicy() {
		return refreshPolicy;
	}

	@Nullable
	public ActiveShardCount getWaitForActiveShards() {
		return waitForActiveShards;
	}

	@Nullable
	public String getPipeline() {
		return pipeline;
	}

	@Nullable
	public String getRoutingId() {
		return routingId;
	}

	/**
	 * @return the maximum number of operations sent in one bulk request, {@literal null} if not limited.
	 * @since 6.2
	 */
	@Nullable
	public Integer getMaxOperations() {
		return maxOperations;
	}

	/**
	 * @return the maximum size in bytes of the operations sent in one bulk request, {@literal null} if not limited.
	 * @since 6.2
	 */
	@Nullable
	public Long getMaxSize() {
		return maxSize;
	}

	/**
	 * Create a new {@link BulkOptionsBuilder} to build {@link BulkOptions}.
	 *
	 * @return a new {@link BulkOptionsBuilder} to build {@link BulkOptions}.
	 */
	public static BulkOptionsBuilder builder() {
		return new BulkOptionsBuilder();
	}

	/**
	 * Return default {@link BulkOptions}.
	 *
	 * @return default {@link BulkOptions}.
	 */
	public static BulkOptions defaultOptions() {
		return defaultOptions;
	}

	/**
	 * Builder for {@link BulkOptions}.
	 */
	public static class BulkOptionsBuilder {

		private @Nullable Duration timeout;
		private @Nullable RefreshPolicy refreshPolicy;
		private @Nullable ActiveShardCount waitForActiveShards;
		private @Nullable String pipeline;
		private @Nullable String routingId;
		private @Nullable Integer maxOperations;
		private @Nullable Long maxSize;

		private BulkOptionsBuilder() {}

		public BulkOptionsBuilder withTimeout(Duration timeout) {
			this.timeout = timeout;
			return this;
		}

		public BulkOptionsBuilder withRefreshPolicy(RefreshPolicy refreshPolicy) {
			this.refreshPolicy = refreshPolicy;
			return this;
		}

		public BulkOptionsBuilder withWaitForActiveShards(ActiveShardCount waitForActiveShards) {
			this.waitForActiveShards = waitForActiveShards;
			return this;
		}

		public BulkOptionsBuilder withPipeline(String pipeline) {
			this.pipeline = pipeline;
			return this;
		}

		public BulkOptionsBuilder withRoutingId(String routingId) {
			this.routingId = routingId;
			return this;
		}

		/**
		 * Sets the maximum number of operations that are sent in one bulk request. If a bulk operation contains more
		 * operations, it is split into several bulk requests that are sent one after the other.
		 *
		 * @param maxOperations the maximum number of operations per bulk request, must be greater than 0
		 * @return this builder
		 * @since 6.2
		 */
		public BulkOptionsBuilder withMaxOperations(int maxOperations) {

			Assert.isTrue(maxOperations > 0, "maxOperations must be greater than 0");

			this.maxOperations = maxOperations;
			return this;
		}

		/**
		 * Sets the maximum size in bytes of the operations that are sent in one bulk request. If a bulk operation is
		 * larger, it is split into several bulk requests that are sent one after the other. A single operation that is
		 * larger than this value is sent in a bulk request of its own.
		 *
		 * @param maxSize the maximum size in bytes per bulk request, must be greater than 0
		 * @return this builder
		 * @since 6.2
		 */
		public BulkOptionsBuilder withMaxSize(long maxSize) {

			Assert.isTrue(maxSize > 0, "maxSize must be greater than 0");

			this.maxSize = maxSize;
			return this;
		}

		public BulkOptions build() {
			return new BulkOptions(timeout, refreshPolicy, waitForActiveShards, pipeline, routingId, maxOperations,
					maxSize);
		}
	}
}
