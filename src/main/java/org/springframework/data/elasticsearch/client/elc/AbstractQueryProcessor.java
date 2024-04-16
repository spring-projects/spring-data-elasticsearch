/*
 * Copyright 2024 the original author or authors.
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
package org.springframework.data.elasticsearch.client.elc;

import java.util.function.Consumer;

import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.lang.Nullable;

/**
 * An abstract class that serves as a base for query processors. It provides a common interface and basic functionality
 * for query processing.
 *
 * @author Aouichaoui Youssef
 * @since 5.3
 */
public abstract class AbstractQueryProcessor {

	/**
	 * Convert a spring-data-elasticsearch {@literal query} to an Elasticsearch {@literal query}.
	 *
	 * @param query spring-data-elasticsearch {@literal query}.
	 * @param queryConverter correct mapped field names and the values to the converted values.
	 * @return an Elasticsearch {@literal query}.
	 */
	@Nullable
	static co.elastic.clients.elasticsearch._types.query_dsl.Query getEsQuery(@Nullable Query query,
			@Nullable Consumer<Query> queryConverter) {
		if (query == null) {
			return null;
		}

		if (queryConverter != null) {
			queryConverter.accept(query);
		}

		co.elastic.clients.elasticsearch._types.query_dsl.Query esQuery = null;

		if (query instanceof CriteriaQuery criteriaQuery) {
			esQuery = CriteriaQueryProcessor.createQuery(criteriaQuery.getCriteria());
		} else if (query instanceof StringQuery stringQuery) {
			esQuery = Queries.wrapperQueryAsQuery(stringQuery.getSource());
		} else if (query instanceof NativeQuery nativeQuery) {
			if (nativeQuery.getQuery() != null) {
				esQuery = nativeQuery.getQuery();
			} else if (nativeQuery.getSpringDataQuery() != null) {
				esQuery = getEsQuery(nativeQuery.getSpringDataQuery(), queryConverter);
			}
		} else {
			throw new IllegalArgumentException("unhandled Query implementation " + query.getClass().getName());
		}

		return esQuery;
	}
}
