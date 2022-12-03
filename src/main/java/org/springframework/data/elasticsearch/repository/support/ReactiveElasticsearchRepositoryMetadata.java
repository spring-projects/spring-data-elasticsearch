/*
 * Copyright 2020-2022 the original author or authors.
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
package org.springframework.data.elasticsearch.repository.support;

import java.lang.reflect.ParameterizedType;

import org.springframework.data.util.ReactiveWrappers;

/**
 * @author Peter-Josef Meisch
 * @since 4.0
 */
public class ReactiveElasticsearchRepositoryMetadata extends ElasticsearchRepositoryMetadata {

	public ReactiveElasticsearchRepositoryMetadata(Class<?> repositoryInterface) {
		super(repositoryInterface);
	}

	@Override
	protected boolean isAllowedGenericType(ParameterizedType methodGenericReturnType) {
		return super.isAllowedGenericType(methodGenericReturnType)
				|| ReactiveWrappers.supports((Class<?>) methodGenericReturnType.getRawType());
	}
}
