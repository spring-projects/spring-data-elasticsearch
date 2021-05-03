/*
 * Copyright 2013-2020 the original author or authors.
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
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.data.elasticsearch.repository.support.StringQueryUtil;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.util.Assert;

/**
 * ElasticsearchStringQuery
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Mark Paluch
 * @author Taylor Ono
 * @author Peter-Josef Meisch
 */
public class ElasticsearchStringQuery extends AbstractElasticsearchRepositoryQuery {

	private String query;

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
		Class<?> clazz = queryMethod.getResultProcessor().getReturnedType().getDomainType();
		if (queryMethod.isPageQuery()) {
			stringQuery.setPageable(accessor.getPageable());
			return elasticsearchOperations.queryForPage(stringQuery, clazz);
		} else if (queryMethod.isCollectionQuery()) {
			if (accessor.getPageable().isPaged()) {
				stringQuery.setPageable(accessor.getPageable());
			}
			return elasticsearchOperations.queryForList(stringQuery, clazz);
		}

		return elasticsearchOperations.queryForObject(stringQuery, clazz);
	}

	protected StringQuery createQuery(ParametersParameterAccessor parameterAccessor) {
		String queryString = StringQueryUtil.replacePlaceholders(this.query, parameterAccessor);
		return new StringQuery(queryString);
	}
}
