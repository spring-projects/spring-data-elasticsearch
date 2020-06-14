/*
 * Copyright 2020 the original author or authors.
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

import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.lang.Nullable;

/**
 * value object to describe alias information.
 * 
 * @author Peter-Josef Meisch
 */
public class AliasData {
	private String alias;
	@Nullable Document filter;
	@Nullable private String indexRouting;
	@Nullable private String searchRouting;
	@Nullable private Boolean isWriteIndex;
	@Nullable private Boolean isHidden;

	private AliasData(String alias, @Nullable Document filter, @Nullable String indexRouting,
			@Nullable String searchRouting, Boolean isWriteIndex, Boolean isHidden) {
		this.alias = alias;
		this.filter = filter;
		this.indexRouting = indexRouting;
		this.searchRouting = searchRouting;
		this.isWriteIndex = isWriteIndex;
		this.isHidden = isHidden;
	}

	public static AliasData of(String alias,
							   @Nullable Document filter,
							   @Nullable String indexRouting,
							   @Nullable String searchRouting,
							   @Nullable Boolean isWriteIndex,
							   @Nullable Boolean isHidden) {
		return new AliasData(alias, filter, indexRouting, searchRouting, isWriteIndex, isHidden);
	}

	public String getAlias() {
		return alias;
	}

	public Document getFilter() {
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
	public Boolean isWriteIndex() {
		return isWriteIndex;
	}

	@Nullable
	public Boolean isHidden() {
		return isHidden;
	}
}
