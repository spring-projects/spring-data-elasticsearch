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

import java.lang.reflect.Method;
import java.util.List;

import org.springframework.core.MethodParameter;
import org.springframework.data.elasticsearch.repository.query.ElasticsearchParameters.ElasticsearchParameter;
import org.springframework.data.geo.Distance;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.Parameters;

/**
 * @author Christoph Strobl
 * @since 3.2
 */
public class ElasticsearchParameters extends Parameters<ElasticsearchParameters, ElasticsearchParameter> {

	public ElasticsearchParameters(Method method) {
		super(method);
	}

	private ElasticsearchParameters(List<ElasticsearchParameter> parameters) {
		super(parameters);
	}

	@Override
	protected ElasticsearchParameter createParameter(MethodParameter parameter) {
		return new ElasticsearchParameter(parameter);
	}

	@Override
	protected ElasticsearchParameters createFrom(List<ElasticsearchParameter> parameters) {
		return new ElasticsearchParameters(parameters);
	}

	/**
	 * Custom {@link Parameter} implementation adding parameters of type {@link Distance} to the special ones.
	 *
	 * @author Christoph Strobl
	 */
	class ElasticsearchParameter extends Parameter {

		private final MethodParameter parameter;

		/**
		 * Creates a new {@link ElasticsearchParameter}.
		 *
		 * @param parameter must not be {@literal null}.
		 */
		ElasticsearchParameter(MethodParameter parameter) {

			super(parameter);
			this.parameter = parameter;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.repository.query.Parameter#isSpecialParameter()
		 */
		@Override
		public boolean isSpecialParameter() {
			return super.isSpecialParameter();
		}
	}
}
