/*
 * Copyright 2014-2019 the original author or authors.
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
package org.springframework.data.elasticsearch.core;

import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.util.Assert;

/**
 * @author Artur Konczak
 * @author Christoph Strobl
 */
public abstract class AbstractResultMapper implements ResultsMapper {

	private final EntityMapper entityMapper;
	private final ProjectionFactory projectionFactory;

	/**
	 * Create a new {@link AbstractResultMapper}.
	 *
	 * @param entityMapper must not be {@literal null}.
	 */
	public AbstractResultMapper(EntityMapper entityMapper) {
		this(entityMapper, new SpelAwareProxyProjectionFactory());
	}

	/**
	 * Create a new {@link AbstractResultMapper}.
	 *
	 * @param entityMapper must not be {@literal null}.
	 * @param projectionFactory must not be {@literal null}.
	 * @since 3.2
	 */
	public AbstractResultMapper(EntityMapper entityMapper, ProjectionFactory projectionFactory) {

		Assert.notNull(entityMapper, "EntityMapper must not be null!");
		Assert.notNull(projectionFactory, "ProjectionFactory must not be null!");

		this.entityMapper = entityMapper;
		this.projectionFactory = projectionFactory;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.core.ResultsMapper#getEntityMapper()
	 */
	@Override
	public EntityMapper getEntityMapper() {
		return this.entityMapper;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.core.ResultsMapper#getProjectionFactory()
	 */
	@Override
	public ProjectionFactory getProjectionFactory() {
		return this.projectionFactory;
	}
}
