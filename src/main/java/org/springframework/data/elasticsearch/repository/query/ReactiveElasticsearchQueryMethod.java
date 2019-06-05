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

import static org.springframework.data.repository.util.ClassUtils.*;

import java.lang.reflect.Method;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentProperty;
import org.springframework.data.elasticsearch.repository.query.ElasticsearchParameters.ElasticsearchParameter;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.util.ReactiveWrapperConverters;
import org.springframework.data.repository.util.ReactiveWrappers;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.ClassUtils;

/**
 * @author Christoph Strobl
 * @since 3.2
 */
public class ReactiveElasticsearchQueryMethod extends ElasticsearchQueryMethod {

	private static final ClassTypeInformation<Page> PAGE_TYPE = ClassTypeInformation.from(Page.class);
	private static final ClassTypeInformation<Slice> SLICE_TYPE = ClassTypeInformation.from(Slice.class);

	private final Method method;

	public ReactiveElasticsearchQueryMethod(Method method, RepositoryMetadata metadata, ProjectionFactory factory,
			MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> mappingContext) {

		super(method, metadata, factory, mappingContext);
		this.method = method;

		if (hasParameterOfType(method, Pageable.class)) {

			TypeInformation<?> returnType = ClassTypeInformation.fromReturnTypeOf(method);

			boolean multiWrapper = ReactiveWrappers.isMultiValueType(returnType.getType());
			boolean singleWrapperWithWrappedPageableResult = ReactiveWrappers.isSingleValueType(returnType.getType())
					&& (PAGE_TYPE.isAssignableFrom(returnType.getRequiredComponentType())
							|| SLICE_TYPE.isAssignableFrom(returnType.getRequiredComponentType()));

			if (singleWrapperWithWrappedPageableResult) {
				throw new InvalidDataAccessApiUsageException(
						String.format("'%s.%s' must not use sliced or paged execution. Please use Flux.buffer(size, skip).",
								ClassUtils.getShortName(method.getDeclaringClass()), method.getName()));
			}

			if (!multiWrapper) {
				throw new IllegalStateException(String.format(
						"Method has to use a either multi-item reactive wrapper return type or a wrapped Page/Slice type. Offending method: %s",
						method.toString()));
			}

			if (hasParameterOfType(method, Sort.class)) {
				throw new IllegalStateException(String.format("Method must not have Pageable *and* Sort parameter. "
						+ "Use sorting capabilities on Pageble instead! Offending method: %s", method.toString()));
			}
		}
	}

	@Override
	protected ElasticsearchParameters createParameters(Method method) {
		return new ElasticsearchParameters(method);
	}

	/**
	 * Check if the given {@link org.springframework.data.repository.query.QueryMethod} receives a reactive parameter
	 * wrapper as one of its parameters.
	 *
	 * @return
	 */
	public boolean hasReactiveWrapperParameter() {

		for (ElasticsearchParameter param : getParameters()) {
			if (ReactiveWrapperConverters.supports(param.getType())) {
				return true;
			}
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.QueryMethod#isStreamQuery()
	 */
	@Override
	public boolean isStreamQuery() {

		// All reactive query methods are streaming.
		return true;
	}

	@Override
	public ElasticsearchParameters getParameters() {
		return (ElasticsearchParameters) super.getParameters();
	}
}
