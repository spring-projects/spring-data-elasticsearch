/*
 * Copyright 2013-2019 the original author or authors.
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

/**
 * @author Mohsin Husen
 */
public class AliasBuilder {

	private String indexName;
	private String aliasName;
	private QueryBuilder filterBuilder;
	private Map<String, Object> filter;
	private String searchRouting;
	private String indexRouting;
	private String routing;

	public AliasBuilder withIndexName(String indexName) {
		this.indexName = indexName;
		return this;
	}

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
		AliasQuery aliasQuery = new AliasQuery();
		aliasQuery.setIndexName(indexName);
		aliasQuery.setAliasName(aliasName);
		aliasQuery.setFilterBuilder(filterBuilder);
		aliasQuery.setFilter(filter);
		aliasQuery.setSearchRouting(searchRouting);
		aliasQuery.setIndexRouting(indexRouting);
		aliasQuery.setRouting(routing);
		return aliasQuery;
	}
}
