/*
 * Copyright 2014-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.elasticsearch.core;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.annotation.Transient;
import org.springframework.data.elasticsearch.annotations.CompletionField;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.GeoPointField;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.Mapping;
import org.springframework.data.elasticsearch.annotations.MultiField;
import org.springframework.data.elasticsearch.core.completion.Completion;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import static org.apache.commons.lang.StringUtils.*;
import static org.elasticsearch.common.xcontent.XContentFactory.*;
import static org.springframework.util.StringUtils.*;

/**
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Artur Konczak
 * @author Kevin Leturc
 * @author Alexander Volz
 * @author Dennis Maaß
 * @author Pavel Luhin
 * @author Mark Paluch
 */
class MappingBuilder {

	public static final String FIELD_DATA = "fielddata";
	public static final String FIELD_STORE = "store";
	public static final String FIELD_TYPE = "type";
	public static final String FIELD_INDEX = "index";
	public static final String FIELD_FORMAT = "format";
	public static final String FIELD_SEARCH_ANALYZER = "search_analyzer";
	public static final String FIELD_INDEX_ANALYZER = "analyzer";
	public static final String FIELD_PROPERTIES = "properties";
	public static final String FIELD_PARENT = "_parent";

	public static final String COMPLETION_PRESERVE_SEPARATORS = "preserve_separators";
	public static final String COMPLETION_PRESERVE_POSITION_INCREMENTS = "preserve_position_increments";
	public static final String COMPLETION_MAX_INPUT_LENGTH = "max_input_length";

	public static final String INDEX_VALUE_NOT_ANALYZED = "not_analyzed";
	public static final String TYPE_VALUE_STRING = "text";
	public static final String TYPE_VALUE_GEO_POINT = "geo_point";
	public static final String TYPE_VALUE_COMPLETION = "completion";
	public static final String TYPE_VALUE_GEO_HASH_PREFIX = "geohash_prefix";
	public static final String TYPE_VALUE_GEO_HASH_PRECISION = "geohash_precision";

	private static SimpleTypeHolder SIMPLE_TYPE_HOLDER = SimpleTypeHolder.DEFAULT;

	static XContentBuilder buildMapping(Class clazz, String indexType, String idFieldName, String parentType) throws IOException {

		XContentBuilder mapping = jsonBuilder().startObject().startObject(indexType);
		// Parent
		if (hasText(parentType)) {
			mapping.startObject(FIELD_PARENT).field(FIELD_TYPE, parentType).endObject();
		}

		// Properties
		XContentBuilder xContentBuilder = mapping.startObject(FIELD_PROPERTIES);

		mapEntity(xContentBuilder, clazz, true, idFieldName, EMPTY, false, FieldType.Auto, null);

		return xContentBuilder.endObject().endObject().endObject();
	}

	private static void mapEntity(XContentBuilder xContentBuilder, Class clazz, boolean isRootObject, String idFieldName,
								  String nestedObjectFieldName, boolean nestedOrObjectField, FieldType fieldType, Field fieldAnnotation) throws IOException {

		java.lang.reflect.Field[] fields = retrieveFields(clazz);

		if (!isRootObject && (isAnyPropertyAnnotatedAsField(fields) || nestedOrObjectField)) {
			String type = FieldType.Object.toString().toLowerCase();
			if (nestedOrObjectField) {
				type = fieldType.toString().toLowerCase();
			}
			XContentBuilder t = xContentBuilder.startObject(nestedObjectFieldName).field(FIELD_TYPE, type);

			if (nestedOrObjectField && FieldType.Nested == fieldType && fieldAnnotation.includeInParent()) {
				t.field("include_in_parent", fieldAnnotation.includeInParent());
			}
			t.startObject(FIELD_PROPERTIES);
		}

		for (java.lang.reflect.Field field : fields) {

			if (field.isAnnotationPresent(Transient.class) || isInIgnoreFields(field, fieldAnnotation) || Modifier.isStatic(field.getModifiers()) || Modifier.isProtected(field.getModifiers())) {
				continue;
			}

			if (field.isAnnotationPresent(Mapping.class)) {
				String mappingPath = field.getAnnotation(Mapping.class).mappingPath();
				if (isNotBlank(mappingPath)) {
					ClassPathResource mappings = new ClassPathResource(mappingPath);
					if (mappings.exists()) {
						xContentBuilder.rawField(field.getName(), mappings.getInputStream());
						continue;
					}
				}
			}

			boolean isGeoPointField = isGeoPointField(field);
			boolean isCompletionField = isCompletionField(field);

			Field singleField = field.getAnnotation(Field.class);
			if (!isGeoPointField && !isCompletionField && isEntity(field) && isAnnotated(field)) {
				if (singleField == null) {
					continue;
				}
				boolean nestedOrObject = isNestedOrObjectField(field);
				mapEntity(xContentBuilder, getFieldType(field), false, EMPTY, field.getName(), nestedOrObject, singleField.type(), field.getAnnotation(Field.class));
				if (nestedOrObject) {
					continue;
				}
			}

			MultiField multiField = field.getAnnotation(MultiField.class);

			if (isGeoPointField) {
				applyGeoPointFieldMapping(xContentBuilder, field);
			}

			if (isCompletionField) {
				CompletionField completionField = field.getAnnotation(CompletionField.class);
				applyCompletionFieldMapping(xContentBuilder, field, completionField);
			}

			if (isRootObject && singleField != null && isIdField(field, idFieldName)) {
				applyDefaultIdFieldMapping(xContentBuilder, field);
			} else if (multiField != null) {
				addMultiFieldMapping(xContentBuilder, field, multiField, isNestedOrObjectField(field));
			} else if (singleField != null) {
				addSingleFieldMapping(xContentBuilder, field, singleField, isNestedOrObjectField(field));
			}
		}

		if (!isRootObject && isAnyPropertyAnnotatedAsField(fields) || nestedOrObjectField) {
			xContentBuilder.endObject().endObject();
		}
	}

	private static java.lang.reflect.Field[] retrieveFields(Class clazz) {
		// Create list of fields.
		List<java.lang.reflect.Field> fields = new ArrayList<>();

		// Keep backing up the inheritance hierarchy.
		Class targetClass = clazz;
		do {
			fields.addAll(Arrays.asList(targetClass.getDeclaredFields()));
			targetClass = targetClass.getSuperclass();
		}
		while (targetClass != null && targetClass != Object.class);

		return fields.toArray(new java.lang.reflect.Field[fields.size()]);
	}

	private static boolean isAnnotated(java.lang.reflect.Field field) {
		return field.getAnnotation(Field.class) != null ||
				field.getAnnotation(MultiField.class) != null ||
				field.getAnnotation(GeoPointField.class) != null ||
				field.getAnnotation(CompletionField.class) != null;
	}

	private static void applyGeoPointFieldMapping(XContentBuilder xContentBuilder, java.lang.reflect.Field field) throws IOException {
		xContentBuilder.startObject(field.getName());
		xContentBuilder.field(FIELD_TYPE, TYPE_VALUE_GEO_POINT);
		xContentBuilder.endObject();
	}

	private static void applyCompletionFieldMapping(XContentBuilder xContentBuilder, java.lang.reflect.Field field, CompletionField annotation) throws IOException {
		xContentBuilder.startObject(field.getName());
		xContentBuilder.field(FIELD_TYPE, TYPE_VALUE_COMPLETION);
		if (annotation != null) {
			xContentBuilder.field(COMPLETION_MAX_INPUT_LENGTH, annotation.maxInputLength());
			xContentBuilder.field(COMPLETION_PRESERVE_POSITION_INCREMENTS, annotation.preservePositionIncrements());
			xContentBuilder.field(COMPLETION_PRESERVE_SEPARATORS, annotation.preserveSeparators());
			if (isNotBlank(annotation.searchAnalyzer())) {
				xContentBuilder.field(FIELD_SEARCH_ANALYZER, annotation.searchAnalyzer());
			}
			if (isNotBlank(annotation.analyzer())) {
				xContentBuilder.field(FIELD_INDEX_ANALYZER, annotation.analyzer());
			}
		}
		xContentBuilder.endObject();
	}

	private static void applyDefaultIdFieldMapping(XContentBuilder xContentBuilder, java.lang.reflect.Field field)
			throws IOException {
		xContentBuilder.startObject(field.getName())
				.field(FIELD_TYPE, TYPE_VALUE_STRING)
				.field(FIELD_INDEX, INDEX_VALUE_NOT_ANALYZED);
		xContentBuilder.endObject();
	}

	/**
	 * Apply mapping for a single @Field annotation
	 *
	 * @throws IOException
	 */
	private static void addSingleFieldMapping(XContentBuilder xContentBuilder, java.lang.reflect.Field field,
											  Field fieldAnnotation, boolean nestedOrObjectField) throws IOException {
		xContentBuilder.startObject(field.getName());
		if(!nestedOrObjectField) {
			xContentBuilder.field(FIELD_STORE, fieldAnnotation.store());
		}
		if(fieldAnnotation.fielddata()) {
			xContentBuilder.field(FIELD_DATA, fieldAnnotation.fielddata());
		}

		if (FieldType.Auto != fieldAnnotation.type()) {
			xContentBuilder.field(FIELD_TYPE, fieldAnnotation.type().name().toLowerCase());
			if (FieldType.Date == fieldAnnotation.type() && DateFormat.none != fieldAnnotation.format()) {
				xContentBuilder.field(FIELD_FORMAT, DateFormat.custom == fieldAnnotation.format()
						? fieldAnnotation.pattern() : fieldAnnotation.format());
			}
		}
		if(!fieldAnnotation.index()) {
			xContentBuilder.field(FIELD_INDEX, fieldAnnotation.index());
		}
		if (isNotBlank(fieldAnnotation.searchAnalyzer())) {
			xContentBuilder.field(FIELD_SEARCH_ANALYZER, fieldAnnotation.searchAnalyzer());
		}
		if (isNotBlank(fieldAnnotation.analyzer())) {
			xContentBuilder.field(FIELD_INDEX_ANALYZER, fieldAnnotation.analyzer());
		}
		xContentBuilder.endObject();
	}

	/**
	 * Apply mapping for a single nested @Field annotation
	 *
	 * @throws IOException
	 */
	private static void addNestedFieldMapping(XContentBuilder builder, java.lang.reflect.Field field,
											  InnerField annotation) throws IOException {
		builder.startObject(annotation.suffix());
		//builder.field(FIELD_STORE, annotation.store());
		if (FieldType.Auto != annotation.type()) {
			builder.field(FIELD_TYPE, annotation.type().name().toLowerCase());
		}
		if(!annotation.index()) {
			builder.field(FIELD_INDEX, annotation.index());
		}
		if (isNotBlank(annotation.searchAnalyzer())) {
			builder.field(FIELD_SEARCH_ANALYZER, annotation.searchAnalyzer());
		}
		if (isNotBlank(annotation.indexAnalyzer())) {
			builder.field(FIELD_INDEX_ANALYZER, annotation.indexAnalyzer());
		}
		if (annotation.fielddata()) {
			builder.field(FIELD_DATA, annotation.fielddata());
		}
		builder.endObject();
	}

	/**
	 * Multi field mappings for string type fields, support for sorts and facets
	 *
	 * @throws IOException
	 */
	private static void addMultiFieldMapping(XContentBuilder builder, java.lang.reflect.Field field,
											 MultiField annotation, boolean nestedOrObjectField) throws IOException {
		builder.startObject(field.getName());
		builder.field(FIELD_TYPE, annotation.mainField().type());
		builder.startObject("fields");
		//add standard field
		//addSingleFieldMapping(builder, field, annotation.mainField(), nestedOrObjectField);
		for (InnerField innerField : annotation.otherFields()) {
			addNestedFieldMapping(builder, field, innerField);
		}
		builder.endObject();
		builder.endObject();
	}

	protected static boolean isEntity(java.lang.reflect.Field field) {
		TypeInformation typeInformation = ClassTypeInformation.from(field.getType());
		Class<?> clazz = getFieldType(field);
		boolean isComplexType = !SIMPLE_TYPE_HOLDER.isSimpleType(clazz);
		return isComplexType && !Map.class.isAssignableFrom(typeInformation.getType());
	}

	protected static Class<?> getFieldType(java.lang.reflect.Field field) {

		ResolvableType resolvableType = ResolvableType.forField(field);

		if (resolvableType.isArray()) {
			return resolvableType.getComponentType().getRawClass();
		}

		ResolvableType componentType = resolvableType.getGeneric(0);
		if (Iterable.class.isAssignableFrom(field.getType())
				&& componentType != ResolvableType.NONE) {
			return componentType.getRawClass();
		}

		return resolvableType.getRawClass();
	}

	private static boolean isAnyPropertyAnnotatedAsField(java.lang.reflect.Field[] fields) {
		if (fields != null) {
			for (java.lang.reflect.Field field : fields) {
				if (field.isAnnotationPresent(Field.class)) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean isIdField(java.lang.reflect.Field field, String idFieldName) {
		return idFieldName.equals(field.getName());
	}

	private static boolean isInIgnoreFields(java.lang.reflect.Field field, Field parentFieldAnnotation) {
		if (null != parentFieldAnnotation) {
			String[] ignoreFields = parentFieldAnnotation.ignoreFields();
			return Arrays.asList(ignoreFields).contains(field.getName());
		}
		return false;
	}

	private static boolean isNestedOrObjectField(java.lang.reflect.Field field) {
		Field fieldAnnotation = field.getAnnotation(Field.class);
		return fieldAnnotation != null && (FieldType.Nested == fieldAnnotation.type() || FieldType.Object == fieldAnnotation.type());
	}

	private static boolean isGeoPointField(java.lang.reflect.Field field) {
		return field.getType() == GeoPoint.class || field.getAnnotation(GeoPointField.class) != null;
	}

	private static boolean isCompletionField(java.lang.reflect.Field field) {
		return field.getType() == Completion.class;
	}
}
