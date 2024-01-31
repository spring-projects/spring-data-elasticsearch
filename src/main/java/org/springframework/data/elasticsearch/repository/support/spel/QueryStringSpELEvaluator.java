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
package org.springframework.data.elasticsearch.repository.support.spel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.convert.ConversionService;
import org.springframework.data.elasticsearch.core.convert.ConversionException;
import org.springframework.data.elasticsearch.repository.query.ElasticsearchParametersParameterAccessor;
import org.springframework.data.elasticsearch.repository.support.value.ElasticsearchCollectionValueToStringConverter;
import org.springframework.data.elasticsearch.repository.support.value.ElasticsearchQueryValueConversionService;
import org.springframework.data.elasticsearch.repository.support.value.ElasticsearchStringValueToStringConverter;
import org.springframework.data.repository.query.QueryMethod;
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

/**
 * To evaluate the SpEL expressions of the query string.
 *
 * @author Haibo Liu
 * @since 5.3
 */
public class QueryStringSpELEvaluator {

	private static final SpelExpressionParser PARSER = new SpelExpressionParser();
	private static final Map<String, Expression> QUERY_EXPRESSIONS = new ConcurrentHashMap<>();

	private final String queryString;
	private final ElasticsearchParametersParameterAccessor parameterAccessor;
	private final QueryMethod queryMethod;
	private final QueryMethodEvaluationContextProvider evaluationContextProvider;
	private final TypeConverter elasticsearchSpELTypeConverter;

	public QueryStringSpELEvaluator(String queryString, ElasticsearchParametersParameterAccessor parameterAccessor,
			QueryMethod queryMethod, QueryMethodEvaluationContextProvider evaluationContextProvider,
			ConversionService conversionService) {

		Assert.notNull(queryString, "queryString must not be null");
		Assert.notNull(parameterAccessor, "parameterAccessor must not be null");
		Assert.notNull(queryMethod, "queryMethod must not be null");
		Assert.notNull(evaluationContextProvider, "evaluationContextProvider must not be null");
		Assert.notNull(conversionService, "conversionService must not be null");

		this.queryString = queryString;
		this.parameterAccessor = parameterAccessor;
		this.queryMethod = queryMethod;
		this.evaluationContextProvider = evaluationContextProvider;
		this.elasticsearchSpELTypeConverter = new StandardTypeConverter(
				ElasticsearchQueryValueConversionService.getInstance(conversionService));
	}

	/**
	 * Evaluate the SpEL parts of the query string.
	 *
	 * @return a plain string with values evaluated
	 */
	public String evaluate() {
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
	 * {@link Expression#getValue(EvaluationContext, Class)} is not used because the value part in SpEL should be
	 * converted by {@link ElasticsearchStringValueToStringConverter} or
	 * {@link ElasticsearchCollectionValueToStringConverter} to escape the quotations, but other literal parts in SpEL
	 * expression should not be processed with these converters. So we just get the string value from
	 * {@link LiteralExpression} directly rather than {@link LiteralExpression#getValue(EvaluationContext, Class)}.
	 */
	private String parseExpressions(Expression rootExpr, EvaluationContext context) {
		StringBuilder parsed = new StringBuilder();

		if (rootExpr instanceof LiteralExpression literalExpression) {
			// get the string literal directly
			parsed.append(literalExpression.getExpressionString());
		} else if (rootExpr instanceof SpelExpression spelExpression) {
			// evaluate the value
			String value = spelExpression.getValue(context, String.class);

			if (value == null) {
				throw new ConversionException(String.format(
						"Parameter value can't be null for SpEL expression '%s' in method '%s' when querying elasticsearch",
						spelExpression.getExpressionString(), queryMethod.getName()));
			}
			parsed.append(value);
		} else if (rootExpr instanceof CompositeStringExpression compositeStringExpression) {
			// parse one by one for composite expression
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
