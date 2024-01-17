/*
 * Copyright 2019-2024 the original author or authors.
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

import org.springframework.data.repository.query.ParametersParameterAccessor;

/**
 * @author Christoph Strobl
 * @since 3.2
 */
public class ElasticsearchParametersParameterAccessor extends ParametersParameterAccessor
		implements ElasticsearchParameterAccessor {

	private final Object[] values;

	/**
	 * Creates a new {@link ElasticsearchParametersParameterAccessor}.
	 *
	 * @param method must not be {@literal null}.
	 * @param values must not be {@literal null}.
	 */
	ElasticsearchParametersParameterAccessor(ElasticsearchQueryMethod method, Object... values) {

		super(method.getParameters(), values);
		this.values = values;
	}

	@Override
	public Object[] getValues() {
		return values;
	}
}
