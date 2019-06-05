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
package org.springframework.data.elasticsearch.repository.support;

import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.beans.factory.FactoryBean} to create
 * {@link org.springframework.data.elasticsearch.repository.ReactiveElasticsearchRepository} instances.
 *
 * @author Christoph Strobl
 * @since 3.2
 * @see org.springframework.data.repository.reactive.ReactiveSortingRepository
 */
public class ReactiveElasticsearchRepositoryFactoryBean<T extends Repository<S, ID>, S, ID>
		extends RepositoryFactoryBeanSupport<T, S, ID> {

	private @Nullable ReactiveElasticsearchOperations operations;
	private boolean mappingContextConfigured = false;

	/**
	 * Creates a new {@link ReactiveElasticsearchRepositoryFactoryBean} for the given repository interface.
	 *
	 * @param repositoryInterface must not be {@literal null}.
	 */
	public ReactiveElasticsearchRepositoryFactoryBean(Class<? extends T> repositoryInterface) {
		super(repositoryInterface);
	}

	/**
	 * Configures the {@link ReactiveElasticsearchOperations} to be used.
	 *
	 * @param operations the operations to set
	 */
	public void setReactiveElasticsearchOperations(@Nullable ReactiveElasticsearchOperations operations) {
		this.operations = operations;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport#setMappingContext(org.springframework.data.mapping.context.MappingContext)
	 */
	@Override
	protected void setMappingContext(MappingContext<?, ?> mappingContext) {

		super.setMappingContext(mappingContext);
		this.mappingContextConfigured = true;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.springframework.data.repository.support.RepositoryFactoryBeanSupport
	 * #createRepositoryFactory()
	 */
	@Override
	protected final RepositoryFactorySupport createRepositoryFactory() {

		return getFactoryInstance(operations);
	}

	/**
	 * Creates and initializes a {@link RepositoryFactorySupport} instance.
	 *
	 * @param operations
	 * @return
	 */
	protected RepositoryFactorySupport getFactoryInstance(ReactiveElasticsearchOperations operations) {
		return new ReactiveElasticsearchRepositoryFactory(operations);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.springframework.data.repository.support.RepositoryFactoryBeanSupport
	 * #afterPropertiesSet()
	 */
	@Override
	public void afterPropertiesSet() {

		super.afterPropertiesSet();
		Assert.state(operations != null, "ReactiveElasticsearchOperations must not be null!");

		if (!mappingContextConfigured) {
			setMappingContext(operations.getElasticsearchConverter().getMappingContext());
		}
	}
}
