/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.elasticsearch.core;

import java.io.IOException;

import org.springframework.data.elasticsearch.ElasticsearchException;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @author Artur Konczak
 */
public abstract class AbstractResultMapper implements ResultsMapper {

	private EntityMapper entityMapper;

	public AbstractResultMapper(EntityMapper entityMapper) {
		
		Assert.notNull(entityMapper, "EntityMapper must not be null!");
		
		this.entityMapper = entityMapper;
	}

	public <T> T mapEntity(String source, Class<T> clazz) {
		if (StringUtils.isEmpty(source)) {
			return null;
		}
		try {
			return entityMapper.mapToObject(source, clazz);
		} catch (IOException e) {
			throw new ElasticsearchException("failed to map source [ " + source + "] to class " + clazz.getSimpleName(), e);
		}
	}

	@Override
	public EntityMapper getEntityMapper() {
		return this.entityMapper;
	}
}
