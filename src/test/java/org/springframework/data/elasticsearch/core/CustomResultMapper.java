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
package org.springframework.data.elasticsearch.core;

import java.util.List;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.search.SearchResponse;

import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;

/**
 * @author Artur Konczak
 * @author Mohsin Husen
 */
public class CustomResultMapper implements ResultsMapper {

	private EntityMapper entityMapper;

	public CustomResultMapper(EntityMapper entityMapper) {
		this.entityMapper = entityMapper;
	}

	@Override
	public EntityMapper getEntityMapper() {
		return entityMapper;
	}

	@Override
	public <T> T mapResult(GetResponse response, Class<T> clazz) {
		return null; // To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public <T> AggregatedPage<T> mapResults(SearchResponse response, Class<T> clazz, Pageable pageable) {
		return null; // To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public <T> List<T> mapResults(MultiGetResponse responses, Class<T> clazz) {
		return null;
	}
}
