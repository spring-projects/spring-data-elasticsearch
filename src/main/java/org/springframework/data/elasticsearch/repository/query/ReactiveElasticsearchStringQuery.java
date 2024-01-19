/*
 * Copyright 2019-2024 the original author or authors.
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

import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.BaseQuery;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.data.elasticsearch.repository.support.StringQueryUtil;
import org.springframework.data.elasticsearch.repository.support.spel.QueryStringSpELEvaluator;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * @author Christoph Strobl
 * @author Taylor Ono
 * @author Haibo Liu
 * @since 3.2
 */
public class ReactiveElasticsearchStringQuery extends AbstractReactiveElasticsearchRepositoryQuery {

	private final String query;
	private final QueryMethodEvaluationContextProvider evaluationContextProvider;

	public ReactiveElasticsearchStringQuery(ReactiveElasticsearchQueryMethod queryMethod,
			ReactiveElasticsearchOperations operations, SpelExpressionParser expressionParser,
			QueryMethodEvaluationContextProvider evaluationContextProvider) {

		this(queryMethod.getAnnotatedQuery(), queryMethod, operations, expressionParser, evaluationContextProvider);
	}

	public ReactiveElasticsearchStringQuery(String query, ReactiveElasticsearchQueryMethod queryMethod,
			ReactiveElasticsearchOperations operations, SpelExpressionParser expressionParser,
			QueryMethodEvaluationContextProvider evaluationContextProvider) {

		super(queryMethod, operations);
		this.query = query;
		this.evaluationContextProvider = evaluationContextProvider;
	}

	@Override
	protected BaseQuery createQuery(ElasticsearchParametersParameterAccessor parameterAccessor) {
		String queryString = new StringQueryUtil(
				getElasticsearchOperations().getElasticsearchConverter().getConversionService())
				.replacePlaceholders(this.query, parameterAccessor);

		QueryStringSpELEvaluator evaluator = new QueryStringSpELEvaluator(queryString, parameterAccessor, queryMethod,
				evaluationContextProvider);
		return new StringQuery(evaluator.evaluate());
	}

	@Override
	boolean isCountQuery() {
		return queryMethod.hasCountQueryAnnotation();
	}

	@Override
	boolean isDeleteQuery() {
		return false;
	}

	@Override
	boolean isExistsQuery() {
		return false;
	}

	@Override
	boolean isLimiting() {
		return false;
	}
}
