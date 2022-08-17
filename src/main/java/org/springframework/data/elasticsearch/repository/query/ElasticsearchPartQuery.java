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

import java.util.Collections;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHitSupport;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.SearchHitsImpl;
import org.springframework.data.elasticsearch.core.TotalHitsRelation;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentProperty;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.repository.query.parser.ElasticsearchQueryCreator;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.data.util.StreamUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * ElasticsearchPartQuery
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Kevin Leturc
 * @author Mark Paluch
 * @author Rasmus Faber-Espensen
 * @author Peter-Josef Meisch
 */
public class ElasticsearchPartQuery extends AbstractElasticsearchRepositoryQuery {

	private final PartTree tree;
	private final MappingContext<?, ElasticsearchPersistentProperty> mappingContext;

	public ElasticsearchPartQuery(ElasticsearchQueryMethod method, ElasticsearchOperations elasticsearchOperations) {
		super(method, elasticsearchOperations);
		this.tree = new PartTree(queryMethod.getName(), queryMethod.getResultProcessor().getReturnedType().getDomainType());
		this.mappingContext = elasticsearchConverter.getMappingContext();
	}

	@Override
	public boolean isCountQuery() {
		return tree.isCountProjection();
	}

	@Override
	public Object execute(Object[] parameters) {

		Class<?> clazz = queryMethod.getResultProcessor().getReturnedType().getDomainType();
		ParametersParameterAccessor parameterAccessor = new ParametersParameterAccessor(queryMethod.getParameters(),
				parameters);

		CriteriaQuery query = createQuery(parameterAccessor);

		Assert.notNull(query, "unsupported query");

		prepareQuery(query, clazz, parameterAccessor);

		IndexCoordinates index = elasticsearchOperations.getIndexCoordinatesFor(clazz);

		Object result = null;

		if (tree.isLimiting()) {
			// noinspection ConstantConditions
			query.setMaxResults(tree.getMaxResults());
		}

		if (tree.isDelete()) {
			result = countOrGetDocumentsForDelete(query, parameterAccessor);
			elasticsearchOperations.delete(query, clazz, index);
			elasticsearchOperations.indexOps(index).refresh();
		} else if (queryMethod.isPageQuery()) {
			query.setPageable(parameterAccessor.getPageable());
			SearchHits<?> searchHits = elasticsearchOperations.search(query, clazz, index);
			if (queryMethod.isSearchPageMethod()) {
				result = SearchHitSupport.searchPageFor(searchHits, query.getPageable());
			} else {
				result = SearchHitSupport.unwrapSearchHits(SearchHitSupport.searchPageFor(searchHits, query.getPageable()));
			}
		} else if (queryMethod.isStreamQuery()) {
			if (parameterAccessor.getPageable().isUnpaged()) {
				query.setPageable(PageRequest.of(0, DEFAULT_STREAM_BATCH_SIZE));
			} else {
				query.setPageable(parameterAccessor.getPageable());
			}
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

		} else if (tree.isCountProjection()) {
			result = elasticsearchOperations.count(query, clazz, index);
		} else if (tree.isExistsProjection()) {
			long count = elasticsearchOperations.count(query, clazz, index);
			result = count > 0;
		} else {
			result = elasticsearchOperations.searchOne(query, clazz, index);
		}

		return (queryMethod.isNotSearchHitMethod() && queryMethod.isNotSearchPageMethod())
				? SearchHitSupport.unwrapSearchHits(result)
				: result;
	}

	@Nullable
	private Object countOrGetDocumentsForDelete(CriteriaQuery query, ParametersParameterAccessor accessor) {

		Object result = null;
		Class<?> clazz = queryMethod.getEntityInformation().getJavaType();
		IndexCoordinates index = elasticsearchOperations.getIndexCoordinatesFor(clazz);

		if (queryMethod.isCollectionQuery()) {

			if (accessor.getPageable().isUnpaged()) {
				int itemCount = (int) elasticsearchOperations.count(query, clazz, index);
				query.setPageable(PageRequest.of(0, Math.max(1, itemCount)));
			} else {
				query.setPageable(accessor.getPageable());
			}
			result = elasticsearchOperations.search(query, clazz, index);
		}

		if (ClassUtils.isAssignable(Number.class, queryMethod.getReturnedObjectType())) {
			result = elasticsearchOperations.count(query, clazz, index);
		}
		return result;
	}

	public CriteriaQuery createQuery(ParametersParameterAccessor accessor) {
		return new ElasticsearchQueryCreator(tree, accessor, mappingContext).createQuery();
	}
}
