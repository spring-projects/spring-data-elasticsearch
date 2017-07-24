/*
 * Copyright 2013-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import org.springframework.data.elasticsearch.core.SearchResultMapper;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentProperty;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.data.elasticsearch.repository.query.parser.ElasticsearchQueryCreator;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.util.ClassUtils;
import org.springframework.data.util.CloseableIterator;
import org.springframework.data.util.StreamUtils;

/**
 * ElasticsearchPartQuery
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Kevin Leturc
 * @author Mark Paluch
 * @author zzt
 */
public class ElasticsearchPartQuery extends AbstractElasticsearchRepositoryQuery {

	private final PartTree tree;
	private final MappingContext<?, ElasticsearchPersistentProperty> mappingContext;

	public ElasticsearchPartQuery(ElasticsearchQueryMethod method, ElasticsearchOperations elasticsearchOperations) {
		super(method, elasticsearchOperations);
		this.tree = new PartTree(method.getName(), method.getEntityInformation().getJavaType());
		this.mappingContext = elasticsearchOperations.getElasticsearchConverter().getMappingContext();
	}

	@Override
	public Object execute(Object[] parameters) {
		ParametersParameterAccessor accessor = new ParametersParameterAccessor(queryMethod.getParameters(), parameters);
		CriteriaQuery query = createQuery(accessor);
		if(tree.isDelete()) {
			Object result = countOrGetDocumentsForDelete(query, accessor);
			elasticsearchOperations.delete(query, queryMethod.getEntityInformation().getJavaType());
			return result;
		} else if (tree.isCountProjection()) {
			return elasticsearchOperations.count(query, queryMethod.getEntityInformation().getJavaType());
		}

		if (queryMethod.hasExtras()) {
			NativeSearchQuery searchQuery = queryMethod.toNativeSearchBuilder()
					.withQuery(query.toQuery()).withFilter(query.toFilter()).build();
			return queryDiff(accessor, searchQuery, queryMethod.getHighlightMapper());
		}
		return queryDiff(accessor, query, null);

	}

	private Object queryDiff(ParametersParameterAccessor accessor, Query query, SearchResultMapper mapper) {
		Class<?> javaType = queryMethod.getEntityInformation().getJavaType();
		if(queryMethod.isPageQuery()) {
			query.setPageable(accessor.getPageable());
			return mapper == null ? elasticsearchOperations.queryForPage((CriteriaQuery) query, javaType)
					: elasticsearchOperations.queryForPage((SearchQuery) query, javaType, mapper);
		} else if (queryMethod.isStreamQuery()) {
			if (query.getPageable().isUnpaged()) {
				int itemCount = (int) elasticsearchOperations.count((CriteriaQuery) query, javaType);
				query.setPageable(PageRequest.of(0, Math.max(1, itemCount)));
			}

			CloseableIterator<?> stream = mapper == null ? elasticsearchOperations.stream((CriteriaQuery) query, javaType)
					: elasticsearchOperations.stream((SearchQuery) query, javaType, mapper);
			return StreamUtils.createStreamFromIterator((CloseableIterator<Object>) stream);

		} else if (queryMethod.isCollectionQuery()) {
			if (accessor.getPageable() == null) {
				int itemCount = (int) elasticsearchOperations.count((CriteriaQuery) query, javaType);
				query.setPageable(PageRequest.of(0, Math.max(1, itemCount)));
			} else {
				query.setPageable(accessor.getPageable());
			}
			return mapper == null ? elasticsearchOperations.queryForList((CriteriaQuery) query, javaType)
					: elasticsearchOperations.queryForList((SearchQuery) query, javaType, mapper);
		}
		return mapper == null ? elasticsearchOperations.queryForObject((CriteriaQuery) query, javaType)
				: elasticsearchOperations.queryForObject((SearchQuery) query, javaType, mapper);
	}

	private Object countOrGetDocumentsForDelete(CriteriaQuery query, ParametersParameterAccessor accessor) {

		Object result = null;

		if (queryMethod.isCollectionQuery()) {
			if (accessor.getPageable().isUnpaged()) {
				int itemCount = (int) elasticsearchOperations.count(query, queryMethod.getEntityInformation().getJavaType());
				query.setPageable(PageRequest.of(0, Math.max(1, itemCount)));
			} else {
				query.setPageable(accessor.getPageable());
			}
			result = elasticsearchOperations.queryForList(query, queryMethod.getEntityInformation().getJavaType());
		}

		if (ClassUtils.isAssignable(Number.class, queryMethod.getReturnedObjectType())) {
			result = elasticsearchOperations.count(query, queryMethod.getEntityInformation().getJavaType());
		}
		return result;
	}

	public CriteriaQuery createQuery(ParametersParameterAccessor accessor) {
		return new ElasticsearchQueryCreator(tree, accessor, mappingContext).createQuery();
	}
}
