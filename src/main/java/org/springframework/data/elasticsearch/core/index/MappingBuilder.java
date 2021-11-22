/*
 * Copyright 2014-2022 the original author or authors.
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

import static org.springframework.data.elasticsearch.core.index.MappingParameters.*;
import static org.springframework.util.StringUtils.*;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.annotation.Transient;
import org.springframework.data.elasticsearch.annotations.*;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.ResourceUtil;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchTypeMapper;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentProperty;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.util.RawValue;

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
 * @author Subhobrata Dey
 */
public class MappingBuilder {

	private static final Log LOGGER = LogFactory.getLog(ElasticsearchRestTemplate.class);

	private static final String FIELD_INDEX = "index";
	private static final String FIELD_PROPERTIES = "properties";
	@Deprecated private static final String FIELD_PARENT = "_parent";
	private static final String FIELD_CONTEXT_NAME = "name";
	private static final String FIELD_CONTEXT_TYPE = "type";
	private static final String FIELD_CONTEXT_PATH = "path";
	private static final String FIELD_CONTEXT_PRECISION = "precision";
	private static final String FIELD_DYNAMIC_TEMPLATES = "dynamic_templates";
	private static final String FIELD_INCLUDE_IN_PARENT = "include_in_parent";

	private static final String COMPLETION_PRESERVE_SEPARATORS = "preserve_separators";
	private static final String COMPLETION_PRESERVE_POSITION_INCREMENTS = "preserve_position_increments";
	private static final String COMPLETION_MAX_INPUT_LENGTH = "max_input_length";
	private static final String COMPLETION_CONTEXTS = "contexts";

	private static final String TYPEHINT_PROPERTY = ElasticsearchTypeMapper.DEFAULT_TYPE_KEY;

	private static final String TYPE_DYNAMIC = "dynamic";
	private static final String TYPE_VALUE_KEYWORD = "keyword";
	private static final String TYPE_VALUE_GEO_POINT = "geo_point";
	private static final String TYPE_VALUE_GEO_SHAPE = "geo_shape";
	private static final String TYPE_VALUE_JOIN = "join";
	private static final String TYPE_VALUE_COMPLETION = "completion";

	private static final String JOIN_TYPE_RELATIONS = "relations";

	private static final String MAPPING_ENABLED = "enabled";
	private static final String DATE_DETECTION = "date_detection";
	private static final String NUMERIC_DETECTION = "numeric_detection";
	private static final String DYNAMIC_DATE_FORMATS = "dynamic_date_formats";
	private static final String RUNTIME = "runtime";
	private static final String SOURCE = "_source";
	private static final String SOURCE_EXCLUDES = "excludes";

	protected final ElasticsearchConverter elasticsearchConverter;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public MappingBuilder(ElasticsearchConverter elasticsearchConverter) {
		this.elasticsearchConverter = elasticsearchConverter;
	}

	/**
	 * builds the Elasticsearch mapping for the given clazz.
	 *
	 * @return JSON string
	 * @throws MappingException on errors while building the mapping
	 */
	public String buildPropertyMapping(Class<?> clazz) throws MappingException {

		ElasticsearchPersistentEntity<?> entity = elasticsearchConverter.getMappingContext()
				.getRequiredPersistentEntity(clazz);

		return buildPropertyMapping(entity, getRuntimeFields(entity));
	}

	protected String buildPropertyMapping(ElasticsearchPersistentEntity<?> entity,
			@Nullable org.springframework.data.elasticsearch.core.document.Document runtimeFields) {

		InternalBuilder internalBuilder = new InternalBuilder();
		return internalBuilder.buildPropertyMapping(entity, runtimeFields);
	}

	@Nullable
	private org.springframework.data.elasticsearch.core.document.Document getRuntimeFields(
			@Nullable ElasticsearchPersistentEntity<?> entity) {

		if (entity != null) {
			Mapping mappingAnnotation = entity.findAnnotation(Mapping.class);
			if (mappingAnnotation != null) {
				String runtimeFieldsPath = mappingAnnotation.runtimeFieldsPath();

				if (hasText(runtimeFieldsPath)) {
					String jsonString = ResourceUtil.readFileFromClasspath(runtimeFieldsPath);
					return org.springframework.data.elasticsearch.core.document.Document.parse(jsonString);
				}
			}
		}
		return null;
	}

	private class InternalBuilder {

		private boolean writeTypeHints = true;
		private List<String> excludeFromSource = new ArrayList<>();
		private String nestedPropertyPrefix = "";

		protected String buildPropertyMapping(ElasticsearchPersistentEntity<?> entity,
				@Nullable org.springframework.data.elasticsearch.core.document.Document runtimeFields) {

			try {

				writeTypeHints = entity.writeTypeHints();

				ObjectNode objectNode = objectMapper.createObjectNode();

				// Dynamic templates
				addDynamicTemplatesMapping(objectNode, entity);

				mapEntity(objectNode, entity, true, "", false, FieldType.Auto, null,
						entity.findAnnotation(DynamicMapping.class), runtimeFields);

				if (!excludeFromSource.isEmpty()) {
					ObjectNode sourceNode = objectNode.putObject(SOURCE);
					ArrayNode excludes = sourceNode.putArray(SOURCE_EXCLUDES);
					excludeFromSource.stream().map(TextNode::new).forEach(excludes::add);
				}

				return objectMapper.writer().writeValueAsString(objectNode);
			} catch (IOException e) {
				throw new MappingException("could not build mapping", e);
			}
		}

		private void writeTypeHintMapping(ObjectNode propertiesNode) throws IOException {

			if (writeTypeHints) {
				String typeHintProperty = null;

				if (elasticsearchConverter instanceof MappingElasticsearchConverter) {
					typeHintProperty = ((MappingElasticsearchConverter) elasticsearchConverter).getTypeMapper().getTypeKey();
				}

				if (typeHintProperty == null) {
					typeHintProperty = TYPEHINT_PROPERTY;
				}

				propertiesNode.set(typeHintProperty, objectMapper.createObjectNode() //
						.put(FIELD_PARAM_TYPE, TYPE_VALUE_KEYWORD) //
						.put(FIELD_PARAM_INDEX, false) //
						.put(FIELD_PARAM_DOC_VALUES, false));
			}
		}

		private void mapEntity(ObjectNode objectNode, @Nullable ElasticsearchPersistentEntity<?> entity,
				boolean isRootObject, String nestedObjectFieldName, boolean nestedOrObjectField, FieldType fieldType,
				@Nullable Field parentFieldAnnotation, @Nullable DynamicMapping dynamicMapping,
				@Nullable Document runtimeFields) throws IOException {

			if (entity != null && entity.isAnnotationPresent(Mapping.class)) {
				Mapping mappingAnnotation = entity.getRequiredAnnotation(Mapping.class);

				if (!mappingAnnotation.enabled()) {
					objectNode.put(MAPPING_ENABLED, false);
					return;
				}

				if (mappingAnnotation.dateDetection() != Mapping.Detection.DEFAULT) {
					objectNode.put(DATE_DETECTION, Boolean.parseBoolean(mappingAnnotation.dateDetection().name()));
				}

				if (mappingAnnotation.numericDetection() != Mapping.Detection.DEFAULT) {
					objectNode.put(NUMERIC_DETECTION, Boolean.parseBoolean(mappingAnnotation.numericDetection().name()));
				}

				if (mappingAnnotation.dynamicDateFormats().length > 0) {
					objectNode.putArray(DYNAMIC_DATE_FORMATS).addAll(Arrays.stream(mappingAnnotation.dynamicDateFormats())
							.map(TextNode::valueOf).collect(Collectors.toList()));
				}

				if (runtimeFields != null) {
					objectNode.set(RUNTIME, objectMapper.convertValue(runtimeFields, JsonNode.class));
				}
			}

			boolean writeNestedProperties = !isRootObject && (isAnyPropertyAnnotatedWithField(entity) || nestedOrObjectField);
			if (writeNestedProperties) {

				String type = nestedOrObjectField ? fieldType.getMappedName() : FieldType.Object.getMappedName();

				ObjectNode nestedObjectNode = objectMapper.createObjectNode();
				nestedObjectNode.put(FIELD_PARAM_TYPE, type);

				if (nestedOrObjectField && FieldType.Nested == fieldType && parentFieldAnnotation != null
						&& parentFieldAnnotation.includeInParent()) {
					nestedObjectNode.put(FIELD_INCLUDE_IN_PARENT, true);
				}

				objectNode.set(nestedObjectFieldName, nestedObjectNode);
				// now go on with the nested one
				objectNode = nestedObjectNode;
			}

			if (entity != null && entity.dynamic() != Dynamic.INHERIT) {
				objectNode.put(TYPE_DYNAMIC, entity.dynamic().getMappedName());
			} else if (dynamicMapping != null) {
				objectNode.put(TYPE_DYNAMIC, dynamicMapping.value().getMappedName());
			}

			ObjectNode propertiesNode = objectNode.putObject(FIELD_PROPERTIES);

			writeTypeHintMapping(propertiesNode);

			if (entity != null) {
				entity.doWithProperties((PropertyHandler<ElasticsearchPersistentProperty>) property -> {
					try {
						if (property.isAnnotationPresent(Transient.class) || isInIgnoreFields(property, parentFieldAnnotation)) {
							return;
						}

						if (property.isSeqNoPrimaryTermProperty()) {
							if (property.isAnnotationPresent(Field.class)) {
								LOGGER.warn(String.format("Property %s of %s is annotated for inclusion in mapping, but its type is " + //
								"SeqNoPrimaryTerm that is never mapped, so it is skipped", //
										property.getFieldName(), entity.getType()));
							}
							return;
						}

						buildPropertyMapping(propertiesNode, isRootObject, property);
					} catch (IOException e) {
						LOGGER.warn(String.format("error mapping property with name %s", property.getName()), e);
					}
				});
			}
		}

		private void buildPropertyMapping(ObjectNode propertiesNode, boolean isRootObject,
				ElasticsearchPersistentProperty property) throws IOException {

			if (property.isAnnotationPresent(Mapping.class)) {

				Mapping mapping = property.getRequiredAnnotation(Mapping.class);

				if (mapping.enabled()) {
					String mappingPath = mapping.mappingPath();

					if (StringUtils.hasText(mappingPath)) {

						ClassPathResource mappings = new ClassPathResource(mappingPath);
						if (mappings.exists()) {
							propertiesNode.putRawValue(property.getFieldName(),
									new RawValue(StreamUtils.copyToString(mappings.getInputStream(), Charset.defaultCharset())));
							return;
						}
					}
				} else {
					applyDisabledPropertyMapping(propertiesNode, property);
					return;
				}
			}

			if (property.isGeoPointProperty()) {
				applyGeoPointFieldMapping(propertiesNode, property);
				return;
			}

			if (property.isGeoShapeProperty()) {
				applyGeoShapeMapping(propertiesNode, property);
			}

			if (property.isJoinFieldProperty()) {
				addJoinFieldMapping(propertiesNode, property);
			}

			String nestedPropertyPath = nestedPropertyPrefix.isEmpty() ? property.getFieldName()
					: nestedPropertyPrefix + '.' + property.getFieldName();

			Field fieldAnnotation = property.findAnnotation(Field.class);

			if (fieldAnnotation != null && fieldAnnotation.excludeFromSource()) {
				excludeFromSource.add(nestedPropertyPath);
			}

			boolean isCompletionProperty = property.isCompletionProperty();
			boolean isNestedOrObjectProperty = isNestedOrObjectProperty(property);
			DynamicMapping dynamicMapping = property.findAnnotation(DynamicMapping.class);

			if (!isCompletionProperty && property.isEntity() && hasRelevantAnnotation(property)) {

				if (fieldAnnotation == null) {
					return;
				}

				if (isNestedOrObjectProperty) {
					Iterator<? extends TypeInformation<?>> iterator = property.getPersistentEntityTypeInformation().iterator();
					ElasticsearchPersistentEntity<?> persistentEntity = iterator.hasNext()
							? elasticsearchConverter.getMappingContext().getPersistentEntity(iterator.next())
							: null;

					String currentNestedPropertyPrefix = nestedPropertyPrefix;
					nestedPropertyPrefix = nestedPropertyPath;

					mapEntity(propertiesNode, persistentEntity, false, property.getFieldName(), true, fieldAnnotation.type(),
							fieldAnnotation, dynamicMapping, null);

					nestedPropertyPrefix = currentNestedPropertyPrefix;
					return;
				}
			}

			MultiField multiField = property.findAnnotation(MultiField.class);

			if (isCompletionProperty) {
				CompletionField completionField = property.findAnnotation(CompletionField.class);
				applyCompletionFieldMapping(propertiesNode, property, completionField);
			}

			if (isRootObject && fieldAnnotation != null && property.isIdProperty()) {
				applyDefaultIdFieldMapping(propertiesNode, property);
			} else if (multiField != null) {
				addMultiFieldMapping(propertiesNode, property, multiField, isNestedOrObjectProperty, dynamicMapping);
			} else if (fieldAnnotation != null) {
				addSingleFieldMapping(propertiesNode, property, fieldAnnotation, isNestedOrObjectProperty, dynamicMapping);
			}
		}

		private boolean hasRelevantAnnotation(ElasticsearchPersistentProperty property) {

			return property.findAnnotation(Field.class) != null || property.findAnnotation(MultiField.class) != null
					|| property.findAnnotation(GeoPointField.class) != null
					|| property.findAnnotation(CompletionField.class) != null;
		}

		private void applyGeoPointFieldMapping(ObjectNode propertiesNode, ElasticsearchPersistentProperty property)
				throws IOException {
			propertiesNode.set(property.getFieldName(),
					objectMapper.createObjectNode().put(FIELD_PARAM_TYPE, TYPE_VALUE_GEO_POINT));
		}

		private void applyGeoShapeMapping(ObjectNode propertiesNode, ElasticsearchPersistentProperty property)
				throws IOException {

			ObjectNode shapeNode = propertiesNode.putObject(property.getFieldName());
			GeoShapeMappingParameters mappingParameters = GeoShapeMappingParameters
					.from(property.findAnnotation(GeoShapeField.class));
			mappingParameters.writeTypeAndParametersTo(shapeNode);
		}

		private void applyCompletionFieldMapping(ObjectNode propertyNode, ElasticsearchPersistentProperty property,
				@Nullable CompletionField annotation) throws IOException {

			ObjectNode completionNode = propertyNode.putObject(property.getFieldName());
			completionNode.put(FIELD_PARAM_TYPE, TYPE_VALUE_COMPLETION);

			if (annotation != null) {
				completionNode.put(COMPLETION_MAX_INPUT_LENGTH, annotation.maxInputLength());
				completionNode.put(COMPLETION_PRESERVE_POSITION_INCREMENTS, annotation.preservePositionIncrements());
				completionNode.put(COMPLETION_PRESERVE_SEPARATORS, annotation.preserveSeparators());

				if (StringUtils.hasLength(annotation.searchAnalyzer())) {
					completionNode.put(FIELD_PARAM_SEARCH_ANALYZER, annotation.searchAnalyzer());
				}

				if (StringUtils.hasLength(annotation.analyzer())) {
					completionNode.put(FIELD_PARAM_INDEX_ANALYZER, annotation.analyzer());
				}

				if (annotation.contexts().length > 0) {

					ArrayNode contextsNode = completionNode.putArray(COMPLETION_CONTEXTS);
					for (CompletionContext context : annotation.contexts()) {

						ObjectNode contextNode = contextsNode.addObject();
						contextNode.put(FIELD_CONTEXT_NAME, context.name());
						contextNode.put(FIELD_CONTEXT_TYPE, context.type().getMappedName());

						if (context.precision().length() > 0) {
							contextNode.put(FIELD_CONTEXT_PRECISION, context.precision());
						}

						if (StringUtils.hasText(context.path())) {
							contextNode.put(FIELD_CONTEXT_PATH, context.path());
						}
					}
				}
			}
		}

		private void applyDefaultIdFieldMapping(ObjectNode propertyNode, ElasticsearchPersistentProperty property)
				throws IOException {
			propertyNode.set(property.getFieldName(), objectMapper.createObjectNode()//
					.put(FIELD_PARAM_TYPE, TYPE_VALUE_KEYWORD) //
					.put(FIELD_INDEX, true) //
			);
		}

		private void applyDisabledPropertyMapping(ObjectNode propertiesNode, ElasticsearchPersistentProperty property) {

			try {
				Field field = property.getRequiredAnnotation(Field.class);

				if (field.type() != FieldType.Object) {
					throw new IllegalArgumentException("Field type must be 'object");
				}

				propertiesNode.set(property.getFieldName(), objectMapper.createObjectNode() //
						.put(FIELD_PARAM_TYPE, field.type().getMappedName()) //
						.put(MAPPING_ENABLED, false) //
				);

			} catch (Exception e) {
				throw new MappingException("Could not write enabled: false mapping for " + property.getFieldName(), e);
			}
		}

		/**
		 * Add mapping for @Field annotation
		 *
		 * @throws IOException
		 */
		private void addSingleFieldMapping(ObjectNode propertiesNode, ElasticsearchPersistentProperty property,
				Field annotation, boolean nestedOrObjectField, @Nullable DynamicMapping dynamicMapping) throws IOException {

			// build the property json, if empty skip it as this is no valid mapping
			ObjectNode fieldNode = objectMapper.createObjectNode();
			addFieldMappingParameters(fieldNode, annotation, nestedOrObjectField);

			if (fieldNode.isEmpty()) {
				return;
			}

			propertiesNode.set(property.getFieldName(), fieldNode);

			if (nestedOrObjectField) {
				if (annotation.dynamic() != Dynamic.INHERIT) {
					fieldNode.put(TYPE_DYNAMIC, annotation.dynamic().getMappedName());
				} else if (dynamicMapping != null) {
					fieldNode.put(TYPE_DYNAMIC, dynamicMapping.value().getMappedName());
				}
			}
		}

		private void addJoinFieldMapping(ObjectNode propertiesNode, ElasticsearchPersistentProperty property)
				throws IOException {
			JoinTypeRelation[] joinTypeRelations = property.getRequiredAnnotation(JoinTypeRelations.class).relations();

			if (joinTypeRelations.length == 0) {
				LOGGER.warn(String.format("Property %s's type is JoinField but its annotation JoinTypeRelation is " + //
						"not properly maintained", //
						property.getFieldName()));
				return;
			}

			ObjectNode propertyNode = propertiesNode.putObject(property.getFieldName());
			propertyNode.put(FIELD_PARAM_TYPE, TYPE_VALUE_JOIN);

			ObjectNode relationsNode = propertyNode.putObject(JOIN_TYPE_RELATIONS);

			for (JoinTypeRelation joinTypeRelation : joinTypeRelations) {
				String parent = joinTypeRelation.parent();
				String[] children = joinTypeRelation.children();

				if (children.length > 1) {
					relationsNode.putArray(parent)
							.addAll(Arrays.stream(children).map(TextNode::valueOf).collect(Collectors.toList()));
				} else if (children.length == 1) {
					relationsNode.put(parent, children[0]);
				}
			}
		}

		/**
		 * Add mapping for @MultiField annotation
		 *
		 * @throws IOException
		 */
		private void addMultiFieldMapping(ObjectNode propertyNode, ElasticsearchPersistentProperty property,
				MultiField annotation, boolean nestedOrObjectField, @Nullable DynamicMapping dynamicMapping)
				throws IOException {

			// main field
			ObjectNode mainFieldNode = objectMapper.createObjectNode();
			propertyNode.set(property.getFieldName(), mainFieldNode);

			if (nestedOrObjectField) {
				if (annotation.mainField().dynamic() != Dynamic.INHERIT) {
					mainFieldNode.put(TYPE_DYNAMIC, annotation.mainField().dynamic().getMappedName());
				} else if (dynamicMapping != null) {
					mainFieldNode.put(TYPE_DYNAMIC, dynamicMapping.value().getMappedName());
				}
			}

			addFieldMappingParameters(mainFieldNode, annotation.mainField(), nestedOrObjectField);

			// inner fields
			ObjectNode innerFieldsNode = mainFieldNode.putObject("fields");

			for (InnerField innerField : annotation.otherFields()) {

				ObjectNode innerFieldNode = innerFieldsNode.putObject(innerField.suffix());
				addFieldMappingParameters(innerFieldNode, innerField, false);

			}
		}

		private void addFieldMappingParameters(ObjectNode fieldNode, Annotation annotation, boolean nestedOrObjectField)
				throws IOException {

			MappingParameters mappingParameters = MappingParameters.from(annotation);

			if (!nestedOrObjectField && mappingParameters.isStore()) {
				fieldNode.put(FIELD_PARAM_STORE, true);
			}
			mappingParameters.writeTypeAndParametersTo(fieldNode);
		}

		/**
		 * Apply mapping for dynamic templates.
		 *
		 * @throws IOException
		 */
		private void addDynamicTemplatesMapping(ObjectNode objectNode, ElasticsearchPersistentEntity<?> entity)
				throws IOException {

			if (entity.isAnnotationPresent(DynamicTemplates.class)) {
				String mappingPath = entity.getRequiredAnnotation(DynamicTemplates.class).mappingPath();
				if (hasText(mappingPath)) {

					String jsonString = ResourceUtil.readFileFromClasspath(mappingPath);
					if (hasText(jsonString)) {

						JsonNode jsonNode = objectMapper.readTree(jsonString).get("dynamic_templates");
						if (jsonNode != null && jsonNode.isArray()) {
							objectNode.set(FIELD_DYNAMIC_TEMPLATES, jsonNode);
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
	}
}
