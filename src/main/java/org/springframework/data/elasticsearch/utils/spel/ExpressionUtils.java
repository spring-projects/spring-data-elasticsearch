/*
 * Copyright 2021-present the original author or authors.
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
package org.springframework.data.elasticsearch.utils.spel;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;
import org.springframework.data.expression.ValueEvaluationContext;
import org.springframework.data.expression.ValueExpression;
import org.springframework.data.expression.ValueExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.lang.Contract;
import org.springframework.util.StringUtils;

/**
 * Internal utility class for dealing with {@link ValueExpression} and potential ones. Shamelessly copied from Spring
 * Data MongoDB. Adapted afterwards to our needs.
 *
 * @author Christoph Strobl
 * @since 6.2
 */
public final class ExpressionUtils {

	private static final ValueExpressionParser PARSER = ValueExpressionParser.create(SpelExpressionParser::new);

	private static final ConcurrentHashMap<String, ValueExpression> expressionCache = new ConcurrentHashMap<>();

	/**
	 * Returns a SpEL {@link ValueExpression} if the given {@link String} is not empty. ValueExpressions are stored in a
	 * cash.
	 *
	 * @param potentialExpression can be {@literal null}
	 * @return {@link ValueExpression} or null when input is empty
	 */
	@Contract("null -> null")
	public static @Nullable ValueExpression detectExpression(@Nullable String potentialExpression) {

		if (!StringUtils.hasText(potentialExpression)) {
			return null;
		}

		return expressionCache.computeIfAbsent(potentialExpression,
				key -> PARSER.parse(potentialExpression));
	}

	/**
	 * evbaluates value against the context provided by valueEvaluationContextSupplier. If value cannot be transformed
	 * into a {@link ValueExpression}, the defaultValue is returned.
	 *
	 * @param value the value to evaluate
	 * @param valueEvaluationContextSupplier supplies the context to use for evaluation
	 * @param defaultValue
	 * @return
	 */
	public static @Nullable Object evaluate(String value, Supplier<ValueEvaluationContext> valueEvaluationContextSupplier,
			@Nullable Object defaultValue) {

		ValueExpression expression = detectExpression(value);

		if (expression == null) {
			return defaultValue;
		}

		var evaluated = expression.evaluate(valueEvaluationContextSupplier.get());
		return evaluated == null ? defaultValue : evaluated;
	}
}
