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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.springframework.core.MethodParameter;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.util.TypeInformation;

/**
 * @author Christoph Strobl
 * @author Peter-Josef Meisch
 * @since 3.2
 */
public class ElasticsearchParameters extends Parameters<ElasticsearchParameters, ElasticsearchParameter> {
	private final List<ElasticsearchParameter> scriptedFields = new ArrayList<>();
	private final List<ElasticsearchParameter> runtimeFields = new ArrayList<>();

	private final int indexCoordinatesIndex;

	public ElasticsearchParameters(Method method, TypeInformation<?> domainType) {

		super(method, parameter -> new ElasticsearchParameter(parameter, domainType));

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
		this.indexCoordinatesIndex = initIndexCoordinatesIndex();
	}

	private int initIndexCoordinatesIndex() {
		int index = 0;
		List<Integer> foundIndices = new ArrayList<>();
		for (ElasticsearchParameter parameter : this) {
			if (parameter.isIndexCoordinatesParameter()) {
				foundIndices.add(index);
			}
			index++;
		}
		if (foundIndices.size() > 1) {
			throw new IllegalArgumentException(this + " can only contain at most one IndexCoordinates parameter.");
		}
		return foundIndices.isEmpty() ? -1 : foundIndices.get(0);
	}

	private ElasticsearchParameter parameterFactory(MethodParameter methodParameter, TypeInformation<?> domainType) {
		return new ElasticsearchParameter(methodParameter, domainType);
	}


	private ElasticsearchParameters(List<ElasticsearchParameter> parameters) {
		super(parameters);
		this.indexCoordinatesIndex = initIndexCoordinatesIndex();
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

	public boolean hasIndexCoordinatesParameter() {
		return this.indexCoordinatesIndex != -1;
	}

	public int getIndexCoordinatesIndex() {
		return indexCoordinatesIndex;
	}
}
