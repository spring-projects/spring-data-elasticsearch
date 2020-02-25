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

import org.elasticsearch.index.query.QueryBuilder;
import org.springframework.lang.Nullable;

/**
 * DeleteQuery
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Peter-Josef Meisch
 * @deprecated since 4.0, use {@link Query} implementations and set {@link Query#setScrollTimeInMillis(Long)} and {@link Query#getMaxResults()}
 */
@Deprecated
public class DeleteQuery {

	@Nullable private QueryBuilder query;
	@Nullable private Integer pageSize;
	@Nullable private Long scrollTimeInMillis;

	@Nullable
	public QueryBuilder getQuery() {
		return query;
	}

	public void setQuery(QueryBuilder query) {
		this.query = query;
	}

	@Nullable
	public Integer getPageSize() {
		return pageSize;
	}

	public void setPageSize(Integer pageSize) {
		this.pageSize = pageSize;
	}

	@Nullable
	public Long getScrollTimeInMillis() {
		return scrollTimeInMillis;
	}

	public void setScrollTimeInMillis(Long scrollTimeInMillis) {
		this.scrollTimeInMillis = scrollTimeInMillis;
	}
}
