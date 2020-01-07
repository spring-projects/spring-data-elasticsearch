/*
 * Copyright 2020-2020 the original author or authors.
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

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.stream.Stream;

import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;

/**
 * @author Peter-Josef Meisch
 * @since 4.0
 */
public class ElasticsearchRepositoryMetadata extends DefaultRepositoryMetadata {

	public ElasticsearchRepositoryMetadata(Class<?> repositoryInterface) {
		super(repositoryInterface);
	}

	@Override
	public Class<?> getReturnedDomainClass(Method method) {
		Class<?> returnedDomainClass = super.getReturnedDomainClass(method);
		if (SearchHit.class.isAssignableFrom(returnedDomainClass)) {
			try {
				// dealing with Collection<SearchHit<T>> or Flux<SearchHit<T>>, getting to T
				ParameterizedType methodGenericReturnType = ((ParameterizedType) method.getGenericReturnType());
				if (isAllowedGenericType(methodGenericReturnType)) {
					ParameterizedType collectionTypeArgument = (ParameterizedType) methodGenericReturnType
							.getActualTypeArguments()[0];
					if (SearchHit.class.isAssignableFrom((Class<?>) collectionTypeArgument.getRawType())) {
						returnedDomainClass = (Class<?>) collectionTypeArgument.getActualTypeArguments()[0];
					}
				}
			} catch (Exception ignored) {}
		}
		return returnedDomainClass;
	}

	protected boolean isAllowedGenericType(ParameterizedType methodGenericReturnType) {
		return Collection.class.isAssignableFrom((Class<?>) methodGenericReturnType.getRawType())
				|| Stream.class.isAssignableFrom((Class<?>) methodGenericReturnType.getRawType());
	}
}
