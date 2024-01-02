/*
 * Copyright 2019-2024 the original author or authors.
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

import org.springframework.data.elasticsearch.core.ActiveShardCount;
import org.springframework.data.elasticsearch.core.RefreshPolicy;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.lang.Nullable;

/**
 * Options that may be passed to an
 * {@link org.springframework.data.elasticsearch.core.DocumentOperations#bulkIndex(List, BulkOptions, IndexCoordinates)}
 * or
 * {@link org.springframework.data.elasticsearch.core.DocumentOperations#bulkUpdate(List, BulkOptions, IndexCoordinates)}
 * call. <br/>
 * Use {@link BulkOptions#builder()} to obtain a builder, then set the desired properties and call
 * {@link BulkOptionsBuilder#build()} to get the BulkOptions object.
 *
 * @author Peter-Josef Meisch
 * @author Mark Paluch
 * @since 3.2
 */
public class BulkOptions {

	private static final BulkOptions defaultOptions = builder().build();

	private final @Nullable Duration timeout;
	private final @Nullable RefreshPolicy refreshPolicy;
	private final @Nullable ActiveShardCount waitForActiveShards;
	private final @Nullable String pipeline;
	private final @Nullable String routingId;

	private BulkOptions(@Nullable Duration timeout, @Nullable RefreshPolicy refreshPolicy,
			@Nullable ActiveShardCount waitForActiveShards, @Nullable String pipeline, @Nullable String routingId) {
		this.timeout = timeout;
		this.refreshPolicy = refreshPolicy;
		this.waitForActiveShards = waitForActiveShards;
		this.pipeline = pipeline;
		this.routingId = routingId;
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

		public BulkOptions build() {
			return new BulkOptions(timeout, refreshPolicy, waitForActiveShards, pipeline, routingId);
		}
	}
}
