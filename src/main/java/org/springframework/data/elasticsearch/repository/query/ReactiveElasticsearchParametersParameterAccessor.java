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

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.repository.util.ReactiveWrapperConverters;
import org.springframework.data.repository.util.ReactiveWrappers;

/**
 * @author Christoph Strobl
 * @since 3.2
 */
class ReactiveElasticsearchParametersParameterAccessor extends ElasticsearchParametersParameterAccessor {

	private final List<MonoProcessor<?>> subscriptions;

	/**
	 * Creates a new {@link ElasticsearchParametersParameterAccessor}.
	 *
	 * @param method must not be {@literal null}.
	 * @param values must not be {@literal null}.
	 */
	ReactiveElasticsearchParametersParameterAccessor(ReactiveElasticsearchQueryMethod method, Object[] values) {
		super(method, values);

		this.subscriptions = new ArrayList<>(values.length);

		for (int i = 0; i < values.length; i++) {

			Object value = values[i];

			if (value == null || !ReactiveWrappers.supports(value.getClass())) {

				subscriptions.add(null);
				continue;
			}

			if (ReactiveWrappers.isSingleValueType(value.getClass())) {
				subscriptions.add(ReactiveWrapperConverters.toWrapper(value, Mono.class).toProcessor());
			} else {
				subscriptions.add(ReactiveWrapperConverters.toWrapper(value, Flux.class).collectList().toProcessor());
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.ParametersParameterAccessor#getValue(int)
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected <T> T getValue(int index) {

		if (subscriptions.get(index) != null) {
			return (T) subscriptions.get(index).block();
		}

		return super.getValue(index);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.repository.query.ElasticsearchParametersParameterAccessor#getValues(int)
	 */
	@Override
	public Object[] getValues() {

		Object[] result = new Object[getValues().length];
		for (int i = 0; i < result.length; i++) {
			result[i] = getValue(i);
		}
		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.ParametersParameterAccessor#getBindableValue(int)
	 */
	public Object getBindableValue(int index) {
		return getValue(getParameters().getBindableParameter(index).getIndex());
	}
}
