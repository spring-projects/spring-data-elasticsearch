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

import java.io.IOException;
import java.util.Map;

import org.elasticsearch.index.get.GetResult;
import org.elasticsearch.search.SearchHit;
import org.springframework.data.elasticsearch.ElasticsearchException;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * ResultsMapper
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Artur Konczak
 * @author Christoph Strobl
 */
public interface ResultsMapper extends SearchResultMapper, GetResultMapper, MultiGetResultMapper {

	EntityMapper getEntityMapper();

	/**
	 * Get the configured {@link ProjectionFactory}. <br />
	 * <strong>NOTE</strong> Should be overwritten in implementation to make use of the type cache.
	 *
	 * @since 3.2
	 */
	default ProjectionFactory getProjectionFactory() {
		return new SpelAwareProxyProjectionFactory();
	}

	@Nullable
	default <T> T mapEntity(String source, Class<T> clazz) {

		if (StringUtils.isEmpty(source)) {
			return null;
		}
		try {
			return getEntityMapper().mapToObject(source, clazz);
		} catch (IOException e) {
			throw new ElasticsearchException("failed to map source [ " + source + "] to class " + clazz.getSimpleName(), e);
		}
	}

	/**
	 * Map a single {@link GetResult} to an instance of the given type.
	 *
	 * @param getResult must not be {@literal null}.
	 * @param type must not be {@literal null}.
	 * @param <T>
	 * @return can be {@literal null} if the {@link GetResult#isSourceEmpty() is empty}.
	 * @since 3.2
	 */
	@Nullable
	default <T> T mapGetResult(GetResult getResult, Class<T> type) {

		if (getResult.isSourceEmpty()) {
			return null;
		}

		Map<String, Object> source = getResult.getSource();
		if (!source.containsKey("id") || source.get("id") == null) {
			source.put("id", getResult.getId());
		}

		Object mappedResult = getEntityMapper().readObject(source, type);

		if (mappedResult == null) {
			return (T) null;
		}

		if (type.isInterface() || !ClassUtils.isAssignableValue(type, mappedResult)) {
			return getProjectionFactory().createProjection(type, mappedResult);
		}

		return type.cast(mappedResult);
	}

	/**
	 * Map a single {@link SearchHit} to an instance of the given type.
	 *
	 * @param searchHit must not be {@literal null}.
	 * @param type must not be {@literal null}.
	 * @param <T>
	 * @return can be {@literal null} if the {@link SearchHit} does not have {@link SearchHit#hasSource() a source}.
	 * @since 3.2
	 */
	@Nullable
	default <T> T mapSearchHit(SearchHit searchHit, Class<T> type) {

		if (!searchHit.hasSource()) {
			return null;
		}

		Map<String, Object> source = searchHit.getSourceAsMap();
		if (!source.containsKey("id") || source.get("id") == null) {
			source.put("id", searchHit.getId());
		}

		Object mappedResult = getEntityMapper().readObject(source, type);

		if (mappedResult == null) {
			return null;
		}

		if (type.isInterface()) {
			return getProjectionFactory().createProjection(type, mappedResult);
		}

		return type.cast(mappedResult);
	}
}
