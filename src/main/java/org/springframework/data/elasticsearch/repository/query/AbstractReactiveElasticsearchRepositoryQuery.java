/*
 * Copyright 2019-2025 the original author or authors.
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
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHitSupport;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentProperty;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.BaseQuery;
import org.springframework.data.elasticsearch.core.query.ByQueryResponse;
import org.springframework.data.elasticsearch.core.query.DeleteQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.repository.query.ReactiveElasticsearchQueryExecution.ResultProcessingConverter;
import org.springframework.data.elasticsearch.repository.query.ReactiveElasticsearchQueryExecution.ResultProcessingExecution;
import org.springframework.data.expression.ValueEvaluationContextProvider;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.ResultProcessor;
import org.springframework.util.Assert;

/**
 * AbstractElasticsearchRepositoryQuery
 *
 * @author Christoph Strobl
 * @author Peter-Josef Meisch
 * @author Haibo Liu
 * @since 3.2
 */
abstract class AbstractReactiveElasticsearchRepositoryQuery implements RepositoryQuery {

	protected final ReactiveElasticsearchQueryMethod queryMethod;
	private final ReactiveElasticsearchOperations elasticsearchOperations;
	protected final ValueEvaluationContextProvider evaluationContextProvider;

	AbstractReactiveElasticsearchRepositoryQuery(ReactiveElasticsearchQueryMethod queryMethod,
			ReactiveElasticsearchOperations elasticsearchOperations,
			ValueEvaluationContextProvider evaluationContextProvider) {

		Assert.notNull(queryMethod, "queryMethod must not be null");
		Assert.notNull(elasticsearchOperations, "elasticsearchOperations must not be null");
		Assert.notNull(evaluationContextProvider, "evaluationContextProvider must not be null");

		this.queryMethod = queryMethod;
		this.elasticsearchOperations = elasticsearchOperations;
		this.evaluationContextProvider = evaluationContextProvider;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.RepositoryQuery#execute(java.lang.Object[])
	 */
	@Override
	public Object execute(Object[] parameters) {

		Object result = queryMethod.hasReactiveWrapperParameter() ? executeDeferred(parameters)
				: execute(new ReactiveElasticsearchParametersParameterAccessor(queryMethod, parameters));
		return queryMethod.isNotSearchHitMethod() ? SearchHitSupport.unwrapSearchHits(result) : result;
	}

	private Object executeDeferred(Object[] parameters) {

		ReactiveElasticsearchParametersParameterAccessor parameterAccessor = new ReactiveElasticsearchParametersParameterAccessor(
				queryMethod, parameters);

		if (getQueryMethod().isCollectionQuery()) {
			return Flux.defer(() -> (Publisher<Object>) execute(parameterAccessor));
		}

		return Mono.defer(() -> (Mono<Object>) execute(parameterAccessor));
	}

	private Object execute(ElasticsearchParametersParameterAccessor parameterAccessor) {

		ResultProcessor processor = queryMethod.getResultProcessor().withDynamicProjection(parameterAccessor);
		var returnedType = processor.getReturnedType();
		Class<?> domainType = returnedType.getDomainType();
		Class<?> typeToRead = returnedType.getTypeToRead();

		if (SearchHit.class.isAssignableFrom(typeToRead)) {
			typeToRead = queryMethod.unwrappedReturnType;
		}

		var query = createQuery(parameterAccessor);
		Assert.notNull(query, "unsupported query");

		queryMethod.addSpecialMethodParameters(query, parameterAccessor,
				elasticsearchOperations.getElasticsearchConverter(),
				evaluationContextProvider);

		String indexName = queryMethod.getEntityInformation().getIndexName();
		IndexCoordinates index = IndexCoordinates.of(indexName);

		ReactiveElasticsearchQueryExecution execution = getExecution(parameterAccessor,
				new ResultProcessingConverter(processor));

		return execution.execute(query, domainType, typeToRead, index);
	}

	/**
	 * Creates a {@link Query} instance using the given {@link ParameterAccessor}
	 *
	 * @param accessor must not be {@literal null}.
	 * @return
	 */
	protected abstract BaseQuery createQuery(ElasticsearchParametersParameterAccessor accessor);

	private ReactiveElasticsearchQueryExecution getExecution(ElasticsearchParameterAccessor accessor,
			Converter<Object, Object> resultProcessing) {
		return new ResultProcessingExecution(getExecutionToWrap(accessor, elasticsearchOperations), resultProcessing);
	}

	private ReactiveElasticsearchQueryExecution getExecutionToWrap(ElasticsearchParameterAccessor accessor,
			ReactiveElasticsearchOperations operations) {

		if (isDeleteQuery()) {
			return (query, type, targetType, indexCoordinates) -> operations
					.delete(DeleteQuery.builder(query).build(), type, indexCoordinates)
					.map(ByQueryResponse::getDeleted);
		} else if (isCountQuery()) {
			return (query, type, targetType, indexCoordinates) -> operations.count(query, type, indexCoordinates);
		} else if (isExistsQuery()) {
			return (query, type, targetType, indexCoordinates) -> operations.count(query, type, indexCoordinates)
					.map(count -> count > 0);
		} else if (queryMethod.isCollectionQuery()) {
			return (query, type, targetType, indexCoordinates) -> operations.search(query.setPageable(accessor.getPageable()),
					type, targetType, indexCoordinates);
		} else {
			return operations::search;
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
