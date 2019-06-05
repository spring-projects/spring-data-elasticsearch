/*
 * Copyright 2013-2019 the original author or authors.
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

import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.convert.DateTimeConverters;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.util.Assert;
import org.springframework.util.NumberUtils;

/**
 * ElasticsearchStringQuery
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Mark Paluch
 * @author Taylor Ono
 */
public class ElasticsearchStringQuery extends AbstractElasticsearchRepositoryQuery {

	private static final Pattern PARAMETER_PLACEHOLDER = Pattern.compile("\\?(\\d+)");
	private String query;

	private final GenericConversionService conversionService = new GenericConversionService();

	{
		if (!conversionService.canConvert(java.util.Date.class, String.class)) {
			conversionService.addConverter(DateTimeConverters.JavaDateConverter.INSTANCE);
		}
		if (!conversionService.canConvert(org.joda.time.ReadableInstant.class, String.class)) {
			conversionService.addConverter(DateTimeConverters.JodaDateTimeConverter.INSTANCE);
		}
		if (!conversionService.canConvert(org.joda.time.LocalDateTime.class, String.class)) {
			conversionService.addConverter(DateTimeConverters.JodaLocalDateTimeConverter.INSTANCE);
		}
	}

	public ElasticsearchStringQuery(ElasticsearchQueryMethod queryMethod, ElasticsearchOperations elasticsearchOperations,
			String query) {
		super(queryMethod, elasticsearchOperations);
		Assert.notNull(query, "Query cannot be empty");
		this.query = query;
	}

	@Override
	public Object execute(Object[] parameters) {
		ParametersParameterAccessor accessor = new ParametersParameterAccessor(queryMethod.getParameters(), parameters);
		StringQuery stringQuery = createQuery(accessor);
		if (queryMethod.isPageQuery()) {
			stringQuery.setPageable(accessor.getPageable());
			return elasticsearchOperations.queryForPage(stringQuery, queryMethod.getEntityInformation().getJavaType());
		} else if (queryMethod.isCollectionQuery()) {
			if (accessor.getPageable().isPaged()) {
				stringQuery.setPageable(accessor.getPageable());
			}
			return elasticsearchOperations.queryForList(stringQuery, queryMethod.getEntityInformation().getJavaType());
		}

		return elasticsearchOperations.queryForObject(stringQuery, queryMethod.getEntityInformation().getJavaType());
	}

	protected StringQuery createQuery(ParametersParameterAccessor parameterAccessor) {
		String queryString = replacePlaceholders(this.query, parameterAccessor);
		return new StringQuery(queryString);
	}

	private String replacePlaceholders(String input, ParametersParameterAccessor accessor) {

		Matcher matcher = PARAMETER_PLACEHOLDER.matcher(input);
		String result = input;
		while (matcher.find()) {

			String placeholder = Pattern.quote(matcher.group()) + "(?!\\d+)";
			int index = NumberUtils.parseNumber(matcher.group(1), Integer.class);
			result = result.replaceAll(placeholder, getParameterWithIndex(accessor, index));
		}
		return result;
	}

	private String getParameterWithIndex(ParametersParameterAccessor accessor, int index) {
		Object parameter = accessor.getBindableValue(index);
		if (parameter == null) {
			return "null";
		}
		if (conversionService.canConvert(parameter.getClass(), String.class)) {
			return conversionService.convert(parameter, String.class);
		}
		return parameter.toString();
	}
}
