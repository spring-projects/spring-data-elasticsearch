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

import org.elasticsearch.index.query.QueryBuilder;

/**
 * DeleteQuery
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 */
public class DeleteQuery {

	private QueryBuilder query;
	private String index;
	private String type;
	private Integer pageSize;
	private Long scrollTimeInMillis;

	public QueryBuilder getQuery() {
		return query;
	}

	public void setQuery(QueryBuilder query) {
		this.query = query;
	}

	public String getIndex() {
		return index;
	}

	public void setIndex(String index) {
		this.index = index;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Integer getPageSize() {
		return pageSize;
	}

	public void setPageSize(Integer pageSize) {
		this.pageSize = pageSize;
	}

	public Long getScrollTimeInMillis() {
		return scrollTimeInMillis;
	}

	public void setScrollTimeInMillis(Long scrollTimeInMillis) {
		this.scrollTimeInMillis = scrollTimeInMillis;
	}
}
