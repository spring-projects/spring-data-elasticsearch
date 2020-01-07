/*
 * Copyright 2019-2020 the original author or authors.
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

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.repository.query.ResultProcessor;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * @author Christoph Strobl
 * @author Peter-Josef Meisch
 * @since 3.2
 */
public interface ReactiveElasticsearchQueryExecution {

	Object execute(Query query, Class<?> type, @Nullable Class<?> targetType, IndexCoordinates indexCoordinates);

	/**
	 * An {@link ReactiveElasticsearchQueryExecution} that wraps the results of the given delegate with the given result
	 * processing.
	 */
	final class ResultProcessingExecution implements ReactiveElasticsearchQueryExecution {

		private final ReactiveElasticsearchQueryExecution delegate;
		private final Converter<Object, Object> converter;

		public ResultProcessingExecution(ReactiveElasticsearchQueryExecution delegate,
				Converter<Object, Object> converter) {
			Assert.notNull(delegate, "delegate must not be null");
			Assert.notNull(converter, "converter must not be null");
			this.delegate = delegate;
			this.converter = converter;
		}

		@Override
		public Object execute(Query query, Class<?> type, @Nullable Class<?> targetType,
				IndexCoordinates indexCoordinates) {
			return converter.convert(delegate.execute(query, type, targetType, indexCoordinates));
		}
	}

	/**
	 * A {@link Converter} to post-process all source objects using the given {@link ResultProcessor}.
	 *
	 * @author Mark Paluch
	 */
	final class ResultProcessingConverter implements Converter<Object, Object> {

		private final ResultProcessor processor;

		public ResultProcessingConverter(ResultProcessor processor) {
			Assert.notNull(processor, "processor must not be null");
			this.processor = processor;
		}

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
