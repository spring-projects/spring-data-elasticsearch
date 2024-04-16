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
 * Defines a has_parent request.
 *
 * @author Aouichaoui Youssef
 * @see <a href=
 *      "https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-has-parent-query.html">docs</a>
 * @since 5.3
 */
public class HasParentQuery {
	/**
	 * Name of the parent relationship mapped for the join field.
	 */
	private final String parentType;

	/**
	 * Query that specifies the documents to run on parent documents of the {@link #parentType} field.
	 */
	private final Query query;

	/**
	 * Indicates whether the relevance score of a matching parent document is aggregated into its child documents.
	 * Default, this is set to {@code false}.
	 */
	@Nullable private final Boolean score;

	/**
	 * Indicates whether to ignore an unmapped {@link #parentType} and not return any documents instead of an error.
	 * Default, this is set to {@code false}.
	 */
	@Nullable private final Boolean ignoreUnmapped;

	/**
	 * Obtaining nested objects and documents that have a parent-child relationship.
	 */
	@Nullable private final InnerHitsQuery innerHitsQuery;

	public static Builder builder(String parentType) {
		return new Builder(parentType);
	}

	private HasParentQuery(Builder builder) {
		this.parentType = builder.parentType;
		this.query = builder.query;
		this.innerHitsQuery = builder.innerHitsQuery;

		this.score = builder.score;
		this.ignoreUnmapped = builder.ignoreUnmapped;
	}

	public String getParentType() {
		return parentType;
	}

	public Query getQuery() {
		return query;
	}

	@Nullable
	public Boolean getScore() {
		return score;
	}

	@Nullable
	public Boolean getIgnoreUnmapped() {
		return ignoreUnmapped;
	}

	@Nullable
	public InnerHitsQuery getInnerHitsQuery() {
		return innerHitsQuery;
	}

	public static class Builder {
		private final String parentType;
		private Query query;

		@Nullable private Boolean score;
		@Nullable private Boolean ignoreUnmapped;

		@Nullable private InnerHitsQuery innerHitsQuery;

		private Builder(String parentType) {
			Assert.notNull(parentType, "parent_type must not be null.");

			this.parentType = parentType;
		}

		public Builder withQuery(Query query) {
			this.query = query;

			return this;
		}

		public Builder withScore(@Nullable Boolean score) {
			this.score = score;

			return this;
		}

		public Builder withIgnoreUnmapped(@Nullable Boolean ignoreUnmapped) {
			this.ignoreUnmapped = ignoreUnmapped;

			return this;
		}

		/**
		 * Obtaining nested objects and documents that have a parent-child relationship.
		 */
		public Builder withInnerHitsQuery(@Nullable InnerHitsQuery innerHitsQuery) {
			this.innerHitsQuery = innerHitsQuery;

			return this;
		}

		public HasParentQuery build() {
			Assert.notNull(query, "query must not be null.");

			return new HasParentQuery(this);
		}
	}
}
