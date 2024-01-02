/*
 * Copyright 2021-2024 the original author or authors.
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
package org.springframework.data.elasticsearch.core.query.highlight;

import java.util.List;

import org.springframework.util.Assert;

/**
 * @author Peter-Josef Meisch
 * @author Haibo Liu
 * @since 4.3
 */
public class Highlight {

	private final HighlightParameters parameters;
	private final List<HighlightField> fields;

	/**
	 * @since 4.4
	 */
	public Highlight(List<HighlightField> fields) {

		Assert.notNull(fields, "fields must not be null");

		this.parameters = HighlightParameters.builder().build();
		this.fields = fields;
	}

	public Highlight(HighlightParameters parameters, List<HighlightField> fields) {

		Assert.notNull(parameters, "parameters must not be null");
		Assert.notNull(fields, "fields must not be null");

		this.parameters = parameters;
		this.fields = fields;
	}

	public HighlightParameters getParameters() {
		return parameters;
	}

	public List<HighlightField> getFields() {
		return fields;
	}
}
