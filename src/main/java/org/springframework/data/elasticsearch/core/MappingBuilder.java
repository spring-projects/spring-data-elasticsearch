/*
 * Copyright 2014-2019 the original author or authors.
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
import org.springframework.util.StringUtils;

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
 * @author Sascha Woo
 * @author Nordine Bittich
 */
class MappingBuilder {

	public static final String FIELD_DATA = "fielddata";
	public static final String FIELD_STORE = "store";
	public static final String FIELD_TYPE = "type";
	public static final String FIELD_INDEX = "index";
	public static final String FIELD_FORMAT = "format";
	public static final String FIELD_SEARCH_ANALYZER = "search_analyzer";
	public static final String FIELD_INDEX_ANALYZER = "analyzer";
	public static final String FIELD_NORMALIZER = "normalizer";
	public static final String FIELD_PROPERTIES = "properties";
	public static final String FIELD_PARENT = "_parent";
	public static final String FIELD_COPY_TO = "copy_to";

	public static final String COMPLETION_PRESERVE_SEPARATORS = "preserve_separators";
	public static final String COMPLETION_PRESERVE_POSITION_INCREMENTS = "preserve_position_increments";
	public static final String COMPLETION_MAX_INPUT_LENGTH = "max_input_length";

	public static final String TYPE_VALUE_KEYWORD = "keyword";
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

		mapEntity(xContentBuilder, clazz, true, idFieldName, "", false, FieldType.Auto, null);

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

			if (field.isAnnotationPresent(Transient.class) || isInIgnoreFields(field, fieldAnnotation)) {
				continue;
			}

			if (field.isAnnotationPresent(Mapping.class)) {
				String mappingPath = field.getAnnotation(Mapping.class).mappingPath();
				if (!StringUtils.isEmpty(mappingPath)) {
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
				mapEntity(xContentBuilder, getFieldType(field), false, "", field.getName(), nestedOrObject, singleField.type(), field.getAnnotation(Field.class));
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
			if (!StringUtils.isEmpty(annotation.searchAnalyzer())) {
				xContentBuilder.field(FIELD_SEARCH_ANALYZER, annotation.searchAnalyzer());
			}
			if (!StringUtils.isEmpty(annotation.analyzer())) {
				xContentBuilder.field(FIELD_INDEX_ANALYZER, annotation.analyzer());
			}
		}
		xContentBuilder.endObject();
	}

	private static void applyDefaultIdFieldMapping(XContentBuilder xContentBuilder, java.lang.reflect.Field field)
			throws IOException {
		xContentBuilder.startObject(field.getName())
				.field(FIELD_TYPE, TYPE_VALUE_KEYWORD)
				.field(FIELD_INDEX, true);
		xContentBuilder.endObject();
	}

	/**
	 * Add mapping for @Field annotation
	 *
	 * @throws IOException
	 */
	private static void addSingleFieldMapping(XContentBuilder builder, java.lang.reflect.Field field, Field annotation, boolean nestedOrObjectField) throws IOException {
		builder.startObject(field.getName());
		addFieldMappingParameters(builder, annotation, nestedOrObjectField);
		builder.endObject();
	}

	/**
	 * Add mapping for @MultiField annotation
	 *
	 * @throws IOException
	 */
	private static void addMultiFieldMapping(
		XContentBuilder builder,
		java.lang.reflect.Field field,
		MultiField annotation,
		boolean nestedOrObjectField) throws IOException {

		// main field
		builder.startObject(field.getName());
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

	private static void addFieldMappingParameters(XContentBuilder builder, Object annotation, boolean nestedOrObjectField) throws IOException {
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
