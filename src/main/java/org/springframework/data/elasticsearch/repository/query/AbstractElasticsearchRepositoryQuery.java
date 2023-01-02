/*
 * Copyright 2013-2023 the original author or authors.
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
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHitSupport;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.SearchHitsImpl;
import org.springframework.data.elasticsearch.core.TotalHitsRelation;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.util.StreamUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.util.Collections;

/**
 * AbstractElasticsearchRepositoryQuery
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Peter-Josef Meisch
 */

public abstract class AbstractElasticsearchRepositoryQuery implements RepositoryQuery {

	protected static final int DEFAULT_STREAM_BATCH_SIZE = 500;
	protected ElasticsearchQueryMethod queryMethod;
	protected final ElasticsearchOperations elasticsearchOperations;
	protected final ElasticsearchConverter elasticsearchConverter;

	public AbstractElasticsearchRepositoryQuery(ElasticsearchQueryMethod queryMethod,
			ElasticsearchOperations elasticsearchOperations) {
		this.queryMethod = queryMethod;
		this.elasticsearchOperations = elasticsearchOperations;
		this.elasticsearchConverter = elasticsearchOperations.getElasticsearchConverter();
	}

	@Override
	public QueryMethod getQueryMethod() {
		return queryMethod;
	}

	/**
	 * @return {@literal true} if this is a count query
	 * @since 4.2
	 */
	public abstract boolean isCountQuery();

	protected abstract boolean isDeleteQuery();

	protected abstract boolean isExistsQuery();

	@Override
	public Object execute(Object[] parameters) {

		ParametersParameterAccessor parameterAccessor = getParameterAccessor(parameters);
		Class<?> clazz = getResultClass();
		Query query = createQuery(parameters);

		IndexCoordinates index = elasticsearchOperations.getIndexCoordinatesFor(clazz);

		Object result = null;

		if (isDeleteQuery()) {
			result = countOrGetDocumentsForDelete(query, parameterAccessor);
			elasticsearchOperations.delete(query, clazz, index);
			elasticsearchOperations.indexOps(index).refresh();
		} else if (isCountQuery()) {
			result = elasticsearchOperations.count(query, clazz, index);
		} else if (isExistsQuery()) {
			result = elasticsearchOperations.count(query, clazz, index) > 0;
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

			if (parameterAccessor.getPageable().isUnpaged()) {
				int itemCount = (int) elasticsearchOperations.count(query, clazz, index);

				if (itemCount == 0) {
					result = new SearchHitsImpl<>(0, TotalHitsRelation.EQUAL_TO, Float.NaN, null,
						query.getPointInTime() != null ? query.getPointInTime().id() : null, Collections.emptyList(), null, null);
				} else {
					query.setPageable(PageRequest.of(0, Math.max(1, itemCount)));
				}
			} else {
				query.setPageable(parameterAccessor.getPageable());
			}

			if (result == null) {
				result = elasticsearchOperations.search(query, clazz, index);
			}

		} else {
			result = elasticsearchOperations.searchOne(query, clazz, index);
		}

		return (queryMethod.isNotSearchHitMethod() && queryMethod.isNotSearchPageMethod())
			? SearchHitSupport.unwrapSearchHits(result)
			: result;
	}

	public Query createQuery(Object[] parameters) {

		Class<?> clazz = getResultClass();
		ParametersParameterAccessor parameterAccessor = getParameterAccessor(parameters);
		Query query = createQuery(parameterAccessor);

		Assert.notNull(query, "unsupported query");

		if (queryMethod.hasAnnotatedHighlight()) {
			query.setHighlightQuery(queryMethod.getAnnotatedHighlightQuery());
		}

		var sourceFilter = queryMethod.getSourceFilter(parameterAccessor,
				elasticsearchOperations.getElasticsearchConverter());
		if (sourceFilter != null) {
			query.addSourceFilter(sourceFilter);
		}

		// todo #2338 remove that call, this should be done when the real request is built
//		elasticsearchConverter.updateQuery(query, clazz);

		return query;
	}

	private Class<?> getResultClass() {
		return queryMethod.getResultProcessor().getReturnedType().getDomainType();
	}

	private ParametersParameterAccessor getParameterAccessor(Object[] parameters) {
		return new ParametersParameterAccessor(queryMethod.getParameters(), parameters);
	}

	@Nullable
	private Object countOrGetDocumentsForDelete(Query query, ParametersParameterAccessor accessor) {

		Object result = null;
		Class<?> entityClass = queryMethod.getEntityInformation().getJavaType();
		IndexCoordinates index = elasticsearchOperations.getIndexCoordinatesFor(entityClass);

		if (queryMethod.isCollectionQuery()) {

			if (accessor.getPageable().isUnpaged()) {
				int itemCount = (int) elasticsearchOperations.count(query, entityClass, index);
				query.setPageable(PageRequest.of(0, Math.max(1, itemCount)));
			} else {
				query.setPageable(accessor.getPageable());
			}
			result = elasticsearchOperations.search(query, entityClass, index);
		}

		if (ClassUtils.isAssignable(Number.class, queryMethod.getReturnedObjectType())) {
			result = elasticsearchOperations.count(query, entityClass, index);
		}

		return result;
	}

	protected abstract Query createQuery(ParametersParameterAccessor accessor);
}
