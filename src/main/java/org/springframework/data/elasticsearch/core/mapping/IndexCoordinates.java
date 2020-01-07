/*
 * Copyright 2019-2020 the original author or authors.
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

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Immutable Value object encapsulating index name(s) and index type(s). Type names are supported but deprecated as
 * Elasticsearch does not support types anymore.
 *
 * @author Mark Paluch
 * @author Christoph Strobl
 * @author Peter-Josef Meisch
 * @since 4.0
 */
public class IndexCoordinates {

	public static final String TYPE = "_doc";

	private final String[] indexNames;
	private final String[] typeNames;

	public static IndexCoordinates of(String... indexNames) {
		Assert.notNull(indexNames, "indexNames must not be null");
		return new IndexCoordinates(indexNames, null);
	}

	private IndexCoordinates(String[] indexNames, @Nullable String[] typeNames) {
		Assert.notEmpty(indexNames, "indexNames may not be null or empty");
		this.indexNames = indexNames;
		this.typeNames = typeNames != null ? typeNames : new String[] {};
	}

	/**
	 * Using Index types is deprecated in Elasticsearch.
	 * 
	 * @param typeNames
	 * @return
	 */
	@Deprecated
	public IndexCoordinates withTypes(String... typeNames) {
		Assert.notEmpty(typeNames, "typeNames must not be null");
		return new IndexCoordinates(this.indexNames, typeNames);
	}

	public String getIndexName() {
		return indexNames[0];
	}

	public String[] getIndexNames() {
		return Arrays.copyOf(indexNames, indexNames.length);
	}

	@Deprecated
	@Nullable
	public String getTypeName() {
		return typeNames.length > 0 ? typeNames[0] : null;
	}

	@Deprecated
	public String[] getTypeNames() {
		return Arrays.copyOf(typeNames, typeNames.length);
	}

	@Override
	public String toString() {
		return "IndexCoordinates{" + "indexNames=" + Arrays.toString(indexNames) + ", typeNames="
				+ Arrays.toString(typeNames) + '}';
	}
}
