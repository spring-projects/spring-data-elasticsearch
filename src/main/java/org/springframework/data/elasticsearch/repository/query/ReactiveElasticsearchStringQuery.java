/*
 * Copyright 2019 the original author or authors.
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.util.NumberUtils;
import org.springframework.util.ObjectUtils;

/**
 * @author Christoph Strobl
 * @author Taylor Ono
 * @since 3.2
 */
public class ReactiveElasticsearchStringQuery extends AbstractReactiveElasticsearchRepositoryQuery {

	private static final Pattern PARAMETER_PLACEHOLDER = Pattern.compile("\\?(\\d+)");
	private final String query;

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
	}

	@Override
	protected StringQuery createQuery(ElasticsearchParameterAccessor parameterAccessor) {
		String queryString = replacePlaceholders(this.query, parameterAccessor);
		return new StringQuery(queryString);
	}

	private String replacePlaceholders(String input, ElasticsearchParameterAccessor accessor) {

		Matcher matcher = PARAMETER_PLACEHOLDER.matcher(input);
		String result = input;
		while (matcher.find()) {

			String placeholder = Pattern.quote(matcher.group()) + "(?!\\d+)";
			int index = NumberUtils.parseNumber(matcher.group(1), Integer.class);
			result = result.replaceAll(placeholder, getParameterWithIndex(accessor, index));
		}
		return result;
	}

	private String getParameterWithIndex(ElasticsearchParameterAccessor accessor, int index) {
		return ObjectUtils.nullSafeToString(accessor.getBindableValue(index));
	}

	@Override
	boolean isCountQuery() {
		return false;
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
