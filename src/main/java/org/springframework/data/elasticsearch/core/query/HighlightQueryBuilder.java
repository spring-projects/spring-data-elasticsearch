/*
 * Copyright 2020-2020 the original author or authors.
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

import java.util.Arrays;
import java.util.stream.Collectors;

import org.elasticsearch.search.fetch.subphase.highlight.AbstractHighlighterBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.springframework.data.elasticsearch.annotations.Highlight;
import org.springframework.data.elasticsearch.annotations.HighlightField;
import org.springframework.data.elasticsearch.annotations.HighlightParameters;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentProperty;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Converts the {@link Highlight} annotation from a method to an Elasticsearch {@link HighlightBuilder}.
 * 
 * @author Peter-Josef Meisch
 */
public class HighlightQueryBuilder {

	private final MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> mappingContext;

	public HighlightQueryBuilder(
			MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> mappingContext) {
		this.mappingContext = mappingContext;
	}

	/**
	 * creates a HighlightBuilder from an annotation
	 * 
	 * @param highlight, must not be {@literal null}
	 * @param type the entity type, used to map field names. If null, field names are not mapped.
	 * @return the builder for the highlight
	 */
	public HighlightQuery getHighlightQuery(Highlight highlight, @Nullable Class<?> type) {

		Assert.notNull(highlight, "highlight must not be null");

		HighlightBuilder highlightBuilder = new HighlightBuilder();

		addParameters(highlight.parameters(), highlightBuilder, type);

		for (HighlightField highlightField : highlight.fields()) {
			String mappedName = mapFieldName(highlightField.name(), type);
			HighlightBuilder.Field field = new HighlightBuilder.Field(mappedName);

			addParameters(highlightField.parameters(), field, type);

			highlightBuilder.field(field);
		}

		return new HighlightQuery(highlightBuilder);
	}

	private void addParameters(HighlightParameters parameters, AbstractHighlighterBuilder<?> builder, Class<?> type) {

		if (StringUtils.hasLength(parameters.boundaryChars())) {
			builder.boundaryChars(parameters.boundaryChars().toCharArray());
		}

		if (parameters.boundaryMaxScan() > -1) {
			builder.boundaryMaxScan(parameters.boundaryMaxScan());
		}

		if (StringUtils.hasLength(parameters.boundaryScanner())) {
			builder.boundaryScannerType(parameters.boundaryScanner());
		}

		if (StringUtils.hasLength(parameters.boundaryScannerLocale())) {
			builder.boundaryScannerLocale(parameters.boundaryScannerLocale());
		}

		if (parameters.forceSource()) { // default is false
			builder.forceSource(parameters.forceSource());
		}

		if (StringUtils.hasLength(parameters.fragmenter())) {
			builder.fragmenter(parameters.fragmenter());
		}

		if (parameters.fragmentSize() > -1) {
			builder.fragmentSize(parameters.fragmentSize());
		}

		if (parameters.noMatchSize() > -1) {
			builder.noMatchSize(parameters.noMatchSize());
		}

		if (parameters.numberOfFragments() > -1) {
			builder.numOfFragments(parameters.numberOfFragments());
		}

		if (StringUtils.hasLength(parameters.order())) {
			builder.order(parameters.order());
		}

		if (parameters.phraseLimit() > -1) {
			builder.phraseLimit(parameters.phraseLimit());
		}

		if (parameters.preTags().length > 0) {
			builder.preTags(parameters.preTags());
		}

		if (parameters.postTags().length > 0) {
			builder.postTags(parameters.postTags());
		}

		if (!parameters.requireFieldMatch()) { // default is true
			builder.requireFieldMatch(parameters.requireFieldMatch());
		}

		if (StringUtils.hasLength(parameters.type())) {
			builder.highlighterType(parameters.type());
		}

		if (builder instanceof HighlightBuilder) {
			HighlightBuilder highlightBuilder = (HighlightBuilder) builder;

			if (StringUtils.hasLength(parameters.encoder())) {
				highlightBuilder.encoder(parameters.encoder());
			}

			if (StringUtils.hasLength(parameters.tagsSchema())) {
				highlightBuilder.tagsSchema(parameters.tagsSchema());
			}
		}

		if (builder instanceof HighlightBuilder.Field) {
			HighlightBuilder.Field field = (HighlightBuilder.Field) builder;

			if (parameters.fragmentOffset() > -1) {
				field.fragmentOffset(parameters.fragmentOffset());
			}

			if (parameters.matchedFields().length > 0) {
				field.matchedFields(Arrays.stream(parameters.matchedFields()) //
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
