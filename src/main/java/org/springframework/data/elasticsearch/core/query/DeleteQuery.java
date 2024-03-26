/*
 * Copyright 2024 the original author or authors.
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
import java.util.EnumSet;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.query.Query.SearchType;
import org.springframework.data.elasticsearch.core.query.types.ConflictsType;
import org.springframework.data.elasticsearch.core.query.types.OperatorType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Defines a delete request.
 *
 * @author Aouichaoui Youssef
 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-delete-by-query.html">docs</a>
 * @since 5.3
 */
public class DeleteQuery {
	// For Lucene query
	/**
	 * Query in the Lucene query string syntax.
	 */
	@Nullable private final String q;

	/**
	 * If true, wildcard and prefix queries are analyzed. Defaults to false. This parameter can only be used when the
	 * lucene query {@code q} parameter is specified.
	 */
	@Nullable private final Boolean analyzeWildcard;

	/**
	 * Analyzer to use for the query string. This parameter can only be used when the lucene query {@code q} parameter is
	 * specified.
	 */
	@Nullable private final String analyzer;

	/**
	 * The default operator for a query string query: {@literal AND} or {@literal OR}. Defaults to {@literal OR}. This
	 * parameter can only be used when the lucene query {@code q} parameter is specified.
	 */
	@Nullable private final OperatorType defaultOperator;

	/**
	 * Field to be used as the default when no field prefix is specified in the query string. This parameter can only be
	 * used when the lucene query {@code q} parameter is specified.
	 * <p>
	 * e.g: {@code {"query":{"prefix":{"user.name":{"value":"es"}}}} }
	 */
	@Nullable private final String df;

	/**
	 * If a query contains errors related to the format of the data being entered, they will be disregarded unless
	 * specified otherwise. By default, this feature is turned off.
	 */
	@Nullable private final Boolean lenient;

	// For ES query

	/**
	 * An error will occur if the condition is {@code false} and any of the following are true: a wildcard expression, an
	 * index alias, or the {@literal _all value} only targets missing or closed indices. By default, this is set to
	 * {@code true}.
	 */
	@Nullable private final Boolean allowNoIndices;

	/**
	 * Define the types of conflicts that occur when a query encounters version conflicts: abort or proceed. Defaults to
	 * abort.
	 */
	@Nullable private final ConflictsType conflicts;

	/**
	 * Type of index that wildcard patterns can match. Defaults to {@literal open}.
	 */
	@Nullable private final EnumSet<IndicesOptions.WildcardStates> expandWildcards;

	/**
	 * An error occurs if it is directed at an index that is missing or closed when it is {@code false}. By default, this
	 * is set to {@code false}.
	 */
	@Nullable private final Boolean ignoreUnavailable;

	/**
	 * Maximum number of documents to process. Defaults to all documents.
	 */
	@Nullable private final Long maxDocs;

	/**
	 * Specifies the node or shard the operation should be performed on.
	 */
	@Nullable private final String preference;

	/**
	 * Use the request cache when it is {@code true}. By default, use the index-level setting.
	 */
	@Nullable private final Boolean requestCache;

	/**
	 * Refreshes all shards involved in the deleting by query after the request completes when it is {@code true}. By
	 * default, this is set to {@code false}.
	 */
	@Nullable private final Boolean refresh;

	/**
	 * Limited this request to a certain number of sub-requests per second. By default, this is set to {@code -1} (no
	 * throttle).
	 */
	@Nullable private final Float requestsPerSecond;

	/**
	 * Custom value used to route operations to a specific shard.
	 */
	@Nullable private final String routing;

	/**
	 * Period to retain the search context for scrolling.
	 */
	@Nullable private final Duration scroll;

	/**
	 * Size of the scroll request that powers the operation. By default, this is set to {@code 1000}.
	 */
	@Nullable private final Long scrollSize;

	/**
	 * The type of the search operation.
	 */
	@Nullable private final SearchType searchType;

	/**
	 * Explicit timeout for each search request. By default, this is set to no timeout.
	 */
	@Nullable private final Duration searchTimeout;

	/**
	 * The number of slices this task should be divided into. By default, this is set to {@code 1} meaning the task isn’t
	 * sliced into subtasks.
	 */
	@Nullable private final Integer slices;

	/**
	 * Sort search results in a specific order.
	 */
	@Nullable private final Sort sort;

	/**
	 * Specific {@code tag} of the request for logging and statistical purposes.
	 */
	@Nullable private final List<String> stats;

	/**
	 * The Maximum number of documents that can be collected for each shard. If a query exceeds this limit, Elasticsearch
	 * will stop the query.
	 */
	@Nullable private final Long terminateAfter;

	/**
	 * Period each deletion request waits for active shards. By default, this is set to {@code 1m} (one minute).
	 */
	@Nullable private final Duration timeout;

	/**
	 * Returns the document version as part of a hit.
	 */
	@Nullable private final Boolean version;

	// Body
	/**
	 * Query that specifies the documents to delete.
	 */
	private final Query query;

	public static Builder builder(Query query) {
		return new Builder(query);
	}

	private DeleteQuery(Builder builder) {
		this.q = builder.luceneQuery;
		this.analyzeWildcard = builder.analyzeWildcard;
		this.analyzer = builder.analyzer;
		this.defaultOperator = builder.defaultOperator;
		this.df = builder.defaultField;
		this.lenient = builder.lenient;

		this.allowNoIndices = builder.allowNoIndices;
		this.conflicts = builder.conflicts;
		this.expandWildcards = builder.expandWildcards;
		this.ignoreUnavailable = builder.ignoreUnavailable;
		this.maxDocs = builder.maxDocs;
		this.preference = builder.preference;
		this.requestCache = builder.requestCache;
		this.refresh = builder.refresh;
		this.requestsPerSecond = builder.requestsPerSecond;
		this.routing = builder.routing;
		this.scroll = builder.scrollTime;
		this.scrollSize = builder.scrollSize;
		this.searchType = builder.searchType;
		this.searchTimeout = builder.searchTimeout;
		this.slices = builder.slices;
		this.sort = builder.sort;
		this.stats = builder.stats;
		this.terminateAfter = builder.terminateAfter;
		this.timeout = builder.timeout;
		this.version = builder.version;

		this.query = builder.query;
	}

	@Nullable
	public String getQ() {
		return q;
	}

	@Nullable
	public Boolean getAnalyzeWildcard() {
		return analyzeWildcard;
	}

	@Nullable
	public String getAnalyzer() {
		return analyzer;
	}

	@Nullable
	public OperatorType getDefaultOperator() {
		return defaultOperator;
	}

	@Nullable
	public String getDf() {
		return df;
	}

	@Nullable
	public Boolean getLenient() {
		return lenient;
	}

	@Nullable
	public Boolean getAllowNoIndices() {
		return allowNoIndices;
	}

	@Nullable
	public ConflictsType getConflicts() {
		return conflicts;
	}

	@Nullable
	public EnumSet<IndicesOptions.WildcardStates> getExpandWildcards() {
		return expandWildcards;
	}

	@Nullable
	public Boolean getIgnoreUnavailable() {
		return ignoreUnavailable;
	}

	@Nullable
	public Long getMaxDocs() {
		return maxDocs;
	}

	@Nullable
	public String getPreference() {
		return preference;
	}

	@Nullable
	public Boolean getRequestCache() {
		return requestCache;
	}

	@Nullable
	public Boolean getRefresh() {
		return refresh;
	}

	@Nullable
	public Float getRequestsPerSecond() {
		return requestsPerSecond;
	}

	@Nullable
	public String getRouting() {
		return routing;
	}

	@Nullable
	public Duration getScroll() {
		return scroll;
	}

	@Nullable
	public Long getScrollSize() {
		return scrollSize;
	}

	@Nullable
	public SearchType getSearchType() {
		return searchType;
	}

	@Nullable
	public Duration getSearchTimeout() {
		return searchTimeout;
	}

	@Nullable
	public Integer getSlices() {
		return slices;
	}

	@Nullable
	public Sort getSort() {
		return sort;
	}

	@Nullable
	public List<String> getStats() {
		return stats;
	}

	@Nullable
	public Long getTerminateAfter() {
		return terminateAfter;
	}

	@Nullable
	public Duration getTimeout() {
		return timeout;
	}

	@Nullable
	public Boolean getVersion() {
		return version;
	}

	@Nullable
	public Query getQuery() {
		return query;
	}

	public static final class Builder {
		// For Lucene query
		@Nullable private String luceneQuery;
		@Nullable private Boolean analyzeWildcard;
		@Nullable private String analyzer;
		@Nullable private OperatorType defaultOperator;
		@Nullable private String defaultField;
		@Nullable private Boolean lenient;

		// For ES query
		@Nullable private Boolean allowNoIndices;
		@Nullable private ConflictsType conflicts;
		@Nullable private EnumSet<IndicesOptions.WildcardStates> expandWildcards;
		@Nullable private Boolean ignoreUnavailable;
		@Nullable private Long maxDocs;
		@Nullable private String preference;
		@Nullable private Boolean requestCache;
		@Nullable private Boolean refresh;
		@Nullable private Float requestsPerSecond;
		@Nullable private String routing;
		@Nullable private Duration scrollTime;
		@Nullable private Long scrollSize;
		@Nullable private SearchType searchType;
		@Nullable private Duration searchTimeout;
		@Nullable private Integer slices;
		@Nullable private Sort sort;
		@Nullable private List<String> stats;
		@Nullable private Long terminateAfter;
		@Nullable private Duration timeout;
		@Nullable private Boolean version;

		// Body
		private final Query query;

		private Builder(Query query) {
			Assert.notNull(query, "query must not be null");

			this.query = query;
		}

		/**
		 * Query in the Lucene query string syntax.
		 */
		public Builder withLuceneQuery(@Nullable String luceneQuery) {
			this.luceneQuery = luceneQuery;

			return this;
		}

		/**
		 * If true, wildcard and prefix queries are analyzed. Defaults to false. This parameter can only be used when the
		 * lucene query {@code q} parameter is specified.
		 */
		public Builder withAnalyzeWildcard(@Nullable Boolean analyzeWildcard) {
			this.analyzeWildcard = analyzeWildcard;

			return this;
		}

		/**
		 * Analyzer to use for the query string. This parameter can only be used when the lucene query {@code q} parameter
		 * is specified.
		 */
		public Builder withAnalyzer(@Nullable String analyzer) {
			this.analyzer = analyzer;

			return this;
		}

		/**
		 * The default operator for a query string query: {@literal AND} or {@literal OR}. Defaults to {@literal OR}. This
		 * parameter can only be used when the lucene query {@code q} parameter is specified.
		 */
		public Builder withDefaultOperator(@Nullable OperatorType defaultOperator) {
			this.defaultOperator = defaultOperator;

			return this;
		}

		/**
		 * Field to be used as the default when no field prefix is specified in the query string. This parameter can only be
		 * used when the lucene query {@code q} parameter is specified.
		 * <p>
		 * e.g: {@code {"query":{"prefix":{"user.name":{"value":"es"}}}} }
		 */
		public Builder withDefaultField(@Nullable String defaultField) {
			this.defaultField = defaultField;

			return this;
		}

		/**
		 * If a query contains errors related to the format of the data being entered, they will be disregarded unless
		 * specified otherwise. By default, this feature is turned off.
		 */
		public Builder withLenient(@Nullable Boolean lenient) {
			this.lenient = lenient;

			return this;
		}

		/**
		 * An error will occur if the condition is {@code false} and any of the following are true: a wildcard expression,
		 * an index alias, or the {@literal _all value} only targets missing or closed indices. By default, this is set to
		 * {@code true}.
		 */
		public Builder withAllowNoIndices(@Nullable Boolean allowNoIndices) {
			this.allowNoIndices = allowNoIndices;

			return this;
		}

		/**
		 * Define the types of conflicts that occur when a query encounters version conflicts: abort or proceed. Defaults to
		 * abort.
		 */
		public Builder withConflicts(@Nullable ConflictsType conflicts) {
			this.conflicts = conflicts;

			return this;
		}

		/**
		 * Type of index that wildcard patterns can match. Defaults to {@literal open}.
		 */
		public Builder setExpandWildcards(@Nullable EnumSet<IndicesOptions.WildcardStates> expandWildcards) {
			this.expandWildcards = expandWildcards;

			return this;
		}

		/**
		 * An error occurs if it is directed at an index that is missing or closed when it is {@code false}. By default,
		 * this is set to {@code false}.
		 */
		public Builder withIgnoreUnavailable(@Nullable Boolean ignoreUnavailable) {
			this.ignoreUnavailable = ignoreUnavailable;

			return this;
		}

		/**
		 * Maximum number of documents to process. Defaults to all documents.
		 */
		public Builder withMaxDocs(@Nullable Long maxDocs) {
			this.maxDocs = maxDocs;

			return this;
		}

		/**
		 * Specifies the node or shard the operation should be performed on.
		 */
		public Builder withPreference(@Nullable String preference) {
			this.preference = preference;

			return this;
		}

		/**
		 * Use the request cache when it is {@code true}. By default, use the index-level setting.
		 */
		public Builder withRequestCache(@Nullable Boolean requestCache) {
			this.requestCache = requestCache;

			return this;
		}

		/**
		 * Refreshes all shards involved in the deleting by query after the request completes when it is {@code true}. By
		 * default, this is set to {@code false}.
		 */
		public Builder withRefresh(@Nullable Boolean refresh) {
			this.refresh = refresh;

			return this;
		}

		/**
		 * Limited this request to a certain number of sub-requests per second. By default, this is set to {@code -1} (no
		 * throttle).
		 */
		public Builder withRequestsPerSecond(@Nullable Float requestsPerSecond) {
			this.requestsPerSecond = requestsPerSecond;

			return this;
		}

		/**
		 * Custom value used to route operations to a specific shard.
		 */
		public Builder withRouting(@Nullable String routing) {
			this.routing = routing;

			return this;
		}

		/**
		 * Period to retain the search context for scrolling.
		 */
		public Builder withScrollTime(@Nullable Duration scrollTime) {
			this.scrollTime = scrollTime;

			return this;
		}

		/**
		 * Size of the scroll request that powers the operation. By default, this is set to {@code 1000}.
		 */
		public Builder withScrollSize(@Nullable Long scrollSize) {
			this.scrollSize = scrollSize;

			return this;
		}

		/**
		 * The type of the search operation.
		 */
		public Builder withSearchType(@Nullable SearchType searchType) {
			this.searchType = searchType;

			return this;
		}

		/**
		 * Explicit timeout for each search request. By default, this is set to no timeout.
		 */
		public Builder withSearchTimeout(@Nullable Duration searchTimeout) {
			this.searchTimeout = searchTimeout;

			return this;
		}

		/**
		 * The number of slices this task should be divided into. By default, this is set to {@code 1} meaning the task
		 * isn’t sliced into subtasks.
		 */
		public Builder withSlices(@Nullable Integer slices) {
			this.slices = slices;

			return this;
		}

		/**
		 * Sort search results in a specific order.
		 */
		public Builder withSort(@Nullable Sort sort) {
			this.sort = sort;

			return this;
		}

		/**
		 * Specific {@code tag} of the request for logging and statistical purposes.
		 */
		public Builder withStats(@Nullable List<String> stats) {
			this.stats = stats;

			return this;
		}

		/**
		 * The Maximum number of documents that can be collected for each shard. If a query exceeds this limit,
		 * Elasticsearch will stop the query.
		 */
		public Builder withTerminateAfter(@Nullable Long terminateAfter) {
			this.terminateAfter = terminateAfter;

			return this;
		}

		/**
		 * Period each deletion request waits for active shards. By default, this is set to {@code 1m} (one minute).
		 */
		public Builder withTimeout(@Nullable Duration timeout) {
			this.timeout = timeout;

			return this;
		}

		/**
		 * Returns the document version as part of a hit.
		 */
		public Builder withVersion(@Nullable Boolean version) {
			this.version = version;

			return this;
		}

		public DeleteQuery build() {
			if (luceneQuery == null) {
				if (defaultField != null) {
					throw new IllegalArgumentException("When defining the df parameter, you must include the Lucene query.");
				}
				if (analyzer != null) {
					throw new IllegalArgumentException(
							"When defining the analyzer parameter, you must include the Lucene query.");
				}
				if (analyzeWildcard != null) {
					throw new IllegalArgumentException(
							"When defining the analyzeWildcard parameter, you must include the Lucene query.");
				}
				if (defaultOperator != null) {
					throw new IllegalArgumentException(
							"When defining the defaultOperator parameter, you must include the Lucene query.");
				}
				if (lenient != null) {
					throw new IllegalArgumentException("When defining the lenient parameter, you must include the Lucene query.");
				}
			}

			return new DeleteQuery(this);
		}
	}
}
