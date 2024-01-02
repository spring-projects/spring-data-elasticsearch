/*
 * Copyright 2022-2024 the original author or authors.
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
package org.springframework.data.elasticsearch.core.reindex;

import java.util.Collections;
import java.util.List;

import org.springframework.data.elasticsearch.ElasticsearchErrorCause;
import org.springframework.lang.Nullable;

/**
 * Response of reindex request. (@see
 * https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-reindex.html#docs-reindex-api-response-body)
 *
 * @author Sijia Liu
 * @since 4.4
 */
public class ReindexResponse {

	private final long took;
	private final boolean timedOut;
	private final long total;
	private final long created;
	private final long updated;
	private final long deleted;
	private final long batches;
	private final long versionConflicts;
	private final long noops;
	private final long bulkRetries;
	private final long searchRetries;
	private final long throttledMillis;
	private final double requestsPerSecond;
	private final long throttledUntilMillis;
	private final List<Failure> failures;

	private ReindexResponse(long took, boolean timedOut, long total, long created, long updated, long deleted,
			long batches, long versionConflicts, long noops, long bulkRetries, long searchRetries, long throttledMillis,
			double requestsPerSecond, long throttledUntilMillis, List<Failure> failures) {
		this.took = took;
		this.timedOut = timedOut;
		this.total = total;
		this.created = created;
		this.updated = updated;
		this.deleted = deleted;
		this.batches = batches;
		this.versionConflicts = versionConflicts;
		this.noops = noops;
		this.bulkRetries = bulkRetries;
		this.searchRetries = searchRetries;
		this.throttledMillis = throttledMillis;
		this.requestsPerSecond = requestsPerSecond;
		this.throttledUntilMillis = throttledUntilMillis;
		this.failures = failures;
	}

	/**
	 * The number of milliseconds from start to end of the whole operation.
	 */
	public long getTook() {
		return took;
	}

	/**
	 * Did any of the sub-requests that were part of this request timeout?
	 */
	public boolean isTimedOut() {
		return timedOut;
	}

	/**
	 * The number of documents that were successfully processed.
	 */
	public long getTotal() {
		return total;
	}

	/**
	 * The number of documents that were successfully created.
	 */
	public long getCreated() {
		return created;
	}

	/**
	 * The number of documents that were successfully updated.
	 */
	public long getUpdated() {
		return updated;
	}

	/**
	 * The number of documents that were successfully deleted.
	 */
	public long getDeleted() {
		return deleted;
	}

	/**
	 * The number of scroll responses pulled back by the update by query.
	 */
	public long getBatches() {
		return batches;
	}

	/**
	 * The number of version conflicts that the update by query hit.
	 */
	public long getVersionConflicts() {
		return versionConflicts;
	}

	/**
	 * The number of documents that were ignored because the script used for the update by query returned a noop value for
	 * ctx.op.
	 */
	public long getNoops() {
		return noops;
	}

	/**
	 * The number of times that the request had retry bulk actions.
	 */
	public long getBulkRetries() {
		return bulkRetries;
	}

	/**
	 * The number of times that the request had retry search actions.
	 */
	public long getSearchRetries() {
		return searchRetries;
	}

	/**
	 * Number of milliseconds the request slept to conform to requests_per_second.
	 */
	public long getThrottledMillis() {
		return throttledMillis;
	}

	/**
	 * The number of requests per second effectively executed during the reindex.
	 */
	public double getRequestsPerSecond() {
		return requestsPerSecond;
	}

	/**
	 * This field should always be equal to zero in a _reindex response. It only has meaning when using the Task API,
	 * where it indicates the next time (in milliseconds since epoch) a throttled request will be executed again in order
	 * to conform to requests_per_second.
	 */
	public long getThrottledUntilMillis() {
		return throttledUntilMillis;
	}

	/**
	 * All of the bulk failures. Version conflicts are only included if the request sets abortOnVersionConflict to true
	 * (the default).
	 */
	public List<Failure> getFailures() {
		return failures;
	}

	/**
	 * Create a new {@link ReindexResponseBuilder} to build {@link ReindexResponse}
	 *
	 * @return a new {@link ReindexResponseBuilder} to build {@link ReindexResponse}
	 */
	public static ReindexResponseBuilder builder() {
		return new ReindexResponseBuilder();
	}

	public static class Failure {

		@Nullable private final String index;
		@Nullable private final String type;
		@Nullable private final String id;
		@Nullable private final Exception cause;
		@Nullable private final Integer status;
		@Nullable private final Long seqNo;
		@Nullable private final Long term;
		@Nullable private final Boolean aborted;
		@Nullable private final ElasticsearchErrorCause elasticsearchErrorCause;

		private Failure(@Nullable String index, @Nullable String type, @Nullable String id, @Nullable Exception cause,
				@Nullable Integer status, @Nullable Long seqNo, @Nullable Long term, @Nullable Boolean aborted,
				@Nullable ElasticsearchErrorCause elasticsearchErrorCause) {
			this.index = index;
			this.type = type;
			this.id = id;
			this.cause = cause;
			this.status = status;
			this.seqNo = seqNo;
			this.term = term;
			this.aborted = aborted;
			this.elasticsearchErrorCause = elasticsearchErrorCause;
		}

		@Nullable
		public String getIndex() {
			return index;
		}

		@Nullable
		public String getType() {
			return type;
		}

		@Nullable
		public String getId() {
			return id;
		}

		@Nullable
		public Exception getCause() {
			return cause;
		}

		@Nullable
		public Integer getStatus() {
			return status;
		}

		@Nullable
		public Long getSeqNo() {
			return seqNo;
		}

		@Nullable
		public Long getTerm() {
			return term;
		}

		@Nullable
		public Boolean getAborted() {
			return aborted;
		}

		@Nullable
		public ElasticsearchErrorCause getElasticsearchErrorCause() {
			return elasticsearchErrorCause;
		}

		/**
		 * Create a new {@link Failure.FailureBuilder} to build {@link Failure}
		 *
		 * @return a new {@link Failure.FailureBuilder} to build {@link Failure}
		 */
		public static Failure.FailureBuilder builder() {
			return new Failure.FailureBuilder();
		}

		/**
		 * Builder for {@link Failure}
		 */
		public static final class FailureBuilder {
			@Nullable private String index;
			@Nullable private String type;
			@Nullable private String id;
			@Nullable private Exception cause;
			@Nullable private Integer status;
			@Nullable private Long seqNo;
			@Nullable private Long term;
			@Nullable private Boolean aborted;
			@Nullable private ElasticsearchErrorCause elasticsearchErrorCause;

			private FailureBuilder() {}

			public Failure.FailureBuilder withIndex(String index) {
				this.index = index;
				return this;
			}

			public Failure.FailureBuilder withType(String type) {
				this.type = type;
				return this;
			}

			public Failure.FailureBuilder withId(String id) {
				this.id = id;
				return this;
			}

			public Failure.FailureBuilder withCause(@Nullable Exception cause) {
				this.cause = cause;
				return this;
			}

			public Failure.FailureBuilder withStatus(Integer status) {
				this.status = status;
				return this;
			}

			public Failure.FailureBuilder withSeqNo(Long seqNo) {
				this.seqNo = seqNo;
				return this;
			}

			public Failure.FailureBuilder withTerm(Long term) {
				this.term = term;
				return this;
			}

			public Failure.FailureBuilder withAborted(Boolean aborted) {
				this.aborted = aborted;
				return this;
			}

			public Failure.FailureBuilder withErrorCause(@Nullable ElasticsearchErrorCause elasticsearchErrorCause) {
				this.elasticsearchErrorCause = elasticsearchErrorCause;
				return this;
			}

			public Failure build() {
				return new Failure(index, type, id, cause, status, seqNo, term, aborted, elasticsearchErrorCause);
			}
		}
	}

	public static final class ReindexResponseBuilder {
		private long took;
		private boolean timedOut;
		private long total;
		private long created;
		private long updated;
		private long deleted;
		private long batches;
		private long versionConflicts;
		private long noops;
		private long bulkRetries;
		private long searchRetries;
		private long throttledMillis;
		private double requestsPerSecond;
		private long throttledUntilMillis;
		private List<Failure> failures = Collections.emptyList();

		private ReindexResponseBuilder() {}

		public ReindexResponseBuilder withTook(long took) {
			this.took = took;
			return this;
		}

		public ReindexResponseBuilder withTimedOut(boolean timedOut) {
			this.timedOut = timedOut;
			return this;
		}

		public ReindexResponseBuilder withTotal(long total) {
			this.total = total;
			return this;
		}

		public ReindexResponseBuilder withCreated(long created) {
			this.created = created;
			return this;
		}

		public ReindexResponseBuilder withUpdated(long updated) {
			this.updated = updated;
			return this;
		}

		public ReindexResponseBuilder withDeleted(long deleted) {
			this.deleted = deleted;
			return this;
		}

		public ReindexResponseBuilder withBatches(long batches) {
			this.batches = batches;
			return this;
		}

		public ReindexResponseBuilder withVersionConflicts(long versionConflicts) {
			this.versionConflicts = versionConflicts;
			return this;
		}

		public ReindexResponseBuilder withNoops(long noops) {
			this.noops = noops;
			return this;
		}

		public ReindexResponseBuilder withBulkRetries(long bulkRetries) {
			this.bulkRetries = bulkRetries;
			return this;
		}

		public ReindexResponseBuilder withSearchRetries(long searchRetries) {
			this.searchRetries = searchRetries;
			return this;
		}

		public ReindexResponseBuilder withThrottledMillis(long throttledMillis) {
			this.throttledMillis = throttledMillis;
			return this;
		}

		public ReindexResponseBuilder withRequestsPerSecond(double requestsPerSecond) {
			this.requestsPerSecond = requestsPerSecond;
			return this;
		}

		public ReindexResponseBuilder withThrottledUntilMillis(long throttledUntilMillis) {
			this.throttledUntilMillis = throttledUntilMillis;
			return this;
		}

		public ReindexResponseBuilder withFailures(List<Failure> failures) {
			this.failures = failures;
			return this;
		}

		public ReindexResponse build() {
			return new ReindexResponse(took, timedOut, total, created, updated, deleted, batches, versionConflicts, noops,
					bulkRetries, searchRetries, throttledMillis, requestsPerSecond, throttledUntilMillis, failures);
		}
	}
}
