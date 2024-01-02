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
package org.springframework.data.elasticsearch.core.mapping;

import java.util.Arrays;

import org.springframework.util.Assert;

/**
 * Immutable Value object encapsulating index name(s) and index type(s).
 *
 * @author Mark Paluch
 * @author Christoph Strobl
 * @author Peter-Josef Meisch
 * @since 4.0
 */
public class IndexCoordinates {

	public static final String TYPE = "_doc";

	private final String[] indexNames;

	public static IndexCoordinates of(String... indexNames) {
		Assert.notNull(indexNames, "indexNames must not be null");
		return new IndexCoordinates(indexNames);
	}

	private IndexCoordinates(String... indexNames) {
		Assert.notEmpty(indexNames, "indexNames may not be null or empty");
		this.indexNames = indexNames;
	}

	public String getIndexName() {
		return indexNames[0];
	}

	public String[] getIndexNames() {
		return Arrays.copyOf(indexNames, indexNames.length);
	}

	/**
	 * @since 4.2
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		IndexCoordinates that = (IndexCoordinates) o;
		return Arrays.equals(indexNames, that.indexNames);
	}

	/**
	 * @since 4.2
	 */
	@Override
	public int hashCode() {
		return Arrays.hashCode(indexNames);
	}

	@Override
	public String toString() {
		return "IndexCoordinates{" + "indexNames=" + Arrays.toString(indexNames) + '}';
	}
}
