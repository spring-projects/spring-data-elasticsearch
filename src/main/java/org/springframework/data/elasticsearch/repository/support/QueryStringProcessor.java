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
package org.springframework.data.elasticsearch.repository.support;

import org.springframework.core.convert.ConversionService;
import org.springframework.data.elasticsearch.repository.query.ElasticsearchParametersParameterAccessor;
import org.springframework.data.elasticsearch.repository.support.spel.QueryStringSpELEvaluator;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.util.Assert;

/**
 * To process query strings with placeholder replacement and SpEL evaluation by {@link QueryStringPlaceholderReplacer}
 * and {@link QueryStringSpELEvaluator}.
 *
 * @since 5.3
 * @author Haibo Liu
 */
public class QueryStringProcessor {

	private final String query;
	private final QueryMethod queryMethod;
	private final ConversionService conversionService;
	private final QueryMethodEvaluationContextProvider evaluationContextProvider;

	public QueryStringProcessor(String query, QueryMethod queryMethod, ConversionService conversionService,
								QueryMethodEvaluationContextProvider evaluationContextProvider) {

		Assert.notNull(query, "query must not be null");
		Assert.notNull(queryMethod, "queryMethod must not be null");
		Assert.notNull(conversionService, "conversionService must not be null");
		Assert.notNull(evaluationContextProvider, "evaluationContextProvider must not be null");

		this.query = query;
		this.queryMethod = queryMethod;
		this.conversionService = conversionService;
		this.evaluationContextProvider = evaluationContextProvider;
	}

	/**
	 * Process the query string with placeholder replacement and SpEL evaluation.
	 *
	 * @param parameterAccessor parameter info
	 * @return processed string
	 */
	public String createQuery(ElasticsearchParametersParameterAccessor parameterAccessor) {
		String queryString = new QueryStringPlaceholderReplacer(conversionService)
				.replacePlaceholders(query, parameterAccessor);

		QueryStringSpELEvaluator evaluator = new QueryStringSpELEvaluator(queryString, parameterAccessor, queryMethod,
				evaluationContextProvider, conversionService);
		return evaluator.evaluate();
	}
}
