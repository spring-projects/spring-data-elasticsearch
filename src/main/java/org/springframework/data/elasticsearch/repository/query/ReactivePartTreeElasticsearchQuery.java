/*
 * Copyright 2019 the original author or authors.
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

import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.repository.query.parser.ElasticsearchQueryCreator;
import org.springframework.data.repository.query.ResultProcessor;
import org.springframework.data.repository.query.parser.PartTree;

/**
 * @author Christoph Strobl
 * @since 3.2
 */
public class ReactivePartTreeElasticsearchQuery extends AbstractReactiveElasticsearchRepositoryQuery {

	private final PartTree tree;
	private final ResultProcessor processor;

	public ReactivePartTreeElasticsearchQuery(ReactiveElasticsearchQueryMethod queryMethod,
			ReactiveElasticsearchOperations elasticsearchOperations) {
		super(queryMethod, elasticsearchOperations);

		this.processor = queryMethod.getResultProcessor();
		this.tree = new PartTree(queryMethod.getName(), processor.getReturnedType().getDomainType());
	}

	@Override
	protected Query createQuery(ElasticsearchParameterAccessor accessor) {
		return new ElasticsearchQueryCreator(tree, accessor, getMappingContext()).createQuery();
	}

	@Override
	boolean isLimiting() {
		return tree.isLimiting();
	}

	@Override
	boolean isExistsQuery() {
		return tree.isExistsProjection();
	}

	@Override
	boolean isDeleteQuery() {
		return tree.isDelete();
	}

	@Override
	boolean isCountQuery() {
		return tree.isCountProjection();
	}
}
