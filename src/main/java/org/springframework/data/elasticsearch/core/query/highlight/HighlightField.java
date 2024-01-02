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

import org.springframework.util.Assert;

/**
 * @author Peter-Josef Meisch
 * @since 4.3
 */
public class HighlightField {
	private final String name;
	private final HighlightFieldParameters parameters;

	/**
	 * @since 4.4
	 */
	public HighlightField(String name) {

		Assert.notNull(name, "name must not be null");

		this.name = name;
		this.parameters = HighlightFieldParameters.builder().build();
	}

	public HighlightField(String name, HighlightFieldParameters parameters) {

		Assert.notNull(name, "name must not be null");
		Assert.notNull(parameters, "parameters must not be null");

		this.name = name;
		this.parameters = parameters;
	}

	public String getName() {
		return name;
	}

	public HighlightFieldParameters getParameters() {
		return parameters;
	}

	public static HighlightField of(org.springframework.data.elasticsearch.annotations.HighlightField field) {

		org.springframework.data.elasticsearch.annotations.HighlightParameters parameters = field.parameters();
		HighlightFieldParameters highlightParameters = HighlightFieldParameters.builder() //
				.withBoundaryChars(parameters.boundaryChars()) //
				.withBoundaryMaxScan(parameters.boundaryMaxScan()) //
				.withBoundaryScanner(parameters.boundaryScanner()) //
				.withBoundaryScannerLocale(parameters.boundaryScannerLocale()) //
				.withForceSource(parameters.forceSource()) //
				.withFragmenter(parameters.fragmenter()) //
				.withFragmentOffset(parameters.fragmentOffset()) //
				.withFragmentSize(parameters.fragmentSize()) //
				.withMatchedFields(parameters.matchedFields()) //
				.withNoMatchSize(parameters.noMatchSize()) //
				.withNumberOfFragments(parameters.numberOfFragments()) //
				.withOrder(parameters.order()) //
				.withPhraseLimit(parameters.phraseLimit()) //
				.withPreTags(parameters.preTags()) //
				.withPostTags(parameters.postTags()) //
				.withRequireFieldMatch(parameters.requireFieldMatch()) //
				.withType(parameters.type()) //
				.build();

		return new HighlightField(field.name(), highlightParameters);
	}
}
