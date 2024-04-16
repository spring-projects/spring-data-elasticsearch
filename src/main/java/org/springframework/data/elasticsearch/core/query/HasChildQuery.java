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

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Defines a has_child request.
 *
 * @author Aouichaoui Youssef
 * @see <a href=
 *      "https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-has-child-query.html">docs</a>
 * @since 5.3
 */
public class HasChildQuery {
	/**
	 * Name of the child relationship mapped for the join field.
	 */
	private final String type;

	/**
	 * Query that specifies the documents to run on child documents of the {@link #type} field.
	 */
	private final Query query;

	/**
	 * Indicates whether to ignore an unmapped {@link #type} and not return any documents instead of an error. Default,
	 * this is set to {@code false}.
	 */
	@Nullable private final Boolean ignoreUnmapped;

	/**
	 * The Maximum number of child documents that match the {@link #query} allowed for a returned parent document. If the
	 * parent document exceeds this limit, it is excluded from the search results.
	 */
	@Nullable private final Integer maxChildren;

	/**
	 * Minimum number of child documents that match the query required to match the {@link #query} for a returned parent
	 * document. If the parent document does not meet this limit, it is excluded from the search results.
	 */
	@Nullable private final Integer minChildren;

	/**
	 * Indicates how scores for matching child documents affect the root parent document’s relevance score.
	 */
	@Nullable private final ScoreMode scoreMode;

	/**
	 * Obtaining nested objects and documents that have a parent-child relationship.
	 */
	@Nullable private final InnerHitsQuery innerHitsQuery;

	public static Builder builder(String type) {
		return new Builder(type);
	}

	private HasChildQuery(Builder builder) {
		this.type = builder.type;
		this.query = builder.query;
		this.innerHitsQuery = builder.innerHitsQuery;

		this.ignoreUnmapped = builder.ignoreUnmapped;

		this.maxChildren = builder.maxChildren;
		this.minChildren = builder.minChildren;

		this.scoreMode = builder.scoreMode;
	}

	public String getType() {
		return type;
	}

	public Query getQuery() {
		return query;
	}

	@Nullable
	public Boolean getIgnoreUnmapped() {
		return ignoreUnmapped;
	}

	@Nullable
	public Integer getMaxChildren() {
		return maxChildren;
	}

	@Nullable
	public Integer getMinChildren() {
		return minChildren;
	}

	@Nullable
	public ScoreMode getScoreMode() {
		return scoreMode;
	}

	@Nullable
	public InnerHitsQuery getInnerHitsQuery() {
		return innerHitsQuery;
	}

	public enum ScoreMode {
		Default, Avg, Max, Min, Sum
	}

	public static final class Builder {
		private final String type;
		private Query query;

		@Nullable private Boolean ignoreUnmapped;

		@Nullable private Integer maxChildren;
		@Nullable private Integer minChildren;

		@Nullable private ScoreMode scoreMode;

		@Nullable private InnerHitsQuery innerHitsQuery;

		private Builder(String type) {
			Assert.notNull(type, "type must not be null");

			this.type = type;
		}

		/**
		 * Query that specifies the documents to run on child documents of the {@link #type} field.
		 */
		public Builder withQuery(Query query) {
			this.query = query;

			return this;
		}

		/**
		 * Indicates whether to ignore an unmapped {@link #type} and not return any documents instead of an error. Default,
		 * this is set to {@code false}.
		 */
		public Builder withIgnoreUnmapped(@Nullable Boolean ignoreUnmapped) {
			this.ignoreUnmapped = ignoreUnmapped;

			return this;
		}

		/**
		 * The Maximum number of child documents that match the {@link #query} allowed for a returned parent document. If
		 * the parent document exceeds this limit, it is excluded from the search results.
		 */
		public Builder withMaxChildren(@Nullable Integer maxChildren) {
			this.maxChildren = maxChildren;

			return this;
		}

		/**
		 * Minimum number of child documents that match the query required to match the {@link #query} for a returned parent
		 * document. If the parent document does not meet this limit, it is excluded from the search results.
		 */
		public Builder withMinChildren(@Nullable Integer minChildren) {
			this.minChildren = minChildren;

			return this;
		}

		/**
		 * Indicates how scores for matching child documents affect the root parent document’s relevance score.
		 */
		public Builder withScoreMode(@Nullable ScoreMode scoreMode) {
			this.scoreMode = scoreMode;

			return this;
		}

		/**
		 * Obtaining nested objects and documents that have a parent-child relationship.
		 */
		public Builder withInnerHitsQuery(@Nullable InnerHitsQuery innerHitsQuery) {
			this.innerHitsQuery = innerHitsQuery;

			return this;
		}

		public HasChildQuery build() {
			Assert.notNull(query, "query must not be null.");

			return new HasChildQuery(this);
		}
	}
}
