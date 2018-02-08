/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
 * AliasQuery is useful for creating new alias or deleting existing ones
 *
 * @author Mohsin Husen
 */
public class AliasQuery {

	private String indexName;
	private String aliasName;
	private QueryBuilder filterBuilder;
	private Map<String, Object> filter;
	private String searchRouting;
	private String indexRouting;
	private String routing;

	public String getIndexName() {
		return indexName;
	}

	public void setIndexName(String indexName) {
		this.indexName = indexName;
	}

	public String getAliasName() {
		return aliasName;
	}

	public void setAliasName(String aliasName) {
		this.aliasName = aliasName;
	}

	public QueryBuilder getFilterBuilder() {
		return filterBuilder;
	}

	public void setFilterBuilder(QueryBuilder filterBuilder) {
		this.filterBuilder = filterBuilder;
	}

	public Map<String, Object> getFilter() {
		return filter;
	}

	public void setFilter(Map<String, Object> filter) {
		this.filter = filter;
	}

	public String getSearchRouting() {
		return searchRouting;
	}

	public void setSearchRouting(String searchRouting) {
		this.searchRouting = searchRouting;
	}

	public String getIndexRouting() {
		return indexRouting;
	}

	public void setIndexRouting(String indexRouting) {
		this.indexRouting = indexRouting;
	}

	public String getRouting() {
		return routing;
	}

	public void setRouting(String routing) {
		this.routing = routing;
	}
}
