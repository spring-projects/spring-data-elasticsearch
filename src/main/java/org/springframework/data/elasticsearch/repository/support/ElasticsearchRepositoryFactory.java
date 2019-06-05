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
package org.springframework.data.elasticsearch.repository.support;

import static org.springframework.data.querydsl.QuerydslUtils.*;

import java.lang.reflect.Method;
import java.util.Optional;

import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.elasticsearch.repository.query.ElasticsearchPartQuery;
import org.springframework.data.elasticsearch.repository.query.ElasticsearchQueryMethod;
import org.springframework.data.elasticsearch.repository.query.ElasticsearchStringQuery;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.util.Assert;

/**
 * Factory to create {@link ElasticsearchRepository}
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Ryan Henszey
 * @author Gad Akuka
 * @author Mark Paluch
 * @author Christoph Strobl
 * @author Sascha Woo
 */
public class ElasticsearchRepositoryFactory extends RepositoryFactorySupport {

	private final ElasticsearchOperations elasticsearchOperations;
	private final ElasticsearchEntityInformationCreator entityInformationCreator;

	public ElasticsearchRepositoryFactory(ElasticsearchOperations elasticsearchOperations) {

		Assert.notNull(elasticsearchOperations, "ElasticsearchOperations must not be null!");

		this.elasticsearchOperations = elasticsearchOperations;
		this.entityInformationCreator = new ElasticsearchEntityInformationCreatorImpl(
				elasticsearchOperations.getElasticsearchConverter().getMappingContext());
	}

	@Override
	public <T, ID> ElasticsearchEntityInformation<T, ID> getEntityInformation(Class<T> domainClass) {
		return entityInformationCreator.getEntityInformation(domainClass);
	}

	@Override
	protected Object getTargetRepository(RepositoryInformation metadata) {
		return getTargetRepositoryViaReflection(metadata, getEntityInformation(metadata.getDomainType()),
				elasticsearchOperations);
	}

	@Override
	protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
		if (isQueryDslRepository(metadata.getRepositoryInterface())) {
			throw new IllegalArgumentException("QueryDsl Support has not been implemented yet.");
		}

		return SimpleElasticsearchRepository.class;
	}

	private static boolean isQueryDslRepository(Class<?> repositoryInterface) {
		return QUERY_DSL_PRESENT && QuerydslPredicateExecutor.class.isAssignableFrom(repositoryInterface);
	}

	@Override
	protected Optional<QueryLookupStrategy> getQueryLookupStrategy(Key key,
			QueryMethodEvaluationContextProvider evaluationContextProvider) {
		return Optional.of(new ElasticsearchQueryLookupStrategy());
	}

	private class ElasticsearchQueryLookupStrategy implements QueryLookupStrategy {

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.repository.query.QueryLookupStrategy#resolveQuery(java.lang.reflect.Method, org.springframework.data.repository.core.RepositoryMetadata, org.springframework.data.projection.ProjectionFactory, org.springframework.data.repository.core.NamedQueries)
		 */
		@Override
		public RepositoryQuery resolveQuery(Method method, RepositoryMetadata metadata, ProjectionFactory factory,
				NamedQueries namedQueries) {

			ElasticsearchQueryMethod queryMethod = new ElasticsearchQueryMethod(method, metadata, factory,
					elasticsearchOperations.getElasticsearchConverter().getMappingContext());
			String namedQueryName = queryMethod.getNamedQueryName();

			if (namedQueries.hasQuery(namedQueryName)) {
				String namedQuery = namedQueries.getQuery(namedQueryName);
				return new ElasticsearchStringQuery(queryMethod, elasticsearchOperations, namedQuery);
			} else if (queryMethod.hasAnnotatedQuery()) {
				return new ElasticsearchStringQuery(queryMethod, elasticsearchOperations, queryMethod.getAnnotatedQuery());
			}
			return new ElasticsearchPartQuery(queryMethod, elasticsearchOperations);
		}
	}
}
