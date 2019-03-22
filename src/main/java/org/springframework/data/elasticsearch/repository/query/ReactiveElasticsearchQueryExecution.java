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
package org.springframework.data.elasticsearch.repository.query;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.repository.query.ResultProcessor;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * @author Christoph Strobl
 * @since 3.2
 */
public interface ReactiveElasticsearchQueryExecution {

	Object execute(Query query, Class<?> type, String indexName, String indexType, @Nullable Class<?> targetType);

	/**
	 * An {@link ReactiveElasticsearchQueryExecution} that wraps the results of the given delegate with the given result
	 * processing.
	 */
	@RequiredArgsConstructor
	final class ResultProcessingExecution implements ReactiveElasticsearchQueryExecution {

		private final @NonNull ReactiveElasticsearchQueryExecution delegate;
		private final @NonNull Converter<Object, Object> converter;

		@Override
		public Object execute(Query query, Class<?> type, String indexName, String indexType,
				@Nullable Class<?> targetType) {
			return converter.convert(delegate.execute(query, type, indexName, indexType, targetType));
		}
	}

	/**
	 * A {@link Converter} to post-process all source objects using the given {@link ResultProcessor}.
	 *
	 * @author Mark Paluch
	 */
	@RequiredArgsConstructor
	final class ResultProcessingConverter implements Converter<Object, Object> {

		private final @NonNull ResultProcessor processor;
		private final @NonNull ReactiveElasticsearchOperations operations;

		/*
		 * (non-Javadoc)
		 * @see org.springframework.core.convert.converter.Converter#convert(java.lang.Object)
		 */
		@Override
		public Object convert(Object source) {

			ReturnedType returnedType = processor.getReturnedType();

			if (ClassUtils.isPrimitiveOrWrapper(returnedType.getReturnedType())) {
				return source;
			}

			return processor.processResult(source, it -> it);
		}
	}
}
