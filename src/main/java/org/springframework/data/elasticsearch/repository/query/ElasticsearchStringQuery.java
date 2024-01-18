/*
 * Copyright 2013-2024 the original author or authors.
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
import org.springframework.data.elasticsearch.core.query.BaseQuery;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.data.elasticsearch.repository.support.StringQueryUtil;
import org.springframework.data.elasticsearch.repository.support.spel.QueryStringSpELEvaluator;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.util.Assert;

/**
 * ElasticsearchStringQuery
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Mark Paluch
 * @author Taylor Ono
 * @author Peter-Josef Meisch
 * @author Haibo Liu
 */
public class ElasticsearchStringQuery extends AbstractElasticsearchRepositoryQuery {

	private final String queryString;
	private final QueryMethodEvaluationContextProvider evaluationContextProvider;

	public ElasticsearchStringQuery(ElasticsearchQueryMethod queryMethod, ElasticsearchOperations elasticsearchOperations,
			String queryString, QueryMethodEvaluationContextProvider evaluationContextProvider) {
		super(queryMethod, elasticsearchOperations);
		Assert.notNull(queryString, "Query cannot be empty");
		Assert.notNull(evaluationContextProvider, "ExpressionEvaluationContextProvider must not be null");

		this.queryString = queryString;
		this.evaluationContextProvider = evaluationContextProvider;
	}

	@Override
	public boolean isCountQuery() {
		return queryMethod.hasCountQueryAnnotation();
	}

	@Override
	protected boolean isDeleteQuery() {
		return false;
	}

	@Override
	protected boolean isExistsQuery() {
		return false;
	}

	protected BaseQuery createQuery(ElasticsearchParametersParameterAccessor parameterAccessor) {
		String queryString = new StringQueryUtil(elasticsearchOperations.getElasticsearchConverter().getConversionService())
				.replacePlaceholders(this.queryString, parameterAccessor);

		QueryStringSpELEvaluator evaluator = new QueryStringSpELEvaluator(queryString, parameterAccessor, queryMethod,
				evaluationContextProvider);
		var query = new StringQuery(evaluator.evaluate());
		query.addSort(parameterAccessor.getSort());
		return query;
	}

}
