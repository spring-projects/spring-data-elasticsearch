/*
 * Copyright 2020-2022 the original author or authors.
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
package org.springframework.data.elasticsearch.client.erhlc;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.elasticsearch.search.fetch.subphase.highlight.AbstractHighlighterBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentProperty;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightCommonParameters;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightFieldParameters;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightParameters;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * Converts the {@link Highlight} annotation from a method to an Elasticsearch 7 {@link HighlightBuilder}.
 *
 * @author Peter-Josef Meisch
 * @deprecated since 5.0
 */
@Deprecated
public class HighlightQueryBuilder {

	private final MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> mappingContext;

	public HighlightQueryBuilder(
			MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> mappingContext) {
		this.mappingContext = mappingContext;
	}

	/**
	 * creates an Elasticsearch HighlightBuilder from an annotation.
	 *
	 * @param highlight, must not be {@literal null}
	 * @param type the entity type, used to map field names. If null, field names are not mapped.
	 * @return the builder for the highlight
	 */
	public HighlightBuilder getHighlightBuilder(Highlight highlight, @Nullable Class<?> type) {
		HighlightBuilder highlightBuilder = new HighlightBuilder();

		addParameters(highlight.getParameters(), highlightBuilder, type);

		for (HighlightField highlightField : highlight.getFields()) {
			String mappedName = mapFieldName(highlightField.getName(), type);
			HighlightBuilder.Field field = new HighlightBuilder.Field(mappedName);

			addParameters(highlightField.getParameters(), field, type);

			highlightBuilder.field(field);
		}
		return highlightBuilder;
	}

	private <P extends HighlightCommonParameters> void addParameters(P parameters, AbstractHighlighterBuilder<?> builder,
			@Nullable Class<?> type) {

		if (StringUtils.hasLength(parameters.getBoundaryChars())) {
			builder.boundaryChars(parameters.getBoundaryChars().toCharArray());
		}

		if (parameters.getBoundaryMaxScan() > -1) {
			builder.boundaryMaxScan(parameters.getBoundaryMaxScan());
		}

		if (StringUtils.hasLength(parameters.getBoundaryScanner())) {
			builder.boundaryScannerType(parameters.getBoundaryScanner());
		}

		if (StringUtils.hasLength(parameters.getBoundaryScannerLocale())) {
			builder.boundaryScannerLocale(parameters.getBoundaryScannerLocale());
		}

		if (parameters.getForceSource()) { // default is false
			builder.forceSource(true);
		}

		if (StringUtils.hasLength(parameters.getFragmenter())) {
			builder.fragmenter(parameters.getFragmenter());
		}

		if (parameters.getFragmentSize() > -1) {
			builder.fragmentSize(parameters.getFragmentSize());
		}

		if (parameters.getNoMatchSize() > -1) {
			builder.noMatchSize(parameters.getNoMatchSize());
		}

		if (parameters.getNumberOfFragments() > -1) {
			builder.numOfFragments(parameters.getNumberOfFragments());
		}

		if (StringUtils.hasLength(parameters.getOrder())) {
			builder.order(parameters.getOrder());
		}

		if (parameters.getPhraseLimit() > -1) {
			builder.phraseLimit(parameters.getPhraseLimit());
		}

		if (parameters.getPreTags().length > 0) {
			builder.preTags(parameters.getPreTags());
		}

		if (parameters.getPostTags().length > 0) {
			builder.postTags(parameters.getPostTags());
		}

		if (!parameters.getRequireFieldMatch()) { // default is true
			builder.requireFieldMatch(false);
		}

		if (StringUtils.hasLength(parameters.getType())) {
			builder.highlighterType(parameters.getType());
		}

		if (builder instanceof HighlightBuilder && parameters instanceof HighlightParameters) {
			HighlightBuilder highlightBuilder = (HighlightBuilder) builder;
			HighlightParameters highlightParameters = (HighlightParameters) parameters;

			if (StringUtils.hasLength(highlightParameters.getEncoder())) {
				highlightBuilder.encoder(highlightParameters.getEncoder());
			}

			if (StringUtils.hasLength(highlightParameters.getTagsSchema())) {
				highlightBuilder.tagsSchema(highlightParameters.getTagsSchema());
			}
		}

		if (builder instanceof HighlightBuilder.Field && parameters instanceof HighlightFieldParameters) {
			HighlightBuilder.Field field = (HighlightBuilder.Field) builder;
			HighlightFieldParameters fieldParameters = (HighlightFieldParameters) parameters;

			if ((fieldParameters).getFragmentOffset() > -1) {
				field.fragmentOffset(fieldParameters.getFragmentOffset());
			}

			if (fieldParameters.getMatchedFields().length > 0) {
				field.matchedFields(Arrays.stream(fieldParameters.getMatchedFields()) //
						.map(fieldName -> mapFieldName(fieldName, type)) //
						.collect(Collectors.toList()) //
						.toArray(new String[] {})); //
			}
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
