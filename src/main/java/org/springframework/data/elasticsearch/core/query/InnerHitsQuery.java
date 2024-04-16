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

/**
 * Defines an inner_hits request.
 *
 * @author Aouichaoui Youssef
 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/inner-hits.html">docs</a>
 * @since 5.3
 */
public class InnerHitsQuery {
	/**
	 * The name to be used for the particular inner hit definition in the response.
	 */
	@Nullable private final String name;

	/**
	 * The maximum number of hits to return.
	 */
	@Nullable private final Integer size;

	/**
	 * The offset from where the first hit to fetch.
	 */
	@Nullable private final Integer from;

	public static Builder builder() {
		return new Builder();
	}

	private InnerHitsQuery(Builder builder) {
		this.name = builder.name;

		this.from = builder.from;
		this.size = builder.size;
	}

	@Nullable
	public String getName() {
		return name;
	}

	@Nullable
	public Integer getSize() {
		return size;
	}

	@Nullable
	public Integer getFrom() {
		return from;
	}

	public static final class Builder {
		@Nullable private String name;
		@Nullable private Integer size;
		@Nullable private Integer from;

		private Builder() {}

		/**
		 * The name to be used for the particular inner hit definition in the response.
		 */
		public Builder withName(@Nullable String name) {
			this.name = name;

			return this;
		}

		/**
		 * The maximum number of hits to return.
		 */
		public Builder withSize(@Nullable Integer size) {
			this.size = size;

			return this;
		}

		/**
		 * The offset from where the first hit to fetch.
		 */
		public Builder withFrom(@Nullable Integer from) {
			this.from = from;

			return this;
		}

		public InnerHitsQuery build() {
			return new InnerHitsQuery(this);
		}
	}
}
