/*
 * Copyright 2013-2024 the original author or authors.
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

import co.elastic.clients.elasticsearch._types.Conflicts;
import co.elastic.clients.elasticsearch._types.Slices;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import org.springframework.lang.Nullable;

import java.time.Duration;
import java.util.List;

/**
 * Defines a delete request.
 *
 * @author Aouichaoui Youssef
 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-delete-by-query.html">docs</a>
 */
public class DeleteQuery extends BaseQuery {
    // For Lucene query
    /**
     * Query in the Lucene query string syntax.
     */
    @Nullable
    private final String q;

    /**
     * If true, wildcard and prefix queries are analyzed. Defaults to false.
     * This parameter can only be used when the lucene query {@code q} parameter is specified.
     */
    @Nullable
    private final Boolean analyzeWildcard;

    /**
     * Analyzer to use for the query string.
     * This parameter can only be used when the lucene query {@code q} parameter is specified.
     */
    @Nullable
    private final String analyzer;

    /**
     * The default operator for a query string query: {@literal AND} or {@literal OR}. Defaults to {@literal OR}.
     * This parameter can only be used when the lucene query {@code q} parameter is specified.
     */
    @Nullable
    private final Operator defaultOperator;

    /**
     * Field to be used as the default when no field prefix is specified in the query string.
     * This parameter can only be used when the lucene query {@code q} parameter is specified.
     * <p>
     * e.g: {@code {"query":{"prefix":{"user.name":{"value":"es"}}}} }
     */
    @Nullable
    private final String df;

    /**
     * If a query contains errors related to the format of the data being entered, they will be disregarded unless specified otherwise.
     * By default, this feature is turned off.
     */
    @Nullable
    private final Boolean lenient;

    // For ES query
    /**
     * Define the types of conflicts that occur when a query encounters version conflicts: abort or proceed.
     * Defaults to abort.
     */
    @Nullable
    private final Conflicts conflicts;

    /**
     * An error occurs if it is directed at an index that is missing or closed when it is {@code false}.
     * By default, this is set to {@code false}.
     */
    @Nullable
    private final Boolean ignoreUnavailable;

    /**
     * Limited this request to a certain number of sub-requests per second.
     * By default, this is set to {@code -1} (no throttle).
     */
    @Nullable
    private final Float requestsPerSecond;

    /**
     * Size of the scroll request that powers the operation.
     * By default, this is set to {@code 1000}.
     */
    @Nullable
    private final Long scrollSize;

    /**
     * Explicit timeout for each search request.
     * By default, this is set to no timeout.
     */
    @Nullable
    private final Duration searchTimeout;

    /**
     * The number of slices this task should be divided into.
     * By default, this is set to {@code 1} meaning the task isn’t sliced into subtasks.
     */
    @Nullable
    private Slices slices;

    /**
     * Specific {@code tag} of the request for logging and statistical purposes.
     */
    @Nullable
    private final List<String> stats;

    /**
     * The Maximum number of documents that can be collected for each shard.
     * If a query exceeds this limit, Elasticsearch will stop the query.
     */
    @Nullable
    private final Long terminateAfter;

    /**
     * Returns the document version as part of a hit.
     */
    @Nullable
    private final Boolean version;

    // Body
    /**
     * Query that specifies the documents to delete.
     */
    @Nullable
    private final Query query;

    public static Builder builder() {
        return new Builder();
    }

    public DeleteQuery(Builder builder) {
        super(builder);

        this.q = builder.luceneQuery;
        this.analyzeWildcard = builder.analyzeWildcard;
        this.analyzer = builder.analyzer;
        this.defaultOperator = builder.defaultOperator;
        this.df = builder.defaultField;
        this.lenient = builder.lenient;

        this.conflicts = builder.conflicts;
        this.ignoreUnavailable = builder.ignoreUnavailable;
        this.requestsPerSecond = builder.requestsPerSecond;
        this.scrollSize = builder.scrollSize;
        this.searchTimeout = builder.searchTimeout;
        if (builder.slices != null) {
            this.slices = Slices.of(sb -> sb.value(builder.slices));
        }
        this.stats = builder.stats;
        this.terminateAfter = builder.terminateAfter;
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
    public Operator getDefaultOperator() {
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
    public Conflicts getConflicts() {
        return conflicts;
    }

    @Nullable
    public Boolean getIgnoreUnavailable() {
        return ignoreUnavailable;
    }

    @Nullable
    public Float getRequestsPerSecond() {
        return requestsPerSecond;
    }

    @Nullable
    public Long getScrollSize() {
        return scrollSize;
    }

    @Nullable
    public Duration getSearchTimeout() {
        return searchTimeout;
    }

    @Nullable
    public Slices getSlices() {
        return slices;
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
    public Boolean getVersion() {
        return version;
    }

    @Nullable
    public Query getQuery() {
        return query;
    }

    public static final class Builder extends BaseQueryBuilder<DeleteQuery, DeleteQuery.Builder> {
        // For Lucene query
        @Nullable
        private String luceneQuery;
        @Nullable
        private Boolean analyzeWildcard;
        @Nullable
        private String analyzer;
        @Nullable
        private Operator defaultOperator;
        @Nullable
        private String defaultField;
        @Nullable
        private Boolean lenient;

        // For ES query
        @Nullable
        private Conflicts conflicts;
        @Nullable
        private Boolean ignoreUnavailable;
        @Nullable
        private Float requestsPerSecond;
        @Nullable
        private Long scrollSize;
        @Nullable
        private Duration searchTimeout;
        @Nullable
        private Integer slices;
        @Nullable
        private List<String> stats;
        @Nullable
        private Long terminateAfter;
        @Nullable
        private Boolean version;

        // Body
        @Nullable
        private Query query;

        private Builder() {
        }

        /**
         * Query in the Lucene query string syntax.
         */
        public Builder withLuceneQuery(@Nullable String luceneQuery) {
            this.luceneQuery = luceneQuery;

            return this;
        }

        /**
         * If true, wildcard and prefix queries are analyzed. Defaults to false.
         * This parameter can only be used when the lucene query {@code q} parameter is specified.
         */
        public Builder withAnalyzeWildcard(@Nullable Boolean analyzeWildcard) {
            this.analyzeWildcard = analyzeWildcard;

            return this;
        }

        /**
         * Analyzer to use for the query string.
         * This parameter can only be used when the lucene query {@code q} parameter is specified.
         */
        public Builder withAnalyzer(@Nullable String analyzer) {
            this.analyzer = analyzer;

            return this;
        }

        /**
         * The default operator for a query string query: {@literal AND} or {@literal OR}. Defaults to {@literal OR}.
         * This parameter can only be used when the lucene query {@code q} parameter is specified.
         */
        public Builder withDefaultOperator(@Nullable Operator defaultOperator) {
            this.defaultOperator = defaultOperator;

            return this;
        }

        /**
         * Field to be used as the default when no field prefix is specified in the query string.
         * This parameter can only be used when the lucene query {@code q} parameter is specified.
         * <p>
         * e.g: {@code {"query":{"prefix":{"user.name":{"value":"es"}}}} }
         */
        public Builder withDefaultField(@Nullable String defaultField) {
            this.defaultField = defaultField;

            return this;
        }

        /**
         * If a query contains errors related to the format of the data being entered, they will be disregarded unless specified otherwise.
         * By default, this feature is turned off.
         */
        public Builder withLenient(@Nullable Boolean lenient) {
            this.lenient = lenient;

            return this;
        }

        /**
         * Define the types of conflicts that occur when a query encounters version conflicts: abort or proceed.
         * Defaults to abort.
         */
        public Builder withConflicts(@Nullable Conflicts conflicts) {
            this.conflicts = conflicts;

            return this;
        }

        /**
         * An error occurs if it is directed at an index that is missing or closed when it is {@code false}.
         * By default, this is set to {@code false}.
         */
        public Builder withIgnoreUnavailable(@Nullable Boolean ignoreUnavailable) {
            this.ignoreUnavailable = ignoreUnavailable;

            return this;
        }

        /**
         * Limited this request to a certain number of sub-requests per second.
         * By default, this is set to {@code -1} (no throttle).
         */
        public Builder withRequestsPerSecond(@Nullable Float requestsPerSecond) {
            this.requestsPerSecond = requestsPerSecond;

            return this;
        }

        /**
         * Size of the scroll request that powers the operation.
         * By default, this is set to {@code 1000}.
         */
        public Builder withScrollSize(@Nullable Long scrollSize) {
            this.scrollSize = scrollSize;

            return this;
        }

        /**
         * Explicit timeout for each search request.
         * By default, this is set to no timeout.
         */
        public Builder withSearchTimeout(@Nullable Duration searchTimeout) {
            this.searchTimeout = searchTimeout;

            return this;
        }

        /**
         * The number of slices this task should be divided into.
         * By default, this is set to {@code 1} meaning the task isn’t sliced into subtasks.
         */
        public Builder withSlices(@Nullable Integer slices) {
            this.slices = slices;

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
         * The Maximum number of documents that can be collected for each shard.
         * If a query exceeds this limit, Elasticsearch will stop the query.
         */
        public Builder withTerminateAfter(@Nullable Long terminateAfter) {
            this.terminateAfter = terminateAfter;

            return this;
        }

        /**
         * Returns the document version as part of a hit.
         */
        public Builder withVersion(@Nullable Boolean version) {
            this.version = version;

            return this;
        }

        /**
         * Query that specifies the documents to delete.
         */
        public Builder withQuery(Query query) {
            this.query = query;

            return this;
        }

        @Override
        public DeleteQuery build() {
            if (luceneQuery == null) {
                if (defaultField != null) {
                    throw new IllegalArgumentException("When defining the df parameter, you must include the Lucene query.");
                }
                if (analyzer != null) {
                    throw new IllegalArgumentException("When defining the analyzer parameter, you must include the Lucene query.");
                }
                if (analyzeWildcard != null) {
                    throw new IllegalArgumentException("When defining the analyzeWildcard parameter, you must include the Lucene query.");
                }
                if (defaultOperator != null) {
                    throw new IllegalArgumentException("When defining the defaultOperator parameter, you must include the Lucene query.");
                }
                if (lenient != null) {
                    throw new IllegalArgumentException("When defining the lenient parameter, you must include the Lucene query.");
                }
            }

            return new DeleteQuery(this);
        }
    }
}
