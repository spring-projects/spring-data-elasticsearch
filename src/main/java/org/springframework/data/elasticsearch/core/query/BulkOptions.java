/*
 * Copyright 2019 the original author or authors.
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

import java.util.List;

import org.elasticsearch.action.support.ActiveShardCount;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.common.unit.TimeValue;

/**
 * Options that may be passed to an
 * {@link org.springframework.data.elasticsearch.core.ElasticsearchOperations#bulkIndex(List, BulkOptions)} or
 * {@link org.springframework.data.elasticsearch.core.ElasticsearchOperations#bulkUpdate(List, BulkOptions)} call. <br/>
 * Use {@link BulkOptions#builder()} to obtain a builder, then set the desired properties and call
 * {@link BulkOptionsBuilder#build()} to get the BulkOptions object.
 *
 * @author Peter-Josef Meisch
 * @since 3.2
 */
public class BulkOptions {

	private static final BulkOptions defaultOptions = builder().build();

	private final TimeValue timeout;
	private final WriteRequest.RefreshPolicy refreshPolicy;
	private final ActiveShardCount waitForActiveShards;
	private final String pipeline;
	private final String routingId;

	public static BulkOptionsBuilder builder() {
		return new BulkOptionsBuilder();
	}

	public static BulkOptions defaultOptions() {
		return defaultOptions;
	}

	private BulkOptions(TimeValue timeout, WriteRequest.RefreshPolicy refreshPolicy, ActiveShardCount waitForActiveShards,
			String pipeline, String routingId) {
		this.timeout = timeout;
		this.refreshPolicy = refreshPolicy;
		this.waitForActiveShards = waitForActiveShards;
		this.pipeline = pipeline;
		this.routingId = routingId;
	}

	public TimeValue getTimeout() {
		return timeout;
	}

	public WriteRequest.RefreshPolicy getRefreshPolicy() {
		return refreshPolicy;
	}

	public ActiveShardCount getWaitForActiveShards() {
		return waitForActiveShards;
	}

	public String getPipeline() {
		return pipeline;
	}

	public String getRoutingId() {
		return routingId;
	}

	public static final class BulkOptionsBuilder {
		private TimeValue timeout;
		private WriteRequest.RefreshPolicy refreshPolicy;
		private ActiveShardCount waitForActiveShards;
		private String pipeline;
		private String routingId;

		private BulkOptionsBuilder() {}

		public BulkOptionsBuilder withTimeout(TimeValue timeout) {
			this.timeout = timeout;
			return this;
		}

		public BulkOptionsBuilder withRefreshPolicy(WriteRequest.RefreshPolicy refreshPolicy) {
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
