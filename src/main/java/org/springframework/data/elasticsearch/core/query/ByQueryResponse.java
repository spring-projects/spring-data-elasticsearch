/*
 * Copyright 2021-2024 the original author or authors.
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

import java.util.Collections;
import java.util.List;

import org.springframework.data.elasticsearch.ElasticsearchErrorCause;
import org.springframework.lang.Nullable;

/**
 * Response of an update by query operation.
 *
 * @author Farid Faoudi
 * @since 4.2
 */
public class ByQueryResponse {

	private final long took;
	private final boolean timedOut;
	private final long total;
	private final long updated;
	private final long deleted;
	private final int batches;
	private final long versionConflicts;
	private final long noops;
	private final long bulkRetries;
	private final long searchRetries;
	@Nullable private final String reasonCancelled;
	private final List<Failure> failures;
	private final List<SearchFailure> searchFailures;

	private ByQueryResponse(long took, boolean timedOut, long total, long updated, long deleted, int batches,
			long versionConflicts, long noops, long bulkRetries, long searchRetries, @Nullable String reasonCancelled,
			List<Failure> failures, List<SearchFailure> searchFailures) {
		this.took = took;
		this.timedOut = timedOut;
		this.total = total;
		this.updated = updated;
		this.deleted = deleted;
		this.batches = batches;
		this.versionConflicts = versionConflicts;
		this.noops = noops;
		this.bulkRetries = bulkRetries;
		this.searchRetries = searchRetries;
		this.reasonCancelled = reasonCancelled;
		this.failures = failures;
		this.searchFailures = searchFailures;
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
	public boolean getTimedOut() {
		return timedOut;
	}

	/**
	 * The number of documents that were successfully processed.
	 */
	public long getTotal() {
		return total;
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
	public int getBatches() {
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
	 * The reason that the request was canceled or null if it hasn't been.
	 */
	@Nullable
	public String getReasonCancelled() {
		return reasonCancelled;
	}

	/**
	 * All of the bulk failures. Version conflicts are only included if the request sets abortOnVersionConflict to true
	 * (the default).
	 */
	public List<Failure> getFailures() {
		return failures;
	}

	/**
	 * Failures during search phase
	 */
	public List<SearchFailure> getSearchFailures() {
		return searchFailures;
	}

	/**
	 * Create a new {@link ByQueryResponseBuilder} to build {@link ByQueryResponse}
	 *
	 * @return a new {@link ByQueryResponseBuilder} to build {@link ByQueryResponse}
	 */
	public static ByQueryResponseBuilder builder() {
		return new ByQueryResponseBuilder();
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
		 * Create a new {@link FailureBuilder} to build {@link Failure}
		 *
		 * @return a new {@link FailureBuilder} to build {@link Failure}
		 */

		public static FailureBuilder builder() {
			return new FailureBuilder();
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

			public FailureBuilder withIndex(String index) {
				this.index = index;
				return this;
			}

			public FailureBuilder withType(String type) {
				this.type = type;
				return this;
			}

			public FailureBuilder withId(String id) {
				this.id = id;
				return this;
			}

			public FailureBuilder withCause(Exception cause) {
				this.cause = cause;
				return this;
			}

			public FailureBuilder withStatus(Integer status) {
				this.status = status;
				return this;
			}

			public FailureBuilder withSeqNo(Long seqNo) {
				this.seqNo = seqNo;
				return this;
			}

			public FailureBuilder withTerm(Long term) {
				this.term = term;
				return this;
			}

			public FailureBuilder withAborted(Boolean aborted) {
				this.aborted = aborted;
				return this;
			}

			public FailureBuilder withErrorCause(ElasticsearchErrorCause elasticsearchErrorCause) {
				this.elasticsearchErrorCause = elasticsearchErrorCause;
				return this;
			}

			public Failure build() {
				return new Failure(index, type, id, cause, status, seqNo, term, aborted, elasticsearchErrorCause);
			}
		}
	}

	public static class SearchFailure {
		private final Throwable reason;
		@Nullable private final Integer status;
		@Nullable private final String index;
		@Nullable private final Integer shardId;
		@Nullable private final String nodeId;

		private SearchFailure(Throwable reason, @Nullable Integer status, @Nullable String index, @Nullable Integer shardId,
				@Nullable String nodeId) {
			this.reason = reason;
			this.status = status;
			this.index = index;
			this.shardId = shardId;
			this.nodeId = nodeId;
		}

		public Throwable getReason() {
			return reason;
		}

		@Nullable
		public Integer getStatus() {
			return status;
		}

		@Nullable
		public String getIndex() {
			return index;
		}

		@Nullable
		public Integer getShardId() {
			return shardId;
		}

		@Nullable
		public String getNodeId() {
			return nodeId;
		}

		/**
		 * Create a new {@link SearchFailureBuilder} to build {@link SearchFailure}
		 *
		 * @return a new {@link SearchFailureBuilder} to build {@link SearchFailure}
		 */
		public static SearchFailureBuilder builder() {
			return new SearchFailureBuilder();
		}

		/**
		 * Builder for {@link SearchFailure}
		 */
		public static final class SearchFailureBuilder {
			private Throwable reason;
			@Nullable private Integer status;
			@Nullable private String index;
			@Nullable private Integer shardId;
			@Nullable private String nodeId;

			private SearchFailureBuilder() {}

			public SearchFailureBuilder withReason(Throwable reason) {
				this.reason = reason;
				return this;
			}

			public SearchFailureBuilder withStatus(Integer status) {
				this.status = status;
				return this;
			}

			public SearchFailureBuilder withIndex(String index) {
				this.index = index;
				return this;
			}

			public SearchFailureBuilder withShardId(Integer shardId) {
				this.shardId = shardId;
				return this;
			}

			public SearchFailureBuilder withNodeId(String nodeId) {
				this.nodeId = nodeId;
				return this;
			}

			public SearchFailure build() {
				return new SearchFailure(reason, status, index, shardId, nodeId);
			}
		}

	}

	public static final class ByQueryResponseBuilder {
		private long took;
		private boolean timedOut;
		private long total;
		private long updated;
		private long deleted;
		private int batches;
		private long versionConflicts;
		private long noops;
		private long bulkRetries;
		private long searchRetries;
		@Nullable private String reasonCancelled;
		private List<Failure> failures = Collections.emptyList();
		private List<SearchFailure> searchFailures = Collections.emptyList();

		private ByQueryResponseBuilder() {}

		public ByQueryResponseBuilder withTook(long took) {
			this.took = took;
			return this;
		}

		public ByQueryResponseBuilder withTimedOut(boolean timedOut) {
			this.timedOut = timedOut;
			return this;
		}

		public ByQueryResponseBuilder withTotal(long total) {
			this.total = total;
			return this;
		}

		public ByQueryResponseBuilder withUpdated(long updated) {
			this.updated = updated;
			return this;
		}

		public ByQueryResponseBuilder withDeleted(long deleted) {
			this.deleted = deleted;
			return this;
		}

		public ByQueryResponseBuilder withBatches(int batches) {
			this.batches = batches;
			return this;
		}

		public ByQueryResponseBuilder withVersionConflicts(long versionConflicts) {
			this.versionConflicts = versionConflicts;
			return this;
		}

		public ByQueryResponseBuilder withNoops(long noops) {
			this.noops = noops;
			return this;
		}

		public ByQueryResponseBuilder withBulkRetries(long bulkRetries) {
			this.bulkRetries = bulkRetries;
			return this;
		}

		public ByQueryResponseBuilder withSearchRetries(long searchRetries) {
			this.searchRetries = searchRetries;
			return this;
		}

		public ByQueryResponseBuilder withReasonCancelled(String reasonCancelled) {
			this.reasonCancelled = reasonCancelled;
			return this;
		}

		public ByQueryResponseBuilder withFailures(List<Failure> failures) {
			this.failures = failures;
			return this;
		}

		public ByQueryResponseBuilder withSearchFailure(List<SearchFailure> searchFailures) {
			this.searchFailures = searchFailures;
			return this;
		}

		public ByQueryResponse build() {
			return new ByQueryResponse(took, timedOut, total, updated, deleted, batches, versionConflicts, noops, bulkRetries,
					searchRetries, reasonCancelled, failures, searchFailures);
		}
	}
}
