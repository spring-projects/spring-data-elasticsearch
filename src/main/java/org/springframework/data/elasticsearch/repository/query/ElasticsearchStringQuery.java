/*
 * Copyright 2013-2021 the original author or authors.
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

import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHitSupport;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.convert.DateTimeConverters;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.data.elasticsearch.repository.support.StringQueryUtil;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.data.util.StreamUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

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
	public boolean isCountQuery() {
		return queryMethod.hasCountQueryAnnotation();
	}

	@Override
	public Object execute(Object[] parameters) {

		Class<?> clazz = queryMethod.getResultProcessor().getReturnedType().getDomainType();
		ParametersParameterAccessor accessor = new ParametersParameterAccessor(queryMethod.getParameters(), parameters);

		StringQuery stringQuery = createQuery(accessor);

		Assert.notNull(stringQuery, "unsupported query");

		if (queryMethod.hasAnnotatedHighlight()) {
			stringQuery.setHighlightQuery(queryMethod.getAnnotatedHighlightQuery());
		}

		IndexCoordinates index = elasticsearchOperations.getIndexCoordinatesFor(clazz);

		Object result = null;

		if (isCountQuery()) {
			result = elasticsearchOperations.count(stringQuery, clazz, index);
		} else if (queryMethod.isPageQuery()) {
			stringQuery.setPageable(accessor.getPageable());
			SearchHits<?> searchHits = elasticsearchOperations.search(stringQuery, clazz, index);
			if (queryMethod.isSearchPageMethod()) {
				result = SearchHitSupport.searchPageFor(searchHits, stringQuery.getPageable());
			} else {
				result = SearchHitSupport
						.unwrapSearchHits(SearchHitSupport.searchPageFor(searchHits, stringQuery.getPageable()));
			}
		} else if (queryMethod.isStreamQuery()) {
			stringQuery.setPageable(
					accessor.getPageable().isPaged() ? accessor.getPageable() : PageRequest.of(0, DEFAULT_STREAM_BATCH_SIZE));
			result = StreamUtils.createStreamFromIterator(elasticsearchOperations.searchForStream(stringQuery, clazz, index));
		} else if (queryMethod.isCollectionQuery()) {
			stringQuery.setPageable(accessor.getPageable().isPaged() ? accessor.getPageable() : Pageable.unpaged());
			result = elasticsearchOperations.search(stringQuery, clazz, index);
		} else {
			result = elasticsearchOperations.searchOne(stringQuery, clazz, index);
		}

		return (queryMethod.isNotSearchHitMethod() && queryMethod.isNotSearchPageMethod())
				? SearchHitSupport.unwrapSearchHits(result)
				: result;
	}

	protected StringQuery createQuery(ParametersParameterAccessor parameterAccessor) {
		String queryString = new StringQueryUtil(elasticsearchOperations.getElasticsearchConverter().getConversionService())
				.replacePlaceholders(this.query, parameterAccessor);
		return new StringQuery(queryString);
	}

}
