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
package org.springframework.data.elasticsearch.core.mapping;

import java.util.Objects;

import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Immutable Value object encapsulating index alias(es).
 *
 * @author Youssef Aouichaoui
 * @since 5.4
 */
public class Alias {
	/**
	 * Alias name for the index.
	 */
	private final String alias;

	/**
	 * Query used to limit documents the alias can access.
	 */
	@Nullable private final Query filter;

	/**
	 * Used to route indexing operations to a specific shard.
	 */
	@Nullable private final String indexRouting;

	/**
	 * Used to route search operations to a specific shard.
	 */
	@Nullable private final String searchRouting;

	/**
	 * Used to route indexing and search operations to a specific shard.
	 */
	@Nullable private final String routing;

	/**
	 * The alias is hidden? By default, this is set to {@code false}.
	 */
	@Nullable private final Boolean isHidden;

	/**
	 * The index is the 'write index' for the alias? By default, this is set to {@code false}.
	 */
	@Nullable private final Boolean isWriteIndex;

	private Alias(Builder builder) {
		this.alias = builder.alias;

		this.filter = builder.filter;

		this.indexRouting = builder.indexRouting;
		this.searchRouting = builder.searchRouting;
		this.routing = builder.routing;

		this.isHidden = builder.isHidden;
		this.isWriteIndex = builder.isWriteIndex;
	}

	public String getAlias() {
		return alias;
	}

	@Nullable
	public Query getFilter() {
		return filter;
	}

	@Nullable
	public String getIndexRouting() {
		return indexRouting;
	}

	@Nullable
	public String getSearchRouting() {
		return searchRouting;
	}

	@Nullable
	public String getRouting() {
		return routing;
	}

	@Nullable
	public Boolean getHidden() {
		return isHidden;
	}

	@Nullable
	public Boolean getWriteIndex() {
		return isWriteIndex;
	}

	public static Builder builder(String alias) {
		return new Builder(alias);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof Alias that))
			return false;

		return Objects.equals(alias, that.alias) && Objects.equals(filter, that.filter)
				&& Objects.equals(indexRouting, that.indexRouting)
				&& Objects.equals(searchRouting, that.searchRouting)
				&& Objects.equals(routing, that.routing)
				&& Objects.equals(isHidden, that.isHidden)
				&& Objects.equals(isWriteIndex, that.isWriteIndex);
	}

	@Override
	public int hashCode() {
		return Objects.hash(alias, filter, indexRouting, searchRouting, routing, isHidden, isWriteIndex);
	}

	public static class Builder {
		private final String alias;

		@Nullable private Query filter;

		@Nullable private String indexRouting;
		@Nullable private String searchRouting;
		@Nullable private String routing;

		@Nullable private Boolean isHidden;
		@Nullable private Boolean isWriteIndex;

		public Builder(String alias) {
			Assert.notNull(alias, "alias must not be null");
			this.alias = alias;
		}

		/**
		 * Query used to limit documents the alias can access.
		 */
		public Builder withFilter(@Nullable Query filter) {
			this.filter = filter;

			return this;
		}

		/**
		 * Used to route indexing operations to a specific shard.
		 */
		public Builder withIndexRouting(@Nullable String indexRouting) {
			if (indexRouting != null && !indexRouting.trim().isEmpty()) {
				this.indexRouting = indexRouting;
			}

			return this;
		}

		/**
		 * Used to route search operations to a specific shard.
		 */
		public Builder withSearchRouting(@Nullable String searchRouting) {
			if (searchRouting != null && !searchRouting.trim().isEmpty()) {
				this.searchRouting = searchRouting;
			}

			return this;
		}

		/**
		 * Used to route indexing and search operations to a specific shard.
		 */
		public Builder withRouting(@Nullable String routing) {
			if (routing != null && !routing.trim().isEmpty()) {
				this.routing = routing;
			}

			return this;
		}

		/**
		 * The alias is hidden? By default, this is set to {@code false}.
		 */
		public Builder withHidden(@Nullable Boolean hidden) {
			isHidden = hidden;

			return this;
		}

		/**
		 * The index is the 'write index' for the alias? By default, this is set to {@code false}.
		 */
		public Builder withWriteIndex(@Nullable Boolean writeIndex) {
			isWriteIndex = writeIndex;

			return this;
		}

		public Alias build() {
			return new Alias(this);
		}
	}
}
