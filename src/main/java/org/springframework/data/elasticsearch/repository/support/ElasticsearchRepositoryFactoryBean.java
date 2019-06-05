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

import java.io.Serializable;

import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.util.Assert;

/**
 * Spring {@link org.springframework.beans.factory.FactoryBean} implementation to ease container based configuration for
 * XML namespace and JavaConfig.
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Mark Paluch
 */
public class ElasticsearchRepositoryFactoryBean<T extends Repository<S, ID>, S, ID extends Serializable> extends
		RepositoryFactoryBeanSupport<T, S, ID> {

	private ElasticsearchOperations operations;

	/**
	 * Creates a new {@link ElasticsearchRepositoryFactoryBean} for the given repository interface.
	 * 
	 * @param repositoryInterface must not be {@literal null}.
	 */
	public ElasticsearchRepositoryFactoryBean(Class<? extends T> repositoryInterface) {
		super(repositoryInterface);
	}

	/**
	 * Configures the {@link ElasticsearchOperations} to be used to create Elasticsearch repositories.
	 *
	 * @param operations the operations to set
	 */
	public void setElasticsearchOperations(ElasticsearchOperations operations) {
		
		Assert.notNull(operations, "ElasticsearchOperations must not be null!");
		
		setMappingContext(operations.getElasticsearchConverter().getMappingContext());
		this.operations = operations;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport#afterPropertiesSet()
	 */
	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();
		Assert.notNull(operations, "ElasticsearchOperations must be configured!");
	}

	@Override
	protected RepositoryFactorySupport createRepositoryFactory() {
		return new ElasticsearchRepositoryFactory(operations);
	}
}
