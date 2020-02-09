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
 * @author Mohsin Husen
 * @author Peter-Josef Meisch
 */
public class AliasBuilder {

	@Nullable private String aliasName;
	@Nullable private QueryBuilder filterBuilder;
	@Nullable private Map<String, Object> filter;
	@Nullable private String searchRouting;
	@Nullable private String indexRouting;
	@Nullable private String routing;

	public AliasBuilder withAliasName(String aliasName) {
		this.aliasName = aliasName;
		return this;
	}

	public AliasBuilder withFilterBuilder(QueryBuilder filterBuilder) {
		this.filterBuilder = filterBuilder;
		return this;
	}

	public AliasBuilder withFilter(Map<String, Object> filter) {
		this.filter = filter;
		return this;
	}

	public AliasBuilder withSearchRouting(String searchRouting) {
		this.searchRouting = searchRouting;
		return this;
	}

	public AliasBuilder withIndexRouting(String indexRouting) {
		this.indexRouting = indexRouting;
		return this;
	}

	public AliasBuilder withRouting(String routing) {
		this.routing = routing;
		return this;
	}

	public AliasQuery build() {

		Assert.notNull(aliasName, "aliasName must not be null");

		AliasQuery aliasQuery = new AliasQuery(aliasName);
		aliasQuery.setFilterBuilder(filterBuilder);
		aliasQuery.setFilter(filter);
		aliasQuery.setSearchRouting(searchRouting);
		aliasQuery.setIndexRouting(indexRouting);
		aliasQuery.setRouting(routing);
		return aliasQuery;
	}
}
