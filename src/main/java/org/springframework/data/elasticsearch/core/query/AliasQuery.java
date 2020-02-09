/*
 * Copyright 2013-2020 the original author or authors.
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

import java.util.Map;

import org.elasticsearch.index.query.QueryBuilder;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * AliasQuery is useful for creating new alias or deleting existing ones
 *
 * @author Mohsin Husen
 * @author Peter-Josef Meisch
 */
public class AliasQuery {

	public AliasQuery(String aliasName) {

		Assert.notNull(aliasName, "aliasName must not be null");

		this.aliasName = aliasName;
	}

	private String aliasName;
	@Nullable private QueryBuilder filterBuilder;
	@Nullable private Map<String, Object> filter;
	@Nullable private String searchRouting;
	@Nullable private String indexRouting;
	@Nullable private String routing;

	public String getAliasName() {
		return aliasName;
	}

	@Nullable
	public QueryBuilder getFilterBuilder() {
		return filterBuilder;
	}

	public void setFilterBuilder(QueryBuilder filterBuilder) {
		this.filterBuilder = filterBuilder;
	}

	@Nullable
	public Map<String, Object> getFilter() {
		return filter;
	}

	public void setFilter(Map<String, Object> filter) {
		this.filter = filter;
	}

	@Nullable
	public String getSearchRouting() {
		return searchRouting;
	}

	public void setSearchRouting(String searchRouting) {
		this.searchRouting = searchRouting;
	}

	@Nullable
	public String getIndexRouting() {
		return indexRouting;
	}

	public void setIndexRouting(String indexRouting) {
		this.indexRouting = indexRouting;
	}

	@Nullable
	public String getRouting() {
		return routing;
	}

	public void setRouting(String routing) {
		this.routing = routing;
	}
}
