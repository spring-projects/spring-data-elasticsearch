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

import org.elasticsearch.index.get.GetResult;
import org.elasticsearch.search.SearchHit;

import org.springframework.data.elasticsearch.Document;
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
 * @author Mark Paluch
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
	@Deprecated
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
	 * Map a single {@link Document} to an instance of the given type.
	 *
	 * @param document must not be {@literal null}.
	 * @param type must not be {@literal null}.
	 * @param <T>
	 * @return can be {@literal null} if the {@link Document#isEmpty() is empty}.
	 * @since 4.0
	 */
	@Nullable
	default <T> T mapDocument(Document document, Class<T> type) {

		Object mappedResult = getEntityMapper().readObject(document, type);

		if (mappedResult == null) {
			return (T) null;
		}

		if (type.isInterface() || !ClassUtils.isAssignableValue(type, mappedResult)) {
			return getProjectionFactory().createProjection(type, mappedResult);
		}

		return type.cast(mappedResult);
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
		return mapDocument(DocumentAdapters.from(getResult), type);
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
		return mapDocument(DocumentAdapters.from(searchHit), type);
	}
}
