/*
 * Copyright 2024-2025 the original author or authors.
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
package org.springframework.data.elasticsearch.core.sql;

import org.springframework.data.elasticsearch.core.query.SqlQuery;

import reactor.core.publisher.Mono;

/**
 * The reactive version of operations for the
 * <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/sql-search-api.html">SQL search API</a>.
 *
 * @author Aouichaoui Youssef
 * @since 5.4
 */
public interface ReactiveSqlOperations {
	/**
	 * Execute the sql {@code query} against elasticsearch and return result as {@link SqlResponse}
	 *
	 * @param query the query to execute
	 * @return {@link SqlResponse} containing the list of found objects
	 */
	Mono<SqlResponse> search(SqlQuery query);
}
