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

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.reactivestreams.Publisher;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentProperty;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.repository.query.ReactiveElasticsearchQueryExecution.ResultProcessingConverter;
import org.springframework.data.elasticsearch.repository.query.ReactiveElasticsearchQueryExecution.ResultProcessingExecution;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.ResultProcessor;

/**
 * AbstractElasticsearchRepositoryQuery
 *
 * @author Christoph Strobl
 * @since 3.2
 */
abstract class AbstractReactiveElasticsearchRepositoryQuery implements RepositoryQuery {

	private final ReactiveElasticsearchQueryMethod queryMethod;
	private final ReactiveElasticsearchOperations elasticsearchOperations;

	AbstractReactiveElasticsearchRepositoryQuery(ReactiveElasticsearchQueryMethod queryMethod,
			ReactiveElasticsearchOperations elasticsearchOperations) {

		this.queryMethod = queryMethod;
		this.elasticsearchOperations = elasticsearchOperations;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.RepositoryQuery#execute(java.lang.Object[])
	 */
	public Object execute(Object[] parameters) {

		return queryMethod.hasReactiveWrapperParameter() ? executeDeferred(parameters)
				: execute(new ReactiveElasticsearchParametersParameterAccessor(queryMethod, parameters));
	}

	private Object executeDeferred(Object[] parameters) {

		ReactiveElasticsearchParametersParameterAccessor parameterAccessor = new ReactiveElasticsearchParametersParameterAccessor(
				queryMethod, parameters);

		if (getQueryMethod().isCollectionQuery()) {
			return Flux.defer(() -> (Publisher<Object>) execute(parameterAccessor));
		}

		return Mono.defer(() -> (Mono<Object>) execute(parameterAccessor));
	}

	private Object execute(ElasticsearchParameterAccessor parameterAccessor) {

		ResultProcessor processor = queryMethod.getResultProcessor().withDynamicProjection(parameterAccessor);

		Query query = createQuery(
				new ConvertingParameterAccessor(elasticsearchOperations.getElasticsearchConverter(), parameterAccessor));

		Class<?> typeToRead = processor.getReturnedType().getTypeToRead();
		String indexName = queryMethod.getEntityInformation().getIndexName();
		String indexTypeName = queryMethod.getEntityInformation().getIndexTypeName();

		ReactiveElasticsearchQueryExecution execution = getExecution(parameterAccessor,
				new ResultProcessingConverter(processor, elasticsearchOperations));

		return execution.execute(query, processor.getReturnedType().getDomainType(), indexName, indexTypeName, typeToRead);
	}

	private ReactiveElasticsearchQueryExecution getExecution(ElasticsearchParameterAccessor accessor,
			Converter<Object, Object> resultProcessing) {
		return new ResultProcessingExecution(getExecutionToWrap(accessor, elasticsearchOperations), resultProcessing);
	}

	/**
	 * Creates a {@link Query} instance using the given {@link ParameterAccessor}
	 *
	 * @param accessor must not be {@literal null}.
	 * @return
	 */
	protected abstract Query createQuery(ElasticsearchParameterAccessor accessor);

	private ReactiveElasticsearchQueryExecution getExecutionToWrap(ElasticsearchParameterAccessor accessor,
			ReactiveElasticsearchOperations operations) {

		if (isDeleteQuery()) {
			return (q, t, i, it, tt) -> operations.deleteBy(q, t, i, it);
		} else if (isCountQuery()) {
			return (q, t, i, it, tt) -> operations.count(q, t, i, it);
		} else if (isExistsQuery()) {
			return (q, t, i, it, tt) -> operations.count(q, t, i, it).map(count -> count > 0);
		} else if (queryMethod.isCollectionQuery()) {
			return (q, t, i, it, tt) -> operations.find(q.setPageable(accessor.getPageable()), t, i, it, tt);
		} else {
			return (q, t, i, it, tt) -> operations.find(q, t, i, it, tt);
		}
	}

	abstract boolean isDeleteQuery();

	abstract boolean isCountQuery();

	abstract boolean isExistsQuery();

	abstract boolean isLimiting();

	@Override
	public QueryMethod getQueryMethod() {
		return queryMethod;
	}

	protected ReactiveElasticsearchOperations getElasticsearchOperations() {
		return elasticsearchOperations;
	}

	protected MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> getMappingContext() {
		return elasticsearchOperations.getElasticsearchConverter().getMappingContext();
	}
}
