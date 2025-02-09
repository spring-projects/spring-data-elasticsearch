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
package org.springframework.data.elasticsearch.repository.support;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Optional;

import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentProperty;
import org.springframework.data.elasticsearch.repository.query.ReactiveElasticsearchQueryMethod;
import org.springframework.data.elasticsearch.repository.query.ReactivePartTreeElasticsearchQuery;
import org.springframework.data.elasticsearch.repository.query.ReactiveRepositorySearchTemplateQuery;
import org.springframework.data.elasticsearch.repository.query.ReactiveRepositoryStringQuery;
import org.springframework.data.elasticsearch.repository.support.querybyexample.ReactiveQueryByExampleElasticsearchExecutor;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.ReactiveRepositoryFactorySupport;
import org.springframework.data.repository.core.support.RepositoryComposition;
import org.springframework.data.repository.core.support.RepositoryFragment;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.ValueExpressionDelegate;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Factory to create {@link org.springframework.data.elasticsearch.repository.ReactiveElasticsearchRepository}
 * instances.
 *
 * @author Christoph Strobl
 * @author Ivan Greene
 * @author Ezequiel Antúnez Camacho
 * @author Haibo Liu
 * @since 3.2
 */
public class ReactiveElasticsearchRepositoryFactory extends ReactiveRepositoryFactorySupport {

	private final ReactiveElasticsearchOperations operations;
	private final MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> mappingContext;

	/**
	 * Creates a new {@link ReactiveElasticsearchRepositoryFactory} with the given
	 * {@link ReactiveElasticsearchOperations}.
	 *
	 * @param elasticsearchOperations must not be {@literal null}.
	 */
	public ReactiveElasticsearchRepositoryFactory(ReactiveElasticsearchOperations elasticsearchOperations) {

		Assert.notNull(elasticsearchOperations, "ReactiveElasticsearchOperations must not be null!");

		this.operations = elasticsearchOperations;
		this.mappingContext = elasticsearchOperations.getElasticsearchConverter().getMappingContext();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.support.RepositoryFactorySupport#getRepositoryBaseClass(org.springframework.data.repository.core.RepositoryMetadata)
	 */
	@Override
	protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
		return SimpleReactiveElasticsearchRepository.class;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.support.RepositoryFactorySupport#getTargetRepository(org.springframework.data.repository.core.RepositoryInformation)
	 */
	@Override
	protected Object getTargetRepository(RepositoryInformation information) {

		ElasticsearchEntityInformation<?, Serializable> entityInformation = getEntityInformation(
				information.getDomainType(), information);
		return getTargetRepositoryViaReflection(information, entityInformation, operations);
	}

	@Override
	protected Optional<QueryLookupStrategy> getQueryLookupStrategy(@Nullable Key key,
			ValueExpressionDelegate valueExpressionDelegate) {
		return Optional.of(new ElasticsearchQueryLookupStrategy(operations, valueExpressionDelegate, mappingContext));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.support.RepositoryFactorySupport#getEntityInformation(java.lang.Class)
	 */
	@Override
	public <T, ID> ElasticsearchEntityInformation<T, ID> getEntityInformation(Class<T> domainClass) {
		return getEntityInformation(domainClass, null);
	}

	@SuppressWarnings("unchecked")
	private <T, ID> ElasticsearchEntityInformation<T, ID> getEntityInformation(Class<T> domainClass,
			@Nullable RepositoryInformation information) {

		ElasticsearchPersistentEntity<?> entity = mappingContext.getRequiredPersistentEntity(domainClass);
		return new MappingElasticsearchEntityInformation<>((ElasticsearchPersistentEntity<T>) entity);
	}

	@Override
	protected RepositoryMetadata getRepositoryMetadata(Class<?> repositoryInterface) {
		return new ReactiveElasticsearchRepositoryMetadata(repositoryInterface);
	}

	/**
	 * @author Christoph Strobl
	 */
	private static class ElasticsearchQueryLookupStrategy implements QueryLookupStrategy {

		private final ReactiveElasticsearchOperations operations;
		private final ValueExpressionDelegate valueExpressionDelegate;
		private final MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> mappingContext;

		public ElasticsearchQueryLookupStrategy(ReactiveElasticsearchOperations operations,
				ValueExpressionDelegate valueExpressionDelegate,
				MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> mappingContext) {

			Assert.notNull(operations, "operations must not be null");
			Assert.notNull(valueExpressionDelegate, "evaluationContextProvider must not be null");
			Assert.notNull(mappingContext, "mappingContext must not be null");

			this.operations = operations;
			this.valueExpressionDelegate = valueExpressionDelegate;
			this.mappingContext = mappingContext;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.repository.query.QueryLookupStrategy#resolveQuery(java.lang.reflect.Method, org.springframework.data.repository.core.RepositoryMetadata, org.springframework.data.projection.ProjectionFactory, org.springframework.data.repository.core.NamedQueries)
		 */
		@Override
		public RepositoryQuery resolveQuery(Method method, RepositoryMetadata metadata, ProjectionFactory factory,
				NamedQueries namedQueries) {

			ReactiveElasticsearchQueryMethod queryMethod = new ReactiveElasticsearchQueryMethod(method, metadata, factory,
					mappingContext);
			String namedQueryName = queryMethod.getNamedQueryName();

			if (namedQueries.hasQuery(namedQueryName)) {
				String namedQuery = namedQueries.getQuery(namedQueryName);

				return new ReactiveRepositoryStringQuery(namedQuery, queryMethod, operations,
						valueExpressionDelegate);
			} else if (queryMethod.hasAnnotatedQuery()) {
				return new ReactiveRepositoryStringQuery(queryMethod, operations, valueExpressionDelegate);
			} else if (queryMethod.hasAnnotatedSearchTemplateQuery()) {
				var searchTemplateQuery = queryMethod.getAnnotatedSearchTemplateQuery();
				return new ReactiveRepositorySearchTemplateQuery(queryMethod, operations, valueExpressionDelegate,
						searchTemplateQuery.id());
			} else {
				return new ReactivePartTreeElasticsearchQuery(queryMethod, operations, valueExpressionDelegate);
			}
		}
	}

	@Override
	protected RepositoryComposition.RepositoryFragments getRepositoryFragments(RepositoryMetadata metadata) {
		RepositoryComposition.RepositoryFragments fragments = RepositoryComposition.RepositoryFragments.empty();

		if (ReactiveQueryByExampleExecutor.class.isAssignableFrom(metadata.getRepositoryInterface())) {
			fragments = fragments.append(RepositoryFragment.implemented(ReactiveQueryByExampleExecutor.class,
					instantiateClass(ReactiveQueryByExampleElasticsearchExecutor.class, operations)));
		}

		return fragments;
	}
}
