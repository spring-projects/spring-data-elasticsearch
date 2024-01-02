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

/**
 * value object to describe alias information.
 *
 * @author Peter-Josef Meisch
 */
public class AliasData {
	private final String alias;
	@Nullable private final Query filterQuery;
	@Nullable private final String indexRouting;
	@Nullable private final String searchRouting;
	@Nullable private final Boolean isWriteIndex;
	@Nullable private final Boolean isHidden;

	private AliasData(String alias, @Nullable Query filterQuery, @Nullable String indexRouting,
			@Nullable String searchRouting, @Nullable Boolean isWriteIndex, @Nullable Boolean isHidden) {
		this.alias = alias;
		this.filterQuery = filterQuery;
		this.indexRouting = indexRouting;
		this.searchRouting = searchRouting;
		this.isWriteIndex = isWriteIndex;
		this.isHidden = isHidden;
	}

	public static AliasData of(String alias, @Nullable Query filterQuery, @Nullable String indexRouting,
			@Nullable String searchRouting, @Nullable Boolean isWriteIndex, @Nullable Boolean isHidden) {
		return new AliasData(alias, filterQuery, indexRouting, searchRouting, isWriteIndex, isHidden);
	}

	public String getAlias() {
		return alias;
	}

	@Nullable
	public Query getFilterQuery() {
		return filterQuery;
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
	public Boolean isWriteIndex() {
		return isWriteIndex;
	}

	@Nullable
	public Boolean isHidden() {
		return Boolean.TRUE.equals(isHidden);
	}
}
