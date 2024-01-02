/*
 * Copyright 2020-2024 the original author or authors.
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
package org.springframework.data.elasticsearch.core.index;

import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Value class capturing the arguments for an {@link AliasAction}.
 *
 * @author Peter-Josef Meisch
 * @since 4.1
 */
public class AliasActionParameters {
	private final String[] indices;
	@Nullable private final String[] aliases;
	@Nullable private final Query filterQuery;
	@Nullable private final Class<?> filterQueryClass;
	@Nullable private final Boolean isHidden;
	@Nullable private final Boolean isWriteIndex;
	@Nullable private final String routing;
	@Nullable private final String indexRouting;
	@Nullable private final String searchRouting;

	private AliasActionParameters(String[] indices, @Nullable String[] aliases, @Nullable Boolean isHidden,
			@Nullable Boolean isWriteIndex, @Nullable String routing, @Nullable String indexRouting,
			@Nullable String searchRouting, @Nullable Query filterQuery, @Nullable Class<?> filterQueryClass) {
		this.indices = indices;
		this.aliases = aliases;
		this.isHidden = isHidden;
		this.isWriteIndex = isWriteIndex;
		this.routing = routing;
		this.indexRouting = indexRouting;
		this.searchRouting = searchRouting;
		this.filterQuery = filterQuery;
		this.filterQueryClass = filterQueryClass;
	}

	public static Builder builder() {
		return new Builder();
	}

	/**
	 * a Builder to create AliasActionParameters to be used when creating index templates. Automatically sets the index
	 * name to an empty string, as this is not used in templates
	 */
	public static Builder builderForTemplate() {
		return new Builder().withIndices("");
	}

	public String[] getIndices() {
		return indices;
	}

	@Nullable
	public String[] getAliases() {
		return aliases;
	}

	@Nullable
	public Boolean getHidden() {
		return isHidden;
	}

	@Nullable
	public Boolean getWriteIndex() {
		return isWriteIndex;
	}

	@Nullable
	public String getRouting() {
		return routing;
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
	public Query getFilterQuery() {
		return filterQuery;
	}

	@Nullable
	public Class<?> getFilterQueryClass() {
		return filterQueryClass;
	}

	public static final class Builder {
		@Nullable private String[] indices;
		@Nullable private String[] aliases;
		@Nullable private Query filterQuery;
		@Nullable private Class<?> filterQueryClass;
		@Nullable private Boolean isHidden;
		@Nullable private Boolean isWriteIndex;
		@Nullable private String routing;
		@Nullable private String indexRouting;
		@Nullable private String searchRouting;

		private Builder() {}

		public Builder withIndices(String... indices) {
			this.indices = indices;
			return this;
		}

		public Builder withAliases(String... aliases) {
			this.aliases = aliases;
			return this;
		}

		public Builder withFilterQuery(Query filterQuery) {
			return withFilterQuery(filterQuery, null);
		}

		public Builder withFilterQuery(Query filterQuery, @Nullable Class<?> filterQueryClass) {
			this.filterQuery = filterQuery;
			this.filterQueryClass = filterQueryClass;
			return this;
		}

		public Builder withIsHidden(Boolean isHidden) {
			this.isHidden = isHidden;
			return this;
		}

		public Builder withIsWriteIndex(Boolean isWriteIndex) {
			this.isWriteIndex = isWriteIndex;
			return this;
		}

		public Builder withRouting(String routing) {
			this.routing = routing;
			return this;
		}

		public Builder withIndexRouting(String indexRouting) {
			this.indexRouting = indexRouting;
			return this;
		}

		public Builder withSearchRouting(String searchRouting) {
			this.searchRouting = searchRouting;
			return this;
		}

		public AliasActionParameters build() {

			Assert.notNull(indices, "indices must be set");

			return new AliasActionParameters(indices, aliases, isHidden, isWriteIndex, routing, indexRouting, searchRouting,
					filterQuery, filterQueryClass);
		}
	}
}
