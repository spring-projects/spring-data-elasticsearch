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

import java.util.Iterator;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.lang.Nullable;

/**
 * @author Christoph Strobl
 * @since 3.2
 */
public class ConvertingParameterAccessor implements ElasticsearchParameterAccessor {

	private final ElasticsearchConverter converter;
	private final ElasticsearchParameterAccessor delegate;

	public ConvertingParameterAccessor(ElasticsearchConverter converter, ElasticsearchParameterAccessor delegate) {

		this.converter = converter;
		this.delegate = delegate;
	}

	@Override
	public Object[] getValues() {
		return delegate.getValues();
	}

	@Override
	public Pageable getPageable() {
		return delegate.getPageable();
	}

	@Override
	public Sort getSort() {
		return delegate.getSort();
	}

	@Override
	public Optional<Class<?>> getDynamicProjection() {
		return delegate.getDynamicProjection();
	}

	@Override
	public Object getBindableValue(int index) {
		return getConvertedValue(delegate.getBindableValue(index));
	}

	@Override
	public boolean hasBindableNullValue() {
		return delegate.hasBindableNullValue();
	}

	@Override
	public Iterator<Object> iterator() {
		return delegate.iterator();
	}

	@Nullable
	private Object getConvertedValue(Object value) {

		if (value == null) {
			return "null";
		}

		if (converter.getConversionService().canConvert(value.getClass(), String.class)) {
			return converter.getConversionService().convert(value, String.class);
		}

		return value.toString();
	}
}
