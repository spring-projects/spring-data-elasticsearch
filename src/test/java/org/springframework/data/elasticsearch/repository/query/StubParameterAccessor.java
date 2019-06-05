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

import java.util.Arrays;
import java.util.Iterator;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.ParameterAccessor;

/**
 * Simple {@link ParameterAccessor} that returns the given parameters unfiltered.
 *
 * @author Christoph Strobl
 * @currentRead Fool's Fate - Robin Hobb
 */
class StubParameterAccessor implements ElasticsearchParameterAccessor {

	private final Object[] values;

	@SuppressWarnings("unchecked")
	public StubParameterAccessor(Object... values) {
		this.values = values;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.ParameterAccessor#getPageable()
	 */
	public Pageable getPageable() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.ParameterAccessor#getBindableValue(int)
	 */
	public Object getBindableValue(int index) {
		return values[index];
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.ParameterAccessor#hasBindableNullValue()
	 */
	public boolean hasBindableNullValue() {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.ParameterAccessor#getSort()
	 */
	public Sort getSort() {
		return Sort.unsorted();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.ParameterAccessor#iterator()
	 */
	public Iterator<Object> iterator() {
		return Arrays.asList(values).iterator();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.repository.query.ElasticsearchParameterAccessor#getValues()
	 */
	@Override
	public Object[] getValues() {
		return this.values;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.ParameterAccessor#getDynamicProjection()
	 */
	@Override
	public Optional<Class<?>> getDynamicProjection() {
		return Optional.empty();
	}
}
