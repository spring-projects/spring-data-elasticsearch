/*
 * Copyright 2013-2024 the original author or authors.
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
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentProperty;
import org.springframework.data.elasticsearch.core.query.BaseQuery;
import org.springframework.data.elasticsearch.repository.query.parser.ElasticsearchQueryCreator;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.data.repository.query.parser.PartTree;

/**
 * ElasticsearchPartQuery
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Kevin Leturc
 * @author Mark Paluch
 * @author Rasmus Faber-Espensen
 * @author Peter-Josef Meisch
 * @author Haibo Liu
 */
public class ElasticsearchPartQuery extends AbstractElasticsearchRepositoryQuery {

	private final PartTree tree;
	private final MappingContext<?, ElasticsearchPersistentProperty> mappingContext;

	public ElasticsearchPartQuery(ElasticsearchQueryMethod method, ElasticsearchOperations elasticsearchOperations,
			QueryMethodEvaluationContextProvider evaluationContextProvider) {
		super(method, elasticsearchOperations, evaluationContextProvider);
		this.tree = new PartTree(queryMethod.getName(), queryMethod.getResultProcessor().getReturnedType().getDomainType());
		this.mappingContext = elasticsearchConverter.getMappingContext();
	}

	@Override
	public boolean isCountQuery() {
		return tree.isCountProjection();
	}

	@Override
	protected boolean isDeleteQuery() {
		return tree.isDelete();
	}

	@Override
	protected boolean isExistsQuery() {
		return tree.isExistsProjection();
	}

	protected BaseQuery createQuery(ElasticsearchParametersParameterAccessor accessor) {

		BaseQuery query = new ElasticsearchQueryCreator(tree, accessor, mappingContext).createQuery();

		if (tree.getMaxResults() != null) {
			query.setMaxResults(tree.getMaxResults());
		}

		return query;
	}
}
