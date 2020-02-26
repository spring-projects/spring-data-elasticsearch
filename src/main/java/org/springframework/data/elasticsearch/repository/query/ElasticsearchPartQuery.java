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

import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHitSupport;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentProperty;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.repository.query.parser.ElasticsearchQueryCreator;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.data.util.StreamUtils;
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

	private static final int DEFAULT_STREAM_BATCH_SIZE = 500;

	private final PartTree tree;
	private final ElasticsearchConverter elasticsearchConverter;
	private final MappingContext<?, ElasticsearchPersistentProperty> mappingContext;

	public ElasticsearchPartQuery(ElasticsearchQueryMethod method, ElasticsearchOperations elasticsearchOperations) {
		super(method, elasticsearchOperations);
		this.tree = new PartTree(method.getName(), method.getEntityInformation().getJavaType());
		this.elasticsearchConverter = elasticsearchOperations.getElasticsearchConverter();
		this.mappingContext = elasticsearchConverter.getMappingContext();
	}

	@Override
	public Object execute(Object[] parameters) {
		Class<?> clazz = queryMethod.getEntityInformation().getJavaType();
		ParametersParameterAccessor accessor = new ParametersParameterAccessor(queryMethod.getParameters(), parameters);

		CriteriaQuery query = createQuery(accessor);

		Assert.notNull(query, "unsupported query");

		elasticsearchConverter.updateQuery(query, clazz);

		if (queryMethod.hasAnnotatedHighlight()) {
			query.setHighlightQuery(queryMethod.getAnnotatedHighlightQuery());
		}

		IndexCoordinates index = elasticsearchOperations.getIndexCoordinatesFor(clazz);

		Object result = null;

		if (tree.isLimiting()) {
			query.setMaxResults(tree.getMaxResults());
		}

		if (tree.isDelete()) {
			result = countOrGetDocumentsForDelete(query, accessor);
			elasticsearchOperations.delete(query, clazz, index);
			elasticsearchOperations.indexOps(index).refresh();
		} else if (queryMethod.isPageQuery()) {
			query.setPageable(accessor.getPageable());
			SearchHits<?> searchHits = elasticsearchOperations.search(query, clazz, index);
			if (queryMethod.isSearchPageMethod()) {
				result = SearchHitSupport.searchPageFor(searchHits, query.getPageable());
			} else {
				result = SearchHitSupport.page(searchHits, query.getPageable());
			}
		} else if (queryMethod.isStreamQuery()) {
			if (accessor.getPageable().isUnpaged()) {
				query.setPageable(PageRequest.of(0, DEFAULT_STREAM_BATCH_SIZE));
			} else {
				query.setPageable(accessor.getPageable());
			}
			result = StreamUtils.createStreamFromIterator(elasticsearchOperations.searchForStream(query, clazz, index));
		} else if (queryMethod.isCollectionQuery()) {

			if (accessor.getPageable().isUnpaged()) {
				int itemCount = (int) elasticsearchOperations.count(query, clazz, index);
				query.setPageable(PageRequest.of(0, Math.max(1, itemCount)));
			} else {
				query.setPageable(accessor.getPageable());
			}

			result = elasticsearchOperations.search(query, clazz, index);

		} else if (tree.isCountProjection()) {
			result = elasticsearchOperations.count(query, clazz, index);
		} else {
			result = elasticsearchOperations.searchOne(query, clazz, index);
		}

		return queryMethod.isNotSearchHitMethod() ? SearchHitSupport.unwrapSearchHits(result) : result;
	}

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
