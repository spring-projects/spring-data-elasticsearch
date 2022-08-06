/*
 * Copyright 2013-2022 the original author or authors.
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

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHitSupport;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.data.elasticsearch.repository.support.StringQueryUtil;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.data.util.StreamUtils;
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

	private final String queryString;

	public ElasticsearchStringQuery(ElasticsearchQueryMethod queryMethod, ElasticsearchOperations elasticsearchOperations,
			String queryString) {
		super(queryMethod, elasticsearchOperations);
		Assert.notNull(queryString, "Query cannot be empty");
		this.queryString = queryString;
	}

	@Override
	public boolean isCountQuery() {
		return queryMethod.hasCountQueryAnnotation();
	}

	@Override
	public Object execute(Object[] parameters) {

		Class<?> clazz = queryMethod.getResultProcessor().getReturnedType().getDomainType();
		ParametersParameterAccessor parameterAccessor = new ParametersParameterAccessor(queryMethod.getParameters(),
				parameters);

		Query query = createQuery(parameterAccessor);
		Assert.notNull(query, "unsupported query");

		if (queryMethod.hasAnnotatedHighlight()) {
			query.setHighlightQuery(queryMethod.getAnnotatedHighlightQuery());
		}

		prepareQuery(query, clazz, parameterAccessor);

		IndexCoordinates index = elasticsearchOperations.getIndexCoordinatesFor(clazz);

		Object result;

		if (isCountQuery()) {
			result = elasticsearchOperations.count(query, clazz, index);
		} else if (queryMethod.isPageQuery()) {
			query.setPageable(parameterAccessor.getPageable());
			SearchHits<?> searchHits = elasticsearchOperations.search(query, clazz, index);
			if (queryMethod.isSearchPageMethod()) {
				result = SearchHitSupport.searchPageFor(searchHits, query.getPageable());
			} else {
				result = SearchHitSupport.unwrapSearchHits(SearchHitSupport.searchPageFor(searchHits, query.getPageable()));
			}
		} else if (queryMethod.isStreamQuery()) {
			query.setPageable(parameterAccessor.getPageable().isPaged() ? parameterAccessor.getPageable()
					: PageRequest.of(0, DEFAULT_STREAM_BATCH_SIZE));
			result = StreamUtils.createStreamFromIterator(elasticsearchOperations.searchForStream(query, clazz, index));
		} else if (queryMethod.isCollectionQuery()) {
			query.setPageable(
					parameterAccessor.getPageable().isPaged() ? parameterAccessor.getPageable() : Pageable.unpaged());
			result = elasticsearchOperations.search(query, clazz, index);
		} else {
			result = elasticsearchOperations.searchOne(query, clazz, index);
		}

		return (queryMethod.isNotSearchHitMethod() && queryMethod.isNotSearchPageMethod())
				? SearchHitSupport.unwrapSearchHits(result)
				: result;
	}

	protected StringQuery createQuery(ParametersParameterAccessor parameterAccessor) {
		String queryString = new StringQueryUtil(elasticsearchOperations.getElasticsearchConverter().getConversionService())
				.replacePlaceholders(this.queryString, parameterAccessor);
		return new StringQuery(queryString);
	}

}
