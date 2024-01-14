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
import org.springframework.data.elasticsearch.repository.support.ElasticsearchCollectionValueToStringConverter;
import org.springframework.data.elasticsearch.repository.support.ElasticsearchValueSpELConversionService;
import org.springframework.data.elasticsearch.repository.support.StringQueryUtil;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ParserContext;
import org.springframework.expression.TypeConverter;
import org.springframework.expression.common.CompositeStringExpression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.support.StandardTypeConverter;
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

	private static final SpelExpressionParser PARSER = new SpelExpressionParser();
	private static final Map<String, Expression> QUERY_EXPRESSIONS = new ConcurrentHashMap<>();

	private final String queryString;
	private final QueryMethodEvaluationContextProvider evaluationContextProvider;
	private final TypeConverter elasticsearchSpELTypeConverter;

	public ElasticsearchStringQuery(ElasticsearchQueryMethod queryMethod, ElasticsearchOperations elasticsearchOperations,
			String queryString, QueryMethodEvaluationContextProvider evaluationContextProvider) {
		super(queryMethod, elasticsearchOperations);
		Assert.notNull(queryString, "Query cannot be empty");
		Assert.notNull(evaluationContextProvider, "ExpressionEvaluationContextProvider must not be null");

		this.queryString = queryString;
		this.evaluationContextProvider = evaluationContextProvider;
		this.elasticsearchSpELTypeConverter = new StandardTypeConverter(ElasticsearchValueSpELConversionService.CONVERSION_SERVICE_LAZY);
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
				standardEvaluationContext.setTypeConverter(elasticsearchSpELTypeConverter);
			}

			String parsed = parseExpressions(expr, context);
			Assert.notNull(parsed, "Query parsed by SpEL should not be null");
			return parsed;
		}
		return queryString;
	}

	/**
	 * {@link Expression#getValue(EvaluationContext, Class)} is not used because the value part in SpEL should be converted
	 * by {@link org.springframework.data.elasticsearch.repository.support.ElasticsearchStringValueToStringConverter} or
	 * {@link ElasticsearchCollectionValueToStringConverter} to
	 * escape the quotations, but other literal parts in SpEL expression should not be processed with these converters.
	 * So we just get the string value from {@link LiteralExpression} directly rather than
	 * {@link LiteralExpression#getValue(EvaluationContext, Class)}.
	 */
	private String parseExpressions(Expression rootExpr, EvaluationContext context) {
		StringBuilder parsed = new StringBuilder();
		if (rootExpr instanceof LiteralExpression literalExpression) {
			// get the string literal directly
			parsed.append(literalExpression.getExpressionString());
		} else if (rootExpr instanceof SpelExpression spelExpression) {
			// evaluate the value
			parsed.append(spelExpression.getValue(context, String.class));
		} else if (rootExpr instanceof CompositeStringExpression compositeStringExpression) {
			// then it should be another composite expression
			Expression[] expressions = compositeStringExpression.getExpressions();

			for (Expression exp : expressions) {
				parsed.append(parseExpressions(exp, context));
			}
		} else {
			// no more
			parsed.append(rootExpr.getValue(context, String.class));
		}
		return parsed.toString();
	}

	@Nullable
	private Expression getQueryExpression(String queryString) {
		return QUERY_EXPRESSIONS.computeIfAbsent(queryString, f -> {
			Expression expr = PARSER.parseExpression(queryString, ParserContext.TEMPLATE_EXPRESSION);
			return expr instanceof LiteralExpression ? null : expr;
		});
	}
}
