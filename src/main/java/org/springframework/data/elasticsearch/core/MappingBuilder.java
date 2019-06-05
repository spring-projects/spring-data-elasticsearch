/*
 * Copyright 2014-2019 the original author or authors.
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
package org.springframework.data.elasticsearch.core;

import static org.elasticsearch.common.xcontent.XContentFactory.*;
import static org.springframework.util.StringUtils.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.annotation.Transient;
import org.springframework.data.elasticsearch.annotations.CompletionContext;
import org.springframework.data.elasticsearch.annotations.CompletionField;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.DynamicTemplates;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.GeoPointField;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.Mapping;
import org.springframework.data.elasticsearch.annotations.MultiField;
import org.springframework.data.elasticsearch.core.completion.Completion;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentProperty;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Artur Konczak
 * @author Kevin Leturc
 * @author Alexander Volz
 * @author Dennis Maaß
 * @author Pavel Luhin
 * @author Mark Paluch
 * @author Sascha Woo
 * @author Nordine Bittich
 * @author Robert Gruendler
 * @author Petr Kukral
 * @author Peter-Josef Meisch
 */
class MappingBuilder {

	private static final String FIELD_DATA = "fielddata";
	private static final String FIELD_STORE = "store";
	private static final String FIELD_TYPE = "type";
	private static final String FIELD_INDEX = "index";
	private static final String FIELD_FORMAT = "format";
	private static final String FIELD_SEARCH_ANALYZER = "search_analyzer";
	private static final String FIELD_INDEX_ANALYZER = "analyzer";
	private static final String FIELD_NORMALIZER = "normalizer";
	private static final String FIELD_PROPERTIES = "properties";
	private static final String FIELD_PARENT = "_parent";
	private static final String FIELD_COPY_TO = "copy_to";
	private static final String FIELD_CONTEXT_NAME = "name";
	private static final String FIELD_CONTEXT_TYPE = "type";
	private static final String FIELD_CONTEXT_PRECISION = "precision";
	private static final String FIELD_DYNAMIC_TEMPLATES = "dynamic_templates";

	private static final String COMPLETION_PRESERVE_SEPARATORS = "preserve_separators";
	private static final String COMPLETION_PRESERVE_POSITION_INCREMENTS = "preserve_position_increments";
	private static final String COMPLETION_MAX_INPUT_LENGTH = "max_input_length";
	private static final String COMPLETION_CONTEXTS = "contexts";

	private static final String TYPE_VALUE_KEYWORD = "keyword";
	private static final String TYPE_VALUE_GEO_POINT = "geo_point";
	private static final String TYPE_VALUE_COMPLETION = "completion";

	private static final Logger logger = LoggerFactory.getLogger(ElasticsearchRestTemplate.class);

	private final ElasticsearchConverter elasticsearchConverter;

	MappingBuilder(ElasticsearchConverter elasticsearchConverter) {
		this.elasticsearchConverter = elasticsearchConverter;
	}

	/**
	 * builds the Elasticsearch mapping for the given clazz.
	 *
	 * @return JSON string
	 * @throws IOException
	 */
	String buildPropertyMapping(Class<?> clazz) throws IOException {

		ElasticsearchPersistentEntity<?> entity = elasticsearchConverter.getMappingContext()
				.getRequiredPersistentEntity(clazz);

		XContentBuilder builder = jsonBuilder().startObject().startObject(entity.getIndexType());

		// Dynamic templates
		addDynamicTemplatesMapping(builder, entity);

		// Parent
		String parentType = entity.getParentType();
		if (hasText(parentType)) {
			builder.startObject(FIELD_PARENT).field(FIELD_TYPE, parentType).endObject();
		}

		// Properties
		builder.startObject(FIELD_PROPERTIES);

		mapEntity(builder, entity, true, "", false, FieldType.Auto, null);

		builder.endObject() // FIELD_PROPERTIES
				.endObject() // indexType
				.endObject() // root object
				.close();

		return builder.getOutputStream().toString();
	}

	private void mapEntity(XContentBuilder builder, @Nullable ElasticsearchPersistentEntity entity, boolean isRootObject,
			String nestedObjectFieldName, boolean nestedOrObjectField, FieldType fieldType,
			@Nullable Field parentFieldAnnotation) throws IOException {

		boolean writeNestedProperties = !isRootObject && (isAnyPropertyAnnotatedWithField(entity) || nestedOrObjectField);
		if (writeNestedProperties) {

			String type = nestedOrObjectField ? fieldType.toString().toLowerCase()
					: FieldType.Object.toString().toLowerCase();
			builder.startObject(nestedObjectFieldName).field(FIELD_TYPE, type);

			if (nestedOrObjectField && FieldType.Nested == fieldType && parentFieldAnnotation != null
					&& parentFieldAnnotation.includeInParent()) {

				builder.field("include_in_parent", parentFieldAnnotation.includeInParent());
			}

			builder.startObject(FIELD_PROPERTIES);
		}
		if (entity != null) {

			entity.doWithProperties((PropertyHandler<ElasticsearchPersistentProperty>) property -> {
				try {
					if (property.isAnnotationPresent(Transient.class) || isInIgnoreFields(property, parentFieldAnnotation)) {
						return;
					}

					buildPropertyMapping(builder, isRootObject, property);
				} catch (IOException e) {
					logger.warn("error mapping property with name {}", property.getName(), e);
				}
			});
		}

		if (writeNestedProperties) {
			builder.endObject().endObject();
		}
	}

	private void buildPropertyMapping(XContentBuilder builder, boolean isRootObject,
			ElasticsearchPersistentProperty property) throws IOException {

		if (property.isAnnotationPresent(Mapping.class)) {

			String mappingPath = property.getRequiredAnnotation(Mapping.class).mappingPath();
			if (!StringUtils.isEmpty(mappingPath)) {

				ClassPathResource mappings = new ClassPathResource(mappingPath);
				if (mappings.exists()) {
					builder.rawField(property.getFieldName(), mappings.getInputStream(), XContentType.JSON);
					return;
				}
			}
		}

		boolean isGeoPointProperty = isGeoPointProperty(property);
		boolean isCompletionProperty = isCompletionProperty(property);
		boolean isNestedOrObjectProperty = isNestedOrObjectProperty(property);

		Field fieldAnnotation = property.findAnnotation(Field.class);
		if (!isGeoPointProperty && !isCompletionProperty && property.isEntity() && hasRelevantAnnotation(property)) {

			if (fieldAnnotation == null) {
				return;
			}

			Iterator<? extends TypeInformation<?>> iterator = property.getPersistentEntityTypes().iterator();
			ElasticsearchPersistentEntity<?> persistentEntity = iterator.hasNext()
					? elasticsearchConverter.getMappingContext().getPersistentEntity(iterator.next())
					: null;

			mapEntity(builder, persistentEntity, false, property.getFieldName(), isNestedOrObjectProperty,
					fieldAnnotation.type(), fieldAnnotation);

			if (isNestedOrObjectProperty) {
				return;
			}
		}

		MultiField multiField = property.findAnnotation(MultiField.class);

		if (isGeoPointProperty) {
			applyGeoPointFieldMapping(builder, property);
			return;
		}

		if (isCompletionProperty) {
			CompletionField completionField = property.findAnnotation(CompletionField.class);
			applyCompletionFieldMapping(builder, property, completionField);
		}

		if (isRootObject && fieldAnnotation != null && property.isIdProperty()) {
			applyDefaultIdFieldMapping(builder, property);
		} else if (multiField != null) {
			addMultiFieldMapping(builder, property, multiField, isNestedOrObjectProperty);
		} else if (fieldAnnotation != null) {
			addSingleFieldMapping(builder, property, fieldAnnotation, isNestedOrObjectProperty);
		}
	}

	private boolean hasRelevantAnnotation(ElasticsearchPersistentProperty property) {

		return property.findAnnotation(Field.class) != null || property.findAnnotation(MultiField.class) != null
				|| property.findAnnotation(GeoPointField.class) != null
				|| property.findAnnotation(CompletionField.class) != null;
	}

	private void applyGeoPointFieldMapping(XContentBuilder builder, ElasticsearchPersistentProperty property)
			throws IOException {

		builder.startObject(property.getFieldName()).field(FIELD_TYPE, TYPE_VALUE_GEO_POINT).endObject();
	}

	private void applyCompletionFieldMapping(XContentBuilder builder, ElasticsearchPersistentProperty property,
			@Nullable CompletionField annotation) throws IOException {

		builder.startObject(property.getFieldName());
		builder.field(FIELD_TYPE, TYPE_VALUE_COMPLETION);

		if (annotation != null) {

			builder.field(COMPLETION_MAX_INPUT_LENGTH, annotation.maxInputLength());
			builder.field(COMPLETION_PRESERVE_POSITION_INCREMENTS, annotation.preservePositionIncrements());
			builder.field(COMPLETION_PRESERVE_SEPARATORS, annotation.preserveSeparators());
			if (!StringUtils.isEmpty(annotation.searchAnalyzer())) {
				builder.field(FIELD_SEARCH_ANALYZER, annotation.searchAnalyzer());
			}
			if (!StringUtils.isEmpty(annotation.analyzer())) {
				builder.field(FIELD_INDEX_ANALYZER, annotation.analyzer());
			}

			if (annotation.contexts().length > 0) {

				builder.startArray(COMPLETION_CONTEXTS);
				for (CompletionContext context : annotation.contexts()) {

					builder.startObject();
					builder.field(FIELD_CONTEXT_NAME, context.name());
					builder.field(FIELD_CONTEXT_TYPE, context.type().name().toLowerCase());
					if (context.precision().length() > 0) {
						builder.field(FIELD_CONTEXT_PRECISION, context.precision());
					}
					builder.endObject();
				}
				builder.endArray();
			}

		}
		builder.endObject();
	}

	private void applyDefaultIdFieldMapping(XContentBuilder builder, ElasticsearchPersistentProperty property)
			throws IOException {

		builder.startObject(property.getFieldName()).field(FIELD_TYPE, TYPE_VALUE_KEYWORD).field(FIELD_INDEX, true)
				.endObject();
	}

	/**
	 * Add mapping for @Field annotation
	 *
	 * @throws IOException
	 */
	private void addSingleFieldMapping(XContentBuilder builder, ElasticsearchPersistentProperty property,
			Field annotation, boolean nestedOrObjectField) throws IOException {

		builder.startObject(property.getFieldName());
		addFieldMappingParameters(builder, annotation, nestedOrObjectField);
		builder.endObject();
	}

	/**
	 * Add mapping for @MultiField annotation
	 *
	 * @throws IOException
	 */
	private void addMultiFieldMapping(XContentBuilder builder, ElasticsearchPersistentProperty property,
			MultiField annotation, boolean nestedOrObjectField) throws IOException {

		// main field
		builder.startObject(property.getFieldName());
		addFieldMappingParameters(builder, annotation.mainField(), nestedOrObjectField);

		// inner fields
		builder.startObject("fields");
		for (InnerField innerField : annotation.otherFields()) {
			builder.startObject(innerField.suffix());
			addFieldMappingParameters(builder, innerField, false);
			builder.endObject();
		}
		builder.endObject();

		builder.endObject();
	}

	private void addFieldMappingParameters(XContentBuilder builder, Object annotation, boolean nestedOrObjectField)
			throws IOException {

		boolean index = true;
		boolean store = false;
		boolean fielddata = false;
		FieldType type = null;
		DateFormat dateFormat = null;
		String datePattern = null;
		String analyzer = null;
		String searchAnalyzer = null;
		String normalizer = null;
		String[] copyTo = null;

		if (annotation instanceof Field) {
			// @Field
			Field fieldAnnotation = (Field) annotation;
			index = fieldAnnotation.index();
			store = fieldAnnotation.store();
			fielddata = fieldAnnotation.fielddata();
			type = fieldAnnotation.type();
			dateFormat = fieldAnnotation.format();
			datePattern = fieldAnnotation.pattern();
			analyzer = fieldAnnotation.analyzer();
			searchAnalyzer = fieldAnnotation.searchAnalyzer();
			normalizer = fieldAnnotation.normalizer();
			copyTo = fieldAnnotation.copyTo();
		} else if (annotation instanceof InnerField) {
			// @InnerField
			InnerField fieldAnnotation = (InnerField) annotation;
			index = fieldAnnotation.index();
			store = fieldAnnotation.store();
			fielddata = fieldAnnotation.fielddata();
			type = fieldAnnotation.type();
			dateFormat = fieldAnnotation.format();
			datePattern = fieldAnnotation.pattern();
			analyzer = fieldAnnotation.analyzer();
			searchAnalyzer = fieldAnnotation.searchAnalyzer();
			normalizer = fieldAnnotation.normalizer();
		} else {
			throw new IllegalArgumentException("annotation must be an instance of @Field or @InnerField");
		}

		if (!nestedOrObjectField) {
			builder.field(FIELD_STORE, store);
		}
		if (fielddata) {
			builder.field(FIELD_DATA, fielddata);
		}
		if (type != FieldType.Auto) {
			builder.field(FIELD_TYPE, type.name().toLowerCase());

			if (type == FieldType.Date && dateFormat != DateFormat.none) {
				builder.field(FIELD_FORMAT, dateFormat == DateFormat.custom ? datePattern : dateFormat.toString());
			}
		}
		if (!index) {
			builder.field(FIELD_INDEX, index);
		}
		if (!StringUtils.isEmpty(analyzer)) {
			builder.field(FIELD_INDEX_ANALYZER, analyzer);
		}
		if (!StringUtils.isEmpty(searchAnalyzer)) {
			builder.field(FIELD_SEARCH_ANALYZER, searchAnalyzer);
		}
		if (!StringUtils.isEmpty(normalizer)) {
			builder.field(FIELD_NORMALIZER, normalizer);
		}
		if (copyTo != null && copyTo.length > 0) {
			builder.field(FIELD_COPY_TO, copyTo);
		}
	}

	/**
	 * Apply mapping for dynamic templates.
	 *
	 * @throws IOException
	 */
	private void addDynamicTemplatesMapping(XContentBuilder builder, ElasticsearchPersistentEntity<?> entity)
			throws IOException {

		if (entity.isAnnotationPresent(DynamicTemplates.class)) {
			String mappingPath = entity.getRequiredAnnotation(DynamicTemplates.class).mappingPath();
			if (hasText(mappingPath)) {

				String jsonString = ResourceUtil.readFileFromClasspath(mappingPath);
				if (hasText(jsonString)) {

					ObjectMapper objectMapper = new ObjectMapper();
					JsonNode jsonNode = objectMapper.readTree(jsonString).get("dynamic_templates");
					if (jsonNode != null && jsonNode.isArray()) {
						String json = objectMapper.writeValueAsString(jsonNode);
						builder.rawField(FIELD_DYNAMIC_TEMPLATES, new ByteArrayInputStream(json.getBytes()), XContentType.JSON);
					}
				}
			}
		}
	}

	private boolean isAnyPropertyAnnotatedWithField(@Nullable ElasticsearchPersistentEntity entity) {

		return entity != null && entity.getPersistentProperty(Field.class) != null;
	}

	private boolean isInIgnoreFields(ElasticsearchPersistentProperty property, @Nullable Field parentFieldAnnotation) {

		if (null != parentFieldAnnotation) {

			String[] ignoreFields = parentFieldAnnotation.ignoreFields();
			return Arrays.asList(ignoreFields).contains(property.getFieldName());
		}
		return false;
	}

	private boolean isNestedOrObjectProperty(ElasticsearchPersistentProperty property) {

		Field fieldAnnotation = property.findAnnotation(Field.class);
		return fieldAnnotation != null
				&& (FieldType.Nested == fieldAnnotation.type() || FieldType.Object == fieldAnnotation.type());
	}

	private boolean isGeoPointProperty(ElasticsearchPersistentProperty property) {
		return property.getActualType() == GeoPoint.class || property.isAnnotationPresent(GeoPointField.class);
	}

	private boolean isCompletionProperty(ElasticsearchPersistentProperty property) {
		return property.getActualType() == Completion.class;
	}
}
