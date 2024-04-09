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

import java.util.ArrayList;
import java.util.List;

import org.springframework.core.MethodParameter;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.ParametersSource;
import org.springframework.data.util.TypeInformation;

/**
 * @author Christoph Strobl
 * @author Peter-Josef Meisch
 * @since 3.2
 */
public class ElasticsearchParameters extends Parameters<ElasticsearchParameters, ElasticsearchParameter> {

	private final List<ElasticsearchParameter> scriptedFields = new ArrayList<>();
	private final List<ElasticsearchParameter> runtimeFields = new ArrayList<>();

	public ElasticsearchParameters(ParametersSource parametersSource) {

		super(parametersSource,
				parameter -> new ElasticsearchParameter(parameter, parametersSource.getDomainTypeInformation()));

		var domainType = parametersSource.getDomainTypeInformation();
		var method = parametersSource.getMethod();
		int parameterCount = method.getParameterCount();
		for (int i = 0; i < parameterCount; i++) {
			MethodParameter methodParameter = new MethodParameter(method, i);
			var parameter = parameterFactory(methodParameter, domainType);

			if (parameter.isScriptedFieldParameter()) {
				scriptedFields.add(parameter);
			}

			if (parameter.isRuntimeFieldParameter()) {
				runtimeFields.add(parameter);
			}
		}
	}

	private ElasticsearchParameter parameterFactory(MethodParameter methodParameter, TypeInformation<?> domainType) {
		return new ElasticsearchParameter(methodParameter, domainType);
	}

	private ElasticsearchParameters(List<ElasticsearchParameter> parameters) {
		super(parameters);
	}

	@Override
	protected ElasticsearchParameters createFrom(List<ElasticsearchParameter> parameters) {
		return new ElasticsearchParameters(parameters);
	}

	List<ElasticsearchParameter> getScriptedFields() {
		return scriptedFields;
	}

	List<ElasticsearchParameter> getRuntimeFields() {
		return runtimeFields;
	}
}
