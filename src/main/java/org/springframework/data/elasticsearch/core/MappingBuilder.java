/*
 * Copyright 2014-2016 the original author or authors.
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

import static org.apache.commons.lang.StringUtils.*;
import static org.elasticsearch.common.xcontent.XContentFactory.*;
import static org.springframework.util.StringUtils.*;

import java.beans.Introspector;
import java.io.IOException;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.springframework.core.GenericCollectionTypeResolver;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.annotation.Transient;
import org.springframework.data.elasticsearch.annotations.*;
import org.springframework.data.elasticsearch.core.completion.Completion;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;

/**
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Artur Konczak
 * @author Kevin Leturc
 * @author Alexander Volz
 * @author Dennis Maaß
 */

class MappingBuilder {

	public static final String FIELD_STORE = "store";
	public static final String FIELD_TYPE = "type";
	public static final String FIELD_INDEX = "index";
	public static final String FIELD_FORMAT = "format";
	public static final String FIELD_SEARCH_ANALYZER = "search_analyzer";
	public static final String FIELD_INDEX_ANALYZER = "analyzer";
	public static final String FIELD_PROPERTIES = "properties";
	public static final String FIELD_PARENT = "_parent";

	public static final String COMPLETION_PAYLOADS = "payloads";
	public static final String COMPLETION_PRESERVE_SEPARATORS = "preserve_separators";
	public static final String COMPLETION_PRESERVE_POSITION_INCREMENTS = "preserve_position_increments";
	public static final String COMPLETION_MAX_INPUT_LENGTH = "max_input_length";

	public static final String INDEX_VALUE_NOT_ANALYZED = "not_analyzed";
	public static final String TYPE_VALUE_STRING = "string";
	public static final String TYPE_VALUE_GEO_POINT = "geo_point";
	public static final String TYPE_VALUE_COMPLETION = "completion";
	public static final String TYPE_VALUE_GEO_HASH_PREFIX = "geohash_prefix";
	public static final String TYPE_VALUE_GEO_HASH_PRECISION = "geohash_precision";

	private static SimpleTypeHolder SIMPLE_TYPE_HOLDER = new SimpleTypeHolder();

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

		final List<AccessibleObject> members = new ArrayList<AccessibleObject>();

		final java.lang.reflect.Field[] fields = clazz.getDeclaredFields();
		members.addAll(Arrays.asList(fields));
		members.addAll(Arrays.asList(clazz.getDeclaredMethods()));

		if (!isRootObject && (isAnyPropertyAnnotatedAsField(members) || nestedOrObjectField)) {
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

		for (java.lang.reflect.AccessibleObject member : members) {

			if (member.isAnnotationPresent(Transient.class) || isInIgnoreFields(member)) {
				continue;
			}

			if (member.isAnnotationPresent(Mapping.class)) {
				String mappingPath = member.getAnnotation(Mapping.class).mappingPath();
				if (isNotBlank(mappingPath)) {
					ClassPathResource mappings = new ClassPathResource(mappingPath);
					if (mappings.exists()) {
						xContentBuilder.rawField(getMemberName(member), mappings.getInputStream());
						continue;
					}
				}
			}

			boolean isGeoPointField = isGeoPointField(member);
			boolean isCompletionField = isCompletionField(member);

			Field singleField = member.getAnnotation(Field.class);
			if (!isGeoPointField && !isCompletionField && isEntity(member) && isAnnotated(member)) {
				if (singleField == null) {
					continue;
				}
				boolean nestedOrObject = isNestedOrObjectField(member);
				mapEntity(xContentBuilder, getFieldType(member), false, EMPTY, getMemberName(member), nestedOrObject, singleField.type(), member.getAnnotation(Field.class));
				if (nestedOrObject) {
					continue;
				}
			}

			MultiField multiField = member.getAnnotation(MultiField.class);

			if (isGeoPointField) {
				applyGeoPointFieldMapping(xContentBuilder, member);
			}

			if (isCompletionField) {
				CompletionField completionField = member.getAnnotation(CompletionField.class);
				applyCompletionFieldMapping(xContentBuilder, member, completionField);
			}

			if (isRootObject && singleField != null && isIdField(member, idFieldName)) {
				applyDefaultIdFieldMapping(xContentBuilder, member);
			} else if (multiField != null) {
				addMultiFieldMapping(xContentBuilder, member, multiField, isNestedOrObjectField(member));
			} else if (singleField != null) {
				addSingleFieldMapping(xContentBuilder, member, singleField, isNestedOrObjectField(member));
			}
		}

		if (!isRootObject && isAnyPropertyAnnotatedAsField(members) || nestedOrObjectField) {
			xContentBuilder.endObject().endObject();
		}
	}

	private static java.lang.reflect.Field[] retrieveFields(Class clazz) {
		// Create list of fields.
		List<java.lang.reflect.Field> fields = new ArrayList<java.lang.reflect.Field>();

		// Keep backing up the inheritance hierarchy.
		Class targetClass = clazz;
		do {
			fields.addAll(Arrays.asList(targetClass.getDeclaredFields()));
			targetClass = targetClass.getSuperclass();
		}
		while (targetClass != null && targetClass != Object.class);

		return fields.toArray(new java.lang.reflect.Field[fields.size()]);
	}

	private static boolean isAnnotated(java.lang.reflect.AccessibleObject member) {
		return member.getAnnotation(Field.class) != null ||
				member.getAnnotation(MultiField.class) != null ||
				member.getAnnotation(GeoPointField.class) != null ||
				member.getAnnotation(CompletionField.class) != null;
	}

	private static void applyGeoPointFieldMapping(XContentBuilder xContentBuilder, java.lang.reflect.AccessibleObject member) throws IOException {
		xContentBuilder.startObject(getMemberName(member));
		xContentBuilder.field(FIELD_TYPE, TYPE_VALUE_GEO_POINT);

		GeoPointField annotation = member.getAnnotation(GeoPointField.class);
		if (annotation != null) {
			if (annotation.geoHashPrefix()) {
				xContentBuilder.field(TYPE_VALUE_GEO_HASH_PREFIX, true);
				if (StringUtils.isNotEmpty(annotation.geoHashPrecision())) {
					if (NumberUtils.isNumber(annotation.geoHashPrecision())) {
						xContentBuilder.field(TYPE_VALUE_GEO_HASH_PRECISION, Integer.parseInt(annotation.geoHashPrecision()));
					} else {
						xContentBuilder.field(TYPE_VALUE_GEO_HASH_PRECISION, annotation.geoHashPrecision());
					}
				}
			}
		}

		xContentBuilder.endObject();
	}

	private static void applyCompletionFieldMapping(XContentBuilder xContentBuilder, java.lang.reflect.AccessibleObject member, CompletionField annotation) throws IOException {
		xContentBuilder.startObject(getMemberName(member));
		xContentBuilder.field(FIELD_TYPE, TYPE_VALUE_COMPLETION);
		if (annotation != null) {
			xContentBuilder.field(COMPLETION_MAX_INPUT_LENGTH, annotation.maxInputLength());
			xContentBuilder.field(COMPLETION_PAYLOADS, annotation.payloads());
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

	private static void applyDefaultIdFieldMapping(XContentBuilder xContentBuilder, java.lang.reflect.AccessibleObject member)
			throws IOException {
		xContentBuilder.startObject(getMemberName(member))
				.field(FIELD_TYPE, TYPE_VALUE_STRING)
				.field(FIELD_INDEX, INDEX_VALUE_NOT_ANALYZED);
		xContentBuilder.endObject();
	}

	/**
	 * Apply mapping for a single @Field annotation
	 *
	 * @throws IOException
	 */
	private static void addSingleFieldMapping(XContentBuilder xContentBuilder, java.lang.reflect.AccessibleObject member,
											  Field fieldAnnotation, boolean nestedOrObjectField) throws IOException {
		xContentBuilder.startObject(getMemberName(member));
		if(!nestedOrObjectField) {
			xContentBuilder.field(FIELD_STORE, fieldAnnotation.store());
		}
		if (FieldType.Auto != fieldAnnotation.type()) {
			xContentBuilder.field(FIELD_TYPE, fieldAnnotation.type().name().toLowerCase());
			if (FieldType.Date == fieldAnnotation.type() && DateFormat.none != fieldAnnotation.format()) {
				xContentBuilder.field(FIELD_FORMAT, DateFormat.custom == fieldAnnotation.format()
						? fieldAnnotation.pattern() : fieldAnnotation.format());
			}
		}
		if (FieldIndex.not_analyzed == fieldAnnotation.index() || FieldIndex.no == fieldAnnotation.index()) {
			xContentBuilder.field(FIELD_INDEX, fieldAnnotation.index().name().toLowerCase());
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
	private static void addNestedFieldMapping(XContentBuilder builder, java.lang.reflect.AccessibleObject member,
											  InnerField annotation) throws IOException {
		builder.startObject(annotation.suffix());
		//builder.member(FIELD_STORE, annotation.store());
		if (FieldType.Auto != annotation.type()) {
			builder.field(FIELD_TYPE, annotation.type().name().toLowerCase());
		}
		if (FieldIndex.not_analyzed == annotation.index()) {
			builder.field(FIELD_INDEX, annotation.index().name().toLowerCase());
		}
		if (isNotBlank(annotation.searchAnalyzer())) {
			builder.field(FIELD_SEARCH_ANALYZER, annotation.searchAnalyzer());
		}
		if (isNotBlank(annotation.indexAnalyzer())) {
			builder.field(FIELD_INDEX_ANALYZER, annotation.indexAnalyzer());
		}
		builder.endObject();
	}

	/**
	 * Multi field mappings for string type fields, support for sorts and facets
	 *
	 * @throws IOException
	 */
	private static void addMultiFieldMapping(XContentBuilder builder, java.lang.reflect.AccessibleObject member,
											 MultiField annotation, boolean nestedOrObjectField) throws IOException {
		builder.startObject(getMemberName(member));
		builder.field(FIELD_TYPE, "multi_field");
		builder.startObject("fields");
		//add standard field
		addSingleFieldMapping(builder, member, annotation.mainField(),nestedOrObjectField);
		for (InnerField innerField : annotation.otherFields()) {
			addNestedFieldMapping(builder, member, innerField);
		}
		builder.endObject();
		builder.endObject();
	}

	protected static boolean isEntity(java.lang.reflect.AccessibleObject member) {
		TypeInformation typeInformation = ClassTypeInformation.from(getMemberType(member));
		Class<?> clazz = getMemberType(member);
		boolean isComplexType = !SIMPLE_TYPE_HOLDER.isSimpleType(clazz);
		return isComplexType && !Map.class.isAssignableFrom(typeInformation.getType());
	}

	protected static Class<?> getFieldType(java.lang.reflect.AccessibleObject member) {
		Class<?> clazz = getMemberType(member);
		TypeInformation typeInformation = ClassTypeInformation.from(clazz);
		if (typeInformation.isCollectionLike()) {
			if (member instanceof java.lang.reflect.Field) {
				clazz = GenericCollectionTypeResolver.getCollectionFieldType((java.lang.reflect.Field) member) != null
								? GenericCollectionTypeResolver.getCollectionFieldType((java.lang.reflect.Field) member)
								: typeInformation.getComponentType().getType();
			}
		}
		return clazz;
	}

	private static boolean isAnyPropertyAnnotatedAsField(List<java.lang.reflect.AccessibleObject> members) {
		if (members != null) {
			for (java.lang.reflect.AccessibleObject member : members) {
				if (member.isAnnotationPresent(Field.class)) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean isIdField(java.lang.reflect.AccessibleObject member, String idFieldName) {
		return idFieldName.equals(getMemberName(member));
	}

	private static boolean isInIgnoreFields(java.lang.reflect.AccessibleObject member) {
		Field fieldAnnotation = member.getAnnotation(Field.class);
		if (null != fieldAnnotation) {
			String[] ignoreFields = fieldAnnotation.ignoreFields();
			return Arrays.asList(ignoreFields).contains(getMemberName(member));
		}
		return false;
	}

	private static boolean isNestedOrObjectField(java.lang.reflect.AccessibleObject member) {
		Field fieldAnnotation = member.getAnnotation(Field.class);
		return fieldAnnotation != null && (FieldType.Nested == fieldAnnotation.type() || FieldType.Object == fieldAnnotation.type());
	}

	private static boolean isGeoPointField(java.lang.reflect.AccessibleObject member) {
		return getMemberType(member) == GeoPoint.class || member.getAnnotation(GeoPointField.class) != null;
	}

	private static boolean isCompletionField(java.lang.reflect.AccessibleObject member) {
		return getMemberType(member) == Completion.class;
	}

	private static String getMemberName(final java.lang.reflect.AccessibleObject member) {
		if (member instanceof java.lang.reflect.Field) {
			return ((java.lang.reflect.Field) member).getName();
		} else {
			final Method setter = (Method) member;
			final String sname = setter.getName();
			if (sname.startsWith("set")) {
				return Introspector.decapitalize(sname.substring(3));
			}
			return null;
		}
	}

	private static Class<?> getMemberType(final java.lang.reflect.AccessibleObject member) {
		if (member instanceof java.lang.reflect.Field) {
			return ((java.lang.reflect.Field) member).getType();
		} else {
			final Method setter = (Method) member;
			if (setter.getParameterTypes() != null && setter.getParameterTypes().length > 0) {
				return setter.getParameterTypes()[0];
			}
		}
		return null;
	}
}
