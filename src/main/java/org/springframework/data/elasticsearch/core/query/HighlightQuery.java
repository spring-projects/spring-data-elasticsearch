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
package org.springframework.data.elasticsearch.core.query;

import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Combines a {@link Highlight} definition with the type of the entity where it's present on a method.
 *
 * @author Peter-Josef Meisch
 * @since 4.0
 */
public class HighlightQuery {

	private final Highlight highlight;
	@Nullable private final Class<?> type;

	public HighlightQuery(Highlight highlight, @Nullable Class<?> type) {

		Assert.notNull(highlight, "highlight must not be null");

		this.highlight = highlight;
		this.type = type;
	}

	public Highlight getHighlight() {
		return highlight;
	}

	@Nullable
	public Class<?> getType() {
		return type;
	}
}
