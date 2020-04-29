/*
 * Copyright 2014-2020 the original author or authors.
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
package org.springframework.data.elasticsearch.core.index;

import static org.elasticsearch.common.xcontent.XContentFactory.*;
import static org.springframework.data.elasticsearch.core.index.MappingParameters.*;
import static org.springframework.util.StringUtils.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Iterator;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.annotation.Transient;
import org.springframework.data.elasticsearch.ElasticsearchException;
import org.springframework.data.elasticsearch.annotations.CompletionContext;
import org.springframework.data.elasticsearch.annotations.CompletionField;
import org.springframework.data.elasticsearch.annotations.DynamicMapping;
import org.springframework.data.elasticsearch.annotations.DynamicTemplates;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.GeoPointField;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.Mapping;
import org.springframework.data.elasticsearch.annotations.MultiField;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.ResourceUtil;
import org.springframework.data.elasticsearch.core.completion.Completion;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentProperty;
import org.springframework.data.mapping.MappingException;
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
 * @author Xiao Yu
 */
public class MappingBuilder {

	private static final String FIELD_INDEX = "index";
	private static final String FIELD_PROPERTIES = "properties";
	private static final String FIELD_PARENT = "_parent";
	private static final String FIELD_CONTEXT_NAME = "name";
	private static final String FIELD_CONTEXT_TYPE = "type";
	private static final String FIELD_CONTEXT_PATH = "path";
	private static final String FIELD_CONTEXT_PRECISION = "precision";
	private static final String FIELD_DYNAMIC_TEMPLATES = "dynamic_templates";

	private static final String COMPLETION_PRESERVE_SEPARATORS = "preserve_separators";
	private static final String COMPLETION_PRESERVE_POSITION_INCREMENTS = "preserve_position_increments";
	private static final String COMPLETION_MAX_INPUT_LENGTH = "max_input_length";
	private static final String COMPLETION_CONTEXTS = "contexts";

	private static final String TYPE_DYNAMIC = "dynamic";
	private static final String TYPE_VALUE_KEYWORD = "keyword";
	private static final String TYPE_VALUE_GEO_POINT = "geo_point";
	private static final String TYPE_VALUE_COMPLETION = "completion";

	private static final Logger logger = LoggerFactory.getLogger(ElasticsearchRestTemplate.class);

	private final ElasticsearchConverter elasticsearchConverter;

	public MappingBuilder(ElasticsearchConverter elasticsearchConverter) {
		this.elasticsearchConverter = elasticsearchConverter;
	}

	/**
	 * builds the Elasticsearch mapping for the given clazz.
	 *
	 * @return JSON string
	 * @throws ElasticsearchException on errors while building the mapping
	 */
	public String buildPropertyMapping(Class<?> clazz) throws ElasticsearchException {

		try {
			ElasticsearchPersistentEntity<?> entity = elasticsearchConverter.getMappingContext()
					.getRequiredPersistentEntity(clazz);

			XContentBuilder builder = jsonBuilder().startObject();

			// Dynamic templates
			addDynamicTemplatesMapping(builder, entity);

			// Parent
			String parentType = entity.getParentType();
			if (hasText(parentType)) {
				builder.startObject(FIELD_PARENT).field(FIELD_PARAM_TYPE, parentType).endObject();
			}

			mapEntity(builder, entity, true, "", false, FieldType.Auto, null, entity.findAnnotation(DynamicMapping.class));

			builder.endObject() // root object
					.close();

			return builder.getOutputStream().toString();
		} catch (MappingException | IOException e) {
			throw new ElasticsearchException("could not build mapping", e);
		}
	}

	private void mapEntity(XContentBuilder builder, @Nullable ElasticsearchPersistentEntity entity, boolean isRootObject,
			String nestedObjectFieldName, boolean nestedOrObjectField, FieldType fieldType,
			@Nullable Field parentFieldAnnotation, @Nullable DynamicMapping dynamicMapping) throws IOException {

		boolean writeNestedProperties = !isRootObject && (isAnyPropertyAnnotatedWithField(entity) || nestedOrObjectField);
		if (writeNestedProperties) {

			String type = nestedOrObjectField ? fieldType.toString().toLowerCase()
					: FieldType.Object.toString().toLowerCase();
			builder.startObject(nestedObjectFieldName).field(FIELD_PARAM_TYPE, type);

			if (nestedOrObjectField && FieldType.Nested == fieldType && parentFieldAnnotation != null
					&& parentFieldAnnotation.includeInParent()) {
				builder.field("include_in_parent", parentFieldAnnotation.includeInParent());
			}
		}

		if (dynamicMapping != null) {
			builder.field(TYPE_DYNAMIC, dynamicMapping.value().name().toLowerCase());
		}

		builder.startObject(FIELD_PROPERTIES);

		if (entity != null) {
			entity.doWithProperties((PropertyHandler<ElasticsearchPersistentProperty>) property -> {
				try {
					if (property.isAnnotationPresent(Transient.class) || isInIgnoreFields(property, parentFieldAnnotation)) {
						return;
					}

					if (property.isSeqNoPrimaryTermProperty()) {
						if (property.isAnnotationPresent(Field.class)) {
							logger.warn("Property {} of {} is annotated for inclusion in mapping, but its type is " + //
							"SeqNoPrimaryTerm that is never mapped, so it is skipped", //
									property.getFieldName(), entity.getType());
						}
						return;
					}

					buildPropertyMapping(builder, isRootObject, property);
				} catch (IOException e) {
					logger.warn("error mapping property with name {}", property.getName(), e);
				}
			});
		}

		builder.endObject();

		if (writeNestedProperties) {
			builder.endObject();
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

		if (isGeoPointProperty(property)) {
			applyGeoPointFieldMapping(builder, property);
			return;
		}

		Field fieldAnnotation = property.findAnnotation(Field.class);
		boolean isCompletionProperty = isCompletionProperty(property);
		boolean isNestedOrObjectProperty = isNestedOrObjectProperty(property);

		if (!isCompletionProperty && property.isEntity() && hasRelevantAnnotation(property)) {

			if (fieldAnnotation == null) {
				return;
			}

			if (isNestedOrObjectProperty) {
				Iterator<? extends TypeInformation<?>> iterator = property.getPersistentEntityTypes().iterator();
				ElasticsearchPersistentEntity<?> persistentEntity = iterator.hasNext()
						? elasticsearchConverter.getMappingContext().getPersistentEntity(iterator.next())
						: null;

				mapEntity(builder, persistentEntity, false, property.getFieldName(), isNestedOrObjectProperty,
						fieldAnnotation.type(), fieldAnnotation, property.findAnnotation(DynamicMapping.class));
				return;
			}
		}

		MultiField multiField = property.findAnnotation(MultiField.class);

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

		builder.startObject(property.getFieldName()).field(FIELD_PARAM_TYPE, TYPE_VALUE_GEO_POINT).endObject();
	}

	private void applyCompletionFieldMapping(XContentBuilder builder, ElasticsearchPersistentProperty property,
			@Nullable CompletionField annotation) throws IOException {

		builder.startObject(property.getFieldName());
		builder.field(FIELD_PARAM_TYPE, TYPE_VALUE_COMPLETION);

		if (annotation != null) {

			builder.field(COMPLETION_MAX_INPUT_LENGTH, annotation.maxInputLength());
			builder.field(COMPLETION_PRESERVE_POSITION_INCREMENTS, annotation.preservePositionIncrements());
			builder.field(COMPLETION_PRESERVE_SEPARATORS, annotation.preserveSeparators());
			if (!StringUtils.isEmpty(annotation.searchAnalyzer())) {
				builder.field(FIELD_PARAM_SEARCH_ANALYZER, annotation.searchAnalyzer());
			}
			if (!StringUtils.isEmpty(annotation.analyzer())) {
				builder.field(FIELD_PARAM_INDEX_ANALYZER, annotation.analyzer());
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

					if (StringUtils.hasText(context.path())) {
						builder.field(FIELD_CONTEXT_PATH, context.path());
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

		builder.startObject(property.getFieldName()).field(FIELD_PARAM_TYPE, TYPE_VALUE_KEYWORD).field(FIELD_INDEX, true)
				.endObject();
	}

	/**
	 * Add mapping for @Field annotation
	 *
	 * @throws IOException
	 */
	private void addSingleFieldMapping(XContentBuilder builder, ElasticsearchPersistentProperty property,
			Field annotation, boolean nestedOrObjectField) throws IOException {

		// build the property json, if empty skip it as this is no valid mapping
		XContentBuilder propertyBuilder = jsonBuilder().startObject();
		addFieldMappingParameters(propertyBuilder, annotation, nestedOrObjectField);
		propertyBuilder.endObject().close();

		if ("{}".equals(propertyBuilder.getOutputStream().toString())) {
			return;
		}

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

	private void addFieldMappingParameters(XContentBuilder builder, Annotation annotation, boolean nestedOrObjectField)
			throws IOException {

		MappingParameters mappingParameters = MappingParameters.from(annotation);

		if (!nestedOrObjectField && mappingParameters.isStore()) {
			builder.field(FIELD_PARAM_STORE, mappingParameters.isStore());
		}
		mappingParameters.writeTypeAndParametersTo(builder);
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
