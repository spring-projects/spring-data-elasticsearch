/*
 * Copyright 2024-2025 the original author or authors.
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
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Defines an SQL request.
 *
 * @author Aouichaoui Youssef
 * @see <a href= "https://www.elastic.co/guide/en/elasticsearch/reference/current/sql-search-api.html">docs</a>
 * @since 5.4
 */
public class SqlQuery {

	/**
	 * If true, returns partial results if there are shard request timeouts or shard failures.
	 * <p>
	 * Default, this is set to {@code false}.
	 */
	@Nullable private final Boolean allowPartialSearchResults;

	/**
	 * Default catalog/cluster for queries. If unspecified, the queries are executed on the data in the local cluster
	 * only.
	 */
	@Nullable private final String catalog;

	/**
	 * If true, returns results in a columnar format.
	 * <p>
	 * Default, this is set to {@code false}.
	 */
	@Nullable private final Boolean columnar;

	/**
	 * To retrieve a set of paginated results, ignore other request body parameters when specifying a cursor and using the
	 * {@link #columnar} and {@link #timeZone} parameters.
	 */
	@Nullable private final String cursor;

	/**
	 * Maximum number of rows to return in the response.
	 * <p>
	 * Default, this is set to {@code 1000}.
	 */
	@Nullable private final Integer fetchSize;

	/**
	 * If false, the API returns an error for fields containing array values.
	 * <p>
	 * Default, this is set to {@code false}.
	 */
	@Nullable private final Boolean fieldMultiValueLeniency;

	/**
	 * Query that filter documents for the SQL search.
	 */
	@Nullable private final Query filter;

	/**
	 * If true, the search can run on frozen indices.
	 * <p>
	 * Default, this is set to {@code false}.
	 */
	@Nullable private final Boolean indexIncludeFrozen;

	/**
	 * Retention period for an async or saved synchronous search.
	 * <p>
	 * Default, this is set to {@code 5 days}.
	 */
	@Nullable private final Duration keepAlive;

	/**
	 * If it is true, it will store synchronous searches when the {@link #waitForCompletionTimeout} parameter is
	 * specified.
	 */
	@Nullable private final Boolean keepOnCompletion;

	/**
	 * Minimum retention period for the scroll cursor.
	 * <p>
	 * Default, this is set to {@code 45 seconds}.
	 */
	@Nullable private final Duration pageTimeout;

	/**
	 * Timeout before the request fails.
	 * <p>
	 * Default, this is set to {@code 90 seconds}.
	 */
	@Nullable private final Duration requestTimeout;

	/**
	 * Values for parameters in the query.
	 */
	@Nullable private final List<Object> params;

	/**
	 * SQL query to run.
	 */
	private final String query;

	/**
	 * Time zone ID for the search.
	 * <p>
	 * Default, this is set to {@code UTC}.
	 */
	@Nullable private final TimeZone timeZone;

	/**
	 * Period to wait for complete results.
	 * <p>
	 * Default, this is set to no timeout.
	 */
	@Nullable private final Duration waitForCompletionTimeout;

	private SqlQuery(Builder builder) {
		this.allowPartialSearchResults = builder.allowPartialSearchResults;

		this.catalog = builder.catalog;
		this.columnar = builder.columnar;
		this.cursor = builder.cursor;

		this.fetchSize = builder.fetchSize;
		this.fieldMultiValueLeniency = builder.fieldMultiValueLeniency;

		this.filter = builder.filter;

		this.indexIncludeFrozen = builder.indexIncludeFrozen;
		this.keepAlive = builder.keepAlive;
		this.keepOnCompletion = builder.keepOnCompletion;

		this.pageTimeout = builder.pageTimeout;
		this.requestTimeout = builder.requestTimeout;

		this.params = builder.params;
		this.query = builder.query;

		this.timeZone = builder.timeZone;
		this.waitForCompletionTimeout = builder.waitForCompletionTimeout;
	}

	@Nullable
	public Boolean getAllowPartialSearchResults() {
		return allowPartialSearchResults;
	}

	@Nullable
	public String getCatalog() {
		return catalog;
	}

	@Nullable
	public Boolean getColumnar() {
		return columnar;
	}

	@Nullable
	public String getCursor() {
		return cursor;
	}

	@Nullable
	public Integer getFetchSize() {
		return fetchSize;
	}

	@Nullable
	public Boolean getFieldMultiValueLeniency() {
		return fieldMultiValueLeniency;
	}

	@Nullable
	public Query getFilter() {
		return filter;
	}

	@Nullable
	public Boolean getIndexIncludeFrozen() {
		return indexIncludeFrozen;
	}

	@Nullable
	public Duration getKeepAlive() {
		return keepAlive;
	}

	@Nullable
	public Boolean getKeepOnCompletion() {
		return keepOnCompletion;
	}

	@Nullable
	public Duration getPageTimeout() {
		return pageTimeout;
	}

	@Nullable
	public Duration getRequestTimeout() {
		return requestTimeout;
	}

	@Nullable
	public List<Object> getParams() {
		return params;
	}

	public String getQuery() {
		return query;
	}

	@Nullable
	public TimeZone getTimeZone() {
		return timeZone;
	}

	@Nullable
	public Duration getWaitForCompletionTimeout() {
		return waitForCompletionTimeout;
	}

	public static Builder builder(String query) {
		return new Builder(query);
	}

	public static class Builder {
		@Nullable private Boolean allowPartialSearchResults;

		@Nullable private String catalog;
		@Nullable private Boolean columnar;
		@Nullable private String cursor;

		@Nullable private Integer fetchSize;
		@Nullable private Boolean fieldMultiValueLeniency;

		@Nullable private Query filter;

		@Nullable private Boolean indexIncludeFrozen;

		@Nullable private Duration keepAlive;
		@Nullable private Boolean keepOnCompletion;

		@Nullable private Duration pageTimeout;
		@Nullable private Duration requestTimeout;

		@Nullable private List<Object> params;
		private final String query;

		@Nullable private TimeZone timeZone;
		@Nullable private Duration waitForCompletionTimeout;

		private Builder(String query) {
			Assert.notNull(query, "query must not be null");

			this.query = query;
		}

		/**
		 * If true, returns partial results if there are shard request timeouts or shard failures.
		 */
		public Builder withAllowPartialSearchResults(Boolean allowPartialSearchResults) {
			this.allowPartialSearchResults = allowPartialSearchResults;

			return this;
		}

		/**
		 * Default catalog/cluster for queries. If unspecified, the queries are executed on the data in the local cluster
		 * only.
		 */
		public Builder withCatalog(String catalog) {
			this.catalog = catalog;

			return this;
		}

		/**
		 * If true, returns results in a columnar format.
		 */
		public Builder withColumnar(Boolean columnar) {
			this.columnar = columnar;

			return this;
		}

		/**
		 * To retrieve a set of paginated results, ignore other request body parameters when specifying a cursor and using
		 * the {@link #columnar} and {@link #timeZone} parameters.
		 */
		public Builder withCursor(String cursor) {
			this.cursor = cursor;

			return this;
		}

		/**
		 * Maximum number of rows to return in the response.
		 */
		public Builder withFetchSize(Integer fetchSize) {
			this.fetchSize = fetchSize;

			return this;
		}

		/**
		 * If false, the API returns an error for fields containing array values.
		 */
		public Builder withFieldMultiValueLeniency(Boolean fieldMultiValueLeniency) {
			this.fieldMultiValueLeniency = fieldMultiValueLeniency;

			return this;
		}

		/**
		 * Query that filter documents for the SQL search.
		 */
		public Builder setFilter(Query filter) {
			this.filter = filter;

			return this;
		}

		/**
		 * If true, the search can run on frozen indices.
		 */
		public Builder withIndexIncludeFrozen(Boolean indexIncludeFrozen) {
			this.indexIncludeFrozen = indexIncludeFrozen;

			return this;
		}

		/**
		 * Retention period for an async or saved synchronous search.
		 */
		public Builder setKeepAlive(Duration keepAlive) {
			this.keepAlive = keepAlive;

			return this;
		}

		/**
		 * If it is true, it will store synchronous searches when the {@link #waitForCompletionTimeout} parameter is
		 * specified.
		 */
		public Builder withKeepOnCompletion(Boolean keepOnCompletion) {
			this.keepOnCompletion = keepOnCompletion;

			return this;
		}

		/**
		 * Minimum retention period for the scroll cursor.
		 */
		public Builder withPageTimeout(Duration pageTimeout) {
			this.pageTimeout = pageTimeout;

			return this;
		}

		/**
		 * Timeout before the request fails.
		 */
		public Builder withRequestTimeout(Duration requestTimeout) {
			this.requestTimeout = requestTimeout;

			return this;
		}

		/**
		 * Values for parameters in the query.
		 */
		public Builder withParams(List<Object> params) {
			this.params = params;

			return this;
		}

		/**
		 * Value for parameters in the query.
		 */
		public Builder withParam(Object param) {
			if (this.params == null) {
				this.params = new ArrayList<>();
			}
			this.params.add(param);

			return this;
		}

		/**
		 * Time zone ID for the search.
		 */
		public Builder withTimeZone(TimeZone timeZone) {
			this.timeZone = timeZone;

			return this;
		}

		/**
		 * Period to wait for complete results.
		 */
		public Builder withWaitForCompletionTimeout(Duration waitForCompletionTimeout) {
			this.waitForCompletionTimeout = waitForCompletionTimeout;

			return this;
		}

		public SqlQuery build() {
			return new SqlQuery(this);
		}
	}
}
