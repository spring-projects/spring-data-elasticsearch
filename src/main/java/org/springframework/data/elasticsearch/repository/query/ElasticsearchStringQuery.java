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
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ParserContext;
import org.springframework.expression.TypeConverter;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
	private static final SpelExpressionParser PARSER = new SpelExpressionParser();
	private static final Map<String, Expression> QUERY_EXPRESSIONS = new ConcurrentHashMap<>();
	private final QueryMethodEvaluationContextProvider evaluationContextProvider;
	private final TypeConverter typeConverter;

	public ElasticsearchStringQuery(ElasticsearchQueryMethod queryMethod, ElasticsearchOperations elasticsearchOperations,
			String queryString, QueryMethodEvaluationContextProvider evaluationContextProvider,
			TypeConverter typeConverter) {
		super(queryMethod, elasticsearchOperations);
		Assert.notNull(queryString, "Query cannot be empty");
		Assert.notNull(evaluationContextProvider, "ExpressionEvaluationContextProvider must not be null");
		Assert.notNull(typeConverter, "TypeConverter must not be null");

		this.queryString = queryString;
		this.evaluationContextProvider = evaluationContextProvider;
		this.typeConverter = typeConverter;
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

		var query = new StringQuery(parseSpEL(queryString, parameterAccessor));
		query.addSort(parameterAccessor.getSort());
		return query;
	}

	private String parseSpEL(String queryString, ElasticsearchParametersParameterAccessor parameterAccessor) {
		Expression expr = getQueryExpression(queryString);
		if (expr != null) {
			EvaluationContext context = evaluationContextProvider.getEvaluationContext(parameterAccessor.getParameters(),
					parameterAccessor.getValues());
			if (context instanceof StandardEvaluationContext standardEvaluationContext) {
				standardEvaluationContext.setTypeConverter(typeConverter);
			}

			String parsed = expr.getValue(context, String.class);
			Assert.notNull(parsed, "Query parsed by SpEL should not be null");
			return parsed;
		}
		return queryString;
	}

	@Nullable
	private Expression getQueryExpression(String queryString) {
		return QUERY_EXPRESSIONS.computeIfAbsent(queryString, f -> {
			Expression expr = PARSER.parseExpression(queryString, ParserContext.TEMPLATE_EXPRESSION);
			return expr instanceof LiteralExpression ? null : expr;
		});

	}
}
