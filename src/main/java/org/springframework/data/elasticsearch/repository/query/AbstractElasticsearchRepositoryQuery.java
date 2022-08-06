/*
 * Copyright 2013-2022 the original author or authors.
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
package org.springframework.data.elasticsearch.repository.query;

import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;

/**
 * AbstractElasticsearchRepositoryQuery
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Peter-Josef Meisch
 */

public abstract class AbstractElasticsearchRepositoryQuery implements RepositoryQuery {

	protected static final int DEFAULT_STREAM_BATCH_SIZE = 500;
	protected ElasticsearchQueryMethod queryMethod;
	protected final ElasticsearchOperations elasticsearchOperations;
	protected final ElasticsearchConverter elasticsearchConverter;

	public AbstractElasticsearchRepositoryQuery(ElasticsearchQueryMethod queryMethod,
			ElasticsearchOperations elasticsearchOperations) {
		this.queryMethod = queryMethod;
		this.elasticsearchOperations = elasticsearchOperations;
		this.elasticsearchConverter = elasticsearchOperations.getElasticsearchConverter();
	}

	@Override
	public QueryMethod getQueryMethod() {
		return queryMethod;
	}

	/**
	 * @return {@literal true} if this is a count query
	 * @since 4.2
	 */
	public abstract boolean isCountQuery();

	protected void prepareQuery(Query query, Class<?> clazz, ParameterAccessor parameterAccessor) {

		elasticsearchConverter.updateQuery(query, clazz);

		if (queryMethod.hasAnnotatedHighlight()) {
			query.setHighlightQuery(queryMethod.getAnnotatedHighlightQuery());
		}

		var sourceFilter = queryMethod.getSourceFilter(parameterAccessor,
				elasticsearchOperations.getElasticsearchConverter());
		if (sourceFilter != null) {
			query.addSourceFilter(sourceFilter);
		}
	}
}
