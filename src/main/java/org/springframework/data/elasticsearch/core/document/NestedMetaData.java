/*
 * Copyright 2020-2024 the original author or authors.
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
package org.springframework.data.elasticsearch.core.document;

import org.springframework.lang.Nullable;

/**
 * meta data returned for nested inner hits.
 *
 * @author Peter-Josef Meisch
 */
public class NestedMetaData {

	private final String field;
	private final int offset;
	@Nullable private final NestedMetaData child;

	public static NestedMetaData of(String field, int offset, @Nullable NestedMetaData nested) {
		return new NestedMetaData(field, offset, nested);
	}

	private NestedMetaData(String field, int offset, @Nullable NestedMetaData child) {
		this.field = field;
		this.offset = offset;
		this.child = child;
	}

	public String getField() {
		return field;
	}

	public int getOffset() {
		return offset;
	}

	@Nullable
	public NestedMetaData getChild() {
		return child;
	}
}
