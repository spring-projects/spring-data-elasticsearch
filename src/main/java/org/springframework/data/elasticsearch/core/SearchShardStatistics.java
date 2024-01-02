/*
 * Copyright 2023-2024 the original author or authors.
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

import java.util.List;

import org.springframework.data.elasticsearch.ElasticsearchErrorCause;
import org.springframework.lang.Nullable;

/**
 * @author Haibo Liu
 * @since 5.3
 */
public class SearchShardStatistics {
	private final Number failed;

	private final Number successful;

	private final Number total;

	@Nullable private final Number skipped;

	private final List<Failure> failures;

	private SearchShardStatistics(Number failed, Number successful, Number total, @Nullable Number skipped,
			List<Failure> failures) {
		this.failed = failed;
		this.successful = successful;
		this.total = total;
		this.skipped = skipped;
		this.failures = failures;
	}

	public static SearchShardStatistics of(Number failed, Number successful, Number total, @Nullable Number skipped,
			List<Failure> failures) {
		return new SearchShardStatistics(failed, successful, total, skipped, failures);
	}

	public Number getFailed() {
		return failed;
	}

	public Number getSuccessful() {
		return successful;
	}

	public Number getTotal() {
		return total;
	}

	@Nullable
	public Number getSkipped() {
		return skipped;
	}

	public boolean isFailed() {
		return failed.intValue() > 0;
	}

	public List<Failure> getFailures() {
		return failures;
	}

	public static class Failure {
		@Nullable private final String index;
		@Nullable private final String node;
		@Nullable private final String status;
		private final int shard;
		@Nullable private final Exception exception;
		@Nullable private final ElasticsearchErrorCause elasticsearchErrorCause;

		private Failure(@Nullable String index, @Nullable String node, @Nullable String status, int shard,
				@Nullable Exception exception, @Nullable ElasticsearchErrorCause elasticsearchErrorCause) {
			this.index = index;
			this.node = node;
			this.status = status;
			this.shard = shard;
			this.exception = exception;
			this.elasticsearchErrorCause = elasticsearchErrorCause;
		}

		public static SearchShardStatistics.Failure of(@Nullable String index, @Nullable String node,
				@Nullable String status, int shard, @Nullable Exception exception,
				@Nullable ElasticsearchErrorCause elasticsearchErrorCause) {
			return new SearchShardStatistics.Failure(index, node, status, shard, exception, elasticsearchErrorCause);
		}

		@Nullable
		public String getIndex() {
			return index;
		}

		@Nullable
		public String getNode() {
			return node;
		}

		@Nullable
		public String getStatus() {
			return status;
		}

		@Nullable
		public Exception getException() {
			return exception;
		}

		public int getShard() {
			return shard;
		}

		@Nullable
		public ElasticsearchErrorCause getElasticsearchErrorCause() {
			return elasticsearchErrorCause;
		}
	}
}
