/*
 * Copyright 2025 the original author or authors.
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

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.BaseQuery;
import org.springframework.data.elasticsearch.core.query.SearchTemplateQuery;
import org.springframework.data.repository.query.ValueExpressionDelegate;
import org.springframework.util.Assert;

/**
 * A repository query that uses a search template already stored in Elasticsearch.
 *
 * @author P.J. Meisch (pj.meisch@sothawo.com)
 * @since 5.5
 */
public class RepositorySearchTemplateQuery extends AbstractElasticsearchRepositoryQuery {

	private String id;
	private Map<String, Object> params;

	public RepositorySearchTemplateQuery(ElasticsearchQueryMethod queryMethod,
			ElasticsearchOperations elasticsearchOperations, ValueExpressionDelegate valueExpressionDelegate,
			String id) {
		super(queryMethod, elasticsearchOperations,
				valueExpressionDelegate.createValueContextProvider(queryMethod.getParameters()));
		Assert.hasLength(id, "id must not be null or empty");
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public Map<String, Object> getParams() {
		return params;
	}

	@Override
	public boolean isCountQuery() {
		return false;
	}

	@Override
	protected boolean isDeleteQuery() {
		return false;
	}

	@Override
	protected boolean isExistsQuery() {
		return false;
	}

	@Override
	protected BaseQuery createQuery(ElasticsearchParametersParameterAccessor parameterAccessor) {

		var searchTemplateParameters = new LinkedHashMap<String, Object>();
		var values = parameterAccessor.getValues();

		parameterAccessor.getParameters().forEach(parameter -> {
			if (!parameter.isSpecialParameter() && parameter.getName().isPresent() && parameter.getIndex() <= values.length) {
				searchTemplateParameters.put(parameter.getName().get(), values[parameter.getIndex()]);
			}
		});

		return SearchTemplateQuery.builder()
				.withId(id)
				.withParams(searchTemplateParameters)
				.build();
	}
}
