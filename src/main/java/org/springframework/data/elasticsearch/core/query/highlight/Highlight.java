/*
 * Copyright 2021-2023 the original author or authors.
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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.util.Assert;

/**
 * @author Peter-Josef Meisch
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

	/**
	 * Creates a {@link Highlight} from an Annotation instance.
	 *
	 * @param highlight must not be {@literal null}
	 * @return highlight definition
	 */
	public static Highlight of(org.springframework.data.elasticsearch.annotations.Highlight highlight) {

		Assert.notNull(highlight, "highlight must not be null");

		org.springframework.data.elasticsearch.annotations.HighlightParameters parameters = highlight.parameters();
		HighlightParameters highlightParameters = HighlightParameters.builder() //
				.withBoundaryChars(parameters.boundaryChars()) //
				.withBoundaryMaxScan(parameters.boundaryMaxScan()) //
				.withBoundaryScanner(parameters.boundaryScanner()) //
				.withBoundaryScannerLocale(parameters.boundaryScannerLocale()) //
				.withEncoder(parameters.encoder()) //
				.withForceSource(parameters.forceSource()) //
				.withFragmenter(parameters.fragmenter()) //
				.withFragmentSize(parameters.fragmentSize()) //
				.withNoMatchSize(parameters.noMatchSize()) //
				.withNumberOfFragments(parameters.numberOfFragments()) //
				.withOrder(parameters.order()) //
				.withPhraseLimit(parameters.phraseLimit()) //
				.withPreTags(parameters.preTags()) //
				.withPostTags(parameters.postTags()) //
				.withRequireFieldMatch(parameters.requireFieldMatch()) //
				.withTagsSchema(parameters.tagsSchema()) //
				.withType(parameters.type()) //
				.build();

		List<HighlightField> highlightFields = Arrays.stream(highlight.fields()) //
				.map(HighlightField::of) //
				.collect(Collectors.toList());

		return new Highlight(highlightParameters, highlightFields);
	}
}
