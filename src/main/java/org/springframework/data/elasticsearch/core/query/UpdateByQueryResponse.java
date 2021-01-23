/*
 * Copyright 2021 the original author or authors.
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
import java.util.stream.Collectors;

import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.springframework.lang.Nullable;

/**
 * Response of an update by query operation.
 *
 * @author Farid Faoudi
 * @since 4.2
 */
public class UpdateByQueryResponse {

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

	private UpdateByQueryResponse(long took, boolean timedOut, long total, long updated, long deleted, int batches,
			long versionConflicts, long noops, long bulkRetries, long searchRetries, @Nullable String reasonCancelled,
			List<Failure> failures) {
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
	 * Create a new {@link UpdateByQueryResponseBuilder} to build {@link UpdateByQueryResponse}
	 *
	 * @return a new {@link UpdateByQueryResponseBuilder} to build {@link UpdateByQueryResponse}
	 */
	public static UpdateByQueryResponseBuilder builder() {
		return new UpdateByQueryResponseBuilder();
	}

	public static UpdateByQueryResponse of(BulkByScrollResponse bulkByScrollResponse) {
		final List<Failure> failures = bulkByScrollResponse.getBulkFailures() //
				.stream() //
				.map(Failure::of) //
				.collect(Collectors.toList()); //

		return UpdateByQueryResponse.builder() //
				.withTook(bulkByScrollResponse.getTook().getMillis()) //
				.withTimedOut(bulkByScrollResponse.isTimedOut()) //
				.withTotal(bulkByScrollResponse.getTotal()) //
				.withUpdated(bulkByScrollResponse.getUpdated()) //
				.withDeleted(bulkByScrollResponse.getDeleted()) //
				.withBatches(bulkByScrollResponse.getBatches()) //
				.withVersionConflicts(bulkByScrollResponse.getVersionConflicts()) //
				.withNoops(bulkByScrollResponse.getNoops()) //
				.withBulkRetries(bulkByScrollResponse.getBulkRetries()) //
				.withSearchRetries(bulkByScrollResponse.getSearchRetries()) //
				.withReasonCancelled(bulkByScrollResponse.getReasonCancelled()) //
				.withFailures(failures) //
				.build(); //
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

		private Failure(@Nullable String index, @Nullable String type, @Nullable String id, @Nullable Exception cause,
				@Nullable Integer status, @Nullable Long seqNo, @Nullable Long term, @Nullable Boolean aborted) {
			this.index = index;
			this.type = type;
			this.id = id;
			this.cause = cause;
			this.status = status;
			this.seqNo = seqNo;
			this.term = term;
			this.aborted = aborted;
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

		/**
		 * Create a new {@link FailureBuilder} to build {@link Failure}
		 *
		 * @return a new {@link FailureBuilder} to build {@link Failure}
		 */
		public static FailureBuilder builder() {
			return new FailureBuilder();
		}

		/**
		 * Create a new {@link Failure} from {@link BulkItemResponse.Failure}
		 *
		 * @param failure {@link BulkItemResponse.Failure} to translate
		 * @return a new {@link Failure}
		 */
		public static Failure of(BulkItemResponse.Failure failure) {
			return builder() //
					.withIndex(failure.getIndex()) //
					.withType(failure.getType()) //
					.withId(failure.getId()) //
					.withStatus(failure.getStatus().getStatus()) //
					.withAborted(failure.isAborted()) //
					.withCause(failure.getCause()) //
					.withSeqNo(failure.getSeqNo()) //
					.withTerm(failure.getTerm()) //
					.build(); //
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

			public Failure build() {
				return new Failure(index, type, id, cause, status, seqNo, term, aborted);
			}
		}
	}

	public static final class UpdateByQueryResponseBuilder {
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

		private UpdateByQueryResponseBuilder() {}

		public UpdateByQueryResponseBuilder withTook(long took) {
			this.took = took;
			return this;
		}

		public UpdateByQueryResponseBuilder withTimedOut(boolean timedOut) {
			this.timedOut = timedOut;
			return this;
		}

		public UpdateByQueryResponseBuilder withTotal(long total) {
			this.total = total;
			return this;
		}

		public UpdateByQueryResponseBuilder withUpdated(long updated) {
			this.updated = updated;
			return this;
		}

		public UpdateByQueryResponseBuilder withDeleted(long deleted) {
			this.deleted = deleted;
			return this;
		}

		public UpdateByQueryResponseBuilder withBatches(int batches) {
			this.batches = batches;
			return this;
		}

		public UpdateByQueryResponseBuilder withVersionConflicts(long versionConflicts) {
			this.versionConflicts = versionConflicts;
			return this;
		}

		public UpdateByQueryResponseBuilder withNoops(long noops) {
			this.noops = noops;
			return this;
		}

		public UpdateByQueryResponseBuilder withBulkRetries(long bulkRetries) {
			this.bulkRetries = bulkRetries;
			return this;
		}

		public UpdateByQueryResponseBuilder withSearchRetries(long searchRetries) {
			this.searchRetries = searchRetries;
			return this;
		}

		public UpdateByQueryResponseBuilder withReasonCancelled(String reasonCancelled) {
			this.reasonCancelled = reasonCancelled;
			return this;
		}

		public UpdateByQueryResponseBuilder withFailures(List<Failure> failures) {
			this.failures = failures;
			return this;
		}

		public UpdateByQueryResponse build() {
			return new UpdateByQueryResponse(took, timedOut, total, updated, deleted, batches, versionConflicts, noops,
					bulkRetries, searchRetries, reasonCancelled, failures);
		}
	}
}
