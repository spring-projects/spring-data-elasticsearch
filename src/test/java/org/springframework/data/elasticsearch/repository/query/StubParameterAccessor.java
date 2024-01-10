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

import java.util.Arrays;
import java.util.Iterator;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.lang.NonNull;

/**
 * Simple {@link ParameterAccessor} that returns the given parameters unfiltered.
 *
 * @author Christoph Strobl
 * @author Peter-Josef Meisch
 */
class StubParameterAccessor implements ElasticsearchParameterAccessor {

	private final Object[] values;

	StubParameterAccessor(Object... values) {
		this.values = values;
	}

	@Override
	public ScrollPosition getScrollPosition() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.ParameterAccessor#getPageable()
	 */
	@Override
	public Pageable getPageable() {
		return Pageable.unpaged();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.ParameterAccessor#getBindableValue(int)
	 */
	@Override
	public Object getBindableValue(int index) {
		return values[index];
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.ParameterAccessor#hasBindableNullValue()
	 */
	@Override
	public boolean hasBindableNullValue() {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.ParameterAccessor#getSort()
	 */
	@Override
	public Sort getSort() {
		return Sort.unsorted();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.ParameterAccessor#iterator()
	 */
	@Override
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

	@Override
	public IndexCoordinates getIndexCoordinatesOrDefaults(@NonNull IndexCoordinates defaults) {
		return defaults;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.ParameterAccessor#findDynamicProjection()
	 */
	@Override
	public Class<?> findDynamicProjection() {
		return null;
	}
}
