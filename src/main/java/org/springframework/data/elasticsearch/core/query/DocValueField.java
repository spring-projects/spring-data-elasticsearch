/*
 * Copyright 2023-2024 the original author or authors.
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
package org.springframework.data.elasticsearch.core.query;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Record defining a docvalue_field to be used in a query.
 *
 * @author Peter-Josef Meisch
 * @since 5.1
 */
public record DocValueField(String field, @Nullable String format) {
	public DocValueField {
		Assert.notNull(field, "field must not be null");
	}

	public DocValueField(String field) {
		this(field, null);
	}
}
