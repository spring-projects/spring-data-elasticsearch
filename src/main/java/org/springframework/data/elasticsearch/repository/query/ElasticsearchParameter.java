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

import org.springframework.core.MethodParameter;
import org.springframework.data.elasticsearch.core.query.RuntimeField;
import org.springframework.data.elasticsearch.core.query.ScriptedField;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.util.TypeInformation;

/**
 * Custom {@link Parameter} implementation adding specific types to the special ones. Refactored from being defined in
 * {@link ElasticsearchParameters}.
 *
 * @author Christoph Strobl
 * @author Peter-Josef Meisch
 * @since 5.2
 */
public class ElasticsearchParameter extends Parameter {

	/**
	 * Creates a new {@link ElasticsearchParameter}.
	 *
	 * @param parameter must not be {@literal null}.
	 */
	ElasticsearchParameter(MethodParameter parameter, TypeInformation<?> domainType) {
		super(parameter, domainType);
	}

	@Override
	public boolean isSpecialParameter() {
		return super.isSpecialParameter() || isScriptedFieldParameter() || isRuntimeFieldParameter();
	}

	public Boolean isScriptedFieldParameter() {
		return ScriptedField.class.isAssignableFrom(getType());
	}

	public Boolean isRuntimeFieldParameter() {
		return RuntimeField.class.isAssignableFrom(getType());
	}
}
