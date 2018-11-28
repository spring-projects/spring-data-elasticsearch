/*
 * Copyright 2013-2018 the original author or authors.
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

import org.elasticsearch.index.get.GetResult;
import org.elasticsearch.search.SearchHit;
import org.springframework.data.elasticsearch.ElasticsearchException;
import org.springframework.lang.Nullable;
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
	 * @since 4.0
	 */
	@Nullable
	default <T> T mapEntity(GetResult getResult, Class<T> type) {

		if (getResult.isSourceEmpty()) {
			return null;
		}

		String sourceString = getResult.sourceAsString();

		if (sourceString.startsWith("{\"id\":null,")) {
			sourceString = sourceString.replaceFirst("\"id\":null", "\"id\":\"" + getResult.getId() + "\"");
		}

		return mapEntity(sourceString, type);
	}

	/**
	 * Map a single {@link SearchHit} to an instance of the given type.
	 *
	 * @param searchHit must not be {@literal null}.
	 * @param type must not be {@literal null}.
	 * @param <T>
	 * @return can be {@literal null} if the {@link SearchHit} does not have {@link SearchHit#hasSource() a source}.
	 * @since 4.0
	 */
	@Nullable
	default <T> T mapEntity(SearchHit searchHit, Class<T> type) {

		if (!searchHit.hasSource()) {
			return null;
		}

		String sourceString = searchHit.getSourceAsString();

		if (sourceString.startsWith("{\"id\":null,")) {
			sourceString = sourceString.replaceFirst("\"id\":null", "\"id\":\"" + searchHit.getId() + "\"");
		}

		return mapEntity(sourceString, type);
	}
}
