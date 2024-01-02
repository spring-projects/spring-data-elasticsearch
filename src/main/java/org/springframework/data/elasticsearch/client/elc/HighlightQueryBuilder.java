/*
 * Copyright 2022-2024 the original author or authors.
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
package org.springframework.data.elasticsearch.client.elc;

import static org.springframework.data.elasticsearch.client.elc.TypeUtils.*;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentProperty;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightFieldParameters;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightParameters;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * Converts the {@link Highlight} annotation from a method to an ElasticsearchClient
 * {@link co.elastic.clients.elasticsearch.core.search.Highlight}.
 *
 * @author Peter-Josef Meisch
 * @author Haibo Liu
 * @since 4.4
 */
class HighlightQueryBuilder {
	private final MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> mappingContext;
	private final RequestConverter requestConverter;

	HighlightQueryBuilder(
			MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> mappingContext, RequestConverter requestConverter) {
		this.mappingContext = mappingContext;
		this.requestConverter = requestConverter;
	}

	public co.elastic.clients.elasticsearch.core.search.Highlight getHighlight(Highlight highlight,
			@Nullable Class<?> type) {

		co.elastic.clients.elasticsearch.core.search.Highlight.Builder highlightBuilder = new co.elastic.clients.elasticsearch.core.search.Highlight.Builder();

		// in the old implementation we could use one addParameters method, but in the new Elasticsearch client
		// the builder for highlight and highlightfield share no code
		addParameters(highlight.getParameters(), highlightBuilder, type);

		for (HighlightField highlightField : highlight.getFields()) {
			String mappedName = mapFieldName(highlightField.getName(), type);
			highlightBuilder.fields(mappedName, hf -> {
				addParameters(highlightField.getParameters(), hf, type);
				return hf;
			});
		}

		return highlightBuilder.build();
	}

	/*
	 * the builder for highlight and highlight fields don't share code, so we have these two methods here that basically are almost copies
	 */
	private void addParameters(HighlightParameters parameters,
			co.elastic.clients.elasticsearch.core.search.Highlight.Builder builder, @Nullable Class<?> type) {

		if (StringUtils.hasLength(parameters.getBoundaryChars())) {
			builder.boundaryChars(parameters.getBoundaryChars());
		}

		if (parameters.getBoundaryMaxScan() > -1) {
			builder.boundaryMaxScan(parameters.getBoundaryMaxScan());
		}

		if (StringUtils.hasLength(parameters.getBoundaryScanner())) {
			builder.boundaryScanner(boundaryScanner(parameters.getBoundaryScanner()));
		}

		if (StringUtils.hasLength(parameters.getBoundaryScannerLocale())) {
			builder.boundaryScannerLocale(parameters.getBoundaryScannerLocale());
		}

		if (StringUtils.hasLength(parameters.getFragmenter())) {
			builder.fragmenter(highlighterFragmenter(parameters.getFragmenter()));
		}

		if (parameters.getFragmentSize() > -1) {
			builder.fragmentSize(parameters.getFragmentSize());
		}

		if (parameters.getNoMatchSize() > -1) {
			builder.noMatchSize(parameters.getNoMatchSize());
		}

		if (parameters.getNumberOfFragments() > -1) {
			builder.numberOfFragments(parameters.getNumberOfFragments());
		}

		if (parameters.getHighlightQuery() != null) {
			builder.highlightQuery(requestConverter.getQuery(parameters.getHighlightQuery(), type));
		}

		if (StringUtils.hasLength(parameters.getOrder())) {
			builder.order(highlighterOrder(parameters.getOrder()));
		}

		if (parameters.getPreTags().length > 0) {
			builder.preTags(Arrays.asList(parameters.getPreTags()));
		}

		if (parameters.getPostTags().length > 0) {
			builder.postTags(Arrays.asList(parameters.getPostTags()));
		}

		if (!parameters.getRequireFieldMatch()) { // default is true
			builder.requireFieldMatch(false);
		}

		if (StringUtils.hasLength(parameters.getType())) {
			builder.type(highlighterType(parameters.getType()));
		}

		if (StringUtils.hasLength(parameters.getEncoder())) {
			builder.encoder(highlighterEncoder(parameters.getEncoder()));
		}

		if (StringUtils.hasLength(parameters.getTagsSchema())) {
			builder.tagsSchema(highlighterTagsSchema(parameters.getTagsSchema()));
		}
	}

	/*
	 * the builder for highlight and highlight fields don't share code, so we have these two methods here that basically are almost copies
	 */
	private void addParameters(HighlightFieldParameters parameters,
			co.elastic.clients.elasticsearch.core.search.HighlightField.Builder builder, Class<?> type) {

		if (StringUtils.hasLength(parameters.getBoundaryChars())) {
			builder.boundaryChars(parameters.getBoundaryChars());
		}

		if (parameters.getBoundaryMaxScan() > -1) {
			builder.boundaryMaxScan(parameters.getBoundaryMaxScan());
		}

		if (StringUtils.hasLength(parameters.getBoundaryScanner())) {
			builder.boundaryScanner(boundaryScanner(parameters.getBoundaryScanner()));
		}

		if (StringUtils.hasLength(parameters.getBoundaryScannerLocale())) {
			builder.boundaryScannerLocale(parameters.getBoundaryScannerLocale());
		}

		if (parameters.getForceSource()) { // default is false
			builder.forceSource(parameters.getForceSource());
		}

		if (StringUtils.hasLength(parameters.getFragmenter())) {
			builder.fragmenter(highlighterFragmenter(parameters.getFragmenter()));
		}

		if (parameters.getFragmentSize() > -1) {
			builder.fragmentSize(parameters.getFragmentSize());
		}

		if (parameters.getNoMatchSize() > -1) {
			builder.noMatchSize(parameters.getNoMatchSize());
		}

		if (parameters.getNumberOfFragments() > -1) {
			builder.numberOfFragments(parameters.getNumberOfFragments());
		}

		if (parameters.getHighlightQuery() != null) {
			builder.highlightQuery(requestConverter.getQuery(parameters.getHighlightQuery(), type));
		}

		if (StringUtils.hasLength(parameters.getOrder())) {
			builder.order(highlighterOrder(parameters.getOrder()));
		}

		if (parameters.getPhraseLimit() > -1) {
			builder.phraseLimit(parameters.getPhraseLimit());
		}

		if (parameters.getPreTags().length > 0) {
			builder.preTags(Arrays.asList(parameters.getPreTags()));
		}

		if (parameters.getPostTags().length > 0) {
			builder.postTags(Arrays.asList(parameters.getPostTags()));
		}

		if (!parameters.getRequireFieldMatch()) { // default is true
			builder.requireFieldMatch(false);
		}

		if (StringUtils.hasLength(parameters.getType())) {
			builder.type(highlighterType(parameters.getType()));
		}

		if ((parameters).getFragmentOffset() > -1) {
			builder.fragmentOffset(parameters.getFragmentOffset());
		}

		if (parameters.getMatchedFields().length > 0) {
			builder.matchedFields(Arrays.stream(parameters.getMatchedFields()).map(fieldName -> mapFieldName(fieldName, type)) //
					.collect(Collectors.toList()));
		}
	}

	private String mapFieldName(String fieldName, @Nullable Class<?> type) {

		if (type != null) {
			ElasticsearchPersistentEntity<?> persistentEntity = mappingContext.getPersistentEntity(type);

			if (persistentEntity != null) {
				ElasticsearchPersistentProperty persistentProperty = persistentEntity.getPersistentProperty(fieldName);

				if (persistentProperty != null) {
					return persistentProperty.getFieldName();
				}
			}
		}

		return fieldName;
	}

}
