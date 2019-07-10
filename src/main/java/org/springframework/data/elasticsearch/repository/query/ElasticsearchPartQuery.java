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

import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentProperty;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.repository.query.parser.ElasticsearchQueryCreator;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.data.util.CloseableIterator;
import org.springframework.data.util.StreamUtils;
import org.springframework.util.ClassUtils;

/**
 * ElasticsearchPartQuery
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Kevin Leturc
 * @author Mark Paluch
 * @author Rasmus Faber-Espensen
 */
public class ElasticsearchPartQuery extends AbstractElasticsearchRepositoryQuery {

	private static final int DEFAULT_STREAM_BATCH_SIZE = 500;

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
		if (tree.isDelete()) {

			Object result = countOrGetDocumentsForDelete(query, accessor);
			elasticsearchOperations.delete(query, queryMethod.getEntityInformation().getJavaType());
			return result;
		} else if (queryMethod.isPageQuery()) {

			query.setPageable(accessor.getPageable());
			return elasticsearchOperations.queryForPage(query, queryMethod.getEntityInformation().getJavaType());
		} else if (queryMethod.isStreamQuery()) {

			Class<?> entityType = queryMethod.getEntityInformation().getJavaType();
			if (accessor.getPageable().isUnpaged()) {

				query.setPageable(PageRequest.of(0, DEFAULT_STREAM_BATCH_SIZE));
			} else {

				query.setPageable(accessor.getPageable());
			}

			return StreamUtils
					.createStreamFromIterator((CloseableIterator<Object>) elasticsearchOperations.stream(query, entityType));

        } else if (queryMethod.isCollectionQuery()) {

            if (accessor.getPageable().isUnpaged()) {

                int itemCount = (int) elasticsearchOperations.count(query, queryMethod.getEntityInformation().getJavaType());
                query.setPageable(PageRequest.of(0, Math.max(1, itemCount)));
            } else {

                query.setPageable(accessor.getPageable());
            }

            return elasticsearchOperations.queryForList(query, queryMethod.getEntityInformation().getJavaType());
        } else if (tree.isCountProjection()) {

		    return elasticsearchOperations.count(query, queryMethod.getEntityInformation().getJavaType());
        }

		return elasticsearchOperations.queryForObject(query, queryMethod.getEntityInformation().getJavaType());
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
